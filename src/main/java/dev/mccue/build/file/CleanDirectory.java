package dev.mccue.build.file;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;

/// A {@link PathTarget} which will always need to be rebuilt and
/// which will make a fresh directory at the given path.
///
/// The most likely use for this would be to make
final class CleanDirectory
        implements PathTarget {
    private final Path path;

    private CleanDirectory(Path path) {
        this.path = path;
    }

    public static CleanDirectory of(Path path) {
        return new CleanDirectory(path);
    }

    @Override
    public Path path() {
        return path;
    }

    @Override
    public boolean isUpToDate() throws IOException {
        try (var paths = Files.list(path)) {
            var list = paths.toList();
            return list.isEmpty();
        } catch (NoSuchFileException | NotDirectoryException e) {
            return false;
        }
    }

    @Override
    public void build() throws Exception {
        try {
            Files.walkFileTree(path, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path path, BasicFileAttributes basicFileAttributes) throws IOException
                {
                    Files.delete(path);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path directory, IOException ioException) throws IOException
                {
                    Files.delete(directory);
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (NoSuchFileException e) {
            // pass
        }

        Files.createDirectories(path);
    }

    @Override
    public String toString() {
        return path.toString();
    }
}
