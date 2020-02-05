package com.github.jaychenfe;

public class News {
    private Long Id;
    private String url;

    public News(String url, String content, String title) {
        this.url = url;
        this.content = content;
        this.title = title;
    }

    private String content;

    public Long getId() {
        return Id;
    }

    public void setId(Long id) {
        Id = id;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    private String title;


}
