package com.rubberjam.protobuf.another.compiler;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

public class SCCTest {
    @Test
    public void testSCC() {
        SCC.Analyzer analyzer = new SCC.Analyzer(d -> new ArrayList<>());
        assertNotNull(analyzer);
    }
}
