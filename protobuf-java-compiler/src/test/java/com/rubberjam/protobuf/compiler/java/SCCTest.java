package com.rubberjam.protobuf.compiler.java;

import static org.junit.Assert.assertNotNull;

import java.util.ArrayList;

import org.junit.Test;

import com.rubberjam.protobuf.compiler.SCC;
import com.rubberjam.protobuf.compiler.SCC.Analyzer;

public class SCCTest {
    @Test
    public void testSCC() {
        SCC.Analyzer analyzer = new SCC.Analyzer(d -> new ArrayList<>());
        assertNotNull(analyzer);
    }
}
