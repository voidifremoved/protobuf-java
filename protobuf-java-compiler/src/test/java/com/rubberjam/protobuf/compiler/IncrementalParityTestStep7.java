package com.rubberjam.protobuf.compiler;

import org.junit.Test;


public class IncrementalParityTestStep7 extends AbstractProtoParityTest
{
	@Test
	public void testParity() throws Exception
	{

		verifyParity("incremental_test_step_7.proto", "IncrementalTestStep7.java");
	}
}
