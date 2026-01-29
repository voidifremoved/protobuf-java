package com.rubberjam.protobuf.another.compiler;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

public class ParserTest {
    @Test
    public void testParserSkeleton() {
        Parser parser = new Parser();
        assertNotNull(parser);
    }
}
