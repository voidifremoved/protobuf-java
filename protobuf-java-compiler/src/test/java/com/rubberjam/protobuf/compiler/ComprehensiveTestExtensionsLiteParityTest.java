package com.rubberjam.protobuf.compiler;

import org.junit.Ignore;
import org.junit.Test;

public class ComprehensiveTestExtensionsLiteParityTest extends AbstractProtoParityTest {
	@Test
	public void testParity() throws Exception {
		verifyParity("comprehensive_test_extensions_lite_runtime.proto", "ComprehensiveTestExtensionsLite.java");
	}
}
