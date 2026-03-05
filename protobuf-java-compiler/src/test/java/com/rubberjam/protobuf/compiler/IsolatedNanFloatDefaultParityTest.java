package com.rubberjam.protobuf.compiler;

import org.junit.Test;

public class IsolatedNanFloatDefaultParityTest extends AbstractProtoParityTest {
	@Test
	public void testParity() throws Exception {
		verifyParity("IsolatedNanFloatDefault.proto", "IsolatedNanFloatDefault.java");
	}
}
