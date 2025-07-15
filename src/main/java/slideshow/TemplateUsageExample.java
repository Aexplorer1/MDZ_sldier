package slideshow;

import slideshow.model.PromptTemplate;
import slideshow.util.TemplateManager;
import slideshow.model.TemplateCategory;
import java.util.List;
import java.util.Optional;

/**
 * 模板使用示例
 * 演示模板、分类、使用记录的增删改查
 */
public class TemplateUsageExample {

    public static void main(String[] args) {
        System.out.println("=== 提示词模板使用示例 ===");

        // 初始化模板管理器
        TemplateManager templateManager = new TemplateManager();

        // 示例1：查看所有模板
        System.out.println("\n1. 查看所有提示词模板:");
        List<PromptTemplate> allTemplates = templateManager.getAllTemplates();
        for (PromptTemplate template : allTemplates) {
            System.out.println("  - " + template.getName() + " (ID: " + template.getId() + ")");
            System.out.println("    分类: " + template.getCategory().getDisplayName());
            System.out.println("    使用次数: " + template.getMetadata().getUseCount());
            System.out.println("    平均评分: " + template.getMetadata().getAverageRating());
            System.out.println("    收藏: " + template.getMetadata().isFavorite());
        }

        // 示例2：使用模板（增加使用次数）
        System.out.println("\n2. 使用提示词模板:");
        if (!allTemplates.isEmpty()) {
            PromptTemplate firstTemplate = allTemplates.get(0);
            String templateId = firstTemplate.getId();

            System.out.println("  使用提示词模板: " + firstTemplate.getName());
            System.out.println("  使用前次数: " + firstTemplate.getMetadata().getUseCount());

            // 使用模板（增加使用次数）
            templateManager.useTemplate(templateId);

            // 重新获取模板查看使用次数
            Optional<PromptTemplate> updatedTemplate = templateManager.getTemplate(templateId);
            if (updatedTemplate.isPresent()) {
                System.out.println("  使用次数: " + updatedTemplate.get().getMetadata().getUseCount());
            }
        }

        // 示例3：为模板评分
        System.out.println("\n3. 为模板评分:");
        if (!allTemplates.isEmpty()) {
            PromptTemplate firstTemplate = allTemplates.get(0);
            String templateId = firstTemplate.getId();

            System.out.println("  Ϊ模板评分: " + firstTemplate.getName());
            System.out.println("  评分前: " + firstTemplate.getMetadata().getAverageRating());

            // 为模板评分（1-5分）
            boolean ratingSuccess = templateManager.rateTemplate(templateId, 4.5);
            if (ratingSuccess) {
                Optional<PromptTemplate> ratedTemplate = templateManager.getTemplate(templateId);
                if (ratedTemplate.isPresent()) {
                    System.out.println("  评分成功: " + ratedTemplate.get().getMetadata().getAverageRating());
                    System.out.println("  评分次数: " + ratedTemplate.get().getMetadata().getRatingCount());
                }
            }
        }

        // 示例4：收藏模板
        System.out.println("\n4. 收藏模板:");
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
                    System.out.println("  收藏状态: " + favoritedTemplate.get().getMetadata().isFavorite());
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
        System.out.println("\n6. 搜索模板:");
        List<PromptTemplate> searchResults = templateManager.searchTemplates("演讲");
        System.out.println("  搜索'演讲'的结果数量: " + searchResults.size());
        for (PromptTemplate result : searchResults) {
            System.out.println("    - " + result.getName());
        }

        System.out.println("\n=== 示例结束 ===");
    }
}