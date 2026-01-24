package com.rubberjam.protobuf.compiler;

import org.junit.Test;

public class ComprehensiveTestNestedParityTest extends AbstractProtoParityTest
{
	@Test
	public void testParity() throws Exception
	{
		verifyParity("comprehensive_test_nested.proto", "ComprehensiveTestNested.java");
	}
}
