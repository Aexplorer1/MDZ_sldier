package slideshow.elements;


import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.Cursor;

public abstract class SlideElement {
    protected double x;
    protected double y;
    protected boolean selected;
    
    public SlideElement(double x, double y) {
        this.x = x;
        this.y = y;
        this.selected = false;
    }
    
    public abstract void draw(GraphicsContext gc);
    public abstract boolean containsPoint(double x, double y);
    
    public void move(double deltaX, double deltaY) {
        x += deltaX;
        y += deltaY;
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
}