package slideshow;

import slideshow.model.PromptTemplate;
import slideshow.model.TemplateCategory;
import slideshow.util.TemplateManager;

/**
 * Quick Template Test
 * Simple test to verify template functionality
 */
public class QuickTemplateTest {

    public static void main(String[] args) {
        System.out.println("=== Quick Template Test ===");

        try {
            TemplateManager manager = new TemplateManager();
            System.out.println("TemplateManager created successfully");

            // Test creating a simple template
            boolean success = manager.createTemplate(
                    "Test Template",
                    "A test template",
                    "This is a test template with {0} parameter",
                    TemplateCategory.CUSTOM);

            System.out.println("Template creation: " + (success ? "SUCCESS" : "FAILED"));

            // Test getting all templates
            var templates = manager.getAllTemplates();
            System.out.println("Total templates: " + templates.size());

            // Test getting default templates
            var defaultTemplates = manager.getDefaultTemplates();
            System.out.println("Default templates: " + defaultTemplates.size());

            System.out.println("=== Test Completed Successfully ===");

        } catch (Exception e) {
            System.err.println("Test failed with error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}