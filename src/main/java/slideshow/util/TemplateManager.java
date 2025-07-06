package slideshow.util;

import slideshow.model.PromptTemplate;
import slideshow.model.TemplateCategory;
import slideshow.model.TemplateMetadata;

import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * 模板管理器
 * 提供模板管理的核心功能，包括CRUD操作、搜索、统计等
 */
public class TemplateManager {
    private static final Logger logger = Logger.getLogger(TemplateManager.class.getName());

    private final TemplateStorage storage;

    public TemplateManager() {
        this.storage = new JsonTemplateStorage();
    }

    public TemplateManager(TemplateStorage storage) {
        this.storage = storage;
    }

    /**
     * 创建新模板
     */
    public boolean createTemplate(String name, String description, String content, TemplateCategory category) {
        try {
            if (name == null || name.trim().isEmpty()) {
                logger.warning("模板名称不能为空");
                return false;
            }

            if (content == null || content.trim().isEmpty()) {
                logger.warning("模板内容不能为空");
                return false;
            }

            if (category == null) {
                logger.warning("模板分类不能为空");
                return false;
            }

            // 检查名称是否已存在
            if (storage.getTemplateByName(name.trim()).isPresent()) {
                logger.warning("模板名称已存在: " + name);
                return false;
            }

            PromptTemplate template = new PromptTemplate(name.trim(), description, content, category);
            boolean success = storage.saveTemplate(template);

            if (success) {
                logger.info("模板创建成功: " + name);
            }

            return success;

        } catch (Exception e) {
            logger.log(Level.SEVERE, "创建模板失败", e);
            return false;
        }
    }

    /**
     * 更新模板
     */
    public boolean updateTemplate(String id, String name, String description, String content,
            TemplateCategory category) {
        try {
            Optional<PromptTemplate> existing = storage.getTemplateById(id);
            if (!existing.isPresent()) {
                logger.warning("模板不存在: " + id);
                return false;
            }

            PromptTemplate template = existing.get();

            // 检查名称是否与其他模板冲突
            if (!template.getName().equals(name.trim())) {
                Optional<PromptTemplate> nameConflict = storage.getTemplateByName(name.trim());
                if (nameConflict.isPresent() && !nameConflict.get().getId().equals(id)) {
                    logger.warning("模板名称已存在: " + name);
                    return false;
                }
            }

            template.setName(name.trim());
            template.setDescription(description);
            template.setContent(content);
            template.setCategory(category);
            template.getMetadata().update();

            boolean success = storage.updateTemplate(template);

            if (success) {
                logger.info("模板更新成功: " + name);
            }

            return success;

        } catch (Exception e) {
            logger.log(Level.SEVERE, "更新模板失败", e);
            return false;
        }
    }

    /**
     * 删除模板
     */
    public boolean deleteTemplate(String id) {
        try {
            Optional<PromptTemplate> template = storage.getTemplateById(id);
            if (!template.isPresent()) {
                logger.warning("模板不存在: " + id);
                return false;
            }

            // 不允许删除默认模板
            if (template.get().isDefault()) {
                logger.warning("不能删除默认模板: " + template.get().getName());
                return false;
            }

            boolean success = storage.deleteTemplate(id);

            if (success) {
                logger.info("模板删除成功: " + template.get().getName());
            }

            return success;

        } catch (Exception e) {
            logger.log(Level.SEVERE, "删除模板失败", e);
            return false;
        }
    }

    /**
     * 获取模板
     */
    public Optional<PromptTemplate> getTemplate(String id) {
        return storage.getTemplateById(id);
    }

    /**
     * 根据名称获取模板
     */
    public Optional<PromptTemplate> getTemplateByName(String name) {
        return storage.getTemplateByName(name);
    }

    /**
     * 获取所有模板
     */
    public List<PromptTemplate> getAllTemplates() {
        return storage.getAllTemplates();
    }

    /**
     * 根据分类获取模板
     */
    public List<PromptTemplate> getTemplatesByCategory(TemplateCategory category) {
        return storage.getTemplatesByCategory(category);
    }

    /**
     * 搜索模板
     */
    public List<PromptTemplate> searchTemplates(String keyword) {
        return storage.searchTemplates(keyword);
    }

    /**
     * 根据标签搜索模板
     */
    public List<PromptTemplate> getTemplatesByTag(String tag) {
        return storage.getTemplatesByTag(tag);
    }

    /**
     * 获取收藏的模板
     */
    public List<PromptTemplate> getFavoriteTemplates() {
        return storage.getFavoriteTemplates();
    }

    /**
     * 获取最常用的模板
     */
    public List<PromptTemplate> getMostUsedTemplates(int limit) {
        return storage.getMostUsedTemplates(limit);
    }

    /**
     * 获取评分最高的模板
     */
    public List<PromptTemplate> getTopRatedTemplates(int limit) {
        return storage.getTopRatedTemplates(limit);
    }

    /**
     * 获取默认模板
     */
    public List<PromptTemplate> getDefaultTemplates() {
        return storage.getDefaultTemplates();
    }

    /**
     * 使用模板（增加使用次数）
     */
    public void useTemplate(String id) {
        try {
            Optional<PromptTemplate> template = storage.getTemplateById(id);
            if (template.isPresent()) {
                template.get().getMetadata().incrementUseCount();
                storage.updateTemplate(template.get());
                logger.info("模板使用次数更新: " + template.get().getName());
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "更新模板使用次数失败", e);
        }
    }

    /**
     * 为模板评分
     */
    public boolean rateTemplate(String id, double rating) {
        try {
            Optional<PromptTemplate> template = storage.getTemplateById(id);
            if (template.isPresent()) {
                template.get().getMetadata().addRating(rating);
                storage.updateTemplate(template.get());
                logger.info("模板评分更新: " + template.get().getName() + ", 评分: " + rating);
                return true;
            }
            return false;
        } catch (Exception e) {
            logger.log(Level.SEVERE, "更新模板评分失败", e);
            return false;
        }
    }

    /**
     * 设置/取消收藏
     */
    public boolean toggleFavorite(String id) {
        try {
            Optional<PromptTemplate> template = storage.getTemplateById(id);
            if (template.isPresent()) {
                boolean currentFavorite = template.get().getMetadata().isFavorite();
                template.get().getMetadata().setFavorite(!currentFavorite);
                storage.updateTemplate(template.get());
                logger.info("模板收藏状态更新: " + template.get().getName() + ", 收藏: " + !currentFavorite);
                return true;
            }
            return false;
        } catch (Exception e) {
            logger.log(Level.SEVERE, "更新模板收藏状态失败", e);
            return false;
        }
    }

    /**
     * 添加标签
     */
    public boolean addTag(String id, String tag) {
        try {
            Optional<PromptTemplate> template = storage.getTemplateById(id);
            if (template.isPresent()) {
                template.get().addTag(tag);
                storage.updateTemplate(template.get());
                logger.info("模板标签添加: " + template.get().getName() + ", 标签: " + tag);
                return true;
            }
            return false;
        } catch (Exception e) {
            logger.log(Level.SEVERE, "添加模板标签失败", e);
            return false;
        }
    }

    /**
     * 移除标签
     */
    public boolean removeTag(String id, String tag) {
        try {
            Optional<PromptTemplate> template = storage.getTemplateById(id);
            if (template.isPresent()) {
                template.get().removeTag(tag);
                storage.updateTemplate(template.get());
                logger.info("模板标签移除: " + template.get().getName() + ", 标签: " + tag);
                return true;
            }
            return false;
        } catch (Exception e) {
            logger.log(Level.SEVERE, "移除模板标签失败", e);
            return false;
        }
    }

    /**
     * 获取模板统计信息
     */
    public TemplateStatistics getStatistics() {
        List<PromptTemplate> allTemplates = storage.getAllTemplates();

        int totalCount = allTemplates.size();
        int defaultCount = (int) allTemplates.stream().filter(PromptTemplate::isDefault).count();
        int favoriteCount = (int) allTemplates.stream().filter(t -> t.getMetadata().isFavorite()).count();

        double averageRating = allTemplates.stream()
                .filter(t -> t.getMetadata().getRatingCount() > 0)
                .mapToDouble(t -> t.getMetadata().getRating())
                .average()
                .orElse(0.0);

        int totalUseCount = allTemplates.stream()
                .mapToInt(t -> t.getMetadata().getUseCount())
                .sum();

        return new TemplateStatistics(totalCount, defaultCount, favoriteCount, averageRating, totalUseCount);
    }

    /**
     * 备份模板
     */
    public boolean backupTemplates(String backupPath) {
        return storage.backupTemplates(backupPath);
    }

    /**
     * 恢复模板
     */
    public boolean restoreTemplates(String backupPath) {
        return storage.restoreTemplates(backupPath);
    }

    /**
     * 导出模板到指定文件
     */
    public boolean exportTemplates(String filePath) {
        try {
            List<PromptTemplate> allTemplates = storage.getAllTemplates();
            if (allTemplates.isEmpty()) {
                logger.warning("No templates to export");
                return false;
            }

            // 使用JsonTemplateStorage的备份功能，但指定具体文件名
            String json = new com.google.gson.GsonBuilder().setPrettyPrinting().create()
                    .toJson(allTemplates);

            java.nio.file.Path file = java.nio.file.Paths.get(filePath);
            java.nio.file.Files.write(file, json.getBytes(java.nio.charset.StandardCharsets.UTF_8));

            logger.info("Templates exported successfully to: " + filePath);
            return true;
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to export templates", e);
            return false;
        }
    }

    /**
     * 模板统计信息类
     */
    public static class TemplateStatistics {
        private final int totalCount;
        private final int defaultCount;
        private final int favoriteCount;
        private final double averageRating;
        private final int totalUseCount;

        public TemplateStatistics(int totalCount, int defaultCount, int favoriteCount, double averageRating,
                int totalUseCount) {
            this.totalCount = totalCount;
            this.defaultCount = defaultCount;
            this.favoriteCount = favoriteCount;
            this.averageRating = averageRating;
            this.totalUseCount = totalUseCount;
        }

        public int getTotalCount() {
            return totalCount;
        }

        public int getDefaultCount() {
            return defaultCount;
        }

        public int getFavoriteCount() {
            return favoriteCount;
        }

        public double getAverageRating() {
            return averageRating;
        }

        public int getTotalUseCount() {
            return totalUseCount;
        }
    }
}