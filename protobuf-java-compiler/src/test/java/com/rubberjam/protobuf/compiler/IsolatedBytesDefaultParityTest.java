package com.rubberjam.protobuf.compiler;

import org.junit.Test;

public class IsolatedBytesDefaultParityTest extends AbstractProtoParityTest {
	@Test
	public void testParity() throws Exception {
		verifyParity("IsolatedBytesDefault.proto", "IsolatedBytesDefault.java");
	}
}
