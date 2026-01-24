package com.rubberjam.protobuf.compiler;

import org.junit.Test;

public class ComprehensiveTestExtensionsParityTest extends AbstractProtoParityTest
{
	@Test
	public void testParity() throws Exception
	{
		verifyParity("comprehensive_test_extensions.proto", "ComprehensiveTestExtensions.java");
	}
}
