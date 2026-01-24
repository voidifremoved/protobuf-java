package com.rubberjam.protobuf.compiler;

import org.junit.Test;

public class ComprehensiveTestV3ParityTest extends AbstractProtoParityTest
{
	@Test
	public void testParity() throws Exception
	{
		verifyParity("comprehensive_test_v3.proto", "ComprehensiveTestV3.java");
	}
}
