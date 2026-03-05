package com.rubberjam.protobuf.compiler;

import org.junit.Test;

public class IsolatedBoolDefaultParityTest extends AbstractProtoParityTest {
	@Test
	public void testParity() throws Exception {
		verifyParity("IsolatedBoolDefault.proto", "IsolatedBoolDefault.java");
	}
}
