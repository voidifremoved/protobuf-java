package com.rubberjam.protobuf.another.compiler;

import static org.junit.Assert.assertNotNull;

import java.util.ArrayList;

import org.junit.Test;

public class SCCTest {
    @Test
    public void testSCC() {
        SCC.Analyzer analyzer = new SCC.Analyzer(d -> new ArrayList<>());
        assertNotNull(analyzer);
    }
}
