package slideshow.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import slideshow.model.Slide;
import slideshow.elements.SlideElement;
import java.io.*;
import java.util.List;
import java.lang.reflect.Type;

public class SlideSerializer {
    private static final Gson gson = new GsonBuilder()
        .registerTypeAdapter(SlideElement.class, new SlideElementSerializer())
            //.excludeFieldsWithoutExposeAnnotation()
        .create();
    
    public static void savePresentation(List<Slide> slides, String filePath) throws IOException {
        String json = gson.toJson(slides);
        try (FileWriter writer = new FileWriter(filePath)) {
            writer.write(json);
        }
    }
    
    public static List<Slide> loadPresentation(String filePath) throws IOException {
        try (FileReader reader = new FileReader(filePath)) {
            Type listType = new TypeToken<List<Slide>>(){}.getType();
            return gson.fromJson(reader, listType);
        }
    }
} 