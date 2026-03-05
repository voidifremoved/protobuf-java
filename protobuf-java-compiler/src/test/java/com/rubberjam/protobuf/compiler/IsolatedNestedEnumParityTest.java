package com.rubberjam.protobuf.compiler;

import org.junit.Test;

public class IsolatedNestedEnumParityTest extends AbstractProtoParityTest {
	@Test
	public void testParity() throws Exception {
		verifyParity("IsolatedNestedEnum.proto", "IsolatedNestedEnum.java");
	}
}
