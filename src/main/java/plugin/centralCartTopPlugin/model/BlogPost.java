package plugin.centralCartTopPlugin.model;

import com.google.gson.annotations.SerializedName;

/**
 * Representa um post do blog da loja CentralCart
 */
public class BlogPost {

    @SerializedName(value = "id")
    private String id;

    @SerializedName(value = "title")
    private String title;

    @SerializedName(value = "url")
    private String url;

    @SerializedName(value = "created_at", alternate = {"createdAt"})
    private String createdAt;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return "BlogPost{id='" + id + "', title='" + title + "', url='" + url + "', createdAt='" + createdAt + "'}";
    }
}
