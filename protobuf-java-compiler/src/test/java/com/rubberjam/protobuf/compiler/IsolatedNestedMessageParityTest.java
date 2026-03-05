package com.rubberjam.protobuf.compiler;

import org.junit.Test;

public class IsolatedNestedMessageParityTest extends AbstractProtoParityTest {
	@Test
	public void testParity() throws Exception {
		verifyParity("IsolatedNestedMessage.proto", "IsolatedNestedMessage.java");
	}
}
