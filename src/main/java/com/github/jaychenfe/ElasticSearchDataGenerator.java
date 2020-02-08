package com.github.jaychenfe;

import org.apache.http.HttpHost;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentType;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ElasticSearchDataGenerator {
    public static void main(String[] args) {
        SqlSessionFactory sqlSessionFactory;
        try {
            String resource = "db/mybatis/config.xml";
            InputStream inputStream = Resources.getResourceAsStream(resource);
            sqlSessionFactory = new SqlSessionFactoryBuilder().build(inputStream);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        List<News> newsFromDB = getNewsFromDB(sqlSessionFactory);

        for (int i = 0; i < 10; i++) {
            new Thread(() -> writeDataToESInSingleThread(newsFromDB)).start();
        }
    }

    private static void writeDataToESInSingleThread(List<News> newsFromDB) {
        try {
            try (RestHighLevelClient client = new RestHighLevelClient(RestClient.builder(
                    new HttpHost("localhost", 9200, "http")))) {
                // 单线程写入2000*1000=200_0000数据
                for (int i = 0; i < 1000; i++) {
                    BulkRequest bulkRequest = new BulkRequest();
                    for (News news : newsFromDB) {
                        String subContent = getSubContent(news);
                        Map<String, Object> data = new HashMap<>();
                        data.put("title", news.getTitle());
                        data.put("content", subContent);
                        data.put("url", news.getUrl());
                        data.put("createdAt", news.getCreatedAt());
                        data.put("modifiedAt", news.getModifiedAt());
                        IndexRequest request = new IndexRequest("news");
                        request.source(data, XContentType.JSON);
                        bulkRequest.add(request);
                    }
                    BulkResponse bulkResponse = client.bulk(bulkRequest, RequestOptions.DEFAULT);
                    System.out.println("current Thread: " + Thread.currentThread().getName() + " finishes" + i + ": " + bulkResponse.status().getStatus());
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static String getSubContent(News news) {
        String content = news.getContent();
        return content.length() > 10 ? content.substring(0, 10) : content;
    }

    private static List<News> getNewsFromDB(SqlSessionFactory sqlSessionFactory) {
        try (SqlSession session = sqlSessionFactory.openSession()) {
            return session.selectList("com.github.jaychenfe.MockMapper.selectNews");
        }
    }
}
