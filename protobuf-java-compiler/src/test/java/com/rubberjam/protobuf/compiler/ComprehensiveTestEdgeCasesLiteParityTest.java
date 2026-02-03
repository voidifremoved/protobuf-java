package com.rubberjam.protobuf.compiler;

import org.junit.Test;


public class ComprehensiveTestEdgeCasesLiteParityTest extends AbstractProtoParityTest
{
	@Test
	public void testParity() throws Exception
	{
		verifyParity("comprehensive_test_edge_cases_lite_runtime.proto", "ComprehensiveTestEdgeCasesLite.java");
	}
}
