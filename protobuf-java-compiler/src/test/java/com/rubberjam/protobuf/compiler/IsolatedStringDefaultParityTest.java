package com.rubberjam.protobuf.compiler;

import org.junit.Test;

public class IsolatedStringDefaultParityTest extends AbstractProtoParityTest {
	@Test
	public void testParity() throws Exception {
		verifyParity("IsolatedStringDefault.proto", "IsolatedStringDefault.java");
	}
}
