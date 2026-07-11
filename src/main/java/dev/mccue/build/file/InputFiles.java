package dev.mccue.build.file;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

public final class InputFiles extends AbstractList<Path> {
    private final List<Path> paths;

    private InputFiles(List<Path> paths) {
        this.paths = List.copyOf(paths);
    }

    static InputFiles withExtension(Path root, String extension) throws IOException {
        return inFileTree(root, path -> Files.isRegularFile(path) && path.getFileName().toString().endsWith(extension));
    }

    static InputFiles inFileTree(Path root) throws IOException {
        return inFileTree(root, _ -> true);
    }

    static InputFiles inFileTree(Path root, Predicate<Path> keep) throws IOException {
        var visitor = new SimpleFileVisitor<Path>() {
            final List<Path> matches = new ArrayList<>();
            @Override
            public FileVisitResult visitFile(Path path, BasicFileAttributes basicFileAttributes) throws IOException
            {
                if (keep.test(path)) {
                    matches.add(path);
                }
                return FileVisitResult.CONTINUE;
            }
        };
        Files.walkFileTree(root, visitor);
        return new InputFiles(visitor.matches);
    }

    static InputFiles of(Path... paths) {
        return new InputFiles(List.of(paths));
    }

    static InputFiles of(List<Path> paths) {
        return new InputFiles(List.copyOf(paths));
    }

    @Override
    public Path get(int index) {
        return paths.get(index);
    }

    @Override
    public int size() {
        return paths.size();
    }

    @Override
    public String toString() {
        return paths.toString();
    }
}
