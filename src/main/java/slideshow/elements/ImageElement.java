package slideshow.elements;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;

public class ImageElement extends SlideElement {
    private Image image;
    private double width;
    private double height;
    private static final double HANDLE_SIZE = 8; // 控制点大小
    private static final double HANDLE_OFFSET = HANDLE_SIZE / 2;
    
    public ImageElement(double x, double y, Image image) {
        super(x, y);
        this.image = image;
        
        // 获取原始尺寸
        double originalWidth = image.getWidth();
        double originalHeight = image.getHeight();
        
        // 设置最大显示尺寸
        double maxWidth = 800;  // 根据需要调整这个值
        double maxHeight = 600; // 根据需要调整这个值
        
        // 计算缩放比例
        double scale = 1.0;
        if (originalWidth > maxWidth || originalHeight > maxHeight) {
            double scaleX = maxWidth / originalWidth;
            double scaleY = maxHeight / originalHeight;
            scale = Math.min(scaleX, scaleY);
        }
        
        // 设置显示尺寸
        this.width = originalWidth * scale;
        this.height = originalHeight * scale;
        
        System.out.println("图片尺寸：原始 " + originalWidth + "x" + originalHeight + 
                         " 缩放后 " + this.width + "x" + this.height);
    }
    
    @Override
    public void draw(GraphicsContext gc) {
        gc.save();
        
        // 绘制图片
        gc.drawImage(image, x, y, width, height);
        
        // 如果被选中，绘制框和控制点
        if (selected) {
            gc.setStroke(Color.BLUE);
            gc.setLineDashes(5);
            gc.strokeRect(x - 2, y - 2, width + 4, height + 4);
            gc.setLineDashes(null);
            
            // 绘制控制点
            drawResizeHandles(gc);
        }
        
        gc.restore();
    }
    
    private void drawResizeHandles(GraphicsContext gc) {
        gc.setFill(Color.WHITE);
        gc.setStroke(Color.BLUE);
        
        // 绘制四角的控制点
        drawHandle(gc, x - HANDLE_OFFSET, y - HANDLE_OFFSET);                 // NW
        drawHandle(gc, x + width - HANDLE_OFFSET, y - HANDLE_OFFSET);         // NE
        drawHandle(gc, x - HANDLE_OFFSET, y + height - HANDLE_OFFSET);        // SW
        drawHandle(gc, x + width - HANDLE_OFFSET, y + height - HANDLE_OFFSET);// SE
        
        // 绘制边缘中点的控制点
        drawHandle(gc, x + width/2 - HANDLE_OFFSET, y - HANDLE_OFFSET);       // N
        drawHandle(gc, x + width/2 - HANDLE_OFFSET, y + height - HANDLE_OFFSET); // S
        drawHandle(gc, x - HANDLE_OFFSET, y + height/2 - HANDLE_OFFSET);      // W
        drawHandle(gc, x + width - HANDLE_OFFSET, y + height/2 - HANDLE_OFFSET); // E
    }
    
    private void drawHandle(GraphicsContext gc, double x, double y) {
        gc.setFill(Color.WHITE);
        gc.fillRect(x, y, HANDLE_SIZE, HANDLE_SIZE);
        gc.strokeRect(x, y, HANDLE_SIZE, HANDLE_SIZE);
    }
    
    @Override
    public ResizeHandle getResizeHandle(double px, double py) {
        if (!selected) return ResizeHandle.NONE;
        
        // 检查各个控制点
        if (isInHandle(px, py, x, y)) return ResizeHandle.NW;
        if (isInHandle(px, py, x + width, y)) return ResizeHandle.NE;
        if (isInHandle(px, py, x, y + height)) return ResizeHandle.SW;
        if (isInHandle(px, py, x + width, y + height)) return ResizeHandle.SE;
        if (isInHandle(px, py, x + width/2, y)) return ResizeHandle.N;
        if (isInHandle(px, py, x + width/2, y + height)) return ResizeHandle.S;
        if (isInHandle(px, py, x, y + height/2)) return ResizeHandle.W;
        if (isInHandle(px, py, x + width, y + height/2)) return ResizeHandle.E;
        
        return ResizeHandle.NONE;
    }
    
    private boolean isInHandle(double px, double py, double hx, double hy) {
        return px >= hx - HANDLE_OFFSET && px <= hx + HANDLE_OFFSET &&
               py >= hy - HANDLE_OFFSET && py <= hy + HANDLE_OFFSET;
    }
    
    public void resize(double deltaX, double deltaY, ResizeHandle handle) {
        double minSize = 50; // 最小尺寸
        
        switch (handle) {
            case SE: // 右下角
                width = Math.max(minSize, width + deltaX);
                height = Math.max(minSize, height + deltaY);
                break;
            
            case NW: // 左上角
                double newWidth = Math.max(minSize, width - deltaX);
                double newHeight = Math.max(minSize, height - deltaY);
                if (newWidth != width) {
                    x += (width - newWidth);
                    width = newWidth;
                }
                if (newHeight != height) {
                    y += (height - newHeight);
                    height = newHeight;
                }
                break;
            
            case NE: // 右上角
                width = Math.max(minSize, width + deltaX);
                newHeight = Math.max(minSize, height - deltaY);
                if (newHeight != height) {
                    y += (height - newHeight);
                    height = newHeight;
                }
                break;
            
            case SW: // 左下角
                newWidth = Math.max(minSize, width - deltaX);
                height = Math.max(minSize, height + deltaY);
                if (newWidth != width) {
                    x += (width - newWidth);
                    width = newWidth;
                }
                break;
            
            case N: // 上边中点
                newHeight = Math.max(minSize, height - deltaY);
                if (newHeight != height) {
                    y += (height - newHeight);
                    height = newHeight;
                }
                break;
            
            case S: // 下边中点
                height = Math.max(minSize, height + deltaY);
                break;
            
            case W: // 左边中点
                newWidth = Math.max(minSize, width - deltaX);
                if (newWidth != width) {
                    x += (width - newWidth);
                    width = newWidth;
                }
                break;
            
            case E: // 右边中点
                width = Math.max(minSize, width + deltaX);
                break;
        }
    }
    
    public void setResizeHandle(ResizeHandle handle) {
        this.currentHandle = handle;
    }
    
    public ResizeHandle getCurrentHandle() {
        return currentHandle;
    }
    
    @Override
    public boolean containsPoint(double px, double py) {
        return px >= x && px <= x + width &&
               py >= y && py <= y + height;
    }
    
    @Override
    public void setPosition(double x, double y) {
        this.x = x;
        this.y = y;
    }
} 