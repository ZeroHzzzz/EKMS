package com.knowledge.search.config;

import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.action.admin.indices.create.CreateIndexRequest;
import org.elasticsearch.action.admin.indices.create.CreateIndexResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.indices.GetIndexRequest;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * ElasticSearch索引初始化器
 * 在应用启动时自动创建索引（如果不存在）
 */
@Slf4j
@Component
public class ElasticSearchIndexInitializer implements ApplicationListener<ApplicationReadyEvent> {

    private static final String INDEX_NAME = "knowledge_index";

    @Resource
    private RestHighLevelClient elasticsearchClient;

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        try {
            initializeIndex();
        } catch (Exception e) {
            log.error("初始化ElasticSearch索引失败", e);
        }
    }

    /**
     * 初始化索引（公共方法，可被外部调用）
     */
    public void initializeIndex() throws Exception {
        // 检查索引是否存在（使用 GetIndexRequest）
        GetIndexRequest getIndexRequest = new GetIndexRequest(INDEX_NAME);
        boolean exists = elasticsearchClient.indices().exists(getIndexRequest, RequestOptions.DEFAULT);

        if (exists) {
            log.info("索引 {} 已存在，跳过创建", INDEX_NAME);
            return;
        }

        log.info("开始创建索引: {}", INDEX_NAME);

        // 创建索引请求
        CreateIndexRequest request = new CreateIndexRequest(INDEX_NAME);

        // 设置索引映射（使用JSON字符串方式，避免包路径问题）
        // 注意：如果使用了IK分词器或拼音分析器，需要先在Elasticsearch中安装相应插件
        String mapping = "{\n" +
                "  \"properties\": {\n" +
                "    \"id\": {\n" +
                "      \"type\": \"long\"\n" +
                "    },\n" +
                "    \"title\": {\n" +
                "      \"type\": \"text\",\n" +
                "      \"analyzer\": \"standard\",\n" +
                "      \"search_analyzer\": \"standard\",\n" +
                "      \"fields\": {\n" +
                "        \"keyword\": {\n" +
                "          \"type\": \"keyword\"\n" +
                "        }\n" +
                "      }\n" +
                "    },\n" +
                "    \"titlePinyin\": {\n" +
                "      \"type\": \"text\",\n" +
                "      \"analyzer\": \"standard\"\n" +
                "    },\n" +
                "    \"titleInitial\": {\n" +
                "      \"type\": \"text\",\n" +
                "      \"analyzer\": \"standard\"\n" +
                "    },\n" +
                "    \"content\": {\n" +
                "      \"type\": \"text\",\n" +
                "      \"analyzer\": \"standard\",\n" +
                "      \"search_analyzer\": \"standard\"\n" +
                "    },\n" +
                "    \"contentPinyin\": {\n" +
                "      \"type\": \"text\",\n" +
                "      \"analyzer\": \"standard\"\n" +
                "    },\n" +
                "    \"contentInitial\": {\n" +
                "      \"type\": \"text\",\n" +
                "      \"analyzer\": \"standard\"\n" +
                "    },\n" +
                "    \"keywords\": {\n" +
                "      \"type\": \"text\",\n" +
                "      \"analyzer\": \"standard\"\n" +
                "    },\n" +
                "    \"category\": {\n" +
                "      \"type\": \"keyword\"\n" +
                "    },\n" +
                "    \"fileId\": {\n" +
                "      \"type\": \"long\"\n" +
                "    },\n" +
                "    \"fileName\": {\n" +
                "      \"type\": \"text\",\n" +
                "      \"analyzer\": \"standard\"\n" +
                "    },\n" +
                "    \"fileNamePinyin\": {\n" +
                "      \"type\": \"text\",\n" +
                "      \"analyzer\": \"standard\"\n" +
                "    },\n" +
                "    \"fileNameInitial\": {\n" +
                "      \"type\": \"text\",\n" +
                "      \"analyzer\": \"standard\"\n" +
                "    },\n" +
                "    \"clickCount\": {\n" +
                "      \"type\": \"long\"\n" +
                "    },\n" +
                "    \"createTime\": {\n" +
                "      \"type\": \"date\",\n" +
                "      \"format\": \"yyyy-MM-dd HH:mm:ss||yyyy-MM-dd||epoch_millis\"\n" +
                "    },\n" +
                "    \"updateTime\": {\n" +
                "      \"type\": \"date\",\n" +
                "      \"format\": \"yyyy-MM-dd HH:mm:ss||yyyy-MM-dd||epoch_millis\"\n" +
                "    }\n" +
                "  }\n" +
                "}";

        request.mapping(mapping);

        // 执行创建索引
        CreateIndexResponse response = elasticsearchClient.indices().create(request, RequestOptions.DEFAULT);

        if (response.isAcknowledged()) {
            log.info("索引 {} 创建成功", INDEX_NAME);
        } else {
            log.warn("索引 {} 创建请求已提交，但未确认", INDEX_NAME);
        }
    }
}

