package slideshow.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import slideshow.model.PromptTemplate;
import slideshow.model.TemplateCategory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.ArrayList;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * JSON文件模板存储实现
 */
public class JsonTemplateStorage implements TemplateStorage {
    private static final Logger logger = Logger.getLogger(JsonTemplateStorage.class.getName());
    private static final String DEFAULT_STORAGE_FILE = "templates.json";
    private static final String BACKUP_DIR = "backups";

    private final String storageFilePath;
    private final Gson gson;
    private List<PromptTemplate> templates;

    public JsonTemplateStorage() {
        this(DEFAULT_STORAGE_FILE);
    }

    public JsonTemplateStorage(String storageFilePath) {
        this.storageFilePath = storageFilePath;
        this.gson = new GsonBuilder()
                .setPrettyPrinting()
                .create();
        this.templates = loadTemplates();
    }

    @Override
    public boolean saveTemplate(PromptTemplate template) {
        try {
            if (template == null) {
                logger.warning("模板不能为空");
                return false;
            }

            // 检查是否已存在同名模板
            Optional<PromptTemplate> existing = getTemplateByName(template.getName());
            if (existing.isPresent() && !existing.get().getId().equals(template.getId())) {
                logger.warning("模板名称已存在: " + template.getName());
                return false;
            }

            // 更新或新增模板
            boolean updated = false;
            for (int i = 0; i < templates.size(); i++) {
                if (templates.get(i).getId().equals(template.getId())) {
                    templates.set(i, template);
                    updated = true;
                    break;
                }
            }

            if (!updated) {
                templates.add(template);
            }

            saveToFile();
            logger.info("模板保存成功: " + template.getName());
            return true;

        } catch (Exception e) {
            logger.log(Level.SEVERE, "保存模板失败", e);
            return false;
        }
    }

    @Override
    public Optional<PromptTemplate> getTemplateById(String id) {
        if (id == null || id.trim().isEmpty()) {
            return Optional.empty();
        }

        return templates.stream()
                .filter(template -> template.getId().equals(id))
                .findFirst();
    }

    @Override
    public Optional<PromptTemplate> getTemplateByName(String name) {
        if (name == null || name.trim().isEmpty()) {
            return Optional.empty();
        }

        return templates.stream()
                .filter(template -> template.getName().equals(name.trim()))
                .findFirst();
    }

    @Override
    public List<PromptTemplate> getAllTemplates() {
        return new ArrayList<>(templates);
    }

    @Override
    public List<PromptTemplate> getTemplatesByCategory(TemplateCategory category) {
        if (category == null) {
            return new ArrayList<>();
        }

        return templates.stream()
                .filter(template -> template.getCategory() == category)
                .collect(Collectors.toList());
    }

    @Override
    public List<PromptTemplate> getTemplatesByTag(String tag) {
        if (tag == null || tag.trim().isEmpty()) {
            return new ArrayList<>();
        }

        String searchTag = tag.trim().toLowerCase();
        return templates.stream()
                .filter(template -> template.getTags().stream()
                        .anyMatch(t -> t.toLowerCase().contains(searchTag)))
                .collect(Collectors.toList());
    }

    @Override
    public List<PromptTemplate> searchTemplates(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return new ArrayList<>();
        }

        String searchKeyword = keyword.trim().toLowerCase();
        return templates.stream()
                .filter(template -> template.getName().toLowerCase().contains(searchKeyword) ||
                        template.getDescription().toLowerCase().contains(searchKeyword) ||
                        template.getContent().toLowerCase().contains(searchKeyword) ||
                        template.getTags().stream().anyMatch(tag -> tag.toLowerCase().contains(searchKeyword)))
                .collect(Collectors.toList());
    }

    @Override
    public boolean updateTemplate(PromptTemplate template) {
        return saveTemplate(template);
    }

    @Override
    public boolean deleteTemplate(String id) {
        try {
            boolean removed = templates.removeIf(template -> template.getId().equals(id));
            if (removed) {
                saveToFile();
                logger.info("模板删除成功: " + id);
                return true;
            }
            return false;
        } catch (Exception e) {
            logger.log(Level.SEVERE, "删除模板失败", e);
            return false;
        }
    }

    @Override
    public List<PromptTemplate> getFavoriteTemplates() {
        return templates.stream()
                .filter(template -> template.getMetadata().isFavorite())
                .collect(Collectors.toList());
    }

    @Override
    public List<PromptTemplate> getMostUsedTemplates(int limit) {
        return templates.stream()
                .sorted((t1, t2) -> Integer.compare(t2.getMetadata().getUseCount(), t1.getMetadata().getUseCount()))
                .limit(limit)
                .collect(Collectors.toList());
    }

    @Override
    public List<PromptTemplate> getTopRatedTemplates(int limit) {
        return templates.stream()
                .filter(t -> t.getMetadata().getRatingCount() > 0)
                .sorted((t1, t2) -> Double.compare(t2.getMetadata().getRating(), t1.getMetadata().getRating()))
                .limit(limit)
                .collect(Collectors.toList());
    }

    @Override
    public List<PromptTemplate> getDefaultTemplates() {
        return templates.stream()
                .filter(PromptTemplate::isDefault)
                .collect(Collectors.toList());
    }

    @Override
    public boolean templateExists(String id) {
        return getTemplateById(id).isPresent();
    }

    @Override
    public int getTemplateCount() {
        return templates.size();
    }

    @Override
    public boolean clearAllTemplates() {
        try {
            templates.clear();
            saveToFile();
            logger.info("All templates cleared");
            return true;
        } catch (Exception e) {
            logger.log(Level.SEVERE, "清空模板失败", e);
            return false;
        }
    }

    @Override
    public boolean backupTemplates(String backupPath) {
        try {
            Path backupDir = Paths.get(backupPath);
            if (!Files.exists(backupDir)) {
                Files.createDirectories(backupDir);
            }

            String backupFileName = "templates_backup_" + System.currentTimeMillis() + ".json";
            Path backupFile = backupDir.resolve(backupFileName);

            String json = gson.toJson(templates);
            Files.write(backupFile, json.getBytes(StandardCharsets.UTF_8));

            logger.info("Templates backed up successfully: " + backupFile);
            return true;
        } catch (Exception e) {
            logger.log(Level.SEVERE, "备份模板失败", e);
            return false;
        }
    }

    @Override
    public boolean restoreTemplates(String backupPath) {
        try {
            Path backupFile = Paths.get(backupPath);
            if (!Files.exists(backupFile)) {
                logger.warning("备份文件不存在: " + backupPath);
                return false;
            }

            String json = Files.readString(backupFile, StandardCharsets.UTF_8);
            List<PromptTemplate> backupTemplates = gson.fromJson(json,
                    new TypeToken<List<PromptTemplate>>() {
                    }.getType());

            if (backupTemplates != null) {
                templates = backupTemplates;
                saveToFile();
                logger.info("Templates restored successfully");
                return true;
            }
            return false;
        } catch (Exception e) {
            logger.log(Level.SEVERE, "恢复模板失败", e);
            return false;
        }
    }

    /**
     * 从文件加载模板
     */
    private List<PromptTemplate> loadTemplates() {
        try {
            Path file = Paths.get(storageFilePath);
            if (!Files.exists(file)) {
                logger.info("Template file does not exist, creating default templates");
                return createDefaultTemplates();
            }

            String json = Files.readString(file, StandardCharsets.UTF_8);
            List<PromptTemplate> loadedTemplates = gson.fromJson(json,
                    new TypeToken<List<PromptTemplate>>() {
                    }.getType());

            if (loadedTemplates != null) {
                logger.info("Successfully loaded " + loadedTemplates.size() + " templates");
                return loadedTemplates;
            } else {
                logger.warning("Template file format error, creating default templates");
                return createDefaultTemplates();
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "加载模板失败", e);
            return createDefaultTemplates();
        }
    }

    /**
     * 将模板保存到文件
     */
    private void saveToFile() throws IOException {
        String json = gson.toJson(templates);
        Path file = Paths.get(storageFilePath);
        Files.write(file, json.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 创建默认模板
     */
    private List<PromptTemplate> createDefaultTemplates() {
        List<PromptTemplate> defaultTemplates = new ArrayList<>();

        // 主题PPT生成模板
        PromptTemplate themeTemplate = new PromptTemplate(
                "主题PPT",
                "根据指定主题生成PPT内容",
                "You are a PPT assistant. Please generate a complete PPT outline for the following topic:\n"
                        + "Topic: {0}\n"
                        + "Audience: {1}\n"
                        + "Duration: {2} minutes\n\n"
                        + "Requirements:\n"
                        + "1. Design a comprehensive PPT structure for the topic\n"
                        + "2. Each page should include Title, Subtitle, and Bullets\n"
                        + "3. Content should be vivid and informative\n"
                        + "4. Include appropriate visual element descriptions\n\n"
                        + "Important: Output must follow the standard PPT format below:\n"
                        + "---PPT Outline---\n"
                        + "Page 1:\n"
                        + "Title: [Page Title]\n"
                        + "Subtitle: [Page Subtitle]\n"
                        + "Bullet: [Bullet Content]\n"
                        + "Text: [小标题下具体自然段文本,PPT的正文内容]\n"
                        + "Draw: [Draw Description]\n"
                        + "Page 2:\n"
                        + "Title: [Page Title]\n"
                        + "Subtitle: [Page Subtitle]\n"
                        + "Bullet: [Bullet Content]\n"
                        + "Text: [小标题下具体自然段文本,PPT的正文内容]\n"
                        + "Draw: [Draw Description]\n"
                        + "(More pages as needed...)\n\n"
                        + "Please generate the PPT outline:",
                TemplateCategory.PPT_THEME);
        themeTemplate.setDefault(true);
        themeTemplate.addTag("PPT");
        themeTemplate.addTag("主题");
        defaultTemplates.add(themeTemplate);

        // 教育PPT生成模板
        PromptTemplate educationTemplate = new PromptTemplate(
                "教育PPT",
                "生成用于教学的PPT内容",
                "你是一名教育PPT助手，请为以下教学内容生成完整的PPT大纲：\n"
                        +
                        "学科：{0}\n" +
                        "目标学生：{1}\n" +
                        "课程时长：{2}分钟\n\n" +
                        "要求：\n" +
                        "1. 设计包含明确学习目标的教学PPT结构\n" +
                        "2. 每页包含标题、副标题和关键知识点\n" +
                        "3. 内容易于理解和跟进\n" +
                        "4. 包含示例和视觉辅助描述\n\n" +
                        "重要：必须按如下标准PPT格式输出：\n" +
                        "---PPT大纲---\n" +
                        "第1页：\n" +
                        "Title: [页面标题]\n" +
                        "Subtitle: [页面副标题]\n" +
                        "Bullet: [要点内容]\n" +
                        "Text: [小标题下具体自然段文本,PPT的正文内容]\n" +
                        "Draw: [绘图描述]\n" +
                        "第2页：\n" +
                        "Title: [页面标题]\n" +
                        "Subtitle: [页面副标题]\n" +
                        "Bullet: [要点内容]\n" +
                        "Text: [小标题下具体自然段文本,PPT的正文内容]\n" +
                        "Draw: [绘图描述]\n" +
                        "（更多页面以此类推...）\n\n" +
                        "请生成教学PPT大纲：",
                TemplateCategory.PPT_EDUCATION);
        educationTemplate.setDefault(true);
        educationTemplate.addTag("PPT");
        educationTemplate.addTag("教育");
        defaultTemplates.add(educationTemplate);

        // 商业PPT生成模板
        PromptTemplate businessTemplate = new PromptTemplate(
                "商业PPT",
                "生成专业的商业演示内容",
                "你是一名商业PPT助手，请为以下商业内容生成完整的PPT大纲：\n" +
                        "商业主题：{0}\n" +
                        "目标听众：{1}\n" +
                        "演讲时长：{2}分钟\n\n" +
                        "要求：\n" +
                        "1. 设计专业的商业PPT结构\n" +
                        "2. 每页包含标题、副标题和关键业务要点\n" +
                        "3. 内容专业且数据驱动\n" +
                        "4. 包含图表、数据和商业视觉元素描述\n\n" +
                        "重要：必须按如下标准PPT格式输出：\n" +
                        "---PPT大纲---\n" +
                        "第1页：\n" +
                        "Title: [页面标题]\n" +
                        "Subtitle: [页面副标题]\n" +
                        "Bullet: [要点内容]\n" +
                        "Text: [小标题下具体自然段文本,PPT的正文内容]\n" +
                        "Draw: [绘图描述]\n" +
                        "第2页：\n" +
                        "Title: [页面标题]\n" +
                        "Subtitle: [页面副标题]\n" +
                        "Bullet: [要点内容]\n" +
                        "Text: [小标题下具体自然段文本,PPT的正文内容]\n" +
                        "Draw: [绘图描述]\n" +
                        "（更多页面以此类推...）\n\n" +
                        "请生成商业PPT大纲：",
                TemplateCategory.PPT_BUSINESS);
        businessTemplate.setDefault(true);
        businessTemplate.addTag("PPT");
        businessTemplate.addTag("商业");
        defaultTemplates.add(businessTemplate);

        // 技术PPT生成模板
        PromptTemplate technicalTemplate = new PromptTemplate(
                "技术PPT",
                "生成包含详细讲解的技术演示内容",
                "你是一名技术PPT助手，请为以下技术内容生成完整的PPT大纲：\n" +
                        "技术主题：{0}\n" +
                        "目标听众：{1}\n" +
                        "演讲时长：{2}分钟\n\n" +
                        "要求：\n" +
                        "1. 设计详细的技术PPT结构\n" +
                        "2. 每页包含标题、副标题和技术讲解\n" +
                        "3. 内容准确全面，技术性强\n" +
                        "4. 包含图示、流程图和技术视觉元素描述\n\n" +
                        "重要：必须按如下标准PPT格式输出：\n" +
                        "---PPT大纲---\n" +
                        "第1页：\n" +
                        "Title: [页面标题]\n" +
                        "Subtitle: [页面副标题]\n" +
                        "Bullet: [要点内容]\n" +
                        "Text: [小标题下具体自然段文本,PPT的正文内容]\n" +
                        "Draw: [绘图描述]\n" +
                        "第2页：\n" +
                        "Title: [页面标题]\n" +
                        "Subtitle: [页面副标题]\n" +
                        "Bullet: [要点内容]\n" +
                        "Text: [小标题下具体自然段文本,PPT的正文内容]\n" +
                        "Draw: [绘图描述]\n" +
                        "（更多页面以此类推...）\n\n" +
                        "请生成技术PPT大纲：",
                TemplateCategory.PPT_TECHNICAL);
        technicalTemplate.setDefault(true);
        technicalTemplate.addTag("PPT");
        technicalTemplate.addTag("技术");
        defaultTemplates.add(technicalTemplate);

        // 创意PPT生成模板
        PromptTemplate creativeTemplate = new PromptTemplate(
                "创意PPT",
                "生成有创意和吸引力的演示内容",
                "你是一名创意PPT助手，请为以下创意内容生成完整的PPT大纲：\n" +
                        "创意主题：{0}\n" +
                        "目标听众：{1}\n" +
                        "演讲时长：{2}分钟\n\n" +
                        "要求：\n" +
                        "1. 设计创新且有创意的PPT结构\n" +
                        "2. 每页包含标题、副标题和创意内容\n" +
                        "3. 内容应富有吸引力、启发性和视觉美感\n" +
                        "4. 包含创意视觉元素、比喻和故事化描述\n\n" +
                        "重要：必须按如下标准PPT格式输出：\n" +
                        "---PPT大纲---\n" +
                        "第1页：\n" +
                        "Title: [页面标题]\n" +
                        "Subtitle: [页面副标题]\n" +
                        "Bullet: [要点内容]\n" +
                        "Text: [小标题下具体自然段文本,PPT的正文内容]\n" +
                        "Draw: [绘图描述]\n" +
                        "第2页：\n" +
                        "Title: [页面标题]\n" +
                        "Subtitle: [页面副标题]\n" +
                        "Bullet: [要点内容]\n" +
                        "Text: [小标题下具体自然段文本,PPT的正文内容]\n" +
                        "Draw: [绘图描述]\n" +
                        "（更多页面以此类推...）\n\n" +
                        "请生成创意PPT大纲：",
                TemplateCategory.PPT_CREATIVE);
        creativeTemplate.setDefault(true);
        creativeTemplate.addTag("PPT");
        creativeTemplate.addTag("创意");
        defaultTemplates.add(creativeTemplate);

        return defaultTemplates;
    }
}