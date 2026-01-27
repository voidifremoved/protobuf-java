package com.rubberjam.protobuf.compiler;

import org.junit.Test;

public class ComprehensiveTestExtensionsMinimalParityTest extends AbstractProtoParityTest
{
	@Test
	public void testParity() throws Exception
	{
		verifyParity("comprehensive_test_extensions_minimal.proto", "ComprehensiveTestExtensionsMinimal.java");
	}
}
