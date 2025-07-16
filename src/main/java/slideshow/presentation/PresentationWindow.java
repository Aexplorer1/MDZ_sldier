package slideshow.presentation;

import javafx.stage.Stage;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCombination;
import javafx.geometry.Pos;
import javafx.geometry.Insets;
import slideshow.model.Slide;
import slideshow.util.Constants;
import java.util.List;

public class PresentationWindow {
    private Stage stage;
    private Canvas canvas;
    private List<Slide> slides;
    private int currentIndex = 0;
    private Label slideInfoLabel;

    public PresentationWindow(List<Slide> slides) {
        this.slides = slides;
        stage = new Stage();
        
        // 设置更大的画布尺寸，适合放映
        double slideWidth = 1600;
        double slideHeight = 1000;
        canvas = new Canvas(slideWidth, slideHeight);

        // 创建幻灯片信息标签
        Label slideInfoLabel = new Label();
        slideInfoLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #666; -fx-background-color: rgba(255,255,255,0.8); -fx-padding: 5px;");
        slideInfoLabel.setAlignment(Pos.CENTER);
        
        // 创建主布局
        VBox mainLayout = new VBox(10);
        mainLayout.setAlignment(Pos.CENTER);
        mainLayout.setPadding(new Insets(20));
        mainLayout.getChildren().addAll(canvas, slideInfoLabel);
        
        StackPane root = new StackPane(mainLayout);
        Scene scene = new Scene(root);
        
        // 保存标签引用，用于更新信息
        this.slideInfoLabel = slideInfoLabel;

        // 添加键盘事件处理
        scene.setOnKeyPressed(e -> {
            switch (e.getCode()) {
                case RIGHT:
                case SPACE:
                    nextSlide();
                    break;
                case LEFT:
                    previousSlide();
                    break;
                case ESCAPE:
                    stage.close();
                    break;
            }
        });

        // 添加鼠标点击事件处理
        scene.setOnMouseClicked(e -> {
            // 左键单击切换到下一页
            if (e.getButton() == javafx.scene.input.MouseButton.PRIMARY) {
                nextSlide();
            }
            // 右键单击切换到上一页
            else if (e.getButton() == javafx.scene.input.MouseButton.SECONDARY) {
                previousSlide();
            }
        });

        stage.setScene(scene);
        stage.setFullScreen(true); // 改为全屏显示
        stage.setTitle("幻灯片演示"); // 添加窗口标题

        // 确保窗口可以正常控制
        stage.setResizable(true);
        // stage.setFullScreen(false); // 不再需要，已改为全屏
        stage.setFullScreenExitHint("按ESC键退出全屏");
        stage.setFullScreenExitKeyCombination(KeyCombination.NO_MATCH);

        // 添加调试信息
        System.out.println("演示窗口设置:");
        System.out.println("- 全屏模式: " + stage.isFullScreen());
        System.out.println("- 可调整大小: " + stage.isResizable());
        System.out.println("- 最大化: " + stage.isMaximized());
        System.out.println("- 幻灯片数量: " + slides.size());
        
        // 显示操作提示
        System.out.println("放映操作提示:");
        System.out.println("- 右箭头键或空格键：下一张幻灯片");
        System.out.println("- 左箭头键：上一张幻灯片");
        System.out.println("- 鼠标左键单击：下一张幻灯片");
        System.out.println("- 鼠标右键单击：上一张幻灯片");
        System.out.println("- ESC键：退出放映");
    }

    public void start() {
        stage.show();
        showCurrentSlide();
    }

    private void showCurrentSlide() {
        if (currentIndex >= 0 && currentIndex < slides.size()) {
            // 清空画布，避免内容重叠
            canvas.getGraphicsContext2D().clearRect(0, 0, canvas.getWidth(), canvas.getHeight());
            // 设置白色背景
            canvas.getGraphicsContext2D().setFill(javafx.scene.paint.Color.WHITE);
            canvas.getGraphicsContext2D().fillRect(0, 0, canvas.getWidth(), canvas.getHeight());
            // 绘制当前幻灯片
            slides.get(currentIndex).draw(canvas.getGraphicsContext2D());
            
            // 更新幻灯片信息显示
            if (slideInfoLabel != null) {
                slideInfoLabel.setText(String.format("第 %d 张 / 共 %d 张", currentIndex + 1, slides.size()));
            }
            
            // 添加调试信息
            System.out.println("显示幻灯片 " + (currentIndex + 1) + "/" + slides.size());
        }
    }

    private void nextSlide() {
        if (currentIndex < slides.size() - 1) {
            currentIndex++;
            showCurrentSlide();
        }
    }

    private void previousSlide() {
        if (currentIndex > 0) {
            currentIndex--;
            showCurrentSlide();
        }
    }
}