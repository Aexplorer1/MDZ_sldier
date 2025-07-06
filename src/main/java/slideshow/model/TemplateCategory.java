package slideshow.model;

/**
 * Template Category Enum
 * Defines different categories for PPT generation templates
 */
public enum TemplateCategory {
    PPT_OUTLINE("PPT Outline", "Templates for generating PPT outlines"),
    PPT_THEME("PPT Theme", "Templates for theme-based PPT generation"),
    PPT_EDUCATION("PPT Education", "Templates for educational PPT content"),
    PPT_BUSINESS("PPT Business", "Templates for business presentations"),
    PPT_TECHNICAL("PPT Technical", "Templates for technical presentations"),
    PPT_CREATIVE("PPT Creative", "Templates for creative presentations"),
    CUSTOM("Custom", "User-defined templates");

    private final String displayName;
    private final String description;

    TemplateCategory(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    @Override
    public String toString() {
        return displayName;
    }
}