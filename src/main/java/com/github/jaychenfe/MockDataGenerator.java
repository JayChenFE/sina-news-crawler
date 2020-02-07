package com.github.jaychenfe;

import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.ExecutorType;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;

import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.List;
import java.util.Random;

public class MockDataGenerator {
    private static final int TARGET_ROW_COUNT = 57_0000;

    public static void main(String[] args) {
        SqlSessionFactory sqlSessionFactory;
        try {
            String resource = "db/mybatis/config.xml";
            InputStream inputStream = Resources.getResourceAsStream(resource);
            sqlSessionFactory = new SqlSessionFactoryBuilder().build(inputStream);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        mockData(sqlSessionFactory, TARGET_ROW_COUNT);

    }

    private static void mockData(SqlSessionFactory sqlSessionFactory, int targetRowCount) {
        try (SqlSession session = sqlSessionFactory.openSession(ExecutorType.BATCH)) {
            List<News> currentNews = session.selectList("com.github.jaychenfe.MockMapper.selectNews");

            int count = targetRowCount - currentNews.size();
            Random random = new Random();
            try {
                while (count-- > 0) {
                    int index = random.nextInt(currentNews.size());
                    News newsToBeInsert = currentNews.get(index);

                    Instant createdAt = newsToBeInsert.getCreatedAt();
                    createdAt = createdAt.minusSeconds(random.nextInt(3600 * 24 * 365));
                    newsToBeInsert.setCreatedAt(createdAt);
                    newsToBeInsert.setModifiedAt(createdAt);

                    session.insert("com.github.jaychenfe.MockMapper.insertNews", newsToBeInsert);
                    System.out.println("left" + count);

                    if (count % 2000 == 0) {
                        session.flushStatements();
                    }
                }
                session.commit();
            } catch (Exception e) {
                session.rollback();
                throw new RuntimeException(e);
            }
        }
    }
}
