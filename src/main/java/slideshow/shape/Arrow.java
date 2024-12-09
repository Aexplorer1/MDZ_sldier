package slideshow.shape;
import javafx.scene.canvas.GraphicsContext;


public class Arrow extends Shape {
    private static final double ARROW_HEAD_SIZE = 20;
    private static final double ARROW_ANGLE = Math.PI / 6; // 30度
    
    public Arrow(double startX, double startY) {
        super(startX, startY);
    }
    
    @Override
    public void draw(GraphicsContext gc) {
        // 绘制主线
        gc.strokeLine(startX, startY, endX, endY);
        
        // 计算箭头
        double angle = Math.atan2(endY - startY, endX - startX);
        
        // 绘制箭头的两条边
        double x1 = endX - ARROW_HEAD_SIZE * Math.cos(angle + ARROW_ANGLE);
        double y1 = endY - ARROW_HEAD_SIZE * Math.sin(angle + ARROW_ANGLE);
        double x2 = endX - ARROW_HEAD_SIZE * Math.cos(angle - ARROW_ANGLE);
        double y2 = endY - ARROW_HEAD_SIZE * Math.sin(angle - ARROW_ANGLE);
        
        gc.strokeLine(endX, endY, x1, y1);
        gc.strokeLine(endX, endY, x2, y2);
    }
    
    @Override
    public boolean contains(double x, double y) {
        // 计算点到线段的距离
        double lineLength = Math.sqrt(Math.pow(endX - startX, 2) + Math.pow(endY - startY, 2));
        if (lineLength == 0) return false;
        
        double distance = Math.abs((endY - startY) * x - (endX - startX) * y + endX * startY - endY * startX) 
                         / lineLength;
                         
        // 检查点是否在线段的起点和终点之间
        double dotProduct = ((x - startX) * (endX - startX) + (y - startY) * (endY - startY)) 
                          / (lineLength * lineLength);
                          
        return distance < 5 && dotProduct >= 0 && dotProduct <= 1;
    }
} 