package dev.mccue.build.file;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public final class FileTimes {
    private FileTimes() {}

    public static Optional<FileTime> newestFileTime(Path path) throws IOException {
        var visitor = new SimpleFileVisitor<Path>() {
            FileTime newest = null;
            @Override
            public FileVisitResult visitFile(Path path, BasicFileAttributes basicFileAttributes) throws IOException
            {
                var time = Files.getLastModifiedTime(path);
                if (newest == null || newest.compareTo(time) > 0) {
                    newest = time;
                }
                return FileVisitResult.CONTINUE;
            }
        };
        Files.walkFileTree(path, visitor);
        return Optional.ofNullable(visitor.newest);
    }

    public static Optional<FileTime> newestFileTime(List<Path> paths) throws IOException {
        var fileTimes = new ArrayList<FileTime>();
        for (var path : paths) {
            newestFileTime(path).ifPresent(fileTimes::add);
        }
        if (fileTimes.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(Collections.max(fileTimes));
    }

    public static boolean outputIsNewerThanInput(Path input, Path output) throws IOException {
        return outputIsNewerThanInput(List.of(input), output);
    }

    public static boolean outputIsNewerThanInput(List<Path> input, Path output) throws IOException {
        var inputTime = newestFileTime(input).orElse(null);
        var outputTime = newestFileTime(output).orElse(null);

        if (inputTime != null && outputTime != null) {
            return inputTime.compareTo(outputTime) < 0;
        }
        else {
            return false;
        }
    }
}
