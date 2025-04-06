import java.io.File;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.util.Duration;

public class ConsumerGUI extends Application {
    private Consumer consumer = new Consumer();

    @Override
    public void start(Stage primaryStage) {
        VBox root = new VBox(10);

        // For testing without producer
        File testVideosDir = new File("Videos/TestPSET3");  // Change to your test folder
        File[] videoFiles = testVideosDir.listFiles((dir, name) -> name.endsWith(".mp4"));  // Only mp4 files
        if (videoFiles == null || videoFiles.length == 0) {
            System.out.println("No video files found in the directory!");
        }

        System.out.println("Test video folder path: " + testVideosDir.getAbsolutePath());

        if(videoFiles != null) {
            // replace videoFiles with consumer.getVideoFiles() once connection is done
            for (File videoFile : videoFiles) {
                Media media = new Media(videoFile.toURI().toString());
                MediaPlayer mediaPlayer = new MediaPlayer(media);
                MediaView mediaView = new MediaView(mediaPlayer);

                // Set a default size
                mediaView.setFitWidth(200);
                mediaView.setPreserveRatio(true);

                // Preview 10 seconds on mouse hover
                mediaView.setOnMouseEntered(e -> {
                    mediaPlayer.setStartTime(Duration.seconds(0));
                    mediaPlayer.setStopTime(Duration.seconds(10));
                    mediaPlayer.setCycleCount(MediaPlayer.INDEFINITE);
                    mediaPlayer.play();
                });
                mediaView.setOnMouseExited(e -> mediaPlayer.stop());

                // Display full video on click
                mediaView.setOnMouseClicked(e -> {
                    Stage videoStage = new Stage();
                    MediaPlayer fullPlayer = new MediaPlayer(new Media(videoFile.toURI().toString()));
                    MediaView fullView = new MediaView(fullPlayer);
                    fullView.setFitWidth(800);
                    fullView.setPreserveRatio(true);
                    fullPlayer.play();

                    Scene scene = new Scene(new StackPane(fullView), 800, 600);
                    videoStage.setTitle(videoFile.getName());
                    videoStage.setScene(scene);
                    videoStage.show();
                });

                root.getChildren().add(mediaView);
            }
        } else {
            System.out.println("No video files found");
        }

        Scene scene = new Scene(root, 1000, 800);
        primaryStage.setTitle("Consumer Video Gallery");
        primaryStage.setScene(scene);
        primaryStage.show();
    }
}