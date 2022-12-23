package org.fg.VierGewinnt.VierGFX;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Objects;

public class Main extends Application {
    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws IOException {
        System.out.println("Starte 4Gewinnt");
        Parent root = FXMLLoader.load(Objects.requireNonNull(getClass().getClassLoader().getResource("gui.fxml")));
        primaryStage.setTitle("4Gewinnt");
        primaryStage.setMinWidth(642);
        primaryStage.setMinHeight(440);
        primaryStage.setScene(new Scene(root));
        primaryStage.getIcons().add(new Image("ico.png"));

        primaryStage.show();
    }
}