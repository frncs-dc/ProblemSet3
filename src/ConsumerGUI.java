import java.io.File;
import java.util.HashSet;
import java.util.Set;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.util.Duration;

public class ConsumerGUI extends Application {
    private Consumer consumer = new Consumer(2, 3);
    private VBox root = new VBox(10);
    private final Set<String> displayedVideos = new HashSet<>();

    @Override
    public void start(Stage primaryStage) {
        // For testing: use hardcoded folder first
        // File testVideosDir = new File("Videos/TestPSET3");

        // For actual execution: use consumer_directory folder
        File videoDir = new File("consumer_directory");

        // Repeating task every 5 seconds
        Timeline timeline = new Timeline(new KeyFrame(Duration.seconds(5), e -> refreshVideoList(videoDir)));
        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.play();

        // For testing initial call
        // refreshVideoList(testVideosDir);

        // For actual execution: use videoDir
        refreshVideoList(videoDir);

        Scene scene = new Scene(root, 800, 600);
        primaryStage.setTitle("Consumer - Video Library");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private void refreshVideoList(File videoDir) {
        File[] videoFiles = videoDir.listFiles((dir, name) -> name.endsWith(".mp4"));

        if (videoFiles != null) {
            for (File videoFile : videoFiles) {
                if (!displayedVideos.contains(videoFile.getName())) {
                    Platform.runLater(() -> {
                        Media media = new Media(videoFile.toURI().toString());
                        MediaPlayer mediaPlayer = new MediaPlayer(media);
                        MediaView mediaView = new MediaView(mediaPlayer);

                        mediaView.setFitWidth(200);
                        mediaView.setPreserveRatio(true);

                        mediaView.setOnMouseEntered(e -> {
                            mediaPlayer.setStartTime(Duration.seconds(0));
                            mediaPlayer.setStopTime(Duration.seconds(10));
                            mediaPlayer.setCycleCount(MediaPlayer.INDEFINITE);
                            mediaPlayer.play();
                        });

                        mediaView.setOnMouseExited(e -> mediaPlayer.stop());

                        mediaView.setOnMouseClicked(e -> {
                            Stage videoStage = new Stage();
                            MediaPlayer fullPlayer = new MediaPlayer(new Media(videoFile.toURI().toString()));
                            MediaView fullView = new MediaView(fullPlayer);
                            fullView.setFitWidth(800);
                            fullView.setPreserveRatio(true);
                            fullPlayer.play();

                            Scene scene = new Scene(new StackPane(fullView), 900, 600);
                            videoStage.setTitle(videoFile.getName());
                            videoStage.setScene(scene);
                            videoStage.show();
                        });

                        root.getChildren().add(mediaView);
                        displayedVideos.add(videoFile.getName());
                    });
                }
            }
        }
    }
}
