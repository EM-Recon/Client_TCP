//--enable-native-access=javafx.graphics
package com.astier.bts.client_tcp_prof;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.io.IOException;

public class HelloApplication extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("hello-view.fxml"));
        Parent root = loader.load();
        HelloController controller = loader.getController();

        stage.setOnCloseRequest(event -> {
            try {
                controller.fermerConnexion();
            } catch (Exception ex) {
                // on quitte quand même
            } finally {
                System.exit(0);
            }
        });

        stage.setTitle("TCP-Client  MM");
        stage.getIcons().add(new Image("/icone/index.jpg"));
        stage.setScene(new Scene(root));
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}