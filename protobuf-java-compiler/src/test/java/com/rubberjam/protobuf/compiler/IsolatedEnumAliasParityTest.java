package com.rubberjam.protobuf.compiler;

import org.junit.Test;

public class IsolatedEnumAliasParityTest extends AbstractProtoParityTest {
	@Test
	public void testParity() throws Exception {
		verifyParity("IsolatedEnumAlias.proto", "IsolatedEnumAlias.java");
	}
}
