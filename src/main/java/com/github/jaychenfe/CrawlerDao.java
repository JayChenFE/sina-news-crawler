package com.github.jaychenfe;

import java.sql.SQLException;

public interface CrawlerDao {
    String getNextLinkThenDeleteLink() throws SQLException;

    void updateDatabase(String link, String sql) throws SQLException;

    void saveNews(String link, String title, String content) throws SQLException;

    boolean isLinkProcessed(String link) throws SQLException;
}
