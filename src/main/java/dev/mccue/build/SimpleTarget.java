package dev.mccue.build;

import java.util.List;

record SimpleTarget(
        Buildable buildable,
        List<Target> dependencies
) implements Target {
    @Override
    public void build() throws Exception {
        buildable.build();
    }

    @Override
    public List<Target> dependencies() {
        return dependencies;
    }

    @Override
    public boolean isUpToDate() throws Exception {
        return Target.super.isUpToDate();
    }
}
