package ui;

import javafx.application.Application;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import model.MarkdownFileStructureGenerator;
import model.WebsiteSidebarTextGenerator;

import java.io.IOException;

public class FileProcessorUi extends Application {
    private static String pathName = null;
    private static boolean checkboxValue = false;
    TextArea outputArea = new TextArea();
    public void addTextToField(String inputText) {
        outputArea.appendText(System.lineSeparator() + inputText);
    }
    @Override
    public void start(Stage primaryStage) {
        // Erstelle die UI-Komponenten
        TextField pathField = new TextField();
        pathField.setPromptText("Pfad eingeben");
        if (pathName != null) {
            pathField.setText(pathName);
        }

        Button okButton = new Button("OK");

        CheckBox generateNewMdDirectory = new CheckBox("Generiere neues Repository mit Markdown fixes. ACHTUNG NUR WENN DU WEIßT WAS DU TUST");
        generateNewMdDirectory.setSelected(checkboxValue);

        Label statusLabel = new Label("Status: Warten auf Eingabe");

        outputArea = new TextArea();
        outputArea.setEditable(false);

        outputArea.textProperty().addListener(new ChangeListener<Object>() {
            @Override
            public void changed(ObservableValue<?> observable, Object oldValue,
                                Object newValue) {
                outputArea.setScrollTop(Double.MIN_VALUE); //this will scroll to the bottom
                //use Double.MIN_VALUE to scroll to the top
            }
        });

        // OK-Button-Event-Handler
        okButton.setOnAction(event -> {
            String path = pathField.getText();
            try {
                String content = "";
                if (!generateNewMdDirectory.isSelected()) {
                    content = processFile(path);
                    outputArea.clear();
                    statusLabel.setText("berechnet website content Struktur...");
                } else {
                    statusLabel.setText("berechnet neues Repository...");
                    MarkdownFileStructureGenerator generator = new MarkdownFileStructureGenerator(new TextFieldMessageObserver(this));
                    generator.generateFileStructure(path);
                }
                outputArea.appendText(content);
            } catch (Exception e) {
                outputArea.setText("Fehler: " + e.getMessage() + "\n");
                e.printStackTrace();
                for (StackTraceElement ste : e.getStackTrace()) {
                    outputArea.appendText(ste.toString() + "\n");
                }
            }
        });

        // Layout
        VBox root = new VBox(10, new Label("Dateipfad:"), statusLabel, pathField, generateNewMdDirectory, okButton, new Label("Output:"), outputArea);
        Scene scene = new Scene(root, 600, 400);

        // Stage-Einstellungen
        primaryStage.setTitle("File Processor");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    // Beispielmethode zur Dateiverarbeitung
    private String processFile(String path) throws IOException {
        // In dieser Beispielmethode lesen wir einfach den Inhalt der Datei.
        return WebsiteSidebarTextGenerator.generateSidebarText(path);
    }



    public static void main(String[] args) {
        pathName = null;
        checkboxValue = false;
        if (args != null) {
            try {
                pathName = args[1];
                checkboxValue = Boolean.parseBoolean(args[2]);
            } catch (IndexOutOfBoundsException _) {}

        }
        launch(args);
    }
}
