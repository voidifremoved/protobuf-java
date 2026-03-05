package com.rubberjam.protobuf.compiler;

import org.junit.Test;


public class IncrementalParityTestStep6 extends AbstractProtoParityTest
{
	@Test
	public void testParity() throws Exception
	{

		verifyParity("incremental_test_step_6.proto", "IncrementalTestStep6.java");
	}
}
