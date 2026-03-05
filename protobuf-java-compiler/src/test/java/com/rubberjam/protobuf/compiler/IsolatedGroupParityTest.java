package com.rubberjam.protobuf.compiler;

import org.junit.Test;

public class IsolatedGroupParityTest extends AbstractProtoParityTest {
	@Test
	public void testParity() throws Exception {
		verifyParity("IsolatedGroup.proto", "IsolatedGroup.java");
	}
}
