package com.knowledge.search.service.impl;

import com.knowledge.api.dto.HighlightDTO;
import com.knowledge.api.dto.KnowledgeDTO;
import com.knowledge.api.dto.SearchRequestDTO;
import com.knowledge.api.dto.SearchResultDTO;
import com.knowledge.api.dto.SearchSuggestionDTO;
import com.knowledge.api.service.SearchService;
import com.knowledge.api.service.FileService;
import com.knowledge.search.util.PinyinUtil;
import com.knowledge.search.util.SearchTypeDetector;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.apache.dubbo.config.annotation.DubboService;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.indices.GetIndexRequest;
import org.elasticsearch.index.IndexNotFoundException;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@DubboService
public class SearchServiceImpl implements SearchService {

    private static final String INDEX_NAME = "knowledge_index";

    @Resource
    private RestHighLevelClient elasticsearchClient;

    @DubboReference(check = false, timeout = 5000)
    private FileService fileService;

    @Override
    public SearchResultDTO search(SearchRequestDTO request) {
        long startTime = System.currentTimeMillis();
        
        try {
            SearchRequest searchRequest = new SearchRequest(INDEX_NAME);
            SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
            
            BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
            
            // 关键字搜索 - 自动识别搜索类型并同时搜索文件名
            if (request.getKeyword() != null && !request.getKeyword().isEmpty()) {
                String keyword = request.getKeyword();
                String searchType = request.getSearchType();
                
                // 如果没有指定搜索类型，自动检测
                if (searchType == null || searchType.isEmpty() || "AUTO".equals(searchType)) {
                    searchType = SearchTypeDetector.detectSearchType(keyword);
                }
                
                if ("PINYIN".equals(searchType)) {
                    // 拼音搜索 - 搜索标题、内容、关键词和文件名的拼音
                    boolQuery.should(QueryBuilders.matchQuery("titlePinyin", keyword).boost(3.0f));
                    boolQuery.should(QueryBuilders.matchQuery("contentPinyin", keyword).boost(1.0f));
                    boolQuery.should(QueryBuilders.matchQuery("keywords", keyword).boost(2.0f));
                    boolQuery.should(QueryBuilders.matchQuery("fileNamePinyin", keyword).boost(2.5f));
                    boolQuery.minimumShouldMatch(1);
                } else if ("INITIAL".equals(searchType)) {
                    // 首字母搜索 - 搜索标题、内容、关键词和文件名的首字母
                    boolQuery.should(QueryBuilders.matchQuery("titleInitial", keyword).boost(3.0f));
                    boolQuery.should(QueryBuilders.matchQuery("contentInitial", keyword).boost(1.0f));
                    boolQuery.should(QueryBuilders.matchQuery("fileNameInitial", keyword).boost(2.5f));
                    boolQuery.minimumShouldMatch(1);
                } else {
                    // 全文搜索 - 同时搜索标题、内容、关键词和文件名
                    // 使用 should 查询并设置权重，而不是 must + multiMatchQuery
                    boolQuery.should(QueryBuilders.matchQuery("title", keyword).boost(3.0f));
                    boolQuery.should(QueryBuilders.matchQuery("content", keyword).boost(1.0f));
                    boolQuery.should(QueryBuilders.matchQuery("contentText", keyword).boost(0.8f)); // 全文内容搜索
                    boolQuery.should(QueryBuilders.matchQuery("keywords", keyword).boost(2.0f));
                    boolQuery.should(QueryBuilders.matchQuery("fileName", keyword).boost(2.5f));
                    boolQuery.minimumShouldMatch(1);
                }
            }
            
            // 分类筛选
            if (request.getCategory() != null && !request.getCategory().isEmpty()) {
                boolQuery.must(QueryBuilders.termQuery("category", request.getCategory()));
            }
            
            // 状态筛选
            if (request.getStatus() != null && !request.getStatus().isEmpty()) {
                boolQuery.must(QueryBuilders.termQuery("status", request.getStatus()));
            }
            
            // 作者筛选
            if (request.getAuthor() != null && !request.getAuthor().isEmpty()) {
                boolQuery.must(QueryBuilders.termQuery("author", request.getAuthor()));
            }
            
            // 日期范围筛选
            if (request.getStartDate() != null && !request.getStartDate().isEmpty()) {
                boolQuery.must(QueryBuilders.rangeQuery("createTime").gte(request.getStartDate()));
            }
            if (request.getEndDate() != null && !request.getEndDate().isEmpty()) {
                boolQuery.must(QueryBuilders.rangeQuery("createTime").lte(request.getEndDate()));
            }
            
            // 文件类型筛选（暂时移除，因为索引中没有存储fileType字段）
            // 如果需要此功能，需要在索引时通过fileId查询文件信息获取fileType
            // if (request.getFileType() != null) {
            //     boolQuery.must(QueryBuilders.termQuery("fileType", request.getFileType()));
            // }
            
            // 只搜索文件（有fileId的），过滤掉文件夹
            boolQuery.must(QueryBuilders.existsQuery("fileId"));
            
            // 如果查询为空，使用 match_all 查询（但仍需要过滤文件夹）
            if (!boolQuery.hasClauses()) {
                sourceBuilder.query(QueryBuilders.boolQuery().must(QueryBuilders.existsQuery("fileId")));
            } else {
                sourceBuilder.query(boolQuery);
            }
            
            // 排序
            if ("clickCount".equals(request.getSortField())) {
                sourceBuilder.sort("clickCount", SortOrder.DESC);
            }
            
            // 分页
            int from = (request.getPageNum() - 1) * request.getPageSize();
            sourceBuilder.from(from);
            sourceBuilder.size(request.getPageSize());
            
            // 配置高亮（仅在有关键字搜索时）
            if (request.getKeyword() != null && !request.getKeyword().isEmpty()) {
                HighlightBuilder highlightBuilder = new HighlightBuilder();
                // 配置高亮字段
                highlightBuilder.field(new HighlightBuilder.Field("title").fragmentSize(150).numOfFragments(1));
                highlightBuilder.field(new HighlightBuilder.Field("content").fragmentSize(200).numOfFragments(3));
                highlightBuilder.field(new HighlightBuilder.Field("contentText").fragmentSize(200).numOfFragments(3));
                highlightBuilder.field(new HighlightBuilder.Field("keywords").fragmentSize(100).numOfFragments(1));
                highlightBuilder.field(new HighlightBuilder.Field("fileName").fragmentSize(100).numOfFragments(1));
                highlightBuilder.preTags("<mark>");  // 高亮开始标签
                highlightBuilder.postTags("</mark>"); // 高亮结束标签
                sourceBuilder.highlighter(highlightBuilder);
            }
            
            searchRequest.source(sourceBuilder);
            
            SearchResponse response = elasticsearchClient.search(searchRequest, RequestOptions.DEFAULT);
            
            List<KnowledgeDTO> results = new ArrayList<>();
            for (SearchHit hit : response.getHits().getHits()) {
                Map<String, Object> source = hit.getSourceAsMap();
                KnowledgeDTO dto = new KnowledgeDTO();
                dto.setId(Long.valueOf(source.get("id").toString()));
                dto.setTitle((String) source.get("title"));
                dto.setContent((String) source.get("content"));
                dto.setCategory((String) source.get("category"));
                dto.setKeywords((String) source.get("keywords"));
                dto.setClickCount(Long.valueOf(source.get("clickCount").toString()));
                // 设置fileId（搜索结果中只包含文件，所以fileId一定存在）
                if (source.get("fileId") != null) {
                    dto.setFileId(Long.valueOf(source.get("fileId").toString()));
                }
                
                // 提取高亮信息
                if (hit.getHighlightFields() != null && !hit.getHighlightFields().isEmpty()) {
                    HighlightDTO highlight = new HighlightDTO();
                    Map<String, HighlightField> highlightFields = hit.getHighlightFields();
                    
                    if (highlightFields.containsKey("title")) {
                        List<String> titleFragments = new ArrayList<>();
                        for (org.elasticsearch.common.text.Text fragment : highlightFields.get("title").getFragments()) {
                            titleFragments.add(fragment.string());
                        }
                        highlight.setTitle(titleFragments);
                    }
                    if (highlightFields.containsKey("content")) {
                        List<String> contentFragments = new ArrayList<>();
                        for (org.elasticsearch.common.text.Text fragment : highlightFields.get("content").getFragments()) {
                            contentFragments.add(fragment.string());
                        }
                        highlight.setContent(contentFragments);
                    }
                    // 如果content没有高亮，尝试使用contentText的高亮
                    if ((highlight.getContent() == null || highlight.getContent().isEmpty()) && highlightFields.containsKey("contentText")) {
                        List<String> contentTextFragments = new ArrayList<>();
                        for (org.elasticsearch.common.text.Text fragment : highlightFields.get("contentText").getFragments()) {
                            contentTextFragments.add(fragment.string());
                        }
                        highlight.setContent(contentTextFragments);
                    }
                    if (highlightFields.containsKey("keywords")) {
                        List<String> keywordsFragments = new ArrayList<>();
                        for (org.elasticsearch.common.text.Text fragment : highlightFields.get("keywords").getFragments()) {
                            keywordsFragments.add(fragment.string());
                        }
                        highlight.setKeywords(keywordsFragments);
                    }
                    if (highlightFields.containsKey("fileName")) {
                        List<String> fileNameFragments = new ArrayList<>();
                        for (org.elasticsearch.common.text.Text fragment : highlightFields.get("fileName").getFragments()) {
                            fileNameFragments.add(fragment.string());
                        }
                        highlight.setFileName(fileNameFragments);
                    }
                    
                    dto.setHighlight(highlight);
                }
                
                results.add(dto);
            }
            
            SearchResultDTO result = new SearchResultDTO();
            result.setResults(results);
            result.setTotal(response.getHits().getTotalHits().value);
            result.setPageNum(request.getPageNum());
            result.setPageSize(request.getPageSize());
            result.setTookTime(System.currentTimeMillis() - startTime);
            
            return result;
            
        } catch (org.elasticsearch.index.IndexNotFoundException e) {
            log.warn("索引不存在: {}", INDEX_NAME);
            // 索引不存在时返回空结果
            SearchResultDTO result = new SearchResultDTO();
            result.setResults(new ArrayList<>());
            result.setTotal(0L);
            result.setPageNum(request.getPageNum());
            result.setPageSize(request.getPageSize());
            result.setTookTime(System.currentTimeMillis() - startTime);
            return result;
        } catch (Exception e) {
            // 检查是否是索引不存在的异常（可能被包装在其他异常中）
            String errorMessage = e.getMessage() != null ? e.getMessage() : "";
            if (errorMessage.contains("index_not_found_exception") || 
                errorMessage.contains("no such index") ||
                e.getCause() instanceof org.elasticsearch.index.IndexNotFoundException) {
                log.warn("索引不存在: {}", INDEX_NAME);
                // 索引不存在时返回空结果
                SearchResultDTO result = new SearchResultDTO();
                result.setResults(new ArrayList<>());
                result.setTotal(0L);
                result.setPageNum(request.getPageNum());
                result.setPageSize(request.getPageSize());
                result.setTookTime(System.currentTimeMillis() - startTime);
                return result;
            }
            log.error("搜索失败", e);
            // 提取异常消息，避免序列化不可序列化的对象（如 Elasticsearch Response）
            throw new RuntimeException("搜索失败: " + errorMessage);
        }
    }

    @Override
    public void indexKnowledge(Long knowledgeId) {
        // 这个方法需要接收 KnowledgeDTO，由调用方传入
        // 这里只记录日志，实际实现在重载方法中
        log.warn("indexKnowledge(Long) 方法已废弃，请使用 indexKnowledge(KnowledgeDTO)");
    }

    /**
     * 索引知识到ElasticSearch
     * @param knowledgeDTO 知识DTO
     */
    @Override
    public void indexKnowledge(KnowledgeDTO knowledgeDTO) {
        if (knowledgeDTO == null || knowledgeDTO.getId() == null) {
            log.warn("知识数据为空，跳过索引");
            return;
        }
        
        // 只索引文件（有fileId的），不索引文件夹
        if (knowledgeDTO.getFileId() == null) {
            log.debug("跳过文件夹索引: id={}, title={}", knowledgeDTO.getId(), knowledgeDTO.getTitle());
            return;
        }
        
        try {
            IndexRequest request = new IndexRequest(INDEX_NAME);
            request.id(String.valueOf(knowledgeDTO.getId()));
            
            // 使用 Map 构建 JSON 内容，Elasticsearch 会自动转换为 JSON
            Map<String, Object> jsonMap = new HashMap<>();
            jsonMap.put("id", knowledgeDTO.getId());
            
            String title = knowledgeDTO.getTitle() != null ? knowledgeDTO.getTitle() : "";
            String content = knowledgeDTO.getContent() != null ? knowledgeDTO.getContent() : "";
            String contentText = knowledgeDTO.getContentText() != null ? knowledgeDTO.getContentText() : "";
            
            // 主字段存储原始值
            jsonMap.put("title", title);
            jsonMap.put("content", content);
            jsonMap.put("contentText", contentText);
            jsonMap.put("keywords", knowledgeDTO.getKeywords() != null ? knowledgeDTO.getKeywords() : "");
            jsonMap.put("category", knowledgeDTO.getCategory() != null ? knowledgeDTO.getCategory() : "");
            jsonMap.put("fileId", knowledgeDTO.getFileId() != null ? knowledgeDTO.getFileId() : null);
            jsonMap.put("clickCount", knowledgeDTO.getClickCount() != null ? knowledgeDTO.getClickCount() : 0L);
            
            // 生成拼音和首字母字段并添加到索引
            String titlePinyin = PinyinUtil.getPinyin(title);
            String titleInitial = PinyinUtil.getInitial(title);
            String contentPinyin = PinyinUtil.getPinyin(content);
            String contentInitial = PinyinUtil.getInitial(content);
            
            jsonMap.put("titlePinyin", titlePinyin);
            jsonMap.put("titleInitial", titleInitial);
            jsonMap.put("contentPinyin", contentPinyin);
            jsonMap.put("contentInitial", contentInitial);
            
            // 获取文件名并索引
            String fileName = "";
            String fileNamePinyin = "";
            String fileNameInitial = "";
            if (knowledgeDTO.getFileId() != null) {
                try {
                    com.knowledge.api.dto.FileDTO fileDTO = fileService.getFileById(knowledgeDTO.getFileId());
                    if (fileDTO != null && fileDTO.getFileName() != null) {
                        fileName = fileDTO.getFileName();
                        fileNamePinyin = PinyinUtil.getPinyin(fileName);
                        fileNameInitial = PinyinUtil.getInitial(fileName);
                    }
                } catch (Exception e) {
                    log.warn("获取文件信息失败: fileId={}, error={}", knowledgeDTO.getFileId(), e.getMessage());
                }
            }
            jsonMap.put("fileName", fileName);
            jsonMap.put("fileNamePinyin", fileNamePinyin);
            jsonMap.put("fileNameInitial", fileNameInitial);
            
            // 格式化时间
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            if (knowledgeDTO.getCreateTime() != null) {
                jsonMap.put("createTime", knowledgeDTO.getCreateTime().format(formatter));
            }
            if (knowledgeDTO.getUpdateTime() != null) {
                jsonMap.put("updateTime", knowledgeDTO.getUpdateTime().format(formatter));
            }
            
            // 直接使用 Map，Elasticsearch 会自动转换为 JSON
            request.source(jsonMap);
            elasticsearchClient.index(request, RequestOptions.DEFAULT);
            
            log.info("成功索引知识到ES: id={}, title={}", knowledgeDTO.getId(), knowledgeDTO.getTitle());
        } catch (Exception e) {
            log.error("索引知识到ES失败: id={}", knowledgeDTO.getId(), e);
        }
    }

    @Override
    public void deleteIndex(Long knowledgeId) {
        try {
            DeleteRequest request = new DeleteRequest(INDEX_NAME, String.valueOf(knowledgeId));
            elasticsearchClient.delete(request, RequestOptions.DEFAULT);
            log.info("成功从ES删除索引: id={}", knowledgeId);
        } catch (Exception e) {
            log.error("从ES删除索引失败: id={}", knowledgeId, e);
        }
    }

    @Override
    public void updateIndex(Long knowledgeId) {
        // 更新ES索引
        deleteIndex(knowledgeId);
        // 注意：这里无法获取知识数据，需要调用方传入 KnowledgeDTO
        log.warn("updateIndex(Long) 方法需要传入 KnowledgeDTO，请使用 updateIndex(KnowledgeDTO)");
    }

    /**
     * 更新知识索引
     * @param knowledgeDTO 知识DTO
     */
    @Override
    public void updateIndex(KnowledgeDTO knowledgeDTO) {
        indexKnowledge(knowledgeDTO);
    }

    /**
     * 删除整个索引（用于重建索引）
     */
    public void deleteIndex() {
        try {
            GetIndexRequest getIndexRequest = new GetIndexRequest(INDEX_NAME);
            boolean exists = elasticsearchClient.indices().exists(getIndexRequest, RequestOptions.DEFAULT);
            if (exists) {
                DeleteIndexRequest deleteRequest = new DeleteIndexRequest(INDEX_NAME);
                elasticsearchClient.indices().delete(deleteRequest, RequestOptions.DEFAULT);
                log.info("成功删除索引: {}", INDEX_NAME);
            } else {
                log.info("索引 {} 不存在，无需删除", INDEX_NAME);
            }
        } catch (Exception e) {
            log.error("删除索引失败: {}", INDEX_NAME, e);
            throw new RuntimeException("删除索引失败: " + e.getMessage(), e);
        }
    }

    @Override
    public SearchSuggestionDTO getSuggestions(String keyword, int limit) {
        if (keyword == null || keyword.trim().isEmpty()) {
            SearchSuggestionDTO result = new SearchSuggestionDTO();
            result.setSuggestions(new ArrayList<>());
            result.setPreviewResults(new ArrayList<>());
            return result;
        }

        try {
            SearchRequest searchRequest = new SearchRequest(INDEX_NAME);
            SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
            
            // 使用 prefix query 或 match query 来获取建议
            // 搜索标题、内容、关键词和文件名，支持前缀匹配
            String searchType = SearchTypeDetector.detectSearchType(keyword);
            BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
            
            if ("PINYIN".equals(searchType)) {
                boolQuery.should(QueryBuilders.prefixQuery("titlePinyin", keyword.toLowerCase()).boost(3.0f));
                boolQuery.should(QueryBuilders.prefixQuery("fileNamePinyin", keyword.toLowerCase()).boost(2.5f));
                boolQuery.should(QueryBuilders.matchQuery("titlePinyin", keyword).boost(2.0f));
                boolQuery.should(QueryBuilders.matchQuery("keywords", keyword).boost(1.5f));
            } else if ("INITIAL".equals(searchType)) {
                boolQuery.should(QueryBuilders.prefixQuery("titleInitial", keyword.toLowerCase()).boost(3.0f));
                boolQuery.should(QueryBuilders.prefixQuery("fileNameInitial", keyword.toLowerCase()).boost(2.5f));
                boolQuery.should(QueryBuilders.matchQuery("titleInitial", keyword).boost(2.0f));
            } else {
                boolQuery.should(QueryBuilders.prefixQuery("title", keyword.toLowerCase()).boost(3.0f));
                boolQuery.should(QueryBuilders.prefixQuery("fileName", keyword.toLowerCase()).boost(2.5f));
                boolQuery.should(QueryBuilders.matchQuery("title", keyword).boost(2.0f));
                boolQuery.should(QueryBuilders.matchQuery("keywords", keyword).boost(1.5f));
            }
            boolQuery.minimumShouldMatch(1);
            
            sourceBuilder.query(boolQuery);
            sourceBuilder.size(limit); // 限制返回数量
            sourceBuilder.sort("clickCount", SortOrder.DESC); // 按点击量排序
            
            searchRequest.source(sourceBuilder);
            
            SearchResponse response = elasticsearchClient.search(searchRequest, RequestOptions.DEFAULT);
            
            List<String> suggestions = new ArrayList<>();
            List<KnowledgeDTO> previewResults = new ArrayList<>();
            
            // 提取建议和预览结果
            for (SearchHit hit : response.getHits().getHits()) {
                Map<String, Object> source = hit.getSourceAsMap();
                String title = (String) source.get("title");
                String fileName = (String) source.get("fileName");
                
                // 添加标题到建议列表
                if (title != null && !title.isEmpty()) {
                    if (!suggestions.contains(title)) {
                        suggestions.add(title);
                    }
                }
                
                // 添加文件名到建议列表（如果文件名与标题不同）
                if (fileName != null && !fileName.isEmpty() && !fileName.equals(title)) {
                    // 去掉文件扩展名，只保留文件名
                    String fileNameWithoutExt = fileName;
                    int lastDot = fileName.lastIndexOf('.');
                    if (lastDot > 0) {
                        fileNameWithoutExt = fileName.substring(0, lastDot);
                    }
                    if (!suggestions.contains(fileNameWithoutExt) && !suggestions.contains(fileName)) {
                        suggestions.add(fileNameWithoutExt);
                    }
                }
                
                // 添加到预览结果
                KnowledgeDTO dto = new KnowledgeDTO();
                dto.setId(Long.valueOf(source.get("id").toString()));
                dto.setTitle(title);
                dto.setContent((String) source.get("content"));
                dto.setCategory((String) source.get("category"));
                dto.setKeywords((String) source.get("keywords"));
                dto.setClickCount(Long.valueOf(source.get("clickCount").toString()));
                previewResults.add(dto);
            }
            
            // 如果结果不够，尝试从关键词中提取建议
            if (suggestions.size() < limit) {
                SearchRequest keywordRequest = new SearchRequest(INDEX_NAME);
                SearchSourceBuilder keywordBuilder = new SearchSourceBuilder();
                keywordBuilder.query(QueryBuilders.wildcardQuery("keywords", "*" + keyword + "*"));
                keywordBuilder.size(limit - suggestions.size());
                keywordRequest.source(keywordBuilder);
                
                try {
                    SearchResponse keywordResponse = elasticsearchClient.search(keywordRequest, RequestOptions.DEFAULT);
                    for (SearchHit hit : keywordResponse.getHits().getHits()) {
                        Map<String, Object> source = hit.getSourceAsMap();
                        String keywords = (String) source.get("keywords");
                        if (keywords != null && !keywords.isEmpty()) {
                            // 从关键词中提取包含搜索词的片段
                            String[] keywordArray = keywords.split("[，,、\\s]+");
                            for (String kw : keywordArray) {
                                if (kw.contains(keyword) && !suggestions.contains(kw) && suggestions.size() < limit) {
                                    suggestions.add(kw);
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    log.debug("从关键词获取建议失败", e);
                }
            }
            
            SearchSuggestionDTO result = new SearchSuggestionDTO();
            result.setSuggestions(suggestions);
            result.setPreviewResults(previewResults);
            return result;
            
        } catch (Exception e) {
            log.error("获取搜索建议失败", e);
            SearchSuggestionDTO result = new SearchSuggestionDTO();
            result.setSuggestions(new ArrayList<>());
            result.setPreviewResults(new ArrayList<>());
            return result;
        }
    }
}

