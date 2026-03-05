package com.rubberjam.protobuf.compiler;

import org.junit.Test;

public class IsolatedIntDefaultParityTest extends AbstractProtoParityTest {
	@Test
	public void testParity() throws Exception {
		verifyParity("IsolatedIntDefault.proto", "IsolatedIntDefault.java");
	}
}
