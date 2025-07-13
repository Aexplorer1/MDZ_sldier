package slideshow.model;

import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;
import java.util.UUID;

/**
 * Prompt Template Model Class
 * Used for storing and managing AI prompt templates
 */
public class PromptTemplate {
    private String id;
    private String name;
    private String description;
    private String content;
    private TemplateCategory category;
    private List<String> tags;
    private TemplateMetadata metadata;
    private boolean isDefault;
    private boolean isPublic;

    public PromptTemplate() {
        this.id = UUID.randomUUID().toString();
        this.tags = new ArrayList<>();
        this.metadata = new TemplateMetadata();
        this.isDefault = false;
        this.isPublic = false;
    }

    public PromptTemplate(String name, String description, String content, TemplateCategory category) {
        this();
        this.name = name;
        this.description = description;
        this.content = content;
        this.category = category;
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public TemplateCategory getCategory() {
        return category;
    }

    public void setCategory(TemplateCategory category) {
        this.category = category;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    public TemplateMetadata getMetadata() {
        return metadata;
    }

    public void setMetadata(TemplateMetadata metadata) {
        this.metadata = metadata;
    }

    public boolean isDefault() {
        return isDefault;
    }

    public void setDefault(boolean isDefault) {
        this.isDefault = isDefault;
    }

    public boolean isPublic() {
        return isPublic;
    }

    public void setPublic(boolean isPublic) {
        this.isPublic = isPublic;
    }

    /**
     * Add tag
     */
    public void addTag(String tag) {
        if (tag != null && !tag.trim().isEmpty() && !tags.contains(tag)) {
            tags.add(tag.trim());
        }
    }

    /**
     * Remove tag
     */
    public void removeTag(String tag) {
        tags.remove(tag);
    }

    /**
     * Format template content, replace placeholders
     */
    public String formatContent(Object... args) {
        String formatted = content;
        for (int i = 0; i < args.length; i++) {
            formatted = formatted.replace("{" + i + "}", String.valueOf(args[i]));
        }
        return formatted;
    }

    @Override
    public String toString() {
        return "PromptTemplate{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", category=" + category +
                ", tags=" + tags +
                '}';
    }
}