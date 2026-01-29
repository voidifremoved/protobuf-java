package com.rubberjam.protobuf.another.compiler;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.google.protobuf.Descriptors.FileDescriptor;

public class ImporterTest {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    private static class TestErrorCollector implements Importer.MultiFileErrorCollector {
        List<String> errors = new ArrayList<>();

        @Override
        public void recordError(String filename, int line, int column, String message) {
            errors.add(filename + ":" + line + ":" + column + ": " + message);
        }

        @Override
        public void recordWarning(String filename, int line, int column, String message) {
            errors.add("WARNING:" + filename + ":" + line + ":" + column + ": " + message);
        }

        public boolean hasErrors() {
            return !errors.isEmpty();
        }
    }

    private void createProtoFile(File root, String filename, String content) throws Exception {
        File file = new File(root, filename);
        file.getParentFile().mkdirs();
        try (FileOutputStream fos = new FileOutputStream(file)) {
            fos.write(content.getBytes(StandardCharsets.UTF_8));
        }
    }

    @Test
    public void testImportSimple() throws Exception {
        File root = tempFolder.newFolder("root");
        createProtoFile(root, "foo.proto",
            "syntax = \"proto2\";\n" +
            "package com.example;\n" +
            "message Foo {}\n");

        Importer.DiskSourceTree sourceTree = new Importer.DiskSourceTree();
        sourceTree.mapPath("", root.getAbsolutePath());

        TestErrorCollector collector = new TestErrorCollector();
        Importer importer = new Importer(sourceTree, collector);

        FileDescriptor fd = importer.importFile("foo.proto");

        assertNotNull(fd);
        assertEquals("foo.proto", fd.getName());
        assertEquals("com.example", fd.getPackage());
        assertFalse(collector.hasErrors());
    }

    @Test
    public void testImportWithDependency() throws Exception {
        File root = tempFolder.newFolder("root");
        createProtoFile(root, "bar.proto",
            "syntax = \"proto2\";\n" +
            "package com.example;\n" +
            "message Bar {}\n");

        createProtoFile(root, "foo.proto",
            "syntax = \"proto2\";\n" +
            "package com.example;\n" +
            "import \"bar.proto\";\n" +
            "message Foo {\n" +
            "  optional Bar bar = 1;\n" +
            "}\n");

        Importer.DiskSourceTree sourceTree = new Importer.DiskSourceTree();
        sourceTree.mapPath("", root.getAbsolutePath());

        TestErrorCollector collector = new TestErrorCollector();
        Importer importer = new Importer(sourceTree, collector);

        FileDescriptor fd = importer.importFile("foo.proto");

        assertNotNull(fd);
        assertEquals(1, fd.getDependencies().size());
        assertEquals("bar.proto", fd.getDependencies().get(0).getName());
        assertFalse(collector.hasErrors());
    }

    @Test
    public void testImportNotFound() throws Exception {
        Importer.DiskSourceTree sourceTree = new Importer.DiskSourceTree();
        TestErrorCollector collector = new TestErrorCollector();
        Importer importer = new Importer(sourceTree, collector);

        FileDescriptor fd = importer.importFile("notfound.proto");

        assertNull(fd);
        assertTrue(collector.hasErrors());
        assertTrue(collector.errors.get(0).contains("File not found"));
    }

    @Test
    public void testCircularDependency() throws Exception {
        File root = tempFolder.newFolder("root");
        createProtoFile(root, "a.proto",
            "syntax = \"proto2\";\n" +
            "import \"b.proto\";\n");
        createProtoFile(root, "b.proto",
            "syntax = \"proto2\";\n" +
            "import \"a.proto\";\n");

        Importer.DiskSourceTree sourceTree = new Importer.DiskSourceTree();
        sourceTree.mapPath("", root.getAbsolutePath());

        TestErrorCollector collector = new TestErrorCollector();
        Importer importer = new Importer(sourceTree, collector);

        FileDescriptor fd = importer.importFile("a.proto");

        assertNull(fd);
        assertTrue(collector.hasErrors());
    }

    @Test
    public void testPathTraversal() throws Exception {
        Importer.DiskSourceTree sourceTree = new Importer.DiskSourceTree();
        assertNull(sourceTree.open("../foo.proto"));
        assertTrue(sourceTree.getLastErrorMessage().contains("Invalid filename"));
    }
}
