package slideshow.model;

import javafx.scene.canvas.GraphicsContext;
import slideshow.elements.SlideElement;

import java.util.ArrayList;
import java.util.List;

public class Slide {
    private List<SlideElement> elements = new ArrayList<>();
    private double width = 800.0;  // 默认宽度
    private double height = 600.0; // 默认高度
    
    public void addElement(SlideElement element) {
        if (elements == null) {
            elements = new ArrayList<>();
        }
        elements.add(element);
    }
    
    public void removeElement(SlideElement element) {
        elements.remove(element);
    }
    
    /**
     * 清除所有元素
     */
    public void clearElements() {
        elements.clear();
    }
    
    /**
     * 获取幻灯片中的所有元素
     * @return 元素列表的副本
     */
    public List<SlideElement> getElements() {
        return new ArrayList<>(elements);
    }
    
    /**
     * 获取幻灯片中文本元素的内容
     * @return 文本内容列表
     */
    public List<String> getTextContent() {
        List<String> textContent = new ArrayList<>();
        for (SlideElement element : elements) {
            if (element instanceof slideshow.elements.TextElement) {
                slideshow.elements.TextElement textElement = (slideshow.elements.TextElement) element;
                String text = textElement.getText();
                if (text != null && !text.trim().isEmpty()) {
                    textContent.add(text.trim());
                }
            }
        }
        return textContent;
    }
    
    public void draw(GraphicsContext gc) {
        // 添加调试信息
        System.out.println("绘制幻灯片，元素数量：" + (elements != null ? elements.size() : 0));
        if (elements != null) {
            for (int i = 0; i < elements.size(); i++) {
                SlideElement element = elements.get(i);
                // 输出元素类型和坐标
                System.out.printf("元素%d 类型：%s，坐标：(%.2f, %.2f)%n",
                    i + 1,
                    element.getClass().getSimpleName(),
                    element.getX(),
                    element.getY()
                );
                element.draw(gc);
            }
        }
    }
    
    public SlideElement findElementAt(double x, double y) {
        // 从后往前遍历，这样可以选中最上层的元素
        for (int i = elements.size() - 1; i >= 0; i--) {
            SlideElement element = elements.get(i);
            if (element.containsPoint(x, y)) {
                return element;
            }
        }
        return null;
    }
    
    /**
     * 获取幻灯片宽度
     */
    public double getWidth() {
        return width;
    }
    
    /**
     * 设置幻灯片宽度
     */
    public void setWidth(double width) {
        this.width = width;
    }
    
    /**
     * 获取幻灯片高度
     */
    public double getHeight() {
        return height;
    }
    
    /**
     * 设置幻灯片高度
     */
    public void setHeight(double height) {
        this.height = height;
    }
    
    /**
     * 深拷贝当前幻灯片，包括所有元素和尺寸
     */
    public Slide deepClone() {
        Slide clone = new Slide();
        clone.width = this.width;
        clone.height = this.height;
        for (SlideElement element : this.elements) {
            clone.addElement(element.deepClone());
        }
        return clone;
    }
} 