package com.github.jaychenfe;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class JdbcCrawlerDao implements CrawlerDao {

    static final String DB_URL = "jdbc:h2:file:D:\\git_home\\jrg\\java_zb\\sina-news-crawler\\news";
    static final String DB_USERNAME = "root";
    static final String DB_PASSWORD = "root";

    private final Connection connection;

    @SuppressFBWarnings("DMI_CONSTANT_DB_PASSWORD")
    public JdbcCrawlerDao() {
        try {
            this.connection = DriverManager.getConnection(DB_URL, DB_USERNAME, DB_PASSWORD);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String getNextLinkThenDeleteLink() throws SQLException {
        String link = getNextLink();
        if (link != null) {
            updateDatabase(link, "delete from LINKS_TO_BE_PROCESSED where link = ?");
        }
        return link;
    }

    @Override
    public void saveNews(String link, String title, String content) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("insert into NEWS (title, content, url, created_at, modified_at) values (?,?,?,now(),now())")) {
            statement.setString(1, title);
            statement.setString(2, content);
            statement.setString(3, link);
            statement.executeUpdate();
        }
    }

    @Override
    public boolean isLinkProcessed(String link) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("select  LINK from LINKS_ALREADY_PROCESSED where link= ?")) {
            statement.setString(1, link);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public void insertProcessedLink(String link) throws SQLException {
        updateDatabase(link, "insert into LINKS_ALREADY_PROCESSED (LINK) values (?)");

    }

    @Override
    public void insertLinkToBeProcess(String link) throws SQLException {
        updateDatabase(link, "insert into LINKS_TO_BE_PROCESSED (LINK) values (?)");
    }

    private void updateDatabase(String link, String sql) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, link);
            statement.executeUpdate();
        }
    }

    private String getNextLink() throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("select LINK from LINKS_TO_BE_PROCESSED limit 1")) {
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getString(1);
                }
            }
        }
        return null;
    }
}
