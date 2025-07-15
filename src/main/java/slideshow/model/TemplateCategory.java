package slideshow.model;

/**
 * Template Category Enum
 * Defines different categories for PPT generation templates
 */
public enum TemplateCategory {
    PPT_THEME("主题PPT", "基于主题生成PPT的模板"),
    PPT_EDUCATION("教育PPT", "用于教学内容的PPT模板"),
    PPT_BUSINESS("商业PPT", "用于商业演示的PPT模板"),
    PPT_TECHNICAL("技术PPT", "用于技术演示的PPT模板"),
    PPT_CREATIVE("创意PPT", "用于创意演示的PPT模板"),
    OTHER("其他", "其他类型模板");

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