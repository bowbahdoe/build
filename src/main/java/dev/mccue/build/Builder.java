package dev.mccue.build;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/// Builder of targets.
public interface Builder {
    void build(Target target) throws Exception;

    default void build(Buildable buildable) throws Exception {
        if (buildable instanceof Target t) {
            build(t);
        }
        else {
            build(Target.of(buildable));
        }
    }

    /// Builder that builds all targets in a topologically sorted order
    /// on a single thread.
    static Builder singleThreaded() {
        return SingleThreadedBuilder.INSTANCE;
    }
/*
    static Builder multiThreaded() {
        return multiThreaded(Executors.newVirtualThreadPerTaskExecutor());
    }

    static Builder multiThreaded(ExecutorService executorService) {
        return new MultiThreadedBuilder(executorService);
    } */
}
