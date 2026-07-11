package dev.mccue.build.file;

import dev.mccue.build.Target;

import java.nio.file.*;

/// A target which, when ready, will have a path available
/// and ready to use.
public interface PathTarget extends Target {
    /// @return The path produced as a result of building this target.
    Path path();
    
    static PathTarget cleanDirectory(Path p) {
        return CleanDirectory.of(p);
    }
}
