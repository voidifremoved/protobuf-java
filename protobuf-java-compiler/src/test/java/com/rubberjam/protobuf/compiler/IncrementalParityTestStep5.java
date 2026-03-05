package com.rubberjam.protobuf.compiler;

import org.junit.Test;


public class IncrementalParityTestStep5 extends AbstractProtoParityTest
{
	@Test
	public void testParity() throws Exception
	{

		verifyParity("incremental_test_step_5.proto", "IncrementalTestStep5.java");
	}
}
