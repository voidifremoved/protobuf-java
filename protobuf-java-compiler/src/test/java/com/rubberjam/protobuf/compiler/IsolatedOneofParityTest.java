package com.rubberjam.protobuf.compiler;

import org.junit.Test;

public class IsolatedOneofParityTest extends AbstractProtoParityTest {
	@Test
	public void testParity() throws Exception {
		verifyParity("IsolatedOneof.proto", "IsolatedOneof.java");
	}
}
