module ChapterGenerator {
    requires javafx.base;
    requires javafx.graphics;
    requires javafx.controls;
    requires control;
    opens ui to javafx.graphics;
    exports ui;
}
