package com.rubberjam.protobuf.another.compiler;

import static org.junit.Assert.assertNotNull;

import org.junit.Test;

public class RetentionTest {
    @Test
    public void testRetentionSkeleton() {
        // Just verify class exists and methods are reachable for now
        assertNotNull(Retention.class);
    }
}
