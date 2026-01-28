package com.rubberjam.protobuf.compiler;

import org.junit.Ignore;
import org.junit.Test;

@Ignore
public class ComprehensiveTestEdgeCasesLiteParityTest extends AbstractProtoParityTest
{
	@Test
	public void testParity() throws Exception
	{
		verifyParity("comprehensive_test_edge_cases_lite_runtime.proto", "ComprehensiveTestEdgeCasesLite.java");
	}
}
