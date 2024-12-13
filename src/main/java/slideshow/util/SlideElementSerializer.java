package slideshow.util;

import com.google.gson.*;
import java.lang.reflect.Type;
import slideshow.elements.*;
import javafx.scene.paint.Color;
import javafx.scene.text.FontWeight;
import javafx.scene.image.Image;

public class SlideElementSerializer implements JsonSerializer<SlideElement>, JsonDeserializer<SlideElement> {
    
    @Override
    public JsonElement serialize(SlideElement src, Type typeOfSrc, JsonSerializationContext context) {
        JsonObject json = new JsonObject();
        
        // 保存通用属性
        json.addProperty("type", src.getClass().getSimpleName());
        json.addProperty("x", src.getX());
        json.addProperty("y", src.getY());
        
        // 根据具体类型保存特定属性
        if (src instanceof TextElement) {
            TextElement text = (TextElement) src;
            json.addProperty("text", text.getText());
            json.addProperty("fontSize", text.getFontSize());
            json.addProperty("color", text.getColor().toString());
            json.addProperty("fontWeight", text.getFontWeight().toString());
            json.addProperty("italic", text.isItalic());
        } else if (src instanceof ImageElement) {
            ImageElement image = (ImageElement) src;
            json.addProperty("width", image.getWidth());
            json.addProperty("height", image.getHeight());
            json.addProperty("imageUrl", image.getImageUrl());
        }
        
        return json;
    }
    
    @Override
    public SlideElement deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) 
            throws JsonParseException {
        JsonObject jsonObject = json.getAsJsonObject();
        String type = jsonObject.get("type").getAsString();
        double x = jsonObject.get("x").getAsDouble();
        double y = jsonObject.get("y").getAsDouble();
        
        // 根据类型创建相应的元素
        switch (type) {
            case "TextElement":
                String text = jsonObject.get("text").getAsString();
                double fontSize = jsonObject.get("fontSize").getAsDouble();
                Color color = Color.valueOf(jsonObject.get("color").getAsString());
                FontWeight fontWeight = FontWeight.valueOf(jsonObject.get("fontWeight").getAsString());
                boolean italic = jsonObject.get("italic").getAsBoolean();
                return new TextElement(x, y, text, fontSize, color, fontWeight, italic);
                
            case "ImageElement":
                String imageUrl = jsonObject.get("imageUrl").getAsString();
                Image image = new Image(imageUrl);
                return new ImageElement(x, y, image);
                
            default:
                throw new JsonParseException("Unknown element type: " + type);
        }
    }
} 