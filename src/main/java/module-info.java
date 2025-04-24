module slideshow {
    requires javafx.controls;
    requires javafx.graphics;
    requires com.google.gson;
    requires java.logging;
    
    exports slideshow;
    exports slideshow.model;
    exports slideshow.elements;
    exports slideshow.util;
    exports slideshow.presentation;
} 