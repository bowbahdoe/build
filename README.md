# build

```xml
<dependency>
    <groupId>dev.mccue</groupId>
    <artifactId>build</artifactId>
    <version>2026.07.11</version>
</dependency>
```

The simplest build system I can come up with.

## Explanation

The essential property of a build system is minimality.

> A build system is minimal if it executes tasks at most once per build
> and only if they transitively depend on inputs that changed since the previous build.

Put another way, the distinction between "a program that builds code" and "a build system"
is whether that program successfully avoids repeated work.

The central abstraction of this library is a `Target`. A target represents a unit of code that will
result in a side effect. 
- For a target that compiles `.java` files the side effect might be writing `.class`
files to the filesystem. 
- For a target that makes an API call the side effect might be storing the response in a mutable field

And so on.

If a `Target` is "up to date" that means that the side effect it was meant to perform has already been performed
and therefore the target does not need to be built again.

`Target`s can also declare dependencies. Say you want to bundle code into a `jar`.

- The target for building the `jar` might depend on a target that compiles `.java` files into `.class` files.
- The target for compiling the `.java` files might depend on a target that prepares a fresh directory for outputting `.class` files into.

And each of these targets would, at this level of abstraction, be on the hook for knowing whether
they are up to date.

To start a build, a `Target` is given to a `Builder` which is responsible for building that target
and all of its transitive dependencies (at least, any which aren't up to date.)

`Builder`s have leeway in how they can schedule execution of targets. Only one implementation is
provided as part of this library.

- `Builder.singleThreaded()` topologically sorts all the tasks to perform and runs them all on a single thread.

Making a multi-threaded builder is left as an exercise for the reader.

The `FileTimes`, `PathTarget`, and `InputFiles`. classes are provided to help with implementing builds that work with resources on the filesystem.
Specifically implementing the minimality approach of "are the input files newer than the output files" and for
obtaining fresh output directories (with `PathTarget.cleanDirectory(...)`.)



## Example Usage

Given a program that compiles and packages code.

```java 
import module dev.mccue.tools.jdk;
import module dev.mccue.build;

void main() throws Exception {
    List<Path> sources = InputFiles.withExtension(Path.of("src"), ".java");

    var javacOut = Path.of("build", "javac");
    Javac.run(args -> args
            ._d(javacOut)
            .arguments(src));
    
    var jarOut = Path.of("build", "app.jar");
    Jar.run(args -> args
            .__create()
            .__file(jarOut)
            ._C(javacOut, ".")
    );
}
```

We can split that program into targets.

```java 
import module dev.mccue.tools.jdk;
import module dev.mccue.build;

void main() throws Exception {
    List<Path> sources = InputFiles.withExtension(Path.of("src"), ".java");

    var javacOut = Path.of("build", "javac");

    var compileTarget = Target.of(() -> {
        Javac.run(args -> args
                ._d(javacOut)
                .arguments(sources));
    });

    var jarOut = Path.of("build", "app.jar");
    var packageTarget = Target.of(() -> {
        Jar.run(args -> args
                .__create()
                .__file(jarOut)
                ._C(javacOut, "."));
    }, compileTarget); // packageTarget depends on compileTarget


    var builder = Builder.singleThreaded();

    // will build packageTarget followed by compileTarget
    builder.build(jarTarget);
}
```

Using `PathTarget.cleanDirectory()` we can make it so that the output of `javac` always goes into
a new folder without any of the class files from previous runs.

```java 
import module dev.mccue.tools.jdk;
import module dev.mccue.build;

void main() throws Exception {
    List<Path> sources = InputFiles.withExtension(Path.of("src"), ".java");

    var javacOut = Path.of("build", "javac");

    var compileTarget = Target.of(() -> {
        Javac.run(args -> args
                ._d(javacOut)
                .arguments(sources));
    }, PathTarget.cleanDirectory(javacOut));
    // ^ Will clear out any stray class files before run

    var jarOut = Path.of("build", "app.jar");
    var packageTarget = Target.of(() -> {
        Jar.run(args -> args
                .__create()
                .__file(jarOut)
                ._C(javacOut, "."));
    }, compileTarget); // packageTarget depends on compileTarget

    var builder = Builder.singleThreaded();

    // will build packageTarget followed by compileTarget
    builder.build(packageTarget);
}
```

And we can implement any number of schemes to select a task to run by name.

```java 
import module dev.mccue.tools.jdk;
import module dev.mccue.build;

void main(String[] cliArgs) throws Exception {
    List<Path> sources = dev.mccue.build.file.InputFiles.withExtension(Path.of("src"), ".java");

    var javacOut = Path.of("build", "javac");

    var compileTarget = Target.of(() -> {
        Javac.run(args -> args
                ._d(javacOut)
                .arguments(sources));
    }, PathTarget.cleanDirectory(javacOut));
    // ^ Will clear out any stray class files before run

    var jarOut = Path.of("build", "app.jar");
    var packageTarget = Target.of(() -> {
        Jar.run(args -> args
                .__create()
                .__file(jarOut)
                ._C(javacOut, "."));
    }, compileTarget); // jarTarget depends on javacTarget

    var builder = Builder.singleThreaded();

    switch (cliArgs[0]) {
        case "compile" -> builder.build(compileTarget);
        case "package" -> builder.build(packageTarget);
        default -> {
            System.err.println("Unknown target: " + cliArgs[0]);
            System.exit(1);
        }
    }
}
```

This gives us a solid foundation to add more targets to.

```java 
import module dev.mccue.tools.jdk;
import module dev.mccue.build;

void main(String[] cliArgs) throws Exception {
    List<Path> sources = InputFiles.withExtension(Path.of("src"), ".java");

    var javacOut = Path.of("build", "javac");

    var compileTarget = Target.of(() -> {
        Javac.run(args -> args
                ._d(javacOut)
                .arguments(sources));
    }, PathTarget.cleanDirectory(javacOut));
    // ^ Will clear out any stray class files before run

    var jarOut = Path.of("build", "app.jar");
    var packageTarget = Target.of(() -> {
        Jar.run(args -> args
                .__create()
                .__file(jarOut)
                ._C(javacOut, "."));
    }, compileTarget); // jarTarget depends on javacTarget

    var javadocOut = Path.of("build", "javadoc");
    var documentTarget = Target.of(() -> {
        Javadoc.run(args -> args
                ._d(javadocOut)
                .arguments(sources));
    }, PathTarget.cleanDirectory(javadocOut));

    var allTarget = Target.combine(packageTarget, documentTarget);

    var builder = Builder.singleThreaded();

    switch (cliArgs[0]) {
        case "compile" -> builder.build(compileTarget);
        case "package" -> builder.build(packageTarget);
        case "document" -> builder.build(documentTarget);
        default -> builder.build(allTarget);
    }
}
```

Now we have a perfectly functional build, but it is not minimal. If we were to run 
the `compile` target it would always compile from scratch, regardless of if the source
has changed since we last compiled.

To deal with this, we can make our own implementations of `Target`. These can implement any sort of
up-to-dated-ness check that they want to. The `make` strategy of comparing file timestamps
is available via `FileTimes.outputIsNewerThanInput`.

```java 
import module dev.mccue.tools.jdk;
import module dev.mccue.build;

class CompileTarget implements Target {
    final List<Path> sources;
    final Path output;

    CompileTarget(List<Path> sources, Path output) {
        this.sources = sources;
        this.output = output;
    }

    @Override
    public void build() throws Exception {
        Javac.run(args -> args
                ._d(output)
                .arguments(sources));
    }

    @Override
    public List<Target> dependencies() {
        // compilation depends on having a clean output directory
        return List.of(
                dev.mccue.build.file.PathTarget.cleanDirectory(javacOut)
        );
    }

    @Override
    public boolean isUpToDate() throws Exception {
        return FileTimes.outputIsNewerThanInput(src, javacOut);
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
    public void build() throws Exception {
        Jar.run(args -> args
                    .__create()
                    .__file(output)
                    ._C(compileTarget.output, "."));
    }

    @Override
    public List<Target> dependencies() {
        return List.of(compileTarget);
    }

    @Override
    public boolean isUpToDate() throws Exception {
        return compileTarget.isUpToDate() &&
                FileTimes.outputIsNewerThanInput(compileTarget.output, output);
    }
}

class DocumentTarget implements Target {
    final List<Path> sources;
    final Path output;

    DocumentTarget(List<Path> sources, Path output) {
        this.sources = sources;
        this.output = output;
    }

    @Override
    public void build() throws Exception {
        Javadoc.run(args -> args
                        ._d(javadocOut)
                        .arguments(sources));
    }

    @Override
    public List<Target> dependencies() {
        return List.of(
                PathTarget.cleanDirectory(javacOut)
        );
    }

    @Override
    public boolean isUpToDate() throws Exception {
        return FileTimes.outputIsNewerThanInput(src, javacOut);
    }
}

void main(String[] cliArgs) throws Exception {
    List<Path> sources = InputFiles.withExtension(Path.of("src"), ".java");

    var javacOut = Path.of("build", "javac");
    var compileTarget = new CompileTarget(sources, javacOut);

    var jarOut = Path.of("build", "app.jar");
    var packageTarget = new PackageTarget(sources, jarOut);

    // I don't want the example to get too long, so use
    // your imagination for these.
    var javadocOut = Path.of("build", "javadoc");
    var documentTarget = new DocumentTarget(sources, javadocOut);

    var allTarget = Target.combine(packageTarget, documentTarget);

    var builder = Builder.singleThreaded();

    switch (cliArgs[0]) {
        case "compile" -> builder.build(compileTarget);
        case "package" -> builder.build(packageTarget);
        case "document" -> builder.build(documentTarget);
        default -> builder.build(allTarget);
    }
}
```

And that is basically it. More mechanisms can be built on top of this compositionally.
This includes more advanced strategies for tracking up-to-date-ness, different builders,
utilities for building certain languages, and more.


## References

[Build Systems à la Carte](https://www.microsoft.com/en-us/research/wp-content/uploads/2018/03/build-systems.pdf)
> Andrey Mokhov, Neil Mitchell, and Simon Peyton Jones. 2018. Build Systems à la Carte. Proc. ACM Program.
> Lang. 2, ICFP, Article 79 (September 2018), 29 pages. https://doi.org/10.1145/3236774

