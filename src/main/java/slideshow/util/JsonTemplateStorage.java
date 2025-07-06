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
 * 基于JSON文件的模板存储实现
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
                logger.warning("尝试保存空模板");
                return false;
            }

            // 检查是否已存在同名模板
            Optional<PromptTemplate> existing = getTemplateByName(template.getName());
            if (existing.isPresent() && !existing.get().getId().equals(template.getId())) {
                logger.warning("模板名称已存在: " + template.getName());
                return false;
            }

            // 更新或添加模板
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
            logger.info("Template saved successfully: " + template.getName());
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
                logger.info("Template deleted successfully: " + id);
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
     * 保存模板到文件
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

        // PPT大纲生成模板
        PromptTemplate pptOutlineTemplate = new PromptTemplate(
                "PPT Outline Generation",
                "Generate structured PPT outline based on the topic",
                "You are a PPT assistant. Please generate a complete PPT outline based on the following content.\n"
                        +
                        "Requirements:\n" +
                        "1. Design a logical and clear PPT structure\n" +
                        "2. Each slide should have appropriate title, subtitle, and bullet points\n" +
                        "3. Content should be well-organized and easy to understand\n" +
                        "4. Suitable for presentation duration: {0} minutes\n" +
                        "5. Output in Chinese\n\n" +
                        "IMPORTANT: You must output in the following standard PPT format:\n" +
                        "---PPT命令---\n" +
                        "Page 1:\n" +
                        "Title: [Page Title]\n" +
                        "Subtitle: [Page Subtitle]\n" +
                        "Bullet: [Bullet Point Content]\n" +
                        "Draw: [Drawing Description]\n" +
                        "Page 2:\n" +
                        "Title: [Page Title]\n" +
                        "Subtitle: [Page Subtitle]\n" +
                        "Bullet: [Bullet Point Content]\n" +
                        "Draw: [Drawing Description]\n" +
                        "(Continue for more pages...)\n\n" +
                        "Content: {1}\n\n" +
                        "Please generate the PPT outline:",
                TemplateCategory.PPT_OUTLINE);
        pptOutlineTemplate.setDefault(true);
        pptOutlineTemplate.addTag("PPT");
        pptOutlineTemplate.addTag("outline");
        defaultTemplates.add(pptOutlineTemplate);

        // 主题PPT生成模板
        PromptTemplate themeTemplate = new PromptTemplate(
                "Theme-based PPT Generation",
                "Generate PPT content based on specific theme or topic",
                "You are a PPT assistant. Please generate a complete PPT outline for the following theme:\n" +
                        "Theme: {0}\n" +
                        "Target Audience: {1}\n" +
                        "Presentation Duration: {2} minutes\n\n" +
                        "Requirements:\n" +
                        "1. Create a comprehensive PPT structure for the theme\n" +
                        "2. Each slide should include title, subtitle, and relevant bullet points\n" +
                        "3. Content should be engaging and informative\n" +
                        "4. Include appropriate visual elements descriptions\n\n" +
                        "IMPORTANT: You must output in the following standard PPT format:\n" +
                        "---PPT命令---\n" +
                        "Page 1:\n" +
                        "Title: [Page Title]\n" +
                        "Subtitle: [Page Subtitle]\n" +
                        "Bullet: [Bullet Point Content]\n" +
                        "Draw: [Drawing Description]\n" +
                        "Page 2:\n" +
                        "Title: [Page Title]\n" +
                        "Subtitle: [Page Subtitle]\n" +
                        "Bullet: [Bullet Point Content]\n" +
                        "Draw: [Drawing Description]\n" +
                        "(Continue for more pages...)\n\n" +
                        "Please generate the PPT outline:",
                TemplateCategory.PPT_THEME);
        themeTemplate.setDefault(true);
        themeTemplate.addTag("PPT");
        themeTemplate.addTag("theme");
        defaultTemplates.add(themeTemplate);

        // 教育PPT生成模板
        PromptTemplate educationTemplate = new PromptTemplate(
                "Educational PPT Generation",
                "Generate educational PPT content for learning purposes",
                "You are an educational PPT assistant. Please generate a complete PPT outline for educational content:\n"
                        +
                        "Subject: {0}\n" +
                        "Target Students: {1}\n" +
                        "Lesson Duration: {2} minutes\n\n" +
                        "Requirements:\n" +
                        "1. Create an educational PPT structure with clear learning objectives\n" +
                        "2. Each slide should include title, subtitle, and key learning points\n" +
                        "3. Content should be easy to understand and follow\n" +
                        "4. Include examples and visual aids descriptions\n\n" +
                        "IMPORTANT: You must output in the following standard PPT format:\n" +
                        "---PPT命令---\n" +
                        "Page 1:\n" +
                        "Title: [Page Title]\n" +
                        "Subtitle: [Page Subtitle]\n" +
                        "Bullet: [Bullet Point Content]\n" +
                        "Draw: [Drawing Description]\n" +
                        "Page 2:\n" +
                        "Title: [Page Title]\n" +
                        "Subtitle: [Page Subtitle]\n" +
                        "Bullet: [Bullet Point Content]\n" +
                        "Draw: [Drawing Description]\n" +
                        "(Continue for more pages...)\n\n" +
                        "Please generate the educational PPT outline:",
                TemplateCategory.PPT_EDUCATION);
        educationTemplate.setDefault(true);
        educationTemplate.addTag("PPT");
        educationTemplate.addTag("education");
        defaultTemplates.add(educationTemplate);

        // 商务PPT生成模板
        PromptTemplate businessTemplate = new PromptTemplate(
                "Business PPT Generation",
                "Generate professional business presentation content",
                "You are a business PPT assistant. Please generate a complete PPT outline for business content:\n" +
                        "Business Topic: {0}\n" +
                        "Target Audience: {1}\n" +
                        "Presentation Duration: {2} minutes\n\n" +
                        "Requirements:\n" +
                        "1. Create a professional business PPT structure\n" +
                        "2. Each slide should include title, subtitle, and key business points\n" +
                        "3. Content should be professional and data-driven\n" +
                        "4. Include charts, graphs, and business visual elements descriptions\n\n" +
                        "IMPORTANT: You must output in the following standard PPT format:\n" +
                        "---PPT命令---\n" +
                        "Page 1:\n" +
                        "Title: [Page Title]\n" +
                        "Subtitle: [Page Subtitle]\n" +
                        "Bullet: [Bullet Point Content]\n" +
                        "Draw: [Drawing Description]\n" +
                        "Page 2:\n" +
                        "Title: [Page Title]\n" +
                        "Subtitle: [Page Subtitle]\n" +
                        "Bullet: [Bullet Point Content]\n" +
                        "Draw: [Drawing Description]\n" +
                        "(Continue for more pages...)\n\n" +
                        "Please generate the business PPT outline:",
                TemplateCategory.PPT_BUSINESS);
        businessTemplate.setDefault(true);
        businessTemplate.addTag("PPT");
        businessTemplate.addTag("business");
        defaultTemplates.add(businessTemplate);

        // 技术PPT生成模板
        PromptTemplate technicalTemplate = new PromptTemplate(
                "Technical PPT Generation",
                "Generate technical presentation content with detailed explanations",
                "You are a technical PPT assistant. Please generate a complete PPT outline for technical content:\n" +
                        "Technical Topic: {0}\n" +
                        "Target Audience: {1}\n" +
                        "Presentation Duration: {2} minutes\n\n" +
                        "Requirements:\n" +
                        "1. Create a detailed technical PPT structure\n" +
                        "2. Each slide should include title, subtitle, and technical explanations\n" +
                        "3. Content should be technically accurate and comprehensive\n" +
                        "4. Include diagrams, flowcharts, and technical visual elements descriptions\n\n" +
                        "IMPORTANT: You must output in the following standard PPT format:\n" +
                        "---PPT命令---\n" +
                        "Page 1:\n" +
                        "Title: [Page Title]\n" +
                        "Subtitle: [Page Subtitle]\n" +
                        "Bullet: [Bullet Point Content]\n" +
                        "Draw: [Drawing Description]\n" +
                        "Page 2:\n" +
                        "Title: [Page Title]\n" +
                        "Subtitle: [Page Subtitle]\n" +
                        "Bullet: [Bullet Point Content]\n" +
                        "Draw: [Drawing Description]\n" +
                        "(Continue for more pages...)\n\n" +
                        "Please generate the technical PPT outline:",
                TemplateCategory.PPT_TECHNICAL);
        technicalTemplate.setDefault(true);
        technicalTemplate.addTag("PPT");
        technicalTemplate.addTag("technical");
        defaultTemplates.add(technicalTemplate);

        // 创意PPT生成模板
        PromptTemplate creativeTemplate = new PromptTemplate(
                "Creative PPT Generation",
                "Generate creative and engaging presentation content",
                "You are a creative PPT assistant. Please generate a complete PPT outline for creative content:\n" +
                        "Creative Topic: {0}\n" +
                        "Target Audience: {1}\n" +
                        "Presentation Duration: {2} minutes\n\n" +
                        "Requirements:\n" +
                        "1. Create an innovative and creative PPT structure\n" +
                        "2. Each slide should include title, subtitle, and creative content\n" +
                        "3. Content should be engaging, inspiring, and visually appealing\n" +
                        "4. Include creative visual elements, metaphors, and storytelling elements descriptions\n\n" +
                        "IMPORTANT: You must output in the following standard PPT format:\n" +
                        "---PPT命令---\n" +
                        "Page 1:\n" +
                        "Title: [Page Title]\n" +
                        "Subtitle: [Page Subtitle]\n" +
                        "Bullet: [Bullet Point Content]\n" +
                        "Draw: [Drawing Description]\n" +
                        "Page 2:\n" +
                        "Title: [Page Title]\n" +
                        "Subtitle: [Page Subtitle]\n" +
                        "Bullet: [Bullet Point Content]\n" +
                        "Draw: [Drawing Description]\n" +
                        "(Continue for more pages...)\n\n" +
                        "Please generate the creative PPT outline:",
                TemplateCategory.PPT_CREATIVE);
        creativeTemplate.setDefault(true);
        creativeTemplate.addTag("PPT");
        creativeTemplate.addTag("creative");
        defaultTemplates.add(creativeTemplate);

        return defaultTemplates;
    }
}