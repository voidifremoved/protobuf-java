package com.rubberjam.protobuf.compiler;

import org.junit.Test;

public class IsolatedMapStringToIntParityTest extends AbstractProtoParityTest {
	@Test
	public void testParity() throws Exception {
		verifyParity("IsolatedMapStringToInt.proto", "IsolatedMapStringToInt.java");
	}
}
