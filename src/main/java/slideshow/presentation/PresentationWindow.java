package slideshow.presentation;

import javafx.stage.Stage;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.scene.canvas.Canvas;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCombination;
import slideshow.model.Slide;
import slideshow.util.Constants;
import java.util.List;

public class PresentationWindow {
    private Stage stage;
    private Canvas canvas;
    private List<Slide> slides;
    private int currentIndex = 0;

    public PresentationWindow(List<Slide> slides) {
        this.slides = slides;
        stage = new Stage();
        canvas = new Canvas(Constants.DEFAULT_SLIDE_WIDTH, Constants.DEFAULT_SLIDE_HEIGHT);

        StackPane root = new StackPane(canvas);
        Scene scene = new Scene(root);

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

        stage.setScene(scene);
        stage.setMaximized(true); // 改为最大化而不是全屏
        stage.setTitle("幻灯片演示"); // 添加窗口标题

        // 确保窗口可以正常控制
        stage.setResizable(true);
        stage.setFullScreen(false);
        stage.setFullScreenExitHint("按ESC键退出全屏");
        stage.setFullScreenExitKeyCombination(KeyCombination.NO_MATCH);

        // 添加调试信息
        System.out.println("演示窗口设置:");
        System.out.println("- 全屏模式: " + stage.isFullScreen());
        System.out.println("- 可调整大小: " + stage.isResizable());
        System.out.println("- 最大化: " + stage.isMaximized());
    }

    public void start() {
        stage.show();
        showCurrentSlide();
    }

    private void showCurrentSlide() {
        if (currentIndex >= 0 && currentIndex < slides.size()) {
            slides.get(currentIndex).draw(canvas.getGraphicsContext2D());
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