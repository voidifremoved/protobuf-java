package com.rubberjam.protobuf.compiler;

import org.junit.Test;

public class ComprehensiveTestV2MinimalParityTest extends AbstractProtoParityTest
{
	@Test
	public void testParity() throws Exception
	{
		verifyParity("comprehensive_test_v2_minimal.proto", "ComprehensiveTestV2Minimal.java");
	}
}
