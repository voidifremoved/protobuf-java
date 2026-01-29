package com.rubberjam.protobuf.io;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.NoSuchElementException;
import java.util.function.BooleanSupplier;

import org.junit.Before;
import org.junit.Test;

public class PrinterTest
{
	private Printer printer;
	private FakeAnnotationCollector collector;
	private Printer.Options options;

	@Before
	public void setUp()
	{
		options = new Printer.Options();
		collector = new FakeAnnotationCollector();
		options.annotationCollector = collector;
		printer = new Printer(options);
	}

	private static class FakeAnnotationCollector implements Printer.AnnotationCollector
	{
		static class Record
		{
			int start;
			int end;
			String filePath;
			List<Integer> path;
			Printer.Semantic semantic;

			Record(int start, int end, String filePath, List<Integer> path, Printer.Semantic semantic)
			{
				this.start = start;
				this.end = end;
				this.filePath = filePath;
				this.path = path;
				this.semantic = semantic;
			}

			@Override
			public String toString()
			{
				return String.format("Record{%d, %d, \"%s\", %s, %s}",
						start, end, filePath, path, semantic);
			}

			@Override
			public boolean equals(Object o)
			{
				if (this == o) return true;
				if (!(o instanceof Record)) return false;
				Record record = (Record) o;
				return start == record.start &&
						end == record.end &&
						Objects.equals(filePath, record.filePath) &&
						Objects.equals(path, record.path) &&
						semantic == record.semantic;
			}
		}

		final List<Record> annotations = new ArrayList<>();

		@Override
		public void addAnnotation(int begin, int end, String file, List<Integer> path, Printer.Semantic semantic)
		{
			annotations.add(new Record(begin, end, file, new ArrayList<>(path), semantic));
		}

		void assertContains(Record... expected)
		{
			assertEquals("Mismatch in annotations count", expected.length, annotations.size());
			for (int i = 0; i < expected.length; i++)
			{
				assertEquals("Mismatch at index " + i, expected[i], annotations.get(i));
			}
		}
	}

	@Test
	public void testBasicPrinting()
	{
		printer.emit("Hello World!");
		printer.emit("  This is the same line.\n");
		printer.emit("But this is a new one.\nAnd this is another one.");

		assertEquals(
				"Hello World!  This is the same line.\n" +
						"But this is a new one.\n" +
						"And this is another one.",
				printer.toString());
	}

	@Test
	public void testVariableSubstitution()
	{
		Map<String, Object> vars = new java.util.HashMap<>();
		vars.put("foo", "World");
		vars.put("bar", "$foo$");
		vars.put("abcdefg", "1234");

		printer.emit(vars, "Hello $foo$!\nbar = $bar$\n");
		printer.emitRaw("RawBit\n");
		printer.emit(vars, "$abcdefg$\nA literal dollar sign:  $$");

		vars.put("foo", "blah");
		printer.emit(vars, "\nNow foo = $foo$.");

		assertEquals(
				"Hello World!\n" +
						"bar = $foo$\n" +
						"RawBit\n" +
						"1234\n" +
						"A literal dollar sign:  $\n" +
						"Now foo = blah.",
				printer.toString());
	}

	@Test
	public void testIndenting() throws Exception
	{
		Map<String, Object> vars = Map.of("newline", "\n");

		printer.emit("This is not indented.\n");
		try (AutoCloseable i1 = printer.withIndent())
		{
			printer.emit("This is indented\nAnd so is this\n");
		}

		printer.emit("But this is not.");

		try (AutoCloseable i2 = printer.withIndent())
		{
			printer.emit("  And this is still the same line.\nBut this is indented.\n");
			printer.emitRaw("RawBit has indent at start\n");
			printer.emitRaw("but not after a raw newline\n");
			printer.emit(vars, "Note that a newline in a variable will break indenting, as we see$newline$here.\n");

			try (AutoCloseable i3 = printer.withIndent())
			{
				printer.emit("And this");
			}
		}

		printer.emit(" is double-indented\nBack to normal.");

		assertEquals(
				"This is not indented.\n" +
						"  This is indented\n" +
						"  And so is this\n" +
						"But this is not.  And this is still the same line.\n" +
						"  But this is indented.\n" +
						"  RawBit has indent at start\n" +
						"but not after a raw newline\n" +
						"Note that a newline in a variable will break indenting, as we see\n" +
						"here.\n" +
						"    And this is double-indented\n" +
						"Back to normal.",
				printer.toString());
	}

	@Test
	public void testEmitWithSubs()
	{
		printer.emit(
				Map.of("class", "Foo", "f1", "x", "f2", "y", "f3", "z", "init", 42),
				"\n" + """
						  class $class$ {
						    int $f1$, $f2$, $f3$ = $init$;
						  };
						""");

		assertEquals(
				"class Foo {\n" +
						"  int x, y, z = 42;\n" +
						"};\n",
				printer.toString());
	}

	@Test
	public void testEmitComments()
	{
		printer.emit("\n" + """
				  // Yes.
				  //~ No.
				""");
		printer.emit("//~ Not a raw string.");

		assertEquals("// Yes.\n//~ Not a raw string.", printer.toString());
	}

	@Test
	public void testEmitConsumeAfter()
	{
		Printer.PrinterValue val = new Printer.PrinterValue("int x;");
		val.consumeAfter = ";";

		printer.emit(
				Map.of("class", "Foo", "var", val),
				"\n" + """
						  class $class$ {
						    $var$;
						  };
						""");

		assertEquals(
				"class Foo {\n" +
						"  int x;\n" +
						"};\n",
				printer.toString());
	}

	@Test
	public void testEmitWithIndentAndIgnoredComment() throws Exception
	{
		try (AutoCloseable i = printer.withIndent())
		{
			printer.emit(
					Map.of("f1", "x", "f2", "y", "f3", "z"),
					"\n" + """
							  //~ First line comment.
							  class Foo {
							    int $f1$, $f2$, $f3$;
							  };
							""");
		}

		assertEquals(
				"  class Foo {\n" +
						"    int x, y, z;\n" +
						"  };\n",
				printer.toString());
	}

	@Test
	public void testEmitWithCPPDirectiveOnFirstLine()
	{
		printer.emit(
				Map.of("f1", "x", "f2", "y", "f3", "z"),
				"\n" + """
						#if NDEBUG
						#pragma foo
						      class Foo {
						        int $f1$, $f2$, $f3$;
						      };
						#endif
						""");

		assertEquals(
				"#if NDEBUG\n" +
						"#pragma foo\n" +
						"class Foo {\n" +
						"  int x, y, z;\n" +
						"};\n" +
						"#endif\n",
				printer.toString());
	}

	@Test
	public void testEmitCallbacks()
	{
		Printer.PrinterValue methods = new Printer.PrinterValue((BooleanSupplier) () ->
		{
			printer.emit("\n                 int $method$() { return 42; }");
			return true;
		});

		Printer.PrinterValue fields = new Printer.PrinterValue((BooleanSupplier) () ->
		{
			printer.emit("\n                 int $method$_;");
			return true;
		});

		printer.emit(
				Map.of(
						"class", "Foo",
						"method", "bar",
						"methods", methods,
						"fields", fields),
				"\n" + """
						  class $class$ {
						   public:
						    $methods$;

						   private:
						    $fields$;
						  };
						""");

		assertEquals(
				"class Foo {\n" +
						" public:\n" +
						"  int bar() { return 42; }\n" +
						"\n" +
						" private:\n" +
						"  int bar_;\n" +
						"};\n",
				printer.toString());
	}

	@Test
	public void testPreserveNewlinesThroughEmits()
	{
		List<String> insertionLines = List.of("// line 1", "// line 2");

		Printer.PrinterValue insertCallback = new Printer.PrinterValue((BooleanSupplier) () ->
		{
			for (String line : insertionLines)
			{
				printer.emit(Map.of("line", line), "\n                  $line$");
			}
			return true;
		});

		printer.emit(
				Map.of("insert_lines", insertCallback),
				"\n" + """
						  // one
						  // two

						  $insert_lines$;

						  // three
						  // four
						""");

		assertEquals(
				"// one\n" +
						"// two\n" +
						"\n" +
						"// line 1\n" +
						"// line 2\n" +
						"\n" +
						"// three\n" +
						"// four\n",
				printer.toString());
	}

	@Test
	public void testAnnotateMap()
	{
		Printer.AnnotationRecord r1 = new Printer.AnnotationRecord("path_1", List.of(33), Printer.Semantic.NONE);
		Printer.AnnotationRecord r2 = new Printer.AnnotationRecord("path_2", List.of(11, 22), Printer.Semantic.NONE);

		printer.pushVars(Map.of("foo", "3", "bar", "5"));
		printer.pushVars(Map.of("foo", r1, "bar", r2));

		printer.emit("012$foo$4$bar$\n");

		assertEquals("012345\n", printer.toString());
		collector.assertContains(
				new FakeAnnotationCollector.Record(3, 4, "path_1", List.of(33), Printer.Semantic.NONE),
				new FakeAnnotationCollector.Record(5, 6, "path_2", List.of(11, 22), Printer.Semantic.NONE));
	}

	@Test
	public void testAnnotateRange()
	{
		Printer.AnnotationRecord r = new Printer.AnnotationRecord("path", List.of(33), Printer.Semantic.NONE);
		printer.pushVars(Map.of("foo", "3", "bar", "5", "annot", r));

		printer.emit("012$_start$annot$$foo$4$bar$$_end$annot$\n");

		assertEquals("012345\n", printer.toString());
		collector.assertContains(
				new FakeAnnotationCollector.Record(3, 6, "path", List.of(33), Printer.Semantic.NONE));
	}

	@Test
	public void testAnnotateIndent() throws Exception
	{
		printer.emit("0\n");

		try (AutoCloseable i = printer.withIndent())
		{
			Printer.AnnotationRecord r1 = new Printer.AnnotationRecord("path", List.of(44), Printer.Semantic.NONE);
			printer.pushVars(Map.of("foo", "4", "foo_a", r1));
			printer.emit("$_start$foo_a$$foo$$_end$foo_a$");

			printer.emit(",\n");

			Printer.AnnotationRecord r2 = new Printer.AnnotationRecord("path", List.of(99), Printer.Semantic.NONE);
			printer.pushVars(Map.of("bar", "9", "bar_a", r2));
			printer.emit("$_start$bar_a$$bar$$_end$bar_a$");

			Printer.AnnotationRecord r3 = new Printer.AnnotationRecord("path", List.of(1313), Printer.Semantic.NONE);
			printer.pushVars(Map.of("{", "", "}", "", "D", "d", "d_a", r3));

			printer.emit("\n$_start$d_a$${$$D$$}$$_end$d_a$\n");
		}
		printer.emit("\n");

		assertEquals("0\n  4,\n  9\n  d\n\n", printer.toString());

		collector.assertContains(
				new FakeAnnotationCollector.Record(4, 5, "path", List.of(44), Printer.Semantic.NONE),
				new FakeAnnotationCollector.Record(9, 10, "path", List.of(99), Printer.Semantic.NONE),
				new FakeAnnotationCollector.Record(13, 14, "path", List.of(1313), Printer.Semantic.NONE));
	}

	@Test
	public void testAnnotateRangeAnnotation()
	{
		Printer.AnnotationRecord r1 = new Printer.AnnotationRecord("file1.proto", List.of(33), Printer.Semantic.NONE);
		Printer.AnnotationRecord r2 = new Printer.AnnotationRecord("file2.proto", List.of(11, 22), Printer.Semantic.NONE);

		printer.pushVars(Map.of(
				"class", "Foo",
				"f1", "x", "f2", "y", "f3", "z",
				"message", r1,
				"field", r2));

		printer.emit("\n" + """
				  $_start$message$class $class$ {
				    $_start$field$int $f1$, $f2$, $f3$;
				    $_end$field$
				  };
				  $_end$message$
				""");

		assertEquals(
				"class Foo {\n" +
						"  int x, y, z;\n" +
						"};\n",
				printer.toString());

		collector.assertContains(
				new FakeAnnotationCollector.Record(14, 27, "file2.proto", List.of(11, 22), Printer.Semantic.NONE),
				new FakeAnnotationCollector.Record(0, 30, "file1.proto", List.of(33), Printer.Semantic.NONE));
	}

	@Test
	public void testRawStringIndentationStripping()
	{
		printer.emit("\n" + """
				  class Bar {
				    void run() {}
				  }
				""");

		assertEquals(
				"class Bar {\n" +
						"  void run() {}\n" +
						"}\n",
				printer.toString());
	}
}