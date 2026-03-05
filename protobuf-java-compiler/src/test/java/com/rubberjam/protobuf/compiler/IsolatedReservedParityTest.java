package com.rubberjam.protobuf.compiler;

import org.junit.Test;

public class IsolatedReservedParityTest extends AbstractProtoParityTest {
	@Test
	public void testParity() throws Exception {
		verifyParity("IsolatedReserved.proto", "IsolatedReserved.java");
	}
}
