package com.rubberjam.protobuf.compiler;

import org.junit.Test;

public class IsolatedNegativeFloatDefaultParityTest extends AbstractProtoParityTest {
	@Test
	public void testParity() throws Exception {
		verifyParity("IsolatedNegativeFloatDefault.proto", "IsolatedNegativeFloatDefault.java");
	}
}
