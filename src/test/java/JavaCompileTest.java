import dev.mccue.build.*;
import dev.mccue.build.file.PathTarget;
import dev.mccue.tools.jar.Jar;
import dev.mccue.tools.javac.Javac;
import dev.mccue.tools.javadoc.Javadoc;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

public class JavaCompileTest {


    @Test
    public void testCompilingJava() throws Exception {
        var javacOut = PathTarget.cleanDirectory(Path.of("build", "javac"));
        var javac = Target.of(
                () -> Javac.run(args -> args
                        ._d(javacOut)
                        .arguments(Path.of("src", "test", "java", "Demo.java"))
                ),
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

        var builder = Builder.singleThreaded();
        builder.build(jar);
        builder.build(javadoc);
    }
}
