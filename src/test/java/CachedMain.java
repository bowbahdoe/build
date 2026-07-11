import dev.mccue.build.*;
import dev.mccue.build.file.FileTimes;
import dev.mccue.build.file.PathTarget;
import dev.mccue.tools.jar.Jar;
import dev.mccue.tools.javac.Javac;
import dev.mccue.tools.javadoc.Javadoc;

class CompileTarget implements Target {
    final Path input;
    final Path output;

    CompileTarget(Path input, Path output) {
        this.input = input;
        this.output = output;
    }

    @Override
    public List<Target> dependencies() {
        return List.of(
                PathTarget.cleanDirectory(output)
        );
    }

    @Override
    public boolean isUpToDate() throws Exception {
        return FileTimes.outputIsNewerThanInput(input, output);
    }

    @Override
    public void build() throws Exception {
        Javac.run(args -> args
                ._d(output)
                .arguments(Path.of("src", "test", "java", "Demo.java"))
        );
    }
}

class PackageTarget implements Target {
    final CompileTarget compileTarget;
    final Path output;

    PackageTarget(CompileTarget compileTarget, Path output) {
        this.compileTarget = compileTarget;
        this.output = output;
    }

    @Override
    public List<Target> dependencies() {
        return List.of(
                compileTarget,
                PathTarget.cleanDirectory(output)
        );
    }

    @Override
    public boolean isUpToDate() throws Exception {
        return compileTarget.isUpToDate() &&
                FileTimes.outputIsNewerThanInput(compileTarget.output, output);
    }

    @Override
    public void build() throws Exception {
        Jar.run(args -> args
                .__create()
                .__file(output.resolve("out.jar"))
                ._C(compileTarget.output, ".")
        );
    }
}

class DocumentTarget implements Target {
    final Path input;
    final Path output;

    DocumentTarget(Path input, Path output) {
        this.input = input;
        this.output = output;
    }

    @Override
    public List<Target> dependencies() {
        return List.of(
                PathTarget.cleanDirectory(output)
        );
    }

    @Override
    public boolean isUpToDate() throws Exception {
        return FileTimes.outputIsNewerThanInput(input, output);
    }

    @Override
    public void build() throws Exception {
        Javadoc.run(args -> args
                ._d(output)
                ._quiet()
                .argument(Path.of("src", "test", "java", "Demo.java"))
        );
    }
}

void main(String[] cliArgs) throws Exception {
    var source = Path.of("src", "test", "java", "Demo.java");
    var javacOut = Path.of("build", "javac");
    var compile = new CompileTarget(source, javacOut);

    var jarOut = Path.of("build", "jar");
    var package_ = new PackageTarget(compile, jarOut);

    var javadocOut = Path.of("build", "javadoc");
    var document = new DocumentTarget(source, javadocOut);

    var build = Builder.singleThreaded();
    for (var cliArg : cliArgs) {
        var tar = switch (cliArg) {
            case "clean" -> PathTarget.cleanDirectory(Path.of("build"));
            case "compile" -> compile;
            case "package" -> package_;
            case "document" -> document;
            case "all" -> Target.of(() -> {}, List.of(package_, document));
            default -> throw new IllegalStateException("Unexpected value: " + cliArg);
        };
        build.build(tar);
    }
}