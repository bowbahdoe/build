import dev.mccue.build.Builder;
import dev.mccue.build.file.PathTarget;
import dev.mccue.build.Target;
import dev.mccue.feather.DependencyInjector;
import dev.mccue.feather.Key;
import dev.mccue.feather.Provides;
import dev.mccue.tools.jar.Jar;
import dev.mccue.tools.javac.Javac;
import dev.mccue.tools.javadoc.Javadoc;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import java.nio.file.Path;
import java.util.Map;

public class FeatherTest {

    @Provides
    @Named("javacOut")
    @Singleton
    public PathTarget javacOut() {
        return PathTarget.cleanDirectory(Path.of("build", "javac"));
    }

    @Provides
    @Named("javac")
    @Singleton
    public Target javac(@Named("javacOut") PathTarget javacOut) {
        return Target.of(
                () -> Javac.run(args -> args
                        ._d(javacOut)
                        .arguments(Path.of("src", "test", "java", "Demo.java"))
                ),
                javacOut
        );
    }

    @Provides
    @Named("jarOut")
    @Singleton
    public PathTarget jarOut() {
        return PathTarget.cleanDirectory(Path.of("build", "jar"));
    }

    @Provides
    @Named("jar")
    @Singleton
    public Target jar(
            @Named("jarOut") PathTarget jarOut,
            @Named("javacOut") PathTarget javacOut,
            @Named("javac") Target javac
    ) {
        return Target.of(
                () -> Jar.run(args -> args
                        .__create()
                        .__file(jarOut.path().resolve("out.jar"))
                        ._C(javacOut, ".")
                ),
                javac
        );
    }


    @Provides
    @Named("javadocOut")
    @Singleton
    public PathTarget javadocOut() {
        return PathTarget.cleanDirectory(Path.of("build", "javadoc"));
    }


    @Provides
    @Named("javadoc")
    @Singleton
    public Target jar(
            @Named("javadocOut") PathTarget javadocOut
    ) {
        return Target.of(
                () -> Javadoc.run(args -> args
                        ._d(javadocOut)
                        ._quiet()
                        .argument(Path.of("src", "test", "java", "Demo.java"))
                ),
                javadocOut
        );
    }

    void main(String[] cliArgs) throws Exception {

        var di = DependencyInjector.builder()
                .module(this)
                .build();

        var builder = Builder.singleThreaded();
        for (var cliArg : cliArgs) {
            var targets = Map.of(
                    "clean", PathTarget.cleanDirectory(Path.of("build")),
                    "compile", di.instance(Key.of(Target.class, "javac")),
                    "package", di.instance(Key.of(Target.class, "jar")),
                    "document", di.instance(Key.of(Target.class, "javadoc"))
            );
            builder.build(targets.get(cliArg));
        }
    }
}
