package com.knowledge.search.service.impl;

import com.knowledge.api.dto.KnowledgeDTO;
import com.knowledge.api.dto.SearchRequestDTO;
import com.knowledge.api.dto.SearchResultDTO;
import com.knowledge.api.service.SearchService;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboService;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.IndexNotFoundException;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;
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

    @Override
    public SearchResultDTO search(SearchRequestDTO request) {
        long startTime = System.currentTimeMillis();
        
        try {
            SearchRequest searchRequest = new SearchRequest(INDEX_NAME);
            SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
            
            BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
            
            // 关键字搜索
            if (request.getKeyword() != null && !request.getKeyword().isEmpty()) {
                if ("PINYIN".equals(request.getSearchType())) {
                    // 拼音搜索
                    boolQuery.should(QueryBuilders.matchQuery("title.pinyin", request.getKeyword()));
                    boolQuery.should(QueryBuilders.matchQuery("content.pinyin", request.getKeyword()));
                } else if ("INITIAL".equals(request.getSearchType())) {
                    // 首字母搜索
                    boolQuery.should(QueryBuilders.matchQuery("title.initial", request.getKeyword()));
                    boolQuery.should(QueryBuilders.matchQuery("content.initial", request.getKeyword()));
                } else {
                    // 全文搜索
                    boolQuery.must(QueryBuilders.multiMatchQuery(request.getKeyword(), "title", "content", "keywords"));
                }
            }
            
            // 分类筛选
            if (request.getCategory() != null) {
                boolQuery.must(QueryBuilders.termQuery("category", request.getCategory()));
            }
            
            // 文件类型筛选（暂时移除，因为索引中没有存储fileType字段）
            // 如果需要此功能，需要在索引时通过fileId查询文件信息获取fileType
            // if (request.getFileType() != null) {
            //     boolQuery.must(QueryBuilders.termQuery("fileType", request.getFileType()));
            // }
            
            sourceBuilder.query(boolQuery);
            
            // 排序
            if ("clickCount".equals(request.getSortField())) {
                sourceBuilder.sort("clickCount", SortOrder.DESC);
            }
            
            // 分页
            int from = (request.getPageNum() - 1) * request.getPageSize();
            sourceBuilder.from(from);
            sourceBuilder.size(request.getPageSize());
            
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
        
        try {
            IndexRequest request = new IndexRequest(INDEX_NAME);
            request.id(String.valueOf(knowledgeDTO.getId()));
            
            // 使用 Map 构建 JSON 内容，Elasticsearch 会自动转换为 JSON
            Map<String, Object> jsonMap = new HashMap<>();
            jsonMap.put("id", knowledgeDTO.getId());
            jsonMap.put("title", knowledgeDTO.getTitle() != null ? knowledgeDTO.getTitle() : "");
            jsonMap.put("content", knowledgeDTO.getContent() != null ? knowledgeDTO.getContent() : "");
            jsonMap.put("keywords", knowledgeDTO.getKeywords() != null ? knowledgeDTO.getKeywords() : "");
            jsonMap.put("category", knowledgeDTO.getCategory() != null ? knowledgeDTO.getCategory() : "");
            jsonMap.put("fileId", knowledgeDTO.getFileId() != null ? knowledgeDTO.getFileId() : null);
            jsonMap.put("clickCount", knowledgeDTO.getClickCount() != null ? knowledgeDTO.getClickCount() : 0L);
            
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
}

