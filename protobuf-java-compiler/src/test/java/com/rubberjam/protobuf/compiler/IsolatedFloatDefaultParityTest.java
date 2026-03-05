package com.rubberjam.protobuf.compiler;

import org.junit.Test;

public class IsolatedFloatDefaultParityTest extends AbstractProtoParityTest {
	@Test
	public void testParity() throws Exception {
		verifyParity("IsolatedFloatDefault.proto", "IsolatedFloatDefault.java");
	}
}
