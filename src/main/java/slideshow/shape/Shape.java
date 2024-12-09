package slideshow.shape;
import javafx.scene.canvas.GraphicsContext;

public abstract class Shape {
    protected double startX;
    protected double startY;
    protected double endX;
    protected double endY;
    
    public Shape(double startX, double startY) {
        this.startX = startX;
        this.startY = startY;
        this.endX = startX;
        this.endY = startY;
    }
    
    public abstract void draw(GraphicsContext gc);
    public abstract boolean contains(double x, double y);
    
    public void setEndPoint(double x, double y) {
        this.endX = x;
        this.endY = y;
    }
    
    public void move(double deltaX, double deltaY) {
        startX += deltaX;
        startY += deltaY;
        endX += deltaX;
        endY += deltaY;
    }
} 