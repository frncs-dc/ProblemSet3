import java.security.MessageDigest;
import java.util.HashSet;

public class VideoDuplicateChecker {

    private final HashSet<String> knownHashes = new HashSet<>();

    // Add a video file and return true if it's a duplicate
    public boolean isDuplicate(VideoFile videoFile) {
        try {
            String hash = computeSHA256(videoFile.content);
            if (knownHashes.contains(hash)) {
                return true;  // Duplicate
            } else {
                knownHashes.add(hash);
                return false; // New video
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false; // Treat as non-duplicate if hashing fails
        }
    }

    // Compute SHA-256 hash from byte array
    private String computeSHA256(byte[] data) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hashBytes = digest.digest(data);

        StringBuilder sb = new StringBuilder();
        for (byte b : hashBytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    // Inner class for video data
    public static class VideoFile {
        public final String fileName;
        public final byte[] content;

        public VideoFile(String fileName, byte[] content) {
            this.fileName = fileName;
            this.content = content;
        }
    }
}
