package com.rubberjam.protobuf.compiler;

import org.junit.Test;

public class IsolatedOptimizeForSpeedParityTest extends AbstractProtoParityTest {
	@Test
	public void testParity() throws Exception {
		verifyParity("IsolatedOptimizeForSpeed.proto", "IsolatedOptimizeForSpeed.java");
	}
}
