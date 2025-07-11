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

        // 通用属性
        json.addProperty("type", src.getClass().getSimpleName());
        json.addProperty("x", src.getX());
        json.addProperty("y", src.getY());

        // 特定类型属性
        if (src instanceof TextElement) {
            TextElement text = (TextElement) src;
            json.addProperty("text", text.getText());
            json.addProperty("fontSize", text.getFontSize());
            json.addProperty("color", text.getColor().toString());
            json.addProperty("fontWeight", text.getFontWeight().toString());
            json.addProperty("italic", text.isItalic());

        } else if (src instanceof ImageElement) {
            ImageElement image = (ImageElement) src;
            json.addProperty("imageUrl", image.getImageUrl());
            json.addProperty("width", image.getWidth());
            json.addProperty("height", image.getHeight());
        } else if (src instanceof DrawElement) {
            DrawElement draw = (DrawElement) src;
            json.addProperty("shapeType", draw.getShapeType().name());
            json.addProperty("strokeColor", draw.getStrokeColor().toString());
            json.addProperty("strokeWidth", draw.getStrokeWidth());
            json.addProperty("startX", draw.getStartX());
            json.addProperty("startY", draw.getStartY());
            json.addProperty("endX", draw.getEndX());
            json.addProperty("endY", draw.getEndY());
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
                double width = jsonObject.get("width").getAsDouble();
                double height = jsonObject.get("height").getAsDouble();
                return new ImageElement(x, y, imageUrl, width, height);

            case "DrawElement":
                DrawElement.ShapeType shapeType = DrawElement.ShapeType.valueOf(jsonObject.get("shapeType").getAsString());
                Color strokeColor = Color.valueOf(jsonObject.get("strokeColor").getAsString());
                double strokeWidth = jsonObject.get("strokeWidth").getAsDouble();
                double startX = jsonObject.get("startX").getAsDouble();
                double startY = jsonObject.get("startY").getAsDouble();
                double endX = jsonObject.get("endX").getAsDouble();
                double endY = jsonObject.get("endY").getAsDouble();

                DrawElement draw = new DrawElement(startX, startY, shapeType, strokeColor, strokeWidth);
                draw.updateEndPoint(endX, endY);
                return draw;


            default:
                throw new JsonParseException("Unknown element type: " + type);
        }
    }
}