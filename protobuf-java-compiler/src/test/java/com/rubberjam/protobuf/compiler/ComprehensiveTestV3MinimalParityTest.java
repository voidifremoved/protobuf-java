package com.rubberjam.protobuf.compiler;

import org.junit.Test;

public class ComprehensiveTestV3MinimalParityTest extends AbstractProtoParityTest
{
	@Test
	public void testParity() throws Exception
	{
		verifyParity("comprehensive_test_v3_minimal.proto", "ComprehensiveTestV3Minimal.java");
	}
}
