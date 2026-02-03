package com.rubberjam.protobuf.compiler;

import static org.junit.Assert.assertNotNull;

import java.util.ArrayList;

import org.junit.Test;

import com.rubberjam.protobuf.compiler.SCC;

public class SCCTest {
    @Test
    public void testSCC() {
        SCC.Analyzer analyzer = new SCC.Analyzer(d -> new ArrayList<>());
        assertNotNull(analyzer);
    }
}
