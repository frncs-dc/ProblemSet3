import java.io.File;
import java.util.HashSet;
import java.util.Set;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.util.Duration;
import javafx.scene.layout.FlowPane;
import javafx.scene.control.ScrollPane;



public class ConsumerGUI extends Application {
    private Consumer consumer = new Consumer(2, 3);
    private FlowPane flowPane = new FlowPane(); // Changed from VBox to FlowPane
    private final Set<String> displayedVideos = new HashSet<>();

    @Override
    public void start(Stage primaryStage) {
        File videoDir = new File("consumer_directory");

        // Configure FlowPane
        flowPane.setHgap(10);
        flowPane.setVgap(10);
        flowPane.setPadding(new Insets(10));
        flowPane.setPrefWrapLength(600); // Width at which wrapping occurs

        // Create ScrollPane and add FlowPane
        ScrollPane scrollPane = new ScrollPane(flowPane);
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);

        // Timeline setup remains the same
        Timeline timeline = new Timeline(new KeyFrame(Duration.seconds(5), e -> refreshVideoList(videoDir)));
        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.play();

        Scene scene = new Scene(scrollPane, 800, 600); // Changed root to scrollPane
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

                        // Mouse event handlers remain the same
                        mediaView.setOnMouseEntered(e -> {
                            mediaPlayer.setStartTime(Duration.seconds(0));
                            mediaPlayer.setStopTime(Duration.seconds(10));
                            mediaPlayer.setCycleCount(MediaPlayer.INDEFINITE);
                            mediaPlayer.play();
                        });

                        mediaView.setOnMouseExited(e -> mediaPlayer.stop());

                        mediaView.setOnMouseClicked(e -> {
                            Stage videoStage = new Stage();
                            // MediaPlayer fullPlayer = new MediaPlayer(media);
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

                        flowPane.getChildren().add(mediaView); // Changed from root to flowPane
                        displayedVideos.add(videoFile.getName());
                    });
                }
            }
        }
    }
}