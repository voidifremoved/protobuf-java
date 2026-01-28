package com.rubberjam.protobuf.compiler;

import org.junit.Ignore;
import org.junit.Test;

@Ignore
public class ComprehensiveTestV3SpeedParityTest extends AbstractProtoParityTest
{
	@Test
	public void testParity() throws Exception
	{
		verifyParity("comprehensive_test_v3_speed.proto", "ComprehensiveTestV3Speed.java");
	}
}
