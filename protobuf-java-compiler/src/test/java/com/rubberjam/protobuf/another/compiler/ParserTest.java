package com.rubberjam.protobuf.another.compiler;

import static org.junit.Assert.assertNotNull;

import org.junit.Test;

public class ParserTest {
    @Test
    public void testParserSkeleton() {
        Parser parser = new Parser();
        assertNotNull(parser);
    }
}
