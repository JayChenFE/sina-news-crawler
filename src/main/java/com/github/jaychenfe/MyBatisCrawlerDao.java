package com.github.jaychenfe;

import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class MyBatisCrawlerDao implements CrawlerDao {
    private SqlSessionFactory sqlSessionFactory;

    public MyBatisCrawlerDao() {
        String resource = "db/mybatis/config.xml";
        InputStream inputStream;
        try {
            inputStream = Resources.getResourceAsStream(resource);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        this.sqlSessionFactory = new SqlSessionFactoryBuilder().build(inputStream);
    }


    @Override
    public synchronized String getNextLinkThenDeleteLink() {
        try (SqlSession session = sqlSessionFactory.openSession(true)) {
            String link = session.selectOne("com.github.jaychenfe.MyMapper.selectNextLink");
            if (link != null) {
                session.delete("com.github.jaychenfe.MyMapper.deleteLink", link);
            }
            return link;
        }
    }

    @Override
    public void saveNews(String link, String title, String content) {
        try (SqlSession session = sqlSessionFactory.openSession(true)) {
            session.insert("com.github.jaychenfe.MyMapper.insertNews", new News(link, content, title));
        }
    }

    @Override
    public boolean isLinkProcessed(String link) {
        try (SqlSession session = sqlSessionFactory.openSession()) {
            int count = session.selectOne("com.github.jaychenfe.MyMapper.countLink", link);
            return count > 0;
        }
    }

    @Override
    public void insertProcessedLink(String link) {
        insertLink(link, "LINKS_ALREADY_PROCESSED");
    }

    @Override
    public void insertLinkToBeProcess(String link) {
        insertLink(link, "LINKS_TO_BE_PROCESSED");
    }

    private void insertLink(String link, String tableName) {
        Map<String, Object> params = new HashMap<>();
        params.put("tableName", tableName);
        params.put("link", link);

        try (SqlSession session = sqlSessionFactory.openSession(true)) {
            session.insert("com.github.jaychenfe.MyMapper.insertLink", params);
        }
    }
}
