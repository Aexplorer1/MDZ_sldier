package slideshow.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Template metadata class
 * Stores additional information about templates, such as creation time, usage
 * count, ratings, etc.
 */
public class TemplateMetadata {
    private String createdAt;
    private String updatedAt;
    private String lastUsedAt;
    private int useCount;
    private double rating;
    private int ratingCount;
    private String author;
    private String version;
    private boolean isFavorite;

    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public TemplateMetadata() {
        this.createdAt = LocalDateTime.now().format(formatter);
        this.updatedAt = LocalDateTime.now().format(formatter);
        this.useCount = 0;
        this.rating = 0.0;
        this.ratingCount = 0;
        this.author = "User";

        this.version = "1.0";
        this.isFavorite = false;
    }

    // Getters and Setters
    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public String getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(String updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getLastUsedAt() {
        return lastUsedAt;
    }

    public void setLastUsedAt(String lastUsedAt) {
        this.lastUsedAt = lastUsedAt;
    }

    public int getUseCount() {
        return useCount;
    }

    public void setUseCount(int useCount) {
        this.useCount = useCount;
    }

    public double getRating() {
        return rating;
    }

    public void setRating(double rating) {
        this.rating = rating;
    }

    public int getRatingCount() {
        return ratingCount;
    }

    public void setRatingCount(int ratingCount) {
        this.ratingCount = ratingCount;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public boolean isFavorite() {
        return isFavorite;
    }

    public void setFavorite(boolean favorite) {
        isFavorite = favorite;
    }

    /**
     * Increment usage count
     */
    public void incrementUseCount() {
        this.useCount++;
        this.lastUsedAt = LocalDateTime.now().format(formatter);
        this.updatedAt = LocalDateTime.now().format(formatter);
    }

    /**
     * Add rating
     */
    public void addRating(double newRating) {
        if (newRating >= 0 && newRating <= 5) {
            double totalRating = this.rating * this.ratingCount + newRating;
            this.ratingCount++;
            this.rating = totalRating / this.ratingCount;
            this.updatedAt = LocalDateTime.now().format(formatter);
        }
    }

    /**
     * 获取平均评分（保留一位小数）
     */
    public double getAverageRating() {
        return Math.round(rating * 10.0) / 10.0;
    }

    /**
     * Update template
     */
    public void update() {
        this.updatedAt = LocalDateTime.now().format(formatter);
    }

    @Override
    public String toString() {
        return "TemplateMetadata{" +
                "createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                ", useCount=" + useCount +
                ", rating=" + rating +
                ", author='" + author + '\'' +
                ", isFavorite=" + isFavorite +
                '}';
    }
}