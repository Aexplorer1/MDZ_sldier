package slideshow.util;

import slideshow.model.PromptTemplate;
import java.util.List;
import java.util.Optional;

/**
 * 模板存储接口
 * 定义模板存储的基本操作
 */
public interface TemplateStorage {

    /**
     * 保存模板
     * 
     * @param template 要保存的模板
     * @return 是否保存成功
     */
    boolean saveTemplate(PromptTemplate template);

    /**
     * 根据ID获取模板
     * 
     * @param id 模板ID
     * @return 模板对象，如果不存在则返回空
     */
    Optional<PromptTemplate> getTemplateById(String id);

    /**
     * 根据名称获取模板
     * 
     * @param name 模板名称
     * @return 模板对象，如果不存在则返回空
     */
    Optional<PromptTemplate> getTemplateByName(String name);

    /**
     * 获取所有模板
     * 
     * @return 所有模板的列表
     */
    List<PromptTemplate> getAllTemplates();

    /**
     * 根据分类获取模板
     * 
     * @param category 模板分类
     * @return 该分类下的所有模板
     */
    List<PromptTemplate> getTemplatesByCategory(slideshow.model.TemplateCategory category);

    /**
     * 根据标签搜索模板
     * 
     * @param tag 标签
     * @return 包含该标签的所有模板
     */
    List<PromptTemplate> getTemplatesByTag(String tag);

    /**
     * 搜索模板
     * 
     * @param keyword 搜索关键词
     * @return 匹配的模板列表
     */
    List<PromptTemplate> searchTemplates(String keyword);

    /**
     * 更新模板
     * 
     * @param template 要更新的模板
     * @return 是否更新成功
     */
    boolean updateTemplate(PromptTemplate template);

    /**
     * 删除模板
     * 
     * @param id 要删除的模板ID
     * @return 是否删除成功
     */
    boolean deleteTemplate(String id);

    /**
     * 获取收藏的模板
     * 
     * @return 收藏的模板列表
     */
    List<PromptTemplate> getFavoriteTemplates();

    /**
     * 获取最常用的模板
     * 
     * @param limit 返回数量限制
     * @return 最常用的模板列表
     */
    List<PromptTemplate> getMostUsedTemplates(int limit);

    /**
     * 获取评分最高的模板
     * 
     * @param limit 返回数量限制
     * @return 评分最高的模板列表
     */
    List<PromptTemplate> getTopRatedTemplates(int limit);

    /**
     * 获取默认模板
     * 
     * @return 默认模板列表
     */
    List<PromptTemplate> getDefaultTemplates();

    /**
     * 检查模板是否存在
     * 
     * @param id 模板ID
     * @return 是否存在
     */
    boolean templateExists(String id);

    /**
     * 获取模板总数
     * 
     * @return 模板总数
     */
    int getTemplateCount();

    /**
     * 清空所有模板
     * 
     * @return 是否清空成功
     */
    boolean clearAllTemplates();

    /**
     * 备份模板数据
     * 
     * @param backupPath 备份路径
     * @return 是否备份成功
     */
    boolean backupTemplates(String backupPath);

    /**
     * 恢复模板数据
     * 
     * @param backupPath 备份文件路径
     * @return 是否恢复成功
     */
    boolean restoreTemplates(String backupPath);
}