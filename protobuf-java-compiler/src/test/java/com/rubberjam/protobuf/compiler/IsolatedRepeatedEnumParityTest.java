package com.rubberjam.protobuf.compiler;

import org.junit.Test;

public class IsolatedRepeatedEnumParityTest extends AbstractProtoParityTest {
	@Test
	public void testParity() throws Exception {
		verifyParity("IsolatedRepeatedEnum.proto", "IsolatedRepeatedEnum.java");
	}
}
