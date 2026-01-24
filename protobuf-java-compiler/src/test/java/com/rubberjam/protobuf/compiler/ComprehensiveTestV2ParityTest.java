package com.rubberjam.protobuf.compiler;

import org.junit.Test;

public class ComprehensiveTestV2ParityTest extends AbstractProtoParityTest
{
	@Test
	public void testParity() throws Exception
	{
		verifyParity("comprehensive_test_v2.proto", "ComprehensiveTestV2.java");
	}
}
