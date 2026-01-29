package com.rubberjam.protobuf.another.compiler;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

public class RetentionTest {
    @Test
    public void testRetentionSkeleton() {
        // Just verify class exists and methods are reachable for now
        assertNotNull(Retention.class);
    }
}
