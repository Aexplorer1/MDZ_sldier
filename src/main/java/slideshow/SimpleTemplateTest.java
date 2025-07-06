package slideshow;

import slideshow.model.PromptTemplate;
import slideshow.model.TemplateCategory;
import slideshow.util.TemplateManager;
import slideshow.util.TemplateManager.TemplateStatistics;

import java.util.List;
import java.util.Optional;

/**
 * Simple Template Test Class
 * For testing template functionality without encoding issues
 */
public class SimpleTemplateTest {

    public static void main(String[] args) {
        System.out.println("=== Template System Test ===");

        TemplateManager manager = new TemplateManager();

        // Test creating templates
        testCreateTemplate(manager);

        // Test searching templates
        testSearchTemplate(manager);

        // Test category filtering
        testCategoryFilter(manager);

        // Test statistics
        testStatistics(manager);

        System.out.println("=== Test Completed ===");
    }

    private static void testCreateTemplate(TemplateManager manager) {
        System.out.println("\n--- Testing Template Creation ---");

        // Create PPT outline template
        boolean success1 = manager.createTemplate(
                "Academic PPT Generator",
                "Generate academic conference PPT outlines",
                "You are a professional academic PPT assistant. Please generate an academic PPT outline based on the following content:\n"
                        +
                        "Requirements:\n" +
                        "1. Professional and rigorous structure\n" +
                        "2. Clear slide organization and logical flow\n" +
                        "3. Include research background, methods, results, discussion slides\n" +
                        "4. Suitable for {0} minutes presentation\n" +
                        "5. Output in Chinese\n\n" +
                        "IMPORTANT: You must output in the following standard PPT format:\n" +
                        "---PPT√¸¡Ó---\n" +
                        "Page 1:\n" +
                        "Title: [Page Title]\n" +
                        "Subtitle: [Page Subtitle]\n" +
                        "Bullet: [Bullet Point Content]\n" +
                        "Draw: [Drawing Description]\n" +
                        "(Continue for more pages...)\n\n" +
                        "Research content: {1}\n\n" +
                        "Please generate the PPT outline:",
                TemplateCategory.PPT_OUTLINE);

        System.out.println("Create academic speech template: " + (success1 ? "SUCCESS" : "FAILED"));

        // Create business PPT template
        boolean success2 = manager.createTemplate(
                "Business PPT Generator",
                "Generate professional business presentation outlines",
                "You are a business PPT assistant. Please generate a business PPT outline based on the following content:\n"
                        +
                        "Business Topic: {0}\n" +
                        "Target Audience: {1}\n\n" +
                        "Requirements:\n" +
                        "1. Professional business structure\n" +
                        "2. Clear objectives and key points\n" +
                        "3. Include executive summary, market analysis, strategy slides\n" +
                        "4. Suitable for business presentations\n" +
                        "5. Output in Chinese\n\n" +
                        "IMPORTANT: You must output in the following standard PPT format:\n" +
                        "---PPT√¸¡Ó---\n" +
                        "Page 1:\n" +
                        "Title: [Page Title]\n" +
                        "Subtitle: [Page Subtitle]\n" +
                        "Bullet: [Bullet Point Content]\n" +
                        "Draw: [Drawing Description]\n" +
                        "(Continue for more pages...)\n\n" +
                        "Please generate the business PPT outline:",
                TemplateCategory.PPT_BUSINESS);

        System.out.println("Create business email optimizer template: " + (success2 ? "SUCCESS" : "FAILED"));

        // Test duplicate name
        boolean success3 = manager.createTemplate(
                "Academic PPT Generator",
                "Duplicate name template",
                "This is a duplicate name template",
                TemplateCategory.PPT_OUTLINE);

        System.out.println("Create duplicate name template: " + (success3 ? "SUCCESS" : "FAILED"));
    }

    private static void testSearchTemplate(TemplateManager manager) {
        System.out.println("\n--- Testing Template Search ---");

        // Search for templates containing "speech"
        List<PromptTemplate> speechResults = manager.searchTemplates("speech");
        System.out.println("Search 'speech' results count: " + speechResults.size());
        for (PromptTemplate template : speechResults) {
            System.out.println("  - " + template.getName() + " (" + template.getCategory().getDisplayName() + ")");
        }

        // Search for templates containing "optimize"
        List<PromptTemplate> optimizeResults = manager.searchTemplates("optimize");
        System.out.println("Search 'optimize' results count: " + optimizeResults.size());
        for (PromptTemplate template : optimizeResults) {
            System.out.println("  - " + template.getName() + " (" + template.getCategory().getDisplayName() + ")");
        }
    }

    private static void testCategoryFilter(TemplateManager manager) {
        System.out.println("\n--- Testing Category Filter ---");

        // Get PPT outline templates
        List<PromptTemplate> outlineTemplates = manager.getTemplatesByCategory(TemplateCategory.PPT_OUTLINE);
        System.out.println("PPT outline category templates count: " + outlineTemplates.size());
        for (PromptTemplate template : outlineTemplates) {
            System.out.println("  - " + template.getName());
        }

        // Get business PPT templates
        List<PromptTemplate> businessTemplates = manager.getTemplatesByCategory(TemplateCategory.PPT_BUSINESS);
        System.out.println("Business PPT category templates count: " + businessTemplates.size());
        for (PromptTemplate template : businessTemplates) {
            System.out.println("  - " + template.getName());
        }
    }

    private static void testStatistics(TemplateManager manager) {
        System.out.println("\n--- Testing Statistics ---");

        TemplateStatistics stats = manager.getStatistics();
        System.out.println("Template Statistics:");
        System.out.println("  Total count: " + stats.getTotalCount());
        System.out.println("  Default templates: " + stats.getDefaultCount());
        System.out.println("  Favorite templates: " + stats.getFavoriteCount());
        System.out.println("  Average rating: " + stats.getAverageRating());
        System.out.println("  Total use count: " + stats.getTotalUseCount());
    }
}