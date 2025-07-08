package slideshow;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

/**
 * 窗口最小化功能测试类
 */
public class WindowMinimizeTest extends Application {
    
    private Dialog<String> testDialog;
    
    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("窗口最小化测试");
        
        VBox root = new VBox(10);
        root.setPadding(new Insets(10));
        
        Button openDialogBtn = new Button("打开测试对话框");
        openDialogBtn.setOnAction(e -> showTestDialog());
        
        Button minimizeBtn = new Button("最小化对话框");
        minimizeBtn.setOnAction(e -> minimizeDialog());
        
        Button restoreBtn = new Button("恢复对话框");
        restoreBtn.setOnAction(e -> restoreDialog());
        
        root.getChildren().addAll(openDialogBtn, minimizeBtn, restoreBtn);
        
        Scene scene = new Scene(root, 300, 200);
        primaryStage.setScene(scene);
        primaryStage.show();
    }
    
    private void showTestDialog() {
        testDialog = new Dialog<>();
        testDialog.setTitle("测试对话框");
        testDialog.setHeaderText("这是一个测试对话框");
        
        ButtonType minimizeBtnType = new ButtonType("最小化", ButtonBar.ButtonData.OTHER);
        ButtonType closeBtnType = new ButtonType("关闭", ButtonBar.ButtonData.CANCEL_CLOSE);
        testDialog.getDialogPane().getButtonTypes().addAll(minimizeBtnType, closeBtnType);
        
        TextArea contentArea = new TextArea("这是一个测试内容区域，用于验证最小化功能。\n\n" +
                "功能说明：\n" +
                "• 点击'最小化'按钮可以将对话框最小化\n" +
                "• 最小化后可以操作主窗口\n" +
                "• 可以通过任务栏或主窗口按钮恢复对话框");
        contentArea.setPrefRowCount(8);
        contentArea.setPrefColumnCount(40);
        contentArea.setWrapText(true);
        contentArea.setEditable(false);
        
        testDialog.getDialogPane().setContent(contentArea);
        
        // 最小化按钮逻辑
        Button minimizeBtn = (Button) testDialog.getDialogPane().lookupButton(minimizeBtnType);
        minimizeBtn.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            event.consume();
            minimizeDialog();
        });
        
        testDialog.showAndWait();
    }
    
    private void minimizeDialog() {
        if (testDialog != null) {
            try {
                Stage dialogStage = (Stage) testDialog.getDialogPane().getScene().getWindow();
                dialogStage.setIconified(true);
                System.out.println("对话框已最小化");
            } catch (Exception e) {
                System.err.println("最小化失败: " + e.getMessage());
            }
        } else {
            System.out.println("没有打开的对话框");
        }
    }
    
    private void restoreDialog() {
        if (testDialog != null) {
            try {
                Stage dialogStage = (Stage) testDialog.getDialogPane().getScene().getWindow();
                if (dialogStage.isIconified()) {
                    dialogStage.setIconified(false);
                    dialogStage.toFront();
                    System.out.println("对话框已恢复");
                } else {
                    System.out.println("对话框已经是正常状态");
                }
            } catch (Exception e) {
                System.err.println("恢复失败: " + e.getMessage());
            }
        } else {
            System.out.println("没有打开的对话框");
        }
    }
    
    public static void main(String[] args) {
        launch(args);
    }
} 