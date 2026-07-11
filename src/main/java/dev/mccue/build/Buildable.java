package dev.mccue.build;

/// Code that builds something.
@FunctionalInterface
public interface Buildable {
    /// Builds the thing(s)!
    /// @throws Exception if it throws an exception.
    void build() throws Exception;
}
