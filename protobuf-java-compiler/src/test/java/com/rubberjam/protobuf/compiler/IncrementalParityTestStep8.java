package com.rubberjam.protobuf.compiler;

import org.junit.Test;


public class IncrementalParityTestStep8 extends AbstractProtoParityTest
{
	@Test
	public void testParity() throws Exception
	{

		verifyParity("incremental_test_step_8.proto", "IncrementalTestStep8.java");
	}
}
