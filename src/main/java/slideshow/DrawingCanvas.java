package slideshow;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

import slideshow.shape.Arrow;
import slideshow.shape.Circle;
import slideshow.shape.Rectangle;
import slideshow.shape.Shape;
import slideshow.DrawMode;

import java.util.ArrayList;
import java.util.List;

public class DrawingCanvas extends Canvas {
    private List<Shape> shapes = new ArrayList<>();
    private Shape currentShape;
    private Shape selectedShape;
    private DrawMode currentMode = DrawMode.SELECT;
    private double lastX, lastY;
    private Color currentColor = Color.BLACK;
    
    public DrawingCanvas(double width, double height) {
        super(width, height);
        initializeEventHandlers();
        // 设置白色背景
        GraphicsContext gc = getGraphicsContext2D();
        gc.setFill(Color.WHITE);
        gc.fillRect(0, 0, width, height);
    }
    
    public void setColor(Color color) {
        this.currentColor = color;
    }
    
    public void deleteSelected() {
        if (selectedShape != null) {
            shapes.remove(selectedShape);
            selectedShape = null;
            redraw();
        }
    }
    
    public void clearAll() {
        shapes.clear();
        selectedShape = null;
        currentShape = null;
        redraw();
    }
    
    public void setDrawMode(DrawMode mode) {
        this.currentMode = mode;
    }
    
    private void initializeEventHandlers() {
        setOnMousePressed(e -> {
            lastX = e.getX();
            lastY = e.getY();
            
            if (currentMode == DrawMode.SELECT) {
                selectedShape = null;
                // 从后往前检查，以便选择最上层的图形
                for (int i = shapes.size() - 1; i >= 0; i--) {
                    if (shapes.get(i).contains(e.getX(), e.getY())) {
                        selectedShape = shapes.get(i);
                        break;
                    }
                }
            } else {
                currentShape = createShape(e.getX(), e.getY());
            }
            redraw();
        });
        
        setOnMouseDragged(e -> {
            if (currentMode == DrawMode.SELECT && selectedShape != null) {
                double deltaX = e.getX() - lastX;
                double deltaY = e.getY() - lastY;
                selectedShape.move(deltaX, deltaY);
                lastX = e.getX();
                lastY = e.getY();
            } else if (currentShape != null) {
                currentShape.setEndPoint(e.getX(), e.getY());
            }
            redraw();
        });
        
        setOnMouseReleased(e -> {
            if (currentShape != null) {
                shapes.add(currentShape);
                currentShape = null;
            }
            redraw();
        });
    }
    
    private Shape createShape(double x, double y) {
        Shape shape = null;
        switch (currentMode) {
            case RECTANGLE:
                shape = new Rectangle(x, y);
                break;
            case CIRCLE:
                shape = new Circle(x, y);
                break;
            case ARROW:
                shape = new Arrow(x, y);
                break;
        }
        return shape;
    }
    
    private void redraw() {
        GraphicsContext gc = getGraphicsContext2D();
        // 清除画布
        gc.setFill(Color.WHITE);
        gc.fillRect(0, 0, getWidth(), getHeight());
        
        // 设置线条宽度
        gc.setLineWidth(2);
        
        // 绘制所有已有的图形
        for (Shape shape : shapes) {
            if (shape == selectedShape) {
                gc.setStroke(Color.RED);
            } else {
                gc.setStroke(currentColor);
            }
            shape.draw(gc);
        }
        
        // 绘制正在创建的图形
        if (currentShape != null) {
            gc.setStroke(currentColor);
            currentShape.draw(gc);
        }
    }
}
