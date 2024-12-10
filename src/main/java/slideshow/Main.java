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
import javafx.scene.Cursor;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.KeyCode;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
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
import javafx.stage.FileChooser;
import java.io.File;
import javafx.scene.control.Alert;
import javafx.scene.image.Image;
import slideshow.elements.ImageElement;
import slideshow.util.UIStrings;

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
    private SlideElement.ResizeHandle currentResizeHandle = SlideElement.ResizeHandle.NONE;
    
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
        canvasHolder.getStyleClass().add("canvas-holder");
        root.setCenter(canvasHolder);
        
        Scene scene = new Scene(root, Constants.DEFAULT_WINDOW_WIDTH, Constants.DEFAULT_WINDOW_HEIGHT);
        
        // 载CSS样式
        String cssPath = getClass().getResource("/styles/theme.css").toExternalForm();
        try {
            scene.getStylesheets().add(cssPath);
        } catch (Exception e) {
            System.err.println("无法加载CSS文件: " + e.getMessage());
            e.printStackTrace();
        }
        
        primaryStage.setTitle("MDZ_Slider");
        primaryStage.setScene(scene);
        primaryStage.show();
        
        // 创建初始幻灯片
        createNewSlide();
        
        // 添加键盘事件监听
        scene.setOnKeyPressed(this::handleKeyPressed);
    }
    
    private void handleMousePressed(MouseEvent event) {
        if (currentSlide != null) {
            if (selectedElement != null) {
                currentResizeHandle = selectedElement.getResizeHandle(event.getX(), event.getY());
                if (currentResizeHandle != SlideElement.ResizeHandle.NONE) {
                    return;
                }
            }
            
            SlideElement clickedElement = currentSlide.findElementAt(event.getX(), event.getY());
            
            // 如果是右键点击，显示上下文菜单
            if (event.isSecondaryButtonDown() && clickedElement != null) {
                showContextMenu(clickedElement, event.getScreenX(), event.getScreenY());
                return;
            }
            
            // 如果点击了空白处，清除选中状态
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
        if (selectedElement != null && currentResizeHandle != SlideElement.ResizeHandle.NONE) {
            double deltaX = event.getX() - lastMouseX;
            double deltaY = event.getY() - lastMouseY;
            selectedElement.resize(deltaX, deltaY, currentResizeHandle);
            lastMouseX = event.getX();
            lastMouseY = event.getY();
            refreshCanvas();
            return;
        }
        
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
        currentResizeHandle = SlideElement.ResizeHandle.NONE;
        lastMouseX = 0;
        lastMouseY = 0;
        refreshCanvas();
    }
    
    private void handleMouseMoved(MouseEvent event) {
        if (currentSlide != null) {
            if (selectedElement != null) {
                Cursor cursor = Cursor.DEFAULT;
                
                if (selectedElement instanceof ImageElement) {
                    ImageElement imageElement = (ImageElement) selectedElement;
                    ImageElement.ResizeHandle handle = imageElement.getResizeHandle(event.getX(), event.getY());
                    cursor = getResizeCursor(handle);
                } else if (selectedElement instanceof TextElement) {
                    TextElement textElement = (TextElement) selectedElement;
                    TextElement.ResizeHandle handle = textElement.getResizeHandle(event.getX(), event.getY());
                    cursor = getResizeCursor(handle);
                }
                
                if (cursor == Cursor.DEFAULT && selectedElement.containsPoint(event.getX(), event.getY())) {
                    cursor = Cursor.HAND;
                }
                
                canvas.setCursor(cursor);
                return;
            }
            
            SlideElement element = currentSlide.findElementAt(event.getX(), event.getY());
            if (element != null) {
                element.setHoverCursor(canvas);
            } else {
                canvas.setCursor(Cursor.DEFAULT);
            }
        }
    }
    
    private Cursor getResizeCursor(Object handle) {
        if (handle instanceof SlideElement.ResizeHandle) {
            SlideElement.ResizeHandle h = (SlideElement.ResizeHandle) handle;
            switch (h) {
                case NW: case SE: return Cursor.NW_RESIZE;
                case NE: case SW: return Cursor.NE_RESIZE;
                case N: case S: return Cursor.V_RESIZE;
                case E: case W: return Cursor.H_RESIZE;
                default: return Cursor.DEFAULT;
            }
        }
        return Cursor.DEFAULT;
    }
    
    private void refreshCanvas() {
        // 清空画布
        gc.setFill(Color.WHITE);
        gc.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());
        
        // 重新绘制所有元素
        if (currentSlide != null) {
            System.out.println("刷新画布");
            currentSlide.draw(gc);
        }
    }
    
    private ToolBar createToolBar() {
        Button newSlideBtn = new Button(UIStrings.NEW_SLIDE);
        Button addTextBtn = new Button(UIStrings.ADD_TEXT);
        Button addImageBtn = new Button(UIStrings.ADD_IMAGE);
        
        // 为所有按钮添加样式类
        newSlideBtn.getStyleClass().add("button");
        addTextBtn.getStyleClass().add("button");
        addImageBtn.getStyleClass().add("button");
        
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
        
        prevSlideBtn.getStyleClass().add("button");
        nextSlideBtn.getStyleClass().add("button");
        
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
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("选择图片");
        fileChooser.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("图片文件", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.bmp")
        );
        
        File file = fileChooser.showOpenDialog(canvas.getScene().getWindow());
        if (file != null) {
            try {
                Image image = new Image(file.toURI().toString());
                System.out.println("图片加载成功：" + image.getWidth() + "x" + image.getHeight());
                
                // 计算居中位置（考虑缩放后的尺寸）
                double scale = Math.min(800 / image.getWidth(), 600 / image.getHeight());
                double scaledWidth = image.getWidth() * scale;
                double scaledHeight = image.getHeight() * scale;
                
                ImageElement imageElement = new ImageElement(
                    (canvas.getWidth() - scaledWidth) / 2,  // 考虑缩放后的宽度
                    (canvas.getHeight() - scaledHeight) / 2, // 考虑缩放后的高度
                    image
                );
                
                // 确保当前幻灯片存在
                if (currentSlide == null) {
                    System.out.println("错误：当前幻灯片为空");
                    return;
                }
                
                currentSlide.addElement(imageElement);
                System.out.println("图片元素已添加到幻灯片");
                
                refreshCanvas();
            } catch (Exception e) {
                e.printStackTrace(); // 打印详细错误信息
                showError("无法加载图片", "请确保选择的是有效的图片文件���错误：" + e.getMessage());
            }
        }
    }

    private void showError(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
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

    private void showContextMenu(SlideElement element, double x, double y) {
        ContextMenu contextMenu = new ContextMenu();
        MenuItem deleteItem = new MenuItem("删除");
        deleteItem.setOnAction(e -> deleteElement(element));
        contextMenu.getItems().add(deleteItem);
        contextMenu.show(canvas, x, y);
    }
    
    private void handleKeyPressed(KeyEvent event) {
        if (event.getCode() == KeyCode.BACK_SPACE && selectedElement != null) {
            deleteElement(selectedElement);
        }
    }
    
    private void deleteElement(SlideElement element) {
        if (currentSlide != null) {
            currentSlide.removeElement(element);
            if (element == selectedElement) {
                selectedElement = null;
            }
            refreshCanvas();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
} 