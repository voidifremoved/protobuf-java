package com.rubberjam.protobuf.another.compiler;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class CodeGeneratorLiteTest {
    @Test
    public void testParseGeneratorParameter() {
        List<Map.Entry<String, String>> output = new ArrayList<>();
        CodeGeneratorLite.parseGeneratorParameter("foo=bar,baz", output);
        assertEquals(2, output.size());
        assertEquals("foo", output.get(0).getKey());
        assertEquals("bar", output.get(0).getValue());
        assertEquals("baz", output.get(1).getKey());
        assertEquals("", output.get(1).getValue());
    }
}
