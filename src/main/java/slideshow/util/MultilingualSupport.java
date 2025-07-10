package slideshow.util;

import java.util.Locale;
import java.util.ResourceBundle;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.text.MessageFormat;

/**
 * 多语言支持系统
 * 提供国际化框架、多语言内容生成、语言切换机制和本地化支持
 */
public class MultilingualSupport {
    private static final Logger logger = Logger.getLogger(MultilingualSupport.class.getName());
    
    // 支持的语言列表
    public enum SupportedLanguage {
        CHINESE("zh", "zh_CN", "中文"),
        ENGLISH("en", "en_US", "English"),
        JAPANESE("ja", "ja_JP", "日本語"),
        KOREAN("ko", "ko_KR", "한국어"),
        FRENCH("fr", "fr_FR", "Français"),
        GERMAN("de", "de_DE", "Deutsch"),
        SPANISH("es", "es_ES", "Español"),
        RUSSIAN("ru", "ru_RU", "Русский");
        
        private final String languageCode;
        private final String localeCode;
        private final String displayName;
        
        SupportedLanguage(String languageCode, String localeCode, String displayName) {
            this.languageCode = languageCode;
            this.localeCode = localeCode;
            this.displayName = displayName;
        }
        
        public String getLanguageCode() {
            return languageCode;
        }
        
        public String getLocaleCode() {
            return localeCode;
        }
        
        public String getDisplayName() {
            return displayName;
        }
        
        public Locale getLocale() {
            String[] parts = localeCode.split("_");
            if (parts.length == 2) {
                return new Locale(parts[0], parts[1]);
            }
            return new Locale(languageCode);
        }
    }
    
    private static SupportedLanguage currentLanguage = SupportedLanguage.CHINESE;
    private static ResourceBundle resourceBundle;
    private static Map<String, ResourceBundle> resourceBundles = new HashMap<>();
    
    static {
        initializeResourceBundles();
    }
    
    /**
     * 初始化资源包
     */
    private static void initializeResourceBundles() {
        try {
            for (SupportedLanguage language : SupportedLanguage.values()) {
                try {
                    ResourceBundle bundle = ResourceBundle.getBundle("i18n.messages", language.getLocale());
                    resourceBundles.put(language.getLanguageCode(), bundle);
                } catch (Exception e) {
                    logger.log(Level.WARNING, "无法加载语言包: " + language.getDisplayName(), e);
                }
            }
            
            // 设置默认语言包
            resourceBundle = resourceBundles.get(currentLanguage.getLanguageCode());
            if (resourceBundle == null) {
                resourceBundle = ResourceBundle.getBundle("i18n.messages", new Locale("zh", "CN"));
            }
            
        } catch (Exception e) {
            logger.log(Level.SEVERE, "初始化多语言支持失败", e);
        }
    }
    
    /**
     * 切换语言
     * 
     * @param language 目标语言
     */
    public static void switchLanguage(SupportedLanguage language) {
        try {
            currentLanguage = language;
            resourceBundle = resourceBundles.get(language.getLanguageCode());
            
            if (resourceBundle == null) {
                logger.warning("语言包未找到，使用默认语言包");
                resourceBundle = ResourceBundle.getBundle("i18n.messages", new Locale("zh", "CN"));
            }
            
            logger.info("语言已切换到: " + language.getDisplayName());
            
        } catch (Exception e) {
            logger.log(Level.SEVERE, "切换语言失败", e);
        }
    }
    
    /**
     * 获取当前语言
     */
    public static SupportedLanguage getCurrentLanguage() {
        return currentLanguage;
    }
    
    /**
     * 获取本地化字符串
     * 
     * @param key 字符串键
     * @return 本地化字符串
     */
    public static String getString(String key) {
        try {
            if (resourceBundle != null && resourceBundle.containsKey(key)) {
                return resourceBundle.getString(key);
            }
            return key; // 如果找不到，返回键名
        } catch (Exception e) {
            logger.log(Level.WARNING, "获取本地化字符串失败: " + key, e);
            return key;
        }
    }
    
    /**
     * 获取带参数的本地化字符串
     * 
     * @param key 字符串键
     * @param args 参数
     * @return 格式化后的本地化字符串
     */
    public static String getString(String key, Object... args) {
        try {
            String pattern = getString(key);
            return MessageFormat.format(pattern, args);
        } catch (Exception e) {
            logger.log(Level.WARNING, "格式化本地化字符串失败: " + key, e);
            return key;
        }
    }
    
    /**
     * 生成多语言PPT内容
     * 
     * @param originalContent 原始内容
     * @param targetLanguage 目标语言
     * @return 翻译后的内容
     */
    public static String generateMultilingualContent(String originalContent, SupportedLanguage targetLanguage) {
        try {
            logger.info("开始生成多语言内容，目标语言: " + targetLanguage.getDisplayName());
            
            // 这里可以集成翻译API或使用本地翻译词典
            String translatedContent = translateContent(originalContent, targetLanguage);
            
            logger.info("多语言内容生成完成");
            return translatedContent;
            
        } catch (Exception e) {
            logger.log(Level.SEVERE, "生成多语言内容失败", e);
            return originalContent;
        }
    }
    
    /**
     * 翻译内容（改进版本，支持更多词汇和多次翻译）
     */
    private static String translateContent(String content, SupportedLanguage targetLanguage) {
        // 改进的翻译实现，支持更多词汇和多次翻译
        Map<String, Map<String, String>> translationDictionary = getTranslationDictionary();
        
        if (translationDictionary.containsKey(targetLanguage.getLanguageCode())) {
            Map<String, String> translations = translationDictionary.get(targetLanguage.getLanguageCode());
            
            String translatedContent = content;
            
            // 按长度排序，优先翻译较长的词汇
            List<String> sortedKeys = new ArrayList<>(translations.keySet());
            sortedKeys.sort((a, b) -> Integer.compare(b.length(), a.length()));
            
            for (String key : sortedKeys) {
                String value = translations.get(key);
                if (value != null && !value.isEmpty()) {
                    // 使用正则表达式进行更精确的替换
                    String regex = "\\b" + java.util.regex.Pattern.quote(key) + "\\b";
                    translatedContent = translatedContent.replaceAll(regex, value);
                }
            }
            
            // 如果翻译后内容没有变化，尝试使用AI翻译（如果有的话）
            if (translatedContent.equals(content)) {
                logger.info("本地词典翻译无变化，尝试其他翻译方法");
                // 这里可以添加AI翻译API调用
                translatedContent = tryAITranslation(content, targetLanguage);
            }
            
            return translatedContent;
        }
        
        return content;
    }
    
    /**
     * 尝试AI翻译（预留接口）
     */
    private static String tryAITranslation(String content, SupportedLanguage targetLanguage) {
        // 这里可以集成AI翻译API
        // 目前返回原内容，实际应用中应该调用翻译API
        logger.info("AI翻译功能待实现");
        return content;
    }
    
    /**
     * 调用AI大模型翻译API（伪实现，可扩展为真实API调用）
     */
    public static String callAITranslationAPI(String content, SupportedLanguage targetLanguage) {
        // TODO: 这里可集成OpenAI/DeepSeek等API，需配置API KEY
        // 伪实现：返回“[AI翻译]”前缀+原文+目标语言
        try {
            Thread.sleep(1200); // 模拟网络延迟
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return "[AI翻译-" + targetLanguage.getDisplayName() + "] " + content;
    }
    
    /**
     * 获取翻译词典（简化版本）
     */
    private static Map<String, Map<String, String>> getTranslationDictionary() {
        Map<String, Map<String, String>> dictionary = new HashMap<>();
        
        // 英文翻译
        Map<String, String> englishTranslations = new HashMap<>();
        englishTranslations.put("标题", "Title");
        englishTranslations.put("副标题", "Subtitle");
        englishTranslations.put("要点", "Bullet Point");
        englishTranslations.put("绘图", "Drawing");
        englishTranslations.put("图片", "Image");
        englishTranslations.put("新建幻灯片", "New Slide");
        englishTranslations.put("添加文本", "Add Text");
        englishTranslations.put("添加图片", "Add Image");
        englishTranslations.put("人工智能", "Artificial Intelligence");
        englishTranslations.put("机器学习", "Machine Learning");
        englishTranslations.put("深度学习", "Deep Learning");
        englishTranslations.put("自然语言处理", "Natural Language Processing");
        englishTranslations.put("计算机视觉", "Computer Vision");
        englishTranslations.put("应用领域", "Applications");
        englishTranslations.put("医疗诊断", "Medical Diagnosis");
        englishTranslations.put("自动驾驶", "Autonomous Driving");
        englishTranslations.put("智能客服", "Intelligent Customer Service");
        englishTranslations.put("金融风控", "Financial Risk Control");
        dictionary.put("en", englishTranslations);
        
        // 日文翻译
        Map<String, String> japaneseTranslations = new HashMap<>();
        japaneseTranslations.put("标题", "タイトル");
        japaneseTranslations.put("副标题", "サブタイトル");
        japaneseTranslations.put("要点", "要点");
        japaneseTranslations.put("绘图", "図形");
        japaneseTranslations.put("图片", "画像");
        japaneseTranslations.put("新建幻灯片", "新しいスライド");
        japaneseTranslations.put("添加文本", "テキストを追加");
        japaneseTranslations.put("添加图片", "画像を追加");
        japaneseTranslations.put("人工智能", "人工知能");
        japaneseTranslations.put("机器学习", "機械学習");
        japaneseTranslations.put("深度学习", "ディープラーニング");
        japaneseTranslations.put("自然语言处理", "自然言語処理");
        japaneseTranslations.put("计算机视觉", "コンピュータビジョン");
        japaneseTranslations.put("应用领域", "応用分野");
        japaneseTranslations.put("医疗诊断", "医療診断");
        japaneseTranslations.put("自动驾驶", "自動運転");
        japaneseTranslations.put("智能客服", "インテリジェントカスタマーサービス");
        japaneseTranslations.put("金融风控", "金融リスク管理");
        dictionary.put("ja", japaneseTranslations);
        
        // 韩文翻译
        Map<String, String> koreanTranslations = new HashMap<>();
        koreanTranslations.put("标题", "제목");
        koreanTranslations.put("副标题", "부제목");
        koreanTranslations.put("要点", "요점");
        koreanTranslations.put("绘图", "그림");
        koreanTranslations.put("图片", "이미지");
        koreanTranslations.put("新建幻灯片", "새 슬라이드");
        koreanTranslations.put("添加文本", "텍스트 추가");
        koreanTranslations.put("添加图片", "이미지 추가");
        koreanTranslations.put("人工智能", "인공지능");
        koreanTranslations.put("机器学习", "머신러닝");
        koreanTranslations.put("深度学习", "딥러닝");
        koreanTranslations.put("自然语言处理", "자연어 처리");
        koreanTranslations.put("计算机视觉", "컴퓨터 비전");
        koreanTranslations.put("应用领域", "응용 분야");
        koreanTranslations.put("医疗诊断", "의료 진단");
        koreanTranslations.put("自动驾驶", "자율주행");
        koreanTranslations.put("智能客服", "지능형 고객 서비스");
        koreanTranslations.put("金融风控", "금융 리스크 관리");
        dictionary.put("ko", koreanTranslations);
        
        return dictionary;
    }
    
    /**
     * 生成多语言PPT命令
     * 
     * @param originalCommands 原始PPT命令
     * @param targetLanguage 目标语言
     * @return 翻译后的PPT命令
     */
    public static String generateMultilingualPPTCommands(String originalCommands, SupportedLanguage targetLanguage) {
        try {
            logger.info("开始生成多语言PPT命令，目标语言: " + targetLanguage.getDisplayName());
            
            // 解析PPT命令
            String[] lines = originalCommands.split("\n");
            StringBuilder translatedCommands = new StringBuilder();
            
            for (String line : lines) {
                String translatedLine = translatePPTCommandLine(line, targetLanguage);
                translatedCommands.append(translatedLine).append("\n");
            }
            
            logger.info("多语言PPT命令生成完成");
            return translatedCommands.toString();
            
        } catch (Exception e) {
            logger.log(Level.SEVERE, "生成多语言PPT命令失败", e);
            return originalCommands;
        }
    }
    
    /**
     * 翻译PPT命令行（不输出结构字段，只返回内容）
     */
    private static String translatePPTCommandLine(String line, SupportedLanguage targetLanguage) {
        if (line.trim().isEmpty()) {
            return line;
        }
        
        // 处理PPT命令格式，只返回内容部分，不输出结构字段
        if (line.startsWith("Page ")) {
            return ""; // 页码不需要翻译，也不显示
        }
        
        if (line.startsWith("Title:")) {
            String title = line.substring(6).trim();
            String translatedTitle = translateContent(title, targetLanguage);
            return translatedTitle; // 只返回翻译后的内容，不包含"Title: "
        }
        
        if (line.startsWith("Subtitle:")) {
            String subtitle = line.substring(9).trim();
            String translatedSubtitle = translateContent(subtitle, targetLanguage);
            return translatedSubtitle; // 只返回翻译后的内容，不包含"Subtitle: "
        }
        
        if (line.startsWith("Bullet:")) {
            String bullet = line.substring(7).trim();
            String translatedBullet = translateContent(bullet, targetLanguage);
            return translatedBullet; // 只返回翻译后的内容，不包含"Bullet: "
        }
        
        if (line.startsWith("Draw:")) {
            return ""; // 绘图命令不需要翻译，也不显示
        }
        
        if (line.startsWith("Image:")) {
            return ""; // 图片命令不需要翻译，也不显示
        }
        
        // 其他内容进行翻译
        return translateContent(line, targetLanguage);
    }
    
    /**
     * 获取支持的语言列表
     */
    public static List<SupportedLanguage> getSupportedLanguages() {
        List<SupportedLanguage> languages = new ArrayList<>();
        for (SupportedLanguage language : SupportedLanguage.values()) {
            languages.add(language);
        }
        return languages;
    }
    
    /**
     * 检查是否支持指定语言
     */
    public static boolean isLanguageSupported(String languageCode) {
        for (SupportedLanguage language : SupportedLanguage.values()) {
            if (language.getLanguageCode().equals(languageCode)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * 根据语言代码获取语言枚举
     */
    public static SupportedLanguage getLanguageByCode(String languageCode) {
        for (SupportedLanguage language : SupportedLanguage.values()) {
            if (language.getLanguageCode().equals(languageCode)) {
                return language;
            }
        }
        return SupportedLanguage.CHINESE; // 默认返回中文
    }
} 