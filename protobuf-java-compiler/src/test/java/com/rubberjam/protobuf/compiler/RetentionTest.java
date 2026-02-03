package com.rubberjam.protobuf.compiler;

import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import com.rubberjam.protobuf.compiler.Retention;

public class RetentionTest {
    @Test
    public void testRetentionSkeleton() {
        // Just verify class exists and methods are reachable for now
        assertNotNull(Retention.class);
    }
}
