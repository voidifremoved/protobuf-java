package com.rubberjam.protobuf.compiler;

import org.junit.Test;

public class IsolatedInfFloatDefaultParityTest extends AbstractProtoParityTest {
	@Test
	public void testParity() throws Exception {
		verifyParity("IsolatedInfFloatDefault.proto", "IsolatedInfFloatDefault.java");
	}
}
