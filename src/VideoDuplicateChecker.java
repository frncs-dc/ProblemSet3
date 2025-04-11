import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class VideoDuplicateChecker {
    private final Set<String> seenHashes = Collections.synchronizedSet(new HashSet<>());
    private final File saveDir;

    public VideoDuplicateChecker(String saveDirectoryPath) {
        this.saveDir = new File(saveDirectoryPath);
    }

    public boolean isDuplicate(byte[] content) throws IOException {
        String hash = computeHash(content);

        // Check memory (already seen hashes)
        if (seenHashes.contains(hash)) {
            return true;
        }

        // Check on-disk files
        if (checkDiskForDuplicate(hash)) {
            return true;
        }

        // Not found: mark this hash as seen
        seenHashes.add(hash);
        return false;
    }

    private boolean checkDiskForDuplicate(String incomingHash) throws IOException {
        File[] files = saveDir.listFiles();
        if (files == null) return false;

        for (File file : files) {
            if (!file.isFile()) continue;
            String fileHash = computeHash(readFileToBytes(file));
            if (fileHash.equals(incomingHash)) {
                return true;
            }
        }

        return false;
    }

    private byte[] readFileToBytes(File file) throws IOException {
        try (FileInputStream fis = new FileInputStream(file)) {
            return fis.readAllBytes();
        }
    }

    private String computeHash(byte[] data) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(data);

            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }

            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("Failed to compute hash", e);
        }
    }
}
