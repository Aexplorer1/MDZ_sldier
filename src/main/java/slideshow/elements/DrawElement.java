package slideshow.elements;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.geometry.Point2D;
import java.util.ArrayList;
import java.util.List;

public class DrawElement extends SlideElement {
    public enum ShapeType {
        RECTANGLE, CIRCLE, LINE, ARROW
    }
    
    private ShapeType shapeType;
    private Color strokeColor;
    private double strokeWidth;
    private double startX;
    private double startY;
    private double endX;
    private double endY;
    
    public DrawElement(double x, double y, ShapeType shapeType, Color strokeColor, double strokeWidth) {
        super(x, y);
        this.shapeType = shapeType;
        this.strokeColor = strokeColor;
        this.strokeWidth = strokeWidth;
        this.startX = x;
        this.startY = y;
        this.endX = x;
        this.endY = y;
    }
    
    @Override
    public void draw(GraphicsContext gc) {
        gc.save();
        gc.setStroke(strokeColor);
        gc.setLineWidth(strokeWidth);
        
        switch (shapeType) {
            case RECTANGLE:
                drawRectangle(gc);
                break;
            case CIRCLE:
                drawCircle(gc);
                break;
            case LINE:
                drawLine(gc);
                break;
            case ARROW:
                drawArrow(gc);
                break;
        }
        
        if (selected) {
            drawSelectionHandles(gc);
        }
        
        gc.restore();
    }
    
    private void drawRectangle(GraphicsContext gc) {
        double x = Math.min(startX, endX);
        double y = Math.min(startY, endY);
        double width = Math.abs(endX - startX);
        double height = Math.abs(endY - startY);
        gc.strokeRect(x, y, width, height);
    }
    
    private void drawCircle(GraphicsContext gc) {
        double centerX = (startX + endX) / 2;
        double centerY = (startY + endY) / 2;
        double radius = Math.sqrt(Math.pow(endX - startX, 2) + Math.pow(endY - startY, 2)) / 2;
        gc.strokeOval(centerX - radius, centerY - radius, radius * 2, radius * 2);
    }
    
    private void drawLine(GraphicsContext gc) {
        gc.strokeLine(startX, startY, endX, endY);
    }
    
    private void drawArrow(GraphicsContext gc) {
        double arrowLength = 20;
        double arrowWidth = 8;
        
        // 绘制主线
        gc.strokeLine(startX, startY, endX, endY);
        
        // 计算��头
        double angle = Math.atan2(endY - startY, endX - startX);
        double x1 = endX - arrowLength * Math.cos(angle - Math.PI/6);
        double y1 = endY - arrowLength * Math.sin(angle - Math.PI/6);
        double x2 = endX - arrowLength * Math.cos(angle + Math.PI/6);
        double y2 = endY - arrowLength * Math.sin(angle + Math.PI/6);
        
        // 绘制箭头
        gc.strokeLine(endX, endY, x1, y1);
        gc.strokeLine(endX, endY, x2, y2);
    }
    
    public void updateEndPoint(double x, double y) {
        this.endX = x;
        this.endY = y;
    }
    
    @Override
    public boolean containsPoint(double px, double py) {
        // 简单的边界框检测
        double x = Math.min(startX, endX);
        double y = Math.min(startY, endY);
        double width = Math.abs(endX - startX);
        double height = Math.abs(endY - startY);
        
        return px >= x && px <= x + width && py >= y && py <= y + height;
    }
    
    @Override
    public void setPosition(double x, double y) {
        double deltaX = x - this.x;
        double deltaY = y - this.y;
        this.x = x;
        this.y = y;
        this.startX += deltaX;
        this.startY += deltaY;
        this.endX += deltaX;
        this.endY += deltaY;
    }
    
    @Override
    public ResizeHandle getResizeHandle(double px, double py) {
        // 实现调整大小的控制点检测
        return ResizeHandle.NONE;
    }
    
    @Override
    public void resize(double deltaX, double deltaY, ResizeHandle handle) {
        // 实现调整大小的逻辑
    }
    
    private void drawSelectionHandles(GraphicsContext gc) {
        gc.setFill(Color.WHITE);
        gc.setStroke(Color.BLUE);
        
        // 绘制四角的控制点
        drawHandle(gc, startX - HANDLE_OFFSET, startY - HANDLE_OFFSET);           // NW
        drawHandle(gc, endX - HANDLE_OFFSET, startY - HANDLE_OFFSET);            // NE
        drawHandle(gc, startX - HANDLE_OFFSET, endY - HANDLE_OFFSET);            // SW
        drawHandle(gc, endX - HANDLE_OFFSET, endY - HANDLE_OFFSET);              // SE
        
        // 绘制边缘中点的控制点
        drawHandle(gc, (startX + endX)/2 - HANDLE_OFFSET, startY - HANDLE_OFFSET); // N
        drawHandle(gc, (startX + endX)/2 - HANDLE_OFFSET, endY - HANDLE_OFFSET);   // S
        drawHandle(gc, startX - HANDLE_OFFSET, (startY + endY)/2 - HANDLE_OFFSET); // W
        drawHandle(gc, endX - HANDLE_OFFSET, (startY + endY)/2 - HANDLE_OFFSET);   // E
    }
    
    private void drawHandle(GraphicsContext gc, double x, double y) {
        gc.setFill(Color.WHITE);
        gc.fillRect(x, y, HANDLE_SIZE, HANDLE_SIZE);
        gc.strokeRect(x, y, HANDLE_SIZE, HANDLE_SIZE);
    }
} 