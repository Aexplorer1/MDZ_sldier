package slideshow.util;

import javafx.scene.paint.Color;
import javafx.scene.text.FontWeight;
import slideshow.model.Slide;
import slideshow.elements.TextElement;
import slideshow.elements.DrawElement;
import slideshow.elements.SlideElement;

import java.util.List;
import java.util.ArrayList;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * AI生成PPT的分页解析器
 * 负责解析AI返回的PPT命令格式，并创建对应的幻灯片
 */
public class SlideParser {
    
    /**
     * 解析AI生成的PPT命令并创建幻灯片列表
     * @param aiResult AI返回的PPT命令字符串
     * @param slideWidth 幻灯片宽度
     * @return 解析后的幻灯片列表
     */
    public static List<Slide> parseAndCreateSlides(String aiResult, double slideWidth) {
        List<Slide> slides = new ArrayList<>();
        
        // 使用正则表达式分割页面
        // 匹配 "Page 数字:" 或 "Page 数字：" 格式
        String[] pages = aiResult.split("Page\\s*\\d+[:：]");
        
        for (String page : pages) {
            String content = page.trim();
            if (content.isEmpty()) continue;
            
            // 创建新幻灯片
            Slide slide = new Slide();
            slides.add(slide);
            
            // 解析页面内容
            parsePageContent(slide, content, slideWidth);
        }
        
        return slides;
    }
    
    /**
     * 解析单个页面的内容
     * @param slide 目标幻灯片
     * @param content 页面内容字符串
     * @param slideWidth 幻灯片宽度
     */
    private static void parsePageContent(Slide slide, String content, double slideWidth) {
        double y = 60; // 起始Y坐标
        double lineSpacing = 8; // 行间距
        
        // 正则表达式匹配所有PPT命令
        // 匹配格式：Title:内容, Subtitle:内容, Bullet:内容, Draw:图形类型(参数)
        Pattern pattern = Pattern.compile(
            "(Title:([^;\\n]+))|(Subtitle:([^;\\n]+))|(Bullet:([^;\\n]+))|(Draw:\\s*(Line|Rectangle|Circle|Arrow)\\([^)]*\\))"
        );
        Matcher matcher = pattern.matcher(content);
        
        while (matcher.find()) {
            if (matcher.group(1) != null) { // Title
                String text = matcher.group(2).trim();
                TextElement titleElem = new TextElement(slideWidth/2, y, text, 28, Color.DARKBLUE, FontWeight.BOLD, false);
                titleElem.setPosition(slideWidth/2 - titleElem.getWidth()/2, y + titleElem.getHeight());
                slide.addElement(titleElem);
                y += titleElem.getHeight() + lineSpacing + 4;
                
            } else if (matcher.group(3) != null) { // Subtitle
                String text = matcher.group(4).trim();
                TextElement subElem = new TextElement(slideWidth/2, y, text, 20, Color.DARKGRAY, FontWeight.NORMAL, false);
                subElem.setPosition(slideWidth/2 - subElem.getWidth()/2, y + subElem.getHeight());
                slide.addElement(subElem);
                y += subElem.getHeight() + lineSpacing;
                
            } else if (matcher.group(5) != null) { // Bullet
                String text = matcher.group(6).trim();
                TextElement bulletElem = new TextElement(slideWidth/2, y, text, 18, Color.BLACK, FontWeight.NORMAL, false);
                bulletElem.setPosition(slideWidth/2 - bulletElem.getWidth()/2, y + bulletElem.getHeight());
                slide.addElement(bulletElem);
                y += bulletElem.getHeight() + lineSpacing;
                
            } else if (matcher.group(7) != null) { // Draw
                parseDrawCommand(slide, matcher.group(7));
            }
        }
    }
    
    /**
     * 解析绘图命令
     * @param slide 目标幻灯片
     * @param drawCmd 绘图命令字符串
     */
    private static void parseDrawCommand(Slide slide, String drawCmd) {
        // 匹配绘图命令格式：Draw: 图形类型(参数)
        Pattern drawPattern = Pattern.compile("Draw:\\s*(Line|Rectangle|Circle|Arrow)\\(([^)]*)\\)");
        Matcher drawMatcher = drawPattern.matcher(drawCmd);
        
        if (drawMatcher.find()) {
            String shapeType = drawMatcher.group(1);
            String params = drawMatcher.group(2);
            
            try {
                switch (shapeType) {
                    case "Line":
                        createLine(slide, params);
                        break;
                    case "Rectangle":
                        createRectangle(slide, params);
                        break;
                    case "Circle":
                        createCircle(slide, params);
                        break;
                    case "Arrow":
                        createArrow(slide, params);
                        break;
                }
            } catch (Exception e) {
                // 忽略解析错误，继续处理其他元素
                System.err.println("解析绘图命令失败: " + drawCmd + ", 错误: " + e.getMessage());
            }
        }
    }
    
    /**
     * 创建直线
     * @param slide 目标幻灯片
     * @param params 参数字符串，格式：x1,y1,x2,y2
     */
    private static void createLine(Slide slide, String params) {
        String[] xy = params.split(",");
        if (xy.length == 4) {
            double x1 = Double.parseDouble(xy[0].trim());
            double y1 = Double.parseDouble(xy[1].trim());
            double x2 = Double.parseDouble(xy[2].trim());
            double y2 = Double.parseDouble(xy[3].trim());
            
            DrawElement line = new DrawElement(x1, y1, DrawElement.ShapeType.LINE, Color.BLACK, 2.0);
            line.updateEndPoint(x2, y2);
            slide.addElement(line);
        }
    }
    
    /**
     * 创建矩形
     * @param slide 目标幻灯片
     * @param params 参数字符串，格式：x1,y1,x2,y2
     */
    private static void createRectangle(Slide slide, String params) {
        String[] xy = params.split(",");
        if (xy.length == 4) {
            double x1 = Double.parseDouble(xy[0].trim());
            double y1 = Double.parseDouble(xy[1].trim());
            double x2 = Double.parseDouble(xy[2].trim());
            double y2 = Double.parseDouble(xy[3].trim());
            
            DrawElement rect = new DrawElement(x1, y1, DrawElement.ShapeType.RECTANGLE, Color.ORANGE, 2.0);
            rect.updateEndPoint(x2, y2);
            slide.addElement(rect);
        }
    }
    
    /**
     * 创建圆形
     * @param slide 目标幻灯片
     * @param params 参数字符串，格式：centerX,centerY,radius
     */
    private static void createCircle(Slide slide, String params) {
        String[] xy = params.split(",");
        if (xy.length == 3) {
            double centerX = Double.parseDouble(xy[0].trim());
            double centerY = Double.parseDouble(xy[1].trim());
            double radius = Double.parseDouble(xy[2].trim());
            
            DrawElement circle = new DrawElement(centerX - radius, centerY, DrawElement.ShapeType.CIRCLE, Color.GREEN, 2.0);
            circle.updateEndPoint(centerX + radius, centerY);
            slide.addElement(circle);
        }
    }
    
    /**
     * 创建箭头
     * @param slide 目标幻灯片
     * @param params 参数字符串，格式：x1,y1,x2,y2
     */
    private static void createArrow(Slide slide, String params) {
        String[] xy = params.split(",");
        if (xy.length == 4) {
            double x1 = Double.parseDouble(xy[0].trim());
            double y1 = Double.parseDouble(xy[1].trim());
            double x2 = Double.parseDouble(xy[2].trim());
            double y2 = Double.parseDouble(xy[3].trim());
            
            DrawElement arrow = new DrawElement(x1, y1, DrawElement.ShapeType.ARROW, Color.RED, 2.0);
            arrow.updateEndPoint(x2, y2);
            slide.addElement(arrow);
        }
    }
    
    /**
     * 验证AI返回的PPT命令格式是否正确
     * @param aiResult AI返回的命令字符串
     * @return 是否包含有效的PPT命令
     */
    public static boolean isValidPPTCommand(String aiResult) {
        if (aiResult == null || aiResult.trim().isEmpty()) {
            return false;
        }
        
        // 检查是否包含页面标记
        Pattern pagePattern = Pattern.compile("Page\\s*\\d+[:：]");
        return pagePattern.matcher(aiResult).find();
    }
    
    /**
     * 获取PPT命令中的页面数量
     * @param aiResult AI返回的命令字符串
     * @return 页面数量
     */
    public static int getPageCount(String aiResult) {
        if (aiResult == null || aiResult.trim().isEmpty()) {
            return 0;
        }
        
        Pattern pagePattern = Pattern.compile("Page\\s*\\d+[:：]");
        Matcher matcher = pagePattern.matcher(aiResult);
        
        int count = 0;
        while (matcher.find()) {
            count++;
        }
        
        return count;
    }
} 