package ticketing.io;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import ticketing.exception.PersistenceException;

/** Binary (byte stream) file copy using FileInputStream / FileOutputStream - used to back up exported files. */
public final class FileBackup {
    private FileBackup() {
    }

    public static long copy(String source, String target) {
        File src = new File(source);
        if (!src.isFile()) {
            throw new PersistenceException("File not found: " + source, null);
        }
        File dst = new File(target);
        File parent = dst.getParentFile();
        if (parent != null) {
            parent.mkdirs();
        }
        long total = 0;
        try (InputStream in = new FileInputStream(src); OutputStream out = new FileOutputStream(dst)) {
            byte[] buffer = new byte[4096];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
                total += read;
            }
        } catch (IOException e) {
            throw new PersistenceException("Backup failed: " + e.getMessage(), e);
        }
        return total;
    }
}
