package com.rubberjam.protobuf.another.compiler;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

public class ImporterTest {
    @Test
    public void testImporterSkeleton() {
        Importer.DiskSourceTree sourceTree = new Importer.DiskSourceTree();
        Importer importer = new Importer(sourceTree, null);
        assertNotNull(importer);
    }
}
