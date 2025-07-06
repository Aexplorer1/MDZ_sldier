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
     * 
     * @param aiResult   AI返回的PPT命令字符串
     * @param slideWidth 幻灯片宽度
     * @return 解析后的幻灯片列表
     */
    public static List<Slide> parseAndCreateSlides(String aiResult, double slideWidth) {
        List<Slide> slides = new ArrayList<>();

        if (aiResult == null || aiResult.trim().isEmpty()) {
            System.err.println("SlideParser: AI结果为空");
            return slides;
        }

        System.out.println("SlideParser: 开始解析AI结果");
        System.out.println("SlideParser: AI结果内容: " + aiResult.substring(0, Math.min(200, aiResult.length())) + "...");

        // 使用正则表达式查找所有页面
        Pattern pagePattern = Pattern.compile("Page\\s*(\\d+)[:：]\\s*(.*?)(?=Page\\s*\\d+[:：]|$)", Pattern.DOTALL);
        Matcher pageMatcher = pagePattern.matcher(aiResult);

        while (pageMatcher.find()) {
            String pageNumber = pageMatcher.group(1);
            String content = pageMatcher.group(2).trim();

            System.out.println("SlideParser: 找到页面 " + pageNumber + ", 内容长度: " + content.length());
            System.out.println("SlideParser: 页面 " + pageNumber + " 内容预览: "
                    + content.substring(0, Math.min(100, content.length())) + "...");

            if (content.isEmpty()) {
                System.out.println("SlideParser: 页面 " + pageNumber + " 内容为空，跳过");
                continue;
            }

            // 创建新幻灯片
            Slide slide = new Slide();
            slides.add(slide);

            // 解析页面内容
            parsePageContent(slide, content, slideWidth);

            System.out.println("SlideParser: 页面 " + pageNumber + " 解析完成，添加了 " + slide.getElements().size() + " 个元素");
        }

        // 如果没有找到页面标记，尝试将整个内容作为单个页面
        if (slides.isEmpty()) {
            System.out.println("SlideParser: 未找到页面标记，尝试将整个内容作为单个页面");
            Slide slide = new Slide();
            slides.add(slide);
            parsePageContent(slide, aiResult.trim(), slideWidth);
            System.out.println("SlideParser: 单页面解析完成，添加了 " + slide.getElements().size() + " 个元素");
        }

        System.out.println("SlideParser: 总共创建了 " + slides.size() + " 个幻灯片");
        return slides;
    }

    /**
     * 解析单个页面的内容
     * 
     * @param slide      目标幻灯片
     * @param content    页面内容字符串
     * @param slideWidth 幻灯片宽度
     */
    private static void parsePageContent(Slide slide, String content, double slideWidth) {
        double y = 60; // 起始Y坐标
        double lineSpacing = 8; // 行间距

        System.out.println("SlideParser: 解析页面内容: " + content.substring(0, Math.min(100, content.length())) + "...");

        // 按行解析内容
        String[] lines = content.split("\n");
        int elementCount = 0;

        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty())
                continue;

            System.out.println("SlideParser: 处理行: " + line);

            // 检查是否是PPT命令格式
            if (line.startsWith("Title:")) {
                String text = line.substring(6).trim();
                if (!text.isEmpty()) {
                    TextElement titleElem = new TextElement(slideWidth / 2, y, text, 28, Color.DARKBLUE,
                            FontWeight.BOLD, false);
                    titleElem.setPosition(slideWidth / 2 - titleElem.getWidth() / 2, y + titleElem.getHeight());
                    slide.addElement(titleElem);
                    y += titleElem.getHeight() + lineSpacing + 4;
                    elementCount++;
                    System.out.println("SlideParser: 添加标题: " + text);
                }
            } else if (line.startsWith("Subtitle:")) {
                String text = line.substring(9).trim();
                if (!text.isEmpty()) {
                    TextElement subElem = new TextElement(slideWidth / 2, y, text, 20, Color.DARKGRAY,
                            FontWeight.NORMAL, false);
                    subElem.setPosition(slideWidth / 2 - subElem.getWidth() / 2, y + subElem.getHeight());
                    slide.addElement(subElem);
                    y += subElem.getHeight() + lineSpacing;
                    elementCount++;
                    System.out.println("SlideParser: 添加副标题: " + text);
                }
            } else if (line.startsWith("Bullet:")) {
                String text = line.substring(7).trim();
                if (!text.isEmpty()) {
                    TextElement bulletElem = new TextElement(slideWidth / 2, y, text, 18, Color.BLACK,
                            FontWeight.NORMAL, false);
                    bulletElem.setPosition(slideWidth / 2 - bulletElem.getWidth() / 2, y + bulletElem.getHeight());
                    slide.addElement(bulletElem);
                    y += bulletElem.getHeight() + lineSpacing;
                    elementCount++;
                    System.out.println("SlideParser: 添加项目符号: " + text);
                }
            } else if (line.startsWith("Draw:")) {
                parseDrawCommand(slide, line);
                elementCount++;
                System.out.println("SlideParser: 添加绘图元素: " + line);
            } else if (line.startsWith("Image:")) {
                String text = line.substring(6).trim();
                if (!text.isEmpty()) {
                    // 暂时作为文本处理，后续可以扩展为图片元素
                    TextElement imageElem = new TextElement(slideWidth / 2, y, "[图片: " + text + "]", 16, Color.GRAY,
                            FontWeight.NORMAL, false);
                    imageElem.setPosition(slideWidth / 2 - imageElem.getWidth() / 2, y + imageElem.getHeight());
                    slide.addElement(imageElem);
                    y += imageElem.getHeight() + lineSpacing;
                    elementCount++;
                    System.out.println("SlideParser: 添加图片占位符: " + text);
                }
            } else if (line.matches("^\\d+\\..*")) {
                // 处理编号列表
                String text = line.substring(line.indexOf('.') + 1).trim();
                if (!text.isEmpty()) {
                    TextElement listElem = new TextElement(slideWidth / 2, y, text, 18, Color.BLACK, FontWeight.NORMAL,
                            false);
                    listElem.setPosition(slideWidth / 2 - listElem.getWidth() / 2, y + listElem.getHeight());
                    slide.addElement(listElem);
                    y += listElem.getHeight() + lineSpacing;
                    elementCount++;
                    System.out.println("SlideParser: 添加列表项: " + text);
                }
            } else {
                // 如果不是标准格式，尝试作为普通文本处理
                if (!line.isEmpty() && !line.startsWith("Page") && !line.startsWith("---")) {
                    TextElement textElem = new TextElement(slideWidth / 2, y, line, 16, Color.BLACK, FontWeight.NORMAL,
                            false);
                    textElem.setPosition(slideWidth / 2 - textElem.getWidth() / 2, y + textElem.getHeight());
                    slide.addElement(textElem);
                    y += textElem.getHeight() + lineSpacing;
                    elementCount++;
                    System.out.println("SlideParser: 添加普通文本: " + line);
                }
            }
        }

        System.out.println("SlideParser: 页面解析完成，总共添加了 " + elementCount + " 个元素");
    }

    /**
     * 解析绘图命令
     * 
     * @param slide   目标幻灯片
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
     * 
     * @param slide  目标幻灯片
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
     * 
     * @param slide  目标幻灯片
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
     * 
     * @param slide  目标幻灯片
     * @param params 参数字符串，格式：centerX,centerY,radius
     */
    private static void createCircle(Slide slide, String params) {
        String[] xy = params.split(",");
        if (xy.length == 3) {
            double centerX = Double.parseDouble(xy[0].trim());
            double centerY = Double.parseDouble(xy[1].trim());
            double radius = Double.parseDouble(xy[2].trim());

            DrawElement circle = new DrawElement(centerX - radius, centerY, DrawElement.ShapeType.CIRCLE, Color.GREEN,
                    2.0);
            circle.updateEndPoint(centerX + radius, centerY);
            slide.addElement(circle);
        }
    }

    /**
     * 创建箭头
     * 
     * @param slide  目标幻灯片
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
     * 
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
     * 
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