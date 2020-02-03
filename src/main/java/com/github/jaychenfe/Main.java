package com.github.jaychenfe;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class Main {
    static final String DB_URL = "jdbc:h2:file:D:\\git_home\\jrg\\java_zb\\sina-news-crawler\\news";
    static final String DB_USERNAME = "root";
    static final String DB_PASSWORD = "root";

    @SuppressFBWarnings("DMI_CONSTANT_DB_PASSWORD")
    public static void main(String[] args) throws IOException, SQLException {

        Connection connection =
                DriverManager.getConnection(DB_URL, DB_USERNAME, DB_PASSWORD);

        while (true) {
            // 待处理的链接池
            List<String> linkPool = loadUrlsFromDatabase(connection, "select LINK from LINKS_TO_BE_PROCESSED");

            if (linkPool.isEmpty()) {
                break;
            }
            String link = linkPool.remove(linkPool.size() - 1);

            link = fixLink(link);

            insertLinkIntoDatabase(connection, link, "delete from LINKS_TO_BE_PROCESSED where link = ?");

            System.out.println(link);

            if (!isToProcessLink(connection, link)) {
                continue;
            }

            Document doc = httpGetAndParseHtml(link);
            parseUrlsFromPageAndStoreIntoDatabase(linkPool, doc, connection);
            storeToDatabaseIfItIsNewsPage(doc);
            insertLinkIntoDatabase(connection, link, "insert into LINKS_ALREADY_PROCESSED (LINK) values (?)");
        }
    }

    private static void insertLinkIntoDatabase(Connection connection, String link, String sql) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, link);
            statement.executeUpdate();
        }
    }

    private static boolean isToProcessLink(Connection connection, String link) throws SQLException {
        return !isLinkProcessed(connection, link) && isInterestedLink(link);
    }

    private static String fixLink(String link) {
        if (link.startsWith("//")) {
            link = "https:" + link;
        }

        if (link.contains("\\")) {
            link = link.replaceAll("\\\\", "");
        }
        return link;
    }

    private static void parseUrlsFromPageAndStoreIntoDatabase(List<String> linkPool, Document doc, Connection connection) {
        doc.select("a").stream()
                .map(aTag -> aTag.attr("href"))
                .map(Main::fixLink)
                .filter(Main::isInterestedLink)
                .forEach(insertLinkComsumer(linkPool, connection));
    }

    private static Consumer<String> insertLinkComsumer(List<String> linkPool, Connection connection) {
        return link -> {
            linkPool.add(link);
            try {
                insertLinkIntoDatabase(connection, link, "insert into LINKS_TO_BE_PROCESSED (LINK) values (?)");
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        };
    }

    private static boolean isLinkProcessed(Connection connection, String link) throws SQLException {
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

    private static List<String> loadUrlsFromDatabase(Connection connection, String sql) throws SQLException {
        List<String> result = new ArrayList<>();

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    result.add(resultSet.getString(1));
                }
            }
        }
        return result;
    }

    private static void storeToDatabaseIfItIsNewsPage(Document doc) {
        Elements articleTags = doc.select("article");
        if (!articleTags.isEmpty()) {
            String title = articleTags.get(0).child(0).text();
            System.out.println(title);
        }
    }

    private static Document httpGetAndParseHtml(String link) throws IOException {
        CloseableHttpClient httpclient = HttpClients.createDefault();
        HttpGet httpGet = new HttpGet(link);
        httpGet.addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/79.0.3945.130 Safari/537.36");

        try (CloseableHttpResponse response = httpclient.execute(httpGet)) {
            System.out.println(response.getStatusLine());
            HttpEntity entity = response.getEntity();
            String html = EntityUtils.toString(entity);
            return Jsoup.parse(html);
        }
    }

    private static boolean isInterestedLink(String link) {
        return link.contains("news.sina.cn") || "https://sina.cn".equals(link);
    }
}
