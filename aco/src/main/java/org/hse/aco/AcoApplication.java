package org.hse.aco;

import com.google.common.math.DoubleMath;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Objects;

import static java.lang.Math.log;
import static java.lang.Math.log10;

public class AcoApplication extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(AcoApplication.class.getResource("application-main-screen.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 320, 240);
        stage.getIcons().add(new Image(Objects.requireNonNull(AcoApplication.class.getResourceAsStream("ant.png"))));
        stage.setTitle("Ant colony optimization");
        stage.setWidth(840);
        stage.setMinWidth(840);
        stage.setMaxWidth(840);
        stage.setHeight(600);
        stage.setMinHeight(600);
        stage.setScene(scene);
        stage.show();
    }

    @Override
    public void stop() throws Exception {
        super.stop();
        System.exit(0);
    }

    public static void main(String[] args) {
//        var c1 = entropy(48, new int[]{3, 34, 1, 10});
//        var c2 = entropy(55, new int[]{45, 2, 6, 2});
//        var c3 = entropy(53, new int[]{9, 4, 40});
//        var c4 = entropy(59, new int[]{4, 4, 6, 45});
//        var c5 = entropy(35, new int[]{1, 32, 2});
//        System.out.println((c1 + c2 + c3 + c4 + c5) / 250.);

//        var c1 = entropy(10, new int[]{9, 1});
//        var c2 = entropy(10, new int[]{10});
//        System.out.println((c1 + c2) / 20.);

//        var c1 = entropy(6, new int[]{5, 1});
//        var c2 = entropy(6, new int[]{1, 1, 4});
//        var c3 = entropy(5, new int[]{2, 3});
//        System.out.println((c1 + c2 + c3) / 17.);

        launch();
    }

    private static double entropy(int total, int[] classes) {
        double res = 0;
        for (int i : classes) {
            double temp = (double) i / total;
            res += temp * DoubleMath.log2(temp);
//            res += temp * Math.log(temp);
        }
        return 1. * res * total;
    }
}