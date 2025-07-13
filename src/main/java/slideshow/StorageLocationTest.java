package slideshow;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * �洢λ�ò�����
 * ������ʾģ���ļ��Ĵ洢λ��
 */
public class StorageLocationTest {

    public static void main(String[] args) {
        System.out.println("=== ģ��洢λ����Ϣ ===");

        // ��ʾ��ǰ����Ŀ¼
        String currentDir = System.getProperty("user.dir");
        System.out.println("��ǰ����Ŀ¼: " + currentDir);

        // ��ʾĬ�ϴ洢�ļ�·��
        String defaultStorageFile = "templates.json";
        Path storagePath = Paths.get(currentDir, defaultStorageFile);
        System.out.println("Ĭ�ϴ洢�ļ�·��: " + storagePath.toAbsolutePath());

        // ����ļ��Ƿ����
        File storageFile = storagePath.toFile();
        System.out.println("�洢�ļ��Ƿ����: " + storageFile.exists());

        if (storageFile.exists()) {
            System.out.println("�ļ���С: " + storageFile.length() + " �ֽ�");
            System.out.println("����޸�ʱ��: " + new java.util.Date(storageFile.lastModified()));
        }

        // ��ʾ����Ŀ¼
        String backupDir = "backups";
        Path backupPath = Paths.get(currentDir, backupDir);
        System.out.println("����Ŀ¼·��: " + backupPath.toAbsolutePath());
        System.out.println("����Ŀ¼�Ƿ����: " + backupPath.toFile().exists());

        // �г���Ŀ��Ŀ¼�µ������ļ�
        System.out.println("\n=== ��Ŀ��Ŀ¼�ļ��б� ===");
        File projectDir = new File(currentDir);
        File[] files = projectDir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isFile()) {
                    System.out.println("�ļ�: " + file.getName() + " (" + file.length() + " �ֽ�)");
                } else if (file.isDirectory()) {
                    System.out.println("Ŀ¼: " + file.getName());
                }
            }
        }

        System.out.println("\n=== ������� ===");
    }
}