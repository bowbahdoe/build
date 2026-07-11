import dev.mccue.build.Builder;
import dev.mccue.build.file.PathTarget;
import dev.mccue.build.Target;
import dev.mccue.tools.jar.Jar;
import dev.mccue.tools.javac.Javac;
import dev.mccue.tools.javadoc.Javadoc;

void main(String[] cliArgs) throws Exception {
    var javacOut = PathTarget.cleanDirectory(Path.of("build", "javac"));
    var javac = Target.of(
            () -> {
                Javac.run(args -> args
                        ._d(javacOut)
                        .arguments(Path.of("src", "test", "java", "Demo.java"))
                );
            },
            javacOut
    );

    var jarOut = PathTarget.cleanDirectory(Path.of("build", "jar"));
    var jar = Target.of(
            () -> Jar.run(args -> args
                    .__create()
                    .__file(jarOut.path().resolve("out.jar"))
                    ._C(javacOut, ".")
            ),
            javac
    );

    var javadocOut = PathTarget.cleanDirectory(Path.of("build", "javadoc"));
    var javadoc = Target.of(
            () -> Javadoc.run(args -> args
                    ._d(javadocOut)
                    ._quiet()
                    .argument(Path.of("src", "test", "java", "Demo.java"))
            ),
            javadocOut
    );

    var build = Builder.singleThreaded();
    for (var cliArg : cliArgs) {
        var tar = switch (cliArg) {
            case "clean" -> PathTarget.cleanDirectory(Path.of("build"));
            case "compile" -> javac;
            case "package" -> jar;
            case "document" -> javadoc;
            case "all" -> Target.of(() -> {}, List.of(jar, javadoc));
            default -> throw new IllegalStateException("Unexpected value: " + cliArg);
        };
        build.build(tar);
    }
}