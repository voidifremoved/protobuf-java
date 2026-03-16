package com.rubberjam.protobuf.compiler.java;

import org.junit.Test;

public class ComprehensiveTestEdgeCasesMinimalParityTest extends AbstractProtoParityTest
{




	@Test
	public void testParity() throws Exception
	{
		verifyParity("comprehensive_test_edge_cases_minimal.proto", "ComprehensiveTestEdgeCasesMinimal.java");
	}
	
}

