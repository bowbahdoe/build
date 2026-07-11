package dev.mccue.build;

import java.util.*;
import java.util.concurrent.*;

// Proof of Concept multi-threaded builder. Not yet implemented
final class MultiThreadedBuilder implements Builder {
    private final ExecutorService executorService;

    MultiThreadedBuilder(ExecutorService executorService) {
        this.executorService = executorService;
    }

    @Override
    public void build(Target t) throws Exception {
        var order = new ArrayList<Target>();
        var q = new ArrayDeque<Target>();
        var seen = new HashSet<Target>();

        q.add(t);

        while (!q.isEmpty()) {
            var node = q.removeFirst();
            seen.add(node);

            if (node.isUpToDate()) {
                continue;
            }

            order.add(node);

            for (var dependency : node.dependencies()) {
                if (!seen.contains(dependency)) {
                    q.addFirst(dependency);
                }
            }
        }

        var latches = new HashMap<Target, CountDownLatch>();
        for (var node : seen) {
            latches.put(node, new CountDownLatch(1));
        }

        for (var target : order.reversed()) {
            target.build();
        }
    }

    private Future<?> runTreeHelp(
            Map<Target, CountDownLatch> latches,
            Map<Target, List<Target>> tree,
            Target target
    ) {
        return executorService.submit(() -> {
            var children = new ArrayList<Future<?>>();
            for (var dep : tree.getOrDefault(target, List.of())) {
                children.add(runTreeHelp(latches, tree, dep));
            }
            for (var child : children) {
                try {

                    child.get();
                } catch (ExecutionException e) {
                    if (e.getCause() instanceof Exception e2) {
                        throw e2;
                    }
                    throw e;
                }
            }
            target.build();
            latches.get(target).countDown();
            return null;
        });
    }

    private void runTree(
            Map<Target, CountDownLatch> latches,
            Map<Target, List<Target>> tree,
            Target target
    ) throws Exception {
        try {
            runTreeHelp(latches, tree, target).get();
        } catch (ExecutionException e) {
            if (e.getCause() instanceof Exception e2) {
                throw e2;
            }
            throw e;
        }
    }

    @Override
    public String toString() {
        return "LinearBuilder";
    }
}
