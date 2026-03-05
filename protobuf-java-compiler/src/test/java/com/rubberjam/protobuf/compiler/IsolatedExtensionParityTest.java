package com.rubberjam.protobuf.compiler;

import org.junit.Test;

public class IsolatedExtensionParityTest extends AbstractProtoParityTest {
	@Test
	public void testParity() throws Exception {
		verifyParity("IsolatedExtension.proto", "IsolatedExtension.java");
	}
}
