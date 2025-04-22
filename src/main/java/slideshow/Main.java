package slideshow;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import javafx.stage.FileChooser;
import javafx.scene.input.MouseEvent;
import javafx.scene.Cursor;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.text.FontWeight;

import slideshow.util.Constants;
import slideshow.model.Slide;
import slideshow.elements.SlideElement;
import slideshow.elements.TextElement;
import slideshow.elements.ImageElement;
import slideshow.util.UIStrings;
import slideshow.util.SlideSerializer;
import slideshow.presentation.PresentationWindow;
import slideshow.elements.DrawElement;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.ArrayList;
import java.util.Optional;
import java.util.logging.Logger;
import java.util.logging.Level;


/**
 * MDZ_Slider主应用程序类
 * 负责管理用户界面和幻灯片编辑功能
 */
public class Main extends Application {
    private static final Logger logger = Logger.getLogger(Main.class.getName());
    
    private Canvas canvas;
    private GraphicsContext graphicsContext;
    private Slide currentSlide;
    private SlideElement selectedElement;
    private double lastMouseX;
    private double lastMouseY;
    private List<Slide> slides = new ArrayList<>();
    private int currentSlideIndex = -1;
    private Button previousSlideButton;
    private Button nextSlideButton;
    private Label slideCountLabel;
    private SlideElement.ResizeHandle currentResizeHandle = SlideElement.ResizeHandle.NONE;
    private DrawElement.ShapeType currentShape = null;
    private DrawElement currentDrawing = null;
    private ColorPicker drawColorPicker;
    private ComboBox<Double> lineWidthComboBox;
    private ToggleGroup drawGroup;
    
    @Override
    public void start(Stage primaryStage) {
        logger.info("应用程序启动中...");
        BorderPane root = new BorderPane();
        
        // 创建画布
        canvas = new Canvas(Constants.DEFAULT_SLIDE_WIDTH, Constants.DEFAULT_SLIDE_HEIGHT);
        graphicsContext = canvas.getGraphicsContext2D();
        
        // 添加鼠标事件处理
        canvas.setOnMousePressed(this::handleMousePressed);
        canvas.setOnMouseDragged(this::handleMouseDragged);
        canvas.setOnMouseReleased(this::handleMouseReleased);
        canvas.setOnMouseMoved(this::handleMouseMoved);
        
        // 创建菜单栏和工具栏
        MenuBar menuBar = createMenuBar();
        ToolBar toolBar = createToolBar();
        
        // 创建顶部容器
        VBox topContainer = new VBox();
        topContainer.getChildren().addAll(menuBar, toolBar);
        root.setTop(topContainer);
        
        // 设置画布容器
        BorderPane canvasHolder = new BorderPane(canvas);
        canvasHolder.getStyleClass().add("canvas-holder");
        root.setCenter(canvasHolder);
        
        Scene scene = new Scene(root, Constants.DEFAULT_WINDOW_WIDTH, Constants.DEFAULT_WINDOW_HEIGHT);
        
        // 加载CSS样式
        try {
            String cssPath = getClass().getResource("/styles/theme.css").toExternalForm();
            scene.getStylesheets().add(cssPath);
            logger.info("CSS样式加载成功");
        } catch (Exception e) {
            logger.log(Level.SEVERE, "无法加载CSS文件", e);
        }
        
        primaryStage.setTitle("MDZ_Slider");
        primaryStage.setScene(scene);
        primaryStage.show();
        
        // 创建初始幻灯片
        createNewSlide();
        
        // 添加键盘事件监听
        scene.setOnKeyPressed(this::handleKeyPressed);
        logger.info("应用程序启动完成");
    }
    
    private void handleMousePressed(MouseEvent event) {
        if (currentShape != null) {
            // 开始绘制
            currentDrawing = new DrawElement(
                event.getX(), event.getY(),
                currentShape,
                drawColorPicker.getValue(),
                lineWidthComboBox.getValue()
            );
            currentSlide.addElement(currentDrawing);
            return;
        }
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
        if (currentDrawing != null) {
            
            // 更新绘制
            currentDrawing.updateEndPoint(event.getX(), event.getY());
            refreshCanvas();
            return;
        }
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
        if (currentDrawing != null) {
            // 完成绘制
            currentDrawing.updateEndPoint(event.getX(), event.getY());
            currentDrawing = null;
            // 取消当前绘制状态
            currentShape = null;
            // 取消所有绘图按钮的选中状态
            for (Toggle toggle : drawGroup.getToggles()) {
                toggle.setSelected(false);
            }
            refreshCanvas();
            return;
        }
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
        graphicsContext.setFill(Color.WHITE);
        graphicsContext.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());
        
        // 重新绘制所有元素
        if (currentSlide != null) {
            System.out.println("刷新画布");
            currentSlide.draw(graphicsContext);
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
        previousSlideButton = new Button("上一页");
        nextSlideButton = new Button("下一页");
        slideCountLabel = new Label("1/1"); // 显示当前页码
        
        previousSlideButton.getStyleClass().add("button");
        nextSlideButton.getStyleClass().add("button");
        
        previousSlideButton.setOnAction(e -> previousSlide());
        nextSlideButton.setOnAction(e -> nextSlide());
        
        newSlideBtn.setOnAction(e -> createNewSlide());
        addTextBtn.setOnAction(e -> addText());
        addImageBtn.setOnAction(e -> addImage());
        
        // 添加绘图工具按钮
        drawGroup = new ToggleGroup();
        
        ToggleButton rectBtn = new ToggleButton("矩形");
        ToggleButton circleBtn = new ToggleButton("圆形");
        ToggleButton lineBtn = new ToggleButton("直线");
        ToggleButton arrowBtn = new ToggleButton("箭头");
        
        // 添加与其他按钮相同的样式类
        rectBtn.getStyleClass().add("button");
        circleBtn.getStyleClass().add("button");
        lineBtn.getStyleClass().add("button");
        arrowBtn.getStyleClass().add("button");
        
        rectBtn.setToggleGroup(drawGroup);
        circleBtn.setToggleGroup(drawGroup);
        lineBtn.setToggleGroup(drawGroup);
        arrowBtn.setToggleGroup(drawGroup);
        
        rectBtn.setOnAction(e -> currentShape = DrawElement.ShapeType.RECTANGLE);
        circleBtn.setOnAction(e -> currentShape = DrawElement.ShapeType.CIRCLE);
        lineBtn.setOnAction(e -> currentShape = DrawElement.ShapeType.LINE);
        arrowBtn.setOnAction(e -> currentShape = DrawElement.ShapeType.ARROW);
        
        // 添加颜色和线宽控制
        drawColorPicker = new ColorPicker(Color.BLACK);
        lineWidthComboBox = new ComboBox<>();
        lineWidthComboBox.getItems().addAll(1.0, 2.0, 3.0, 4.0, 5.0);
        lineWidthComboBox.setValue(2.0);
        
        return new ToolBar(
            newSlideBtn,
            new Separator(),
            previousSlideButton,
            slideCountLabel,
            nextSlideButton,
            new Separator(),
            addTextBtn,
            addImageBtn,
            new Separator(),
            colorPicker,
            fontSizeCombo,
            fontStyleCombo,
            rectBtn, circleBtn, lineBtn, arrowBtn,
            drawColorPicker,
            lineWidthComboBox
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
                showError("无法加载图片", "请确保选择的是有效的图片文件错误：" + e.getMessage());
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
        previousSlideButton.setDisable(currentSlideIndex <= 0);
        nextSlideButton.setDisable(currentSlideIndex >= slides.size() - 1);
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

    private MenuBar createMenuBar() {
        MenuBar menuBar = new MenuBar();
        menuBar.setUseSystemMenuBar(true);  // 在 macOS 上使用系统菜单栏
        
        // 文件菜单
        Menu fileMenu = new Menu("文件");
        MenuItem newItem = new MenuItem("新建");
        MenuItem openItem = new MenuItem("打开...");
        MenuItem saveItem = new MenuItem("保存");
        MenuItem saveAsItem = new MenuItem("另存为...");
        MenuItem exitItem = new MenuItem("退出");
        
        // 添加快捷键
        newItem.setAccelerator(new KeyCodeCombination(KeyCode.N, KeyCombination.SHORTCUT_DOWN));
        openItem.setAccelerator(new KeyCodeCombination(KeyCode.O, KeyCombination.SHORTCUT_DOWN));
        saveItem.setAccelerator(new KeyCodeCombination(KeyCode.S, KeyCombination.SHORTCUT_DOWN));
        
        fileMenu.getItems().addAll(newItem, openItem, saveItem, saveAsItem, 
                                  new SeparatorMenuItem(), exitItem);
        
        // 编辑菜单
        Menu editMenu = new Menu("编辑");
        MenuItem undoItem = new MenuItem("撤销");
        MenuItem redoItem = new MenuItem("重做");
        MenuItem cutItem = new MenuItem("剪切");
        MenuItem copyItem = new MenuItem("复制");
        MenuItem pasteItem = new MenuItem("粘贴");
        
        editMenu.getItems().addAll(undoItem, redoItem, 
                                  new SeparatorMenuItem(),
                                  cutItem, copyItem, pasteItem);
        
        // 放映菜单
        Menu playMenu = new Menu("放映");
        MenuItem startItem = new MenuItem("开始放映");
        MenuItem settingsItem = new MenuItem("放映设置");
        
        playMenu.getItems().addAll(startItem, settingsItem);
        
        menuBar.getMenus().addAll(fileMenu, editMenu, playMenu);
        
        // 添加事件处理
        newItem.setOnAction(e -> createNewPresentation());
        openItem.setOnAction(e -> openPresentation());
        saveItem.setOnAction(e -> savePresentation());
        saveAsItem.setOnAction(e -> saveAsPresentation());
        exitItem.setOnAction(e -> Platform.exit());
        startItem.setOnAction(e -> startPresentation());
        
        return menuBar;
    }

    private void createNewPresentation() {
        // 提示保存当前文件
        if (!slides.isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("新建幻灯片");
            alert.setHeaderText("是否保存当前灯片？");
            alert.setContentText("如果不保存，当前的修改将丢失。");
            
            ButtonType saveButton = new ButtonType("保存");
            ButtonType noSaveButton = new ButtonType("不保存");
            ButtonType cancelButton = new ButtonType("取消", ButtonBar.ButtonData.CANCEL_CLOSE);
            
            alert.getButtonTypes().setAll(saveButton, noSaveButton, cancelButton);
            
            Optional<ButtonType> result = alert.showAndWait();
            if (result.get() == saveButton) {
                savePresentation();
            } else if (result.get() == cancelButton) {
                return;
            }
        }
        
        // 清空当前幻灯片
        slides.clear();
        currentSlideIndex = -1;
        createNewSlide();
    }

    private void saveAsPresentation() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("另存为");
        fileChooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("幻灯片文件", "*.mdz")
        );
        
        File file = fileChooser.showSaveDialog(canvas.getScene().getWindow());
        if (file != null) {
            try {
                SlideSerializer.savePresentation(slides, file.getPath());
                showInfo("保存成功", "幻灯片已保存到：" + file.getPath());
            } catch (IOException e) {
                showError("保存失败", "无法保存文件：" + e.getMessage());
            }
        }
    }

    private void startPresentation() {
        PresentationWindow presentation = new PresentationWindow(slides);
        presentation.start();
    }

    private void savePresentation() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("保存幻灯片");
        fileChooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("幻灯片文件", "*.mdz")
        );
        
        File file = fileChooser.showSaveDialog(canvas.getScene().getWindow());
        if (file != null) {
            try {
                // 使用 SlideSerializer 保存幻灯片列表
                SlideSerializer.savePresentation(slides, file.getPath());
                showInfo("保存成功", "幻灯片已保存到：" + file.getPath());
            } catch (IOException e) {
                showError("保存失败", "无法保存文件：" + e.getMessage());
            }
        }
    }

    private void openPresentation() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("打开幻灯片");
        fileChooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("幻灯片文件", "*.mdz")
        );
        
        File file = fileChooser.showOpenDialog(canvas.getScene().getWindow());
        if (file != null) {
            try {
                // 使用 SlideSerializer 加载幻灯片列表
                slides = SlideSerializer.loadPresentation(file.getPath());
                currentSlideIndex = 0;
                currentSlide = slides.get(0);
                refreshCanvas();
                updateSlideControls();
                showInfo("打开成功", "已加载幻灯片：" + file.getName());
            } catch (IOException e) {
                showError("打开失败", "无法打开文件：" + e.getMessage());
            }
        }
    }

    private void showInfo(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    public static void main(String[] args) {
        launch(args);
    }
} 