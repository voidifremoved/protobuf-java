package com.rubberjam.protobuf.compiler;

import org.junit.Test;

public class IsolatedRepeatedFieldParityTest extends AbstractProtoParityTest {
	@Test
	public void testParity() throws Exception {
		verifyParity("IsolatedRepeatedField.proto", "IsolatedRepeatedField.java");
	}
}
