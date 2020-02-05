package com.github.jaychenfe;

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.sql.SQLException;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class Crawler {

    CrawlerDao dao = new MyBatisCrawlerDao();

    public static void main(String[] args) throws IOException, SQLException {
        new Crawler().run();
    }

    public void run() throws SQLException, IOException {

        String link;

        while ((link = dao.getNextLinkThenDeleteLink()) != null) {
            System.out.println(link);

            if (!isToProcessLink(link)) {
                continue;
            }

            Document doc = httpGetAndParseHtml(link);
            parseUrlsFromPageAndStoreIntoDatabase(doc);
            storeToDatabaseIfItIsNewsPage(link, doc);
            dao.insertProcessedLink(link);
        }
    }

    private boolean isToProcessLink(String link) throws SQLException {
        return !dao.isLinkProcessed(link) && isInterestedLink(link);
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

    private void parseUrlsFromPageAndStoreIntoDatabase(Document doc) {
        doc.select("a").stream()
                .map(aTag -> aTag.attr("href"))
                .map(Crawler::fixLink)
                .filter(Crawler::isInterestedLink)
                .forEach(generateInsertLinkConsumer());
    }

    private Consumer<String> generateInsertLinkConsumer() {
        return link -> {
            try {
                dao.insertLinkToBeProcess(link);
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        };
    }


    private void storeToDatabaseIfItIsNewsPage(String link, Document doc) throws SQLException {
        Elements articleTags = doc.select("article");
        if (articleTags.isEmpty()) {
            return;
        }
        Element articleTag = articleTags.get(0);
        String title = articleTag.child(0).text();
        System.out.println(title + "\n");
        String content = getContent(articleTag);
        System.out.println(content);
        dao.saveNews(link, title, content);
    }


    private static String getContent(Element articleTag) {
        return articleTag.select("p")
                .stream()
                .filter(node -> node.children().isEmpty())
                .map(Element::text)
                .collect(Collectors.joining("\n"));
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
