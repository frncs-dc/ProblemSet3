import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.BlockingQueue;

public class ConsumerThread implements Runnable {
    private final int id;
    private final BlockingQueue<Consumer.VideoFile> queue;
    private final String saveDirectory;
    private volatile String currentFileName = "Idle";


    public ConsumerThread(int id, BlockingQueue<Consumer.VideoFile> queue, String saveDirectory) {
        this.id = id;
        this.queue = queue;
        this.saveDirectory = saveDirectory;
    }

    @Override
    public void run() {
        while (true) {
            try {
                Consumer.VideoFile videoFile = queue.take();  // Blocks if empty
                currentFileName = videoFile.fileName;
                System.out.println("ConsumerThread-" + id + " processing: " + videoFile.fileName);
                saveFile(videoFile);
                currentFileName = "Idle";
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }


    private void saveFile(Consumer.VideoFile videoFile) {
        Path filePath = Paths.get(saveDirectory, videoFile.fileName);
        try (FileOutputStream fos = new FileOutputStream(filePath.toFile())) {
            fos.write(videoFile.content);
            System.out.println("ConsumerThread-" + id + " saved: " + videoFile.fileName);
        } catch (IOException e) {
            System.err.println("Error saving file: " + videoFile.fileName + " - " + e.getMessage());
        }
    }

    public String getCurrentFileName() {
        return currentFileName;
    }

    public int getId() {
        return id;
    }

}
