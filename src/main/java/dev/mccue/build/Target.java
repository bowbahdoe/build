package dev.mccue.build;

import java.util.*;

@FunctionalInterface
public interface Target {
    /// Whether the target is "up to date." An up-to-date target
    /// does not need to be built again.
    /// @return Whether this target is ready.
    default boolean isUpToDate() throws Exception {
        return false;
    }

    /// Any targets which this one depends on. These
    /// will be built before this target.
    ///
    /// Any outputs from these dependencies must be observed
    /// via observing their side effects.
    ///
    /// Note that dependencies should form a directed acyclic graph.
    ///
    /// @return Any dependencies of this target.
    default List<Target> dependencies() {
        return List.of();
    }

    /// Builds the target. It is expected that this
    /// would be an operation with side effects.
    ///
    /// Files may be written, fields might be initialized, etc.
    void build() throws Exception;

    static Target of(Buildable buildable, List<Target> dependencies) {
        return new SimpleTarget(buildable, dependencies);
    }

    static Target of(Buildable buildable, Target... dependencies) {
        return new SimpleTarget(buildable, List.of(dependencies));
    }

    static Target combine(List<Target> dependencies) {
        return new SimpleTarget(() -> {}, dependencies);
    }

    static Target combine(Target... dependencies) {
        return new SimpleTarget(() -> {}, List.of(dependencies));
    }

    default boolean areDependenciesUpToDate() throws Exception {
        for (var dependency : dependencies()) {
            if (!dependency.isUpToDate()) {
                return false;
            }
        }
        return true;
    }
}
