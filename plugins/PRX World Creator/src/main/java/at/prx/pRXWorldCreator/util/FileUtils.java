package at.prx.pRXWorldCreator.util;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public class FileUtils {

    public static boolean deleteFolder(File file) {

        if (file == null || !file.exists())
            return false;

        for (File child : file.listFiles()) {
            if (child.isDirectory())
                deleteFolder(child);
            else
                child.delete();
        }
        return file.delete();
    }

    public static void copyFolder(Path source, Path target) throws IOException {

        Files.walk(source).forEach(path -> {
            try {
                Path relative = source.relativize(path);
                Path targetPath = target.resolve(relative);

                if (Files.isDirectory(path)) {
                    Files.createDirectories(targetPath);
                } else {
                    Files.copy(path, targetPath, StandardCopyOption.COPY_ATTRIBUTES);
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

}

