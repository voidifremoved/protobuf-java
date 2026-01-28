package com.rubberjam.protobuf.compiler;

import org.junit.Ignore;
import org.junit.Test;

@Ignore
public class ComprehensiveTestV2LiteParityTestLite extends AbstractProtoParityTest
{
	@Test
	public void testParity() throws Exception
	{
		verifyParity("comprehensive_test_v2_lite_runtime.proto", "ComprehensiveTestV2Lite.java");
	}
}
