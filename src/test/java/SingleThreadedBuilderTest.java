import dev.mccue.build.Builder;
import dev.mccue.build.file.PathTarget;
import dev.mccue.build.Target;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class SingleThreadedBuilderTest {

    static abstract class TrackedTarget implements Target {
        boolean built = false;
        int count = 0;

        @Override
        public void build() {
            built = true;
            count++;
        }
    }

    static class SimpleTarget extends TrackedTarget {
    }


    @Test
    public void testSimpleTarget() throws Exception {

        var t = new SimpleTarget();

        var linearBuild = Builder.singleThreaded();
        linearBuild.build(t);

        assertTrue(t.built);
    }

    @Test
    public void testChain() throws Exception {
        var a = new SimpleTarget();
        var b = new SimpleTarget() {
            @Override
            public List<Target> dependencies() {
                return List.of(a);
            }
        };
        var c = new SimpleTarget() {
            @Override
            public List<Target> dependencies() {
                return List.of(b);
            }
        };
        var linearBuild = Builder.singleThreaded();

        linearBuild.build(c);


        assertTrue(a.built);
        assertTrue(b.built);
        assertTrue(c.built);
    }

    @Test
    public void testChainWithReady() throws Exception {
        var a = new SimpleTarget();
        var b = new SimpleTarget() {
            @Override
            public List<Target> dependencies() {
                return List.of(a);
            }

            @Override
            public boolean isUpToDate() {
                return true;
            }
        };
        var c = new SimpleTarget() {
            @Override
            public List<Target> dependencies() {
                return List.of(b);
            }
        };

        var linearBuild = Builder.singleThreaded();
        linearBuild.build(c);


        assertFalse(a.built);
        assertFalse(b.built);
        assertTrue(c.built);
    }


    @Test
    public void testTree() throws Exception {
        var a = new SimpleTarget();
        var b = new SimpleTarget() {
            @Override
            public List<Target> dependencies() {
                return List.of(a);
            }
        };
        var c = new SimpleTarget() {
            @Override
            public List<Target> dependencies() {
                return List.of(a);
            }
        };
        var d = new SimpleTarget() {
            @Override
            public List<Target> dependencies() {
                return List.of(b, c);
            }
        };
        var linearBuild = Builder.singleThreaded();
        linearBuild.build(d);


        assertTrue(a.built);
        assertTrue(b.built);
        assertTrue(c.built);
        assertTrue(d.built);

        assertEquals(1, a.count);
        assertEquals(1, b.count);
        assertEquals(1, c.count);
        assertEquals(1, d.count);
    }


    @Test
    public void testTreeWithReady() throws Exception {
        var a = new SimpleTarget();
        var b = new SimpleTarget() {
            @Override
            public List<Target> dependencies() {
                return List.of(a);
            }
        };
        var c = new SimpleTarget() {
            @Override
            public boolean isUpToDate() {
                return true;
            }

            @Override
            public List<Target> dependencies() {
                return List.of(a);
            }
        };
        var d = new SimpleTarget() {
            @Override
            public List<Target> dependencies() {
                return List.of(b, c);
            }
        };
        var linearBuild = Builder.singleThreaded();
        linearBuild.build(d);


        assertTrue(a.built);
        assertTrue(b.built);
        assertFalse(c.built);
        assertTrue(d.built);

        assertEquals(1, a.count);
        assertEquals(1, b.count);
        assertEquals(0, c.count);
        assertEquals(1, d.count);
    }

    private void deleteAll(Path path) throws Exception {
        try {
            Files.walkFileTree(path, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path path, BasicFileAttributes basicFileAttributes) throws IOException
                {
                    Files.delete(path);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path directory, IOException ioException) throws IOException
                {
                    Files.delete(directory);
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (NoSuchFileException e) {
            // pass
        }
    }
    @Test
    public void testEmptyDir() throws Exception {
        for (var testPath : List.of(
                Path.of("demo"),
                Path.of("demo2"),
                Path.of("demo3")
        )) {
            deleteAll(testPath);
            try {
                var dir = PathTarget.cleanDirectory(testPath);
                assertFalse(dir.isUpToDate());
                assertFalse(Files.exists(testPath));
                dir.build();
                assertTrue(dir.isUpToDate());
                assertTrue(Files.exists(testPath));

            } finally {
                deleteAll(testPath);
            }
        }

        var p = Path.of("a", "b", "c", "d");
        Path a = Path.of("a");
        deleteAll(a);
        try {
            var dir = PathTarget.cleanDirectory(p);
            assertFalse(dir.isUpToDate());
            assertFalse(Files.exists(p));

            dir.build();
            assertTrue(dir.isUpToDate());
            assertTrue(Files.exists(p));
            assertTrue(Files.isDirectory(p));
        } finally {
            deleteAll(a);
        }

    }
}
