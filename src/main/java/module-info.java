module slideshow {
    requires transitive javafx.controls;
    requires transitive javafx.graphics;
    requires javafx.base;
    requires javafx.fxml;
    
    exports slideshow;
    exports slideshow.elements;
    exports slideshow.model;
    exports slideshow.util;
    exports slideshow.shape;
}   