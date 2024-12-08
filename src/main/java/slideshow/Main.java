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
import javafx.scene.control.ColorPicker;
import javafx.scene.control.ComboBox;
import javafx.scene.text.FontWeight;
import javafx.scene.control.Separator;
import javafx.scene.control.Label;
import slideshow.elements.SlideElement;
import java.util.ArrayList;
import java.util.List;

public class Main extends Application {
    private Canvas canvas;
    private GraphicsContext gc;
    private Slide currentSlide;
    private SlideElement selectedElement;
    private double lastMouseX;
    private double lastMouseY;
    private List<Slide> slides = new ArrayList<>();
    private int currentSlideIndex = -1;
    private Button prevSlideBtn;
    private Button nextSlideBtn;
    private Label slideCountLabel;
    
    @Override
    public void start(Stage primaryStage) {
        BorderPane root = new BorderPane();
        
        // 先创建画布
        canvas = new Canvas(Constants.DEFAULT_SLIDE_WIDTH, Constants.DEFAULT_SLIDE_HEIGHT);
        gc = canvas.getGraphicsContext2D();
        
        // 再创建工具栏
        ToolBar toolBar = createToolBar();
        root.setTop(toolBar);
        
        // 添加鼠标事件处理
        canvas.setOnMousePressed(this::handleMousePressed);
        canvas.setOnMouseDragged(this::handleMouseDragged);
        canvas.setOnMouseReleased(this::handleMouseReleased);
        canvas.setOnMouseMoved(this::handleMouseMoved);
        
        // 将画布放在中心
        BorderPane canvasHolder = new BorderPane(canvas);
        canvasHolder.setStyle("-fx-background-color: #f0f0f0;");
        canvasHolder.setPadding(new Insets(20));
        root.setCenter(canvasHolder);
        
        Scene scene = new Scene(root, Constants.DEFAULT_WINDOW_WIDTH, Constants.DEFAULT_WINDOW_HEIGHT);
        primaryStage.setTitle("MDZ_Slider");
        primaryStage.setScene(scene);
        primaryStage.show();
        
        // 创建初始幻灯片
        createNewSlide();
    }
    
    private void handleMousePressed(MouseEvent event) {
        if (currentSlide != null) {
            SlideElement clickedElement = currentSlide.findElementAt(event.getX(), event.getY());
            
            // 如果点击空白处，清除选中状态
            if (clickedElement == null) {
                clearSelection();
                return;
            }
            
            // 如果点击了新元素，更新选中状态
            if (selectedElement != clickedElement) {
                if (selectedElement != null) {
                    selectedElement.setSelected(false);
                }
                selectedElement = clickedElement;
                selectedElement.setSelected(true);
            }
            
            lastMouseX = event.getX();
            lastMouseY = event.getY();
            refreshCanvas();
        }
    }
    
    private void handleMouseDragged(MouseEvent event) {
        if (selectedElement != null) {
            double deltaX = event.getX() - lastMouseX;
            double deltaY = event.getY() - lastMouseY;
            selectedElement.move(deltaX, deltaY);
            lastMouseX = event.getX();
            lastMouseY = event.getY();
            refreshCanvas();
        }
    }
    
    private void handleMouseReleased(MouseEvent event) {
        // 移除取消选中的代码，只需要重置拖动状态
        lastMouseX = 0;
        lastMouseY = 0;
        refreshCanvas();
    }
    
    private void handleMouseMoved(MouseEvent event) {
        if (currentSlide != null) {
            SlideElement element = currentSlide.findElementAt(event.getX(), event.getY());
            if (element != null) {
                element.setHoverCursor(canvas);
            } else {
                canvas.setCursor(javafx.scene.Cursor.DEFAULT);
            }
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
        
        // 添加文字样式控制组件
        ColorPicker colorPicker = new ColorPicker(Color.BLACK);
        ComboBox<Integer> fontSizeCombo = new ComboBox<>();
        fontSizeCombo.getItems().addAll(12, 14, 16, 18, 20, 24, 28, 32, 36, 48);
        fontSizeCombo.setValue(20);
        
        ComboBox<String> fontStyleCombo = new ComboBox<>();
        fontStyleCombo.getItems().addAll("普通", "粗体", "斜体");
        fontStyleCombo.setValue("普通");
        
        // 添加样式更改监听器
        colorPicker.setOnAction(e -> {
            if (selectedElement instanceof TextElement) {
                TextElement textElement = (TextElement) selectedElement;
                textElement.setColor(colorPicker.getValue());
                refreshCanvas();
            }
        });
        
        fontSizeCombo.setOnAction(e -> {
            if (selectedElement instanceof TextElement) {
                TextElement textElement = (TextElement) selectedElement;
                textElement.setFontSize(fontSizeCombo.getValue());
                refreshCanvas();
            }
        });
        
        fontStyleCombo.setOnAction(e -> {
            if (selectedElement instanceof TextElement) {
                TextElement textElement = (TextElement) selectedElement;
                boolean italic = fontStyleCombo.getValue().equals("斜体");
                FontWeight weight = fontStyleCombo.getValue().equals("粗体") ? 
                    FontWeight.BOLD : FontWeight.NORMAL;
                textElement.setFontStyle(weight, italic);
                refreshCanvas();
            }
        });
        
        // 添加选中元素变化时的样式同步
        canvas.setOnMouseClicked(e -> {
            if (selectedElement instanceof TextElement) {
                TextElement textElement = (TextElement) selectedElement;
                colorPicker.setValue(textElement.getColor());
                fontSizeCombo.setValue((int)textElement.getFontSize());
                String style = textElement.getFontWeight() == FontWeight.BOLD ? "粗体" :
                              textElement.isItalic() ? "斜体" : "普通";
                fontStyleCombo.setValue(style);
            }
        });
        
        // 初始化类成员变量，而不是创建新的局部变量
        prevSlideBtn = new Button("上一页");
        nextSlideBtn = new Button("下一页");
        slideCountLabel = new Label("1/1"); // 显示当前页码
        
        prevSlideBtn.setOnAction(e -> previousSlide());
        nextSlideBtn.setOnAction(e -> nextSlide());
        
        newSlideBtn.setOnAction(e -> createNewSlide());
        addTextBtn.setOnAction(e -> addText());
        addImageBtn.setOnAction(e -> addImage());
        
        return new ToolBar(
            newSlideBtn,
            new Separator(),
            prevSlideBtn,
            slideCountLabel,
            nextSlideBtn,
            new Separator(),
            addTextBtn,
            addImageBtn,
            new Separator(),
            colorPicker,
            fontSizeCombo,
            fontStyleCombo
        );
    }
    
    private void createNewSlide() {
        Slide newSlide = new Slide();
        slides.add(newSlide);
        currentSlideIndex = slides.size() - 1;
        currentSlide = newSlide;
        refreshCanvas();
        updateSlideControls(); // 更新幻灯片控制按钮状态
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
                text,
                20, // 默认字体大小
                Color.BLACK, // 默认颜色
                FontWeight.NORMAL, // 默认字重
                false // 默认非斜体
            );
            currentSlide.addElement(textElement);
            refreshCanvas();
        });
    }
    
    private void addImage() {
        // TODO: 实现添加图片功能
    }

    // 添加一个新方法用于清除选中状态
    private void clearSelection() {
        if (selectedElement != null) {
            selectedElement.setSelected(false);
            selectedElement = null;
            refreshCanvas();
        }
    }

    private void previousSlide() {
        if (currentSlideIndex > 0) {
            currentSlideIndex--;
            currentSlide = slides.get(currentSlideIndex);
            refreshCanvas();
            updateSlideControls();
        }
    }

    private void nextSlide() {
        if (currentSlideIndex < slides.size() - 1) {
            currentSlideIndex++;
            currentSlide = slides.get(currentSlideIndex);
            refreshCanvas();
            updateSlideControls();
        }
    }

    private void updateSlideControls() {
        // 更新页码显示
        slideCountLabel.setText(String.format("%d/%d", 
            currentSlideIndex + 1, slides.size()));
        
        // 更新按钮状态
        prevSlideBtn.setDisable(currentSlideIndex <= 0);
        nextSlideBtn.setDisable(currentSlideIndex >= slides.size() - 1);
    }

    public static void main(String[] args) {
        launch(args);
    }
} 