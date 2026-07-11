package dev.mccue.build;

import java.util.*;

final class SingleThreadedBuilder implements Builder {
    static final SingleThreadedBuilder INSTANCE = new SingleThreadedBuilder();

    private SingleThreadedBuilder() {}

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

        for (var target : order.reversed()) {
            target.build();
        }
    }

    @Override
    public String toString() {
        return "LinearBuilder";
    }
}
