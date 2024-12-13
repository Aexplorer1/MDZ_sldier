package slideshow.presentation;

import javafx.stage.Stage;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.scene.canvas.Canvas;
import javafx.scene.input.KeyCode;
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
                case RIGHT: case SPACE: nextSlide(); break;
                case LEFT: previousSlide(); break;
                case ESCAPE: stage.close(); break;
            }
        });
        
        stage.setScene(scene);
        stage.setFullScreen(true);
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