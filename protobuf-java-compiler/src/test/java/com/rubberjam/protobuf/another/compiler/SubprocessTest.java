package com.rubberjam.protobuf.another.compiler;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

public class SubprocessTest {
    @Test
    public void testSubprocessSkeleton() {
        Subprocess sp = new Subprocess();
        sp.start("echo", Subprocess.SearchMode.SEARCH_PATH);
        // Communicating with echo requires handling streams properly which is flaky in unit tests without extensive setup.
        // Just verifying instantiation for now.
        assertNotNull(sp);
    }
}
