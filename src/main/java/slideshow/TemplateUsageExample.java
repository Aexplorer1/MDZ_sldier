package slideshow;

import slideshow.model.PromptTemplate;
import slideshow.util.TemplateManager;
import slideshow.model.TemplateCategory;
import java.util.List;
import java.util.Optional;

/**
 * 模板使用示例类
 * 展示如何使用提示词模板、记录使用次数和评分
 */
public class TemplateUsageExample {

    public static void main(String[] args) {
        System.out.println("=== 模板使用示例 ===");

        // 创建模板管理器
        TemplateManager templateManager = new TemplateManager();

        // 示例1：查看所有模板
        System.out.println("\n1. 查看所有模板:");
        List<PromptTemplate> allTemplates = templateManager.getAllTemplates();
        for (PromptTemplate template : allTemplates) {
            System.out.println("  - " + template.getName() + " (ID: " + template.getId() + ")");
            System.out.println("    分类: " + template.getCategory().getDisplayName());
            System.out.println("    使用次数: " + template.getMetadata().getUseCount());
            System.out.println("    评分: " + template.getMetadata().getAverageRating());
            System.out.println("    收藏: " + template.getMetadata().isFavorite());
        }

        // 示例2：使用模板（增加使用次数）
        System.out.println("\n2. 使用模板示例:");
        if (!allTemplates.isEmpty()) {
            PromptTemplate firstTemplate = allTemplates.get(0);
            String templateId = firstTemplate.getId();

            System.out.println("  使用模板: " + firstTemplate.getName());
            System.out.println("  使用前次数: " + firstTemplate.getMetadata().getUseCount());

            // 使用模板（这会增加使用次数）
            templateManager.useTemplate(templateId);

            // 重新获取模板以查看更新后的使用次数
            Optional<PromptTemplate> updatedTemplate = templateManager.getTemplate(templateId);
            if (updatedTemplate.isPresent()) {
                System.out.println("  使用后次数: " + updatedTemplate.get().getMetadata().getUseCount());
            }
        }

        // 示例3：为模板评分
        System.out.println("\n3. 模板评分示例:");
        if (!allTemplates.isEmpty()) {
            PromptTemplate firstTemplate = allTemplates.get(0);
            String templateId = firstTemplate.getId();

            System.out.println("  为模板评分: " + firstTemplate.getName());
            System.out.println("  评分前: " + firstTemplate.getMetadata().getAverageRating());

            // 为模板评分（1-5分）
            boolean ratingSuccess = templateManager.rateTemplate(templateId, 4.5);
            if (ratingSuccess) {
                Optional<PromptTemplate> ratedTemplate = templateManager.getTemplate(templateId);
                if (ratedTemplate.isPresent()) {
                    System.out.println("  评分后: " + ratedTemplate.get().getMetadata().getAverageRating());
                    System.out.println("  评分次数: " + ratedTemplate.get().getMetadata().getRatingCount());
                }
            }
        }

        // 示例4：收藏模板
        System.out.println("\n4. 收藏模板示例:");
        if (!allTemplates.isEmpty()) {
            PromptTemplate firstTemplate = allTemplates.get(0);
            String templateId = firstTemplate.getId();

            System.out.println("  收藏模板: " + firstTemplate.getName());
            System.out.println("  收藏前状态: " + firstTemplate.getMetadata().isFavorite());

            // 切换收藏状态
            boolean favoriteSuccess = templateManager.toggleFavorite(templateId);
            if (favoriteSuccess) {
                Optional<PromptTemplate> favoritedTemplate = templateManager.getTemplate(templateId);
                if (favoritedTemplate.isPresent()) {
                    System.out.println("  收藏后状态: " + favoritedTemplate.get().getMetadata().isFavorite());
                }
            }
        }

        // 示例5：查看统计信息
        System.out.println("\n5. 模板统计信息:");
        TemplateManager.TemplateStatistics stats = templateManager.getStatistics();
        System.out.println("  总模板数: " + stats.getTotalCount());
        System.out.println("  默认模板数: " + stats.getDefaultCount());
        System.out.println("  收藏模板数: " + stats.getFavoriteCount());
        System.out.println("  平均评分: " + stats.getAverageRating());
        System.out.println("  总使用次数: " + stats.getTotalUseCount());

        // 示例6：搜索模板
        System.out.println("\n6. 搜索模板示例:");
        List<PromptTemplate> searchResults = templateManager.searchTemplates("speech");
        System.out.println("  搜索'speech'的结果数量: " + searchResults.size());
        for (PromptTemplate result : searchResults) {
            System.out.println("    - " + result.getName());
        }

        // 示例7：按分类获取模板
        System.out.println("\n7. 按分类获取模板:");
        List<PromptTemplate> outlineTemplates = templateManager
                .getTemplatesByCategory(TemplateCategory.PPT_OUTLINE);
        System.out.println("  PPT大纲生成模板数量: " + outlineTemplates.size());
        for (PromptTemplate template : outlineTemplates) {
            System.out.println("    - " + template.getName());
        }

        System.out.println("\n=== 示例完成 ===");
    }
}