package slideshow;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 存储位置测试类
 * 用于显示模板文件的存储位置
 */
public class StorageLocationTest {

    public static void main(String[] args) {
        System.out.println("=== 模板存储位置信息 ===");

        // 显示当前工作目录
        String currentDir = System.getProperty("user.dir");
        System.out.println("当前工作目录: " + currentDir);

        // 显示默认存储文件路径
        String defaultStorageFile = "templates.json";
        Path storagePath = Paths.get(currentDir, defaultStorageFile);
        System.out.println("默认存储文件路径: " + storagePath.toAbsolutePath());

        // 检查文件是否存在
        File storageFile = storagePath.toFile();
        System.out.println("存储文件是否存在: " + storageFile.exists());

        if (storageFile.exists()) {
            System.out.println("文件大小: " + storageFile.length() + " 字节");
            System.out.println("最后修改时间: " + new java.util.Date(storageFile.lastModified()));
        }

        // 显示备份目录
        String backupDir = "backups";
        Path backupPath = Paths.get(currentDir, backupDir);
        System.out.println("备份目录路径: " + backupPath.toAbsolutePath());
        System.out.println("备份目录是否存在: " + backupPath.toFile().exists());

        // 列出项目根目录下的所有文件
        System.out.println("\n=== 项目根目录文件列表 ===");
        File projectDir = new File(currentDir);
        File[] files = projectDir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isFile()) {
                    System.out.println("文件: " + file.getName() + " (" + file.length() + " 字节)");
                } else if (file.isDirectory()) {
                    System.out.println("目录: " + file.getName());
                }
            }
        }

        System.out.println("\n=== 测试完成 ===");
    }
}