package slideshow;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.ToolBar;
import javafx.scene.layout.BorderPane;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.scene.input.MouseEvent;
import slideshow.util.Constants;
import slideshow.model.Slide;
import slideshow.elements.TextElement;
import slideshow.elements.SlideElement;

public class Main extends Application {
    private Canvas canvas;
    private GraphicsContext gc;
    private Slide currentSlide;
    private TextElement selectedText;
    
    @Override
    public void start(Stage primaryStage) {
        BorderPane root = new BorderPane();
        
        // 创建工具栏
        ToolBar toolBar = createToolBar();
        root.setTop(toolBar);
        
        // 创建画布
        canvas = new Canvas(Constants.DEFAULT_SLIDE_WIDTH, Constants.DEFAULT_SLIDE_HEIGHT);
        gc = canvas.getGraphicsContext2D();
        
        // 添加鼠标事件处理
        canvas.setOnMousePressed(this::handleMousePressed);
        canvas.setOnMouseDragged(this::handleMouseDragged);
        canvas.setOnMouseReleased(this::handleMouseReleased);
        
        // 将画布放在中心
        BorderPane canvasHolder = new BorderPane(canvas);
        canvasHolder.setStyle("-fx-background-color: #f0f0f0;");
        root.setCenter(canvasHolder);
        
        Scene scene = new Scene(root, Constants.DEFAULT_WINDOW_WIDTH, Constants.DEFAULT_WINDOW_HEIGHT);
        primaryStage.setTitle("幻灯片编辑器");
        primaryStage.setScene(scene);
        primaryStage.show();
        
        // 创建初始幻灯片
        createNewSlide();
    }
    
    private void handleMousePressed(MouseEvent event) {
        if (currentSlide != null) {
            SlideElement element = currentSlide.findElementAt(event.getX(), event.getY());
            if (element instanceof TextElement) {
                selectedText = (TextElement) element;
                selectedText.setSelected(true);
                refreshCanvas();
            }
        }
    }
    
    private void handleMouseDragged(MouseEvent event) {
        if (selectedText != null) {
            selectedText.move(event.getX(), event.getY());
            refreshCanvas();
        }
    }
    
    private void handleMouseReleased(MouseEvent event) {
        if (selectedText != null) {
            selectedText.setSelected(false);
            selectedText = null;
            refreshCanvas();
        }
    }
    
    private void refreshCanvas() {
        // 清空画布
        gc.setFill(Color.WHITE);
        gc.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());
        
        // 重新绘制所有元素
        if (currentSlide != null) {
            currentSlide.draw(gc);
        }
    }
    
    private ToolBar createToolBar() {
        Button newSlideBtn = new Button("新建幻灯片");
        Button addTextBtn = new Button("添加文本");
        Button addImageBtn = new Button("添加图片");
        
        newSlideBtn.setOnAction(e -> createNewSlide());
        addTextBtn.setOnAction(e -> addText());
        addImageBtn.setOnAction(e -> addImage());
        
        return new ToolBar(newSlideBtn, addTextBtn, addImageBtn);
    }
    
    private void createNewSlide() {
        currentSlide = new Slide();
        refreshCanvas();
    }
    
    private void addText() {
        TextInputDialog dialog = new TextInputDialog("新文本");
        dialog.setTitle("添加文本");
        dialog.setHeaderText("请输入文本内容：");
        dialog.setContentText("文本：");
        
        dialog.showAndWait().ifPresent(text -> {
            TextElement textElement = new TextElement(
                canvas.getWidth() / 2,
                canvas.getHeight() / 2,
                text
            );
            currentSlide.addElement(textElement);
            refreshCanvas();
        });
    }
    
    private void addImage() {
        // TODO: 实现添加图片功能
    }

    public static void main(String[] args) {
        launch(args);
    }
} 