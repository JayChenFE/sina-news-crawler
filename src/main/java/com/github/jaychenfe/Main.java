package com.github.jaychenfe;

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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class Main {

    public static void main(String[] args) throws IOException {
        // 待处理的链接池
        ArrayList<String> linkPool = new ArrayList<>();
        // 已处理的链接池
        Set<String> proccessedLinks = new HashSet<>();

        linkPool.add("https://sina.cn");
        while (true) {
            if (linkPool.isEmpty()) {
                break;
            }
            String link = linkPool.remove(linkPool.size() - 1);

            if (proccessedLinks.contains(link)) { // 已经处理过的
                continue;
            }

            if (isInterestLink(link)) {
                // 这是我们不感兴趣的,不处理
                continue;
            }

            if (link.startsWith("//")) {
                link = "https:" + link;
            }

            if (link.contains("\\")) {
                link = link.replaceAll("\\\\", "");
            }

            Document doc = httpGetAndPraseHtml(link);
            doc.select("a")
                    .stream()
                    .map(aTag -> aTag.attr("href"))
                    .forEach(linkPool::add);
            storeToDatabaseIfItIsNewsPage(doc);
            proccessedLinks.add(link);
        }
    }

    private static void storeToDatabaseIfItIsNewsPage(Document doc) {
        Elements articleTags = doc.select("article");
        if (!articleTags.isEmpty()) {
            String title = articleTags.get(0).child(0).text();
            // todo 存db
            System.out.println(title);
        }
    }

    private static Document httpGetAndPraseHtml(String link) throws IOException {
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

    private static boolean isInterestLink(String link) {
        return !link.contains("news.sina.cn") && !"https://sina.cn".equals(link);
    }
}
