package com.knowledge.search.service.impl;

import com.knowledge.api.dto.KnowledgeDTO;
import com.knowledge.api.dto.SearchRequestDTO;
import com.knowledge.api.dto.SearchResultDTO;
import com.knowledge.api.service.SearchService;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboService;
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
import java.util.ArrayList;
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
            
            // 文件类型筛选
            if (request.getFileType() != null) {
                boolQuery.must(QueryBuilders.termQuery("fileType", request.getFileType()));
            }
            
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
            log.error("搜索失败", e);
            // 提取异常消息，避免序列化不可序列化的对象（如 Elasticsearch Response）
            String errorMessage = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            throw new RuntimeException("搜索失败: " + errorMessage);
        }
    }

    @Override
    public void indexKnowledge(Long knowledgeId) {
        // 从数据库获取知识并索引到ES
        // 这里需要调用knowledge-service获取数据
        log.info("索引知识: {}", knowledgeId);
    }

    @Override
    public void deleteIndex(Long knowledgeId) {
        try {
            // 删除ES索引
            log.info("删除索引: {}", knowledgeId);
        } catch (Exception e) {
            log.error("删除索引失败", e);
        }
    }

    @Override
    public void updateIndex(Long knowledgeId) {
        // 更新ES索引
        deleteIndex(knowledgeId);
        indexKnowledge(knowledgeId);
    }
}

