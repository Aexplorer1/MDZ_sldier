package slideshow.util;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * 演讲稿管理器
 * 负责演讲稿文件的保存、加载和管理
 */
public class SpeechManager {
    private static final Logger logger = Logger.getLogger(SpeechManager.class.getName());
    private static final String SPEECH_DIR = "speeches";
    private static final DateTimeFormatter FILE_NAME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
    
    /**
     * 保存演讲稿到本地文件
     * 
     * @param speechContent 演讲稿内容
     * @param presentationName 演示文稿名称
     * @return 保存的文件路径，如果保存失败返回null
     */
    public static String saveSpeechToFile(String speechContent, String presentationName) {
        try {
            // 创建演讲稿目录
            Path speechDir = Paths.get(SPEECH_DIR);
            if (!Files.exists(speechDir)) {
                Files.createDirectories(speechDir);
                logger.info("创建演讲稿目录: " + speechDir.toAbsolutePath());
            }
            
            // 生成文件名
            String timestamp = LocalDateTime.now().format(FILE_NAME_FORMATTER);
            String fileName = String.format("%s_%s_speech.txt", 
                presentationName.replaceAll("[^a-zA-Z0-9\\u4e00-\\u9fa5]", "_"), 
                timestamp);
            Path filePath = speechDir.resolve(fileName);
            
            // 写入文件
            try (BufferedWriter writer = Files.newBufferedWriter(filePath, StandardCharsets.UTF_8)) {
                writer.write("演讲稿生成时间: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
                writer.newLine();
                writer.write("演示文稿: " + presentationName);
                writer.newLine();
                writer.write("=".repeat(50));
                writer.newLine();
                writer.write(speechContent);
                writer.newLine();
            }
            
            logger.info("演讲稿已保存到: " + filePath.toAbsolutePath());
            return filePath.toString();
            
        } catch (IOException e) {
            logger.log(Level.SEVERE, "保存演讲稿失败", e);
            return null;
        }
    }
    
    /**
     * 从文件加载演讲稿
     * 
     * @param filePath 文件路径
     * @return 演讲稿内容，如果加载失败返回null
     */
    public static String loadSpeechFromFile(String filePath) {
        try {
            Path path = Paths.get(filePath);
            if (!Files.exists(path)) {
                logger.warning("演讲稿文件不存在: " + filePath);
                return null;
            }
            
            String content = Files.readString(path, StandardCharsets.UTF_8);
            logger.info("演讲稿已加载: " + filePath);
            return content;
            
        } catch (IOException e) {
            logger.log(Level.SEVERE, "加载演讲稿失败", e);
            return null;
        }
    }
    
    /**
     * 获取最新的演讲稿文件
     * 
     * @return 最新演讲稿文件路径，如果没有找到返回null
     */
    public static String getLatestSpeechFile() {
        try {
            Path speechDir = Paths.get(SPEECH_DIR);
            if (!Files.exists(speechDir)) {
                return null;
            }
            
            // 查找最新的演讲稿文件
            return Files.list(speechDir)
                .filter(path -> path.toString().endsWith("_speech.txt"))
                .sorted((p1, p2) -> {
                    try {
                        return Files.getLastModifiedTime(p2).compareTo(Files.getLastModifiedTime(p1));
                    } catch (IOException e) {
                        return 0;
                    }
                })
                .findFirst()
                .map(Path::toString)
                .orElse(null);
                
        } catch (IOException e) {
            logger.log(Level.SEVERE, "查找最新演讲稿文件失败", e);
            return null;
        }
    }
    
    /**
     * 检查是否有演讲稿文件
     * 
     * @return 如果有演讲稿文件返回true，否则返回false
     */
    public static boolean hasSpeechFile() {
        return getLatestSpeechFile() != null;
    }
    
    /**
     * 清理旧的演讲稿文件（保留最近10个）
     */
    public static void cleanupOldSpeechFiles() {
        try {
            Path speechDir = Paths.get(SPEECH_DIR);
            if (!Files.exists(speechDir)) {
                return;
            }
            
            // 获取所有演讲稿文件并按修改时间排序
            var files = Files.list(speechDir)
                .filter(path -> path.toString().endsWith("_speech.txt"))
                .sorted((p1, p2) -> {
                    try {
                        return Files.getLastModifiedTime(p2).compareTo(Files.getLastModifiedTime(p1));
                    } catch (IOException e) {
                        return 0;
                    }
                })
                .toList();
            
            // 删除超过10个的旧文件
            if (files.size() > 10) {
                for (int i = 10; i < files.size(); i++) {
                    Files.delete(files.get(i));
                    logger.info("删除旧演讲稿文件: " + files.get(i));
                }
            }
            
        } catch (IOException e) {
            logger.log(Level.SEVERE, "清理旧演讲稿文件失败", e);
        }
    }
} 