package slideshow.elements;


import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.Cursor;
import javafx.geometry.Bounds;
import javafx.geometry.BoundingBox;

public abstract class SlideElement {
    protected double x;
    protected double y;
    protected boolean selected;
    
    protected static final double HANDLE_SIZE = 8; // 控制点大小
    protected static final double HANDLE_OFFSET = HANDLE_SIZE / 2;
    
    public enum ResizeHandle {
        NONE, NW, NE, SW, SE, N, S, W, E
    }
    
    protected ResizeHandle currentHandle = ResizeHandle.NONE;
    
    public SlideElement(double x, double y) {
        this.x = x;
        this.y = y;
        this.selected = false;
    }
    
    public abstract void draw(GraphicsContext gc);
    public abstract boolean containsPoint(double x, double y);
    
    public void move(double deltaX, double deltaY) {
        setPosition(x + deltaX, y + deltaY);
    }
    
    public boolean isSelected() {
        return selected;
    }
    
    public double getX() {
        return x;
    }
    
    public double getY() {
        return y;
    }
    
    public void setX(double x) {
        this.x = x;
    }
    
    public void setY(double y) {
        this.y = y;
    }
    
    public void setSelected(boolean selected) {
        this.selected = selected;
    }
    
    public void setHoverCursor(Canvas canvas) {
        canvas.setCursor(Cursor.HAND);  // 手型光标
    }
    
    public void setDefaultCursor(Canvas canvas) {
        canvas.setCursor(Cursor.DEFAULT);  // 默认光标
    }
    
    public abstract void setPosition(double x, double y);
    
    public abstract ResizeHandle getResizeHandle(double x, double y);
    public abstract void resize(double deltaX, double deltaY, ResizeHandle handle);
    
    /**
     * 获取元素宽度
     */
    public abstract double getWidth();
    
    /**
     * 获取元素高度
     */
    public abstract double getHeight();
    
    /**
     * 获取元素包围盒（用于框选/碰撞检测）
     */
    public abstract Bounds getBoundingBox();
    
    /**
     * 深拷贝自身，子类需实现
     */
    public abstract SlideElement deepClone();
}