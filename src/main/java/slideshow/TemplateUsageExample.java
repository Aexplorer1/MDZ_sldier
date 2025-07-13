package slideshow;

import slideshow.model.PromptTemplate;
import slideshow.util.TemplateManager;
import slideshow.model.TemplateCategory;
import java.util.List;
import java.util.Optional;

/**
 * ģ��ʹ��ʾ����
 * չʾ���ʹ����ʾ��ģ�塢��¼ʹ�ô���������
 */
public class TemplateUsageExample {

    public static void main(String[] args) {
        System.out.println("=== ģ��ʹ��ʾ�� ===");

        // ����ģ�������
        TemplateManager templateManager = new TemplateManager();

        // ʾ��1���鿴����ģ��
        System.out.println("\n1. �鿴����ģ��:");
        List<PromptTemplate> allTemplates = templateManager.getAllTemplates();
        for (PromptTemplate template : allTemplates) {
            System.out.println("  - " + template.getName() + " (ID: " + template.getId() + ")");
            System.out.println("    ����: " + template.getCategory().getDisplayName());
            System.out.println("    ʹ�ô���: " + template.getMetadata().getUseCount());
            System.out.println("    ����: " + template.getMetadata().getAverageRating());
            System.out.println("    �ղ�: " + template.getMetadata().isFavorite());
        }

        // ʾ��2��ʹ��ģ�壨����ʹ�ô�����
        System.out.println("\n2. ʹ��ģ��ʾ��:");
        if (!allTemplates.isEmpty()) {
            PromptTemplate firstTemplate = allTemplates.get(0);
            String templateId = firstTemplate.getId();

            System.out.println("  ʹ��ģ��: " + firstTemplate.getName());
            System.out.println("  ʹ��ǰ����: " + firstTemplate.getMetadata().getUseCount());

            // ʹ��ģ�壨�������ʹ�ô�����
            templateManager.useTemplate(templateId);

            // ���»�ȡģ���Բ鿴���º��ʹ�ô���
            Optional<PromptTemplate> updatedTemplate = templateManager.getTemplate(templateId);
            if (updatedTemplate.isPresent()) {
                System.out.println("  ʹ�ú����: " + updatedTemplate.get().getMetadata().getUseCount());
            }
        }

        // ʾ��3��Ϊģ������
        System.out.println("\n3. ģ������ʾ��:");
        if (!allTemplates.isEmpty()) {
            PromptTemplate firstTemplate = allTemplates.get(0);
            String templateId = firstTemplate.getId();

            System.out.println("  Ϊģ������: " + firstTemplate.getName());
            System.out.println("  ����ǰ: " + firstTemplate.getMetadata().getAverageRating());

            // Ϊģ�����֣�1-5�֣�
            boolean ratingSuccess = templateManager.rateTemplate(templateId, 4.5);
            if (ratingSuccess) {
                Optional<PromptTemplate> ratedTemplate = templateManager.getTemplate(templateId);
                if (ratedTemplate.isPresent()) {
                    System.out.println("  ���ֺ�: " + ratedTemplate.get().getMetadata().getAverageRating());
                    System.out.println("  ���ִ���: " + ratedTemplate.get().getMetadata().getRatingCount());
                }
            }
        }

        // ʾ��4���ղ�ģ��
        System.out.println("\n4. �ղ�ģ��ʾ��:");
        if (!allTemplates.isEmpty()) {
            PromptTemplate firstTemplate = allTemplates.get(0);
            String templateId = firstTemplate.getId();

            System.out.println("  �ղ�ģ��: " + firstTemplate.getName());
            System.out.println("  �ղ�ǰ״̬: " + firstTemplate.getMetadata().isFavorite());

            // �л��ղ�״̬
            boolean favoriteSuccess = templateManager.toggleFavorite(templateId);
            if (favoriteSuccess) {
                Optional<PromptTemplate> favoritedTemplate = templateManager.getTemplate(templateId);
                if (favoritedTemplate.isPresent()) {
                    System.out.println("  �ղغ�״̬: " + favoritedTemplate.get().getMetadata().isFavorite());
                }
            }
        }

        // ʾ��5���鿴ͳ����Ϣ
        System.out.println("\n5. ģ��ͳ����Ϣ:");
        TemplateManager.TemplateStatistics stats = templateManager.getStatistics();
        System.out.println("  ��ģ����: " + stats.getTotalCount());
        System.out.println("  Ĭ��ģ����: " + stats.getDefaultCount());
        System.out.println("  �ղ�ģ����: " + stats.getFavoriteCount());
        System.out.println("  ƽ������: " + stats.getAverageRating());
        System.out.println("  ��ʹ�ô���: " + stats.getTotalUseCount());

        // ʾ��6������ģ��
        System.out.println("\n6. ����ģ��ʾ��:");
        List<PromptTemplate> searchResults = templateManager.searchTemplates("speech");
        System.out.println("  ����'speech'�Ľ������: " + searchResults.size());
        for (PromptTemplate result : searchResults) {
            System.out.println("    - " + result.getName());
        }

        // ʾ��7���������ȡģ��
        System.out.println("\n7. �������ȡģ��:");
        List<PromptTemplate> outlineTemplates = templateManager
                .getTemplatesByCategory(TemplateCategory.PPT_OUTLINE);
        System.out.println("  PPT�������ģ������: " + outlineTemplates.size());
        for (PromptTemplate template : outlineTemplates) {
            System.out.println("    - " + template.getName());
        }

        System.out.println("\n=== ʾ����� ===");
    }
}