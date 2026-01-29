package com.rubberjam.protobuf.another.compiler.java;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class DocCommentTest
{

	@Test
	public void testEscaping()
	{
		// Tests based on doc_comment_unittest.cc

		// EXPECT_EQ("foo /&#42; bar *&#47; baz", EscapeJavadoc("foo /* bar */
		// baz"));
		assertEquals("foo /&#42; bar *&#47; baz",
				DocComment.escapeJavadoc("foo /* bar */ baz"));

		// EXPECT_EQ("foo /&#42;&#47; baz", EscapeJavadoc("foo /*/ baz"));
		assertEquals("foo /&#42;&#47; baz",
				DocComment.escapeJavadoc("foo /*/ baz"));

		// EXPECT_EQ("{&#64;foo}", EscapeJavadoc("{@foo}"));
		assertEquals("{&#64;foo}",
				DocComment.escapeJavadoc("{@foo}"));

		// EXPECT_EQ("&lt;i&gt;&amp;&lt;/i&gt;", EscapeJavadoc("<i>&</i>"));
		assertEquals("&lt;i&gt;&amp;&lt;/i&gt;",
				DocComment.escapeJavadoc("<i>&</i>"));

		// EXPECT_EQ("foo&#92;u1234bar", EscapeJavadoc("foo\\u1234bar"));
		assertEquals("foo&#92;u1234bar",
				DocComment.escapeJavadoc("foo\\u1234bar"));

		// EXPECT_EQ("&#64;deprecated", EscapeJavadoc("@deprecated"));
		assertEquals("&#64;deprecated",
				DocComment.escapeJavadoc("@deprecated"));
	}

	@Test
	public void testKDocEscaping()
	{
		// Additional tests for KDoc based on implementation logic in
		// doc_comment.cc

		// KDoc only strictly escapes */ to &#47; (and /* to &#42;)
		assertEquals("foo /&#42; bar *&#47; baz",
				DocComment.escapeKdoc("foo /* bar */ baz"));

		// Other symbols should remain untouched in KDoc
		assertEquals("@foo < bar >",
				DocComment.escapeKdoc("@foo < bar >"));
	}
}