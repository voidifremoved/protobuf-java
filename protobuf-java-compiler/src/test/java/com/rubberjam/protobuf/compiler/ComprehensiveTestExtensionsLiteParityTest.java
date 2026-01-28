package com.rubberjam.protobuf.compiler;

import org.junit.Ignore;
import org.junit.Test;

@Ignore
public class ComprehensiveTestExtensionsLiteParityTest extends AbstractProtoParityTest
{
	@Test
	public void testParity() throws Exception
	{
		verifyParity("comprehensive_test_extensions__lite_runtime.proto", "ComprehensiveTestExtensionsLite.java");
	}
}
