package com.rubberjam.protobuf.compiler;

import org.junit.Test;

public class ComprehensiveTestV3LiteParityTest extends AbstractProtoParityTest
{
	@Test
	public void testParity() throws Exception
	{
		verifyParity("comprehensive_test_v3_lite_runtime.proto", "ComprehensiveTestV3Lite.java");
	}
}
