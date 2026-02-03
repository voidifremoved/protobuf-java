package com.rubberjam.protobuf.io;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import com.rubberjam.protobuf.compiler.ErrorCollector;

@RunWith(JUnit4.class)
public class TokenizerTest {

    private static class TestErrorCollector implements ErrorCollector {
        private final List<String> errors = new ArrayList<>();

        @Override
        public void recordError(int line, int column, String message) {
            errors.add(line + ":" + column + ": " + message);
        }

        public boolean hasErrors() {
            return !errors.isEmpty();
        }

        public List<String> getErrors() {
            return errors;
        }
    }

    private Tokenizer createTokenizer(String input) {
        return new Tokenizer(new StringReader(input), new TestErrorCollector());
    }

    private void assertToken(Tokenizer tokenizer, Tokenizer.TokenType type, String text) {
        assertTrue(tokenizer.next());
        assertEquals(type, tokenizer.current().type);
        assertEquals(text, tokenizer.current().text);
    }

    @Test
    public void testSimpleTokens() {
        Tokenizer t = createTokenizer("ident 123 1.23 \"string\" ;");
        assertToken(t, Tokenizer.TokenType.IDENTIFIER, "ident");
        assertToken(t, Tokenizer.TokenType.INTEGER, "123");
        assertToken(t, Tokenizer.TokenType.FLOAT, "1.23");
        assertToken(t, Tokenizer.TokenType.STRING, "string");
        assertToken(t, Tokenizer.TokenType.SYMBOL, ";");
    }

    @Test
    public void testStringEscapes() {
        // \n, \t, \r, \\, \', \" are currently supported
        // Missing: \a, \b, \f, \v, \?
        // Missing: Octal \123
        // Missing: Hex \x12
        // Missing: Unicode \u1234

        // Let's test what IS supported first to be sure
        Tokenizer t = createTokenizer("\"\\n\\t\\r\\\\\\\"\\'\"");
        assertToken(t, Tokenizer.TokenType.STRING, "\n\t\r\\\"'");

        // Now test missing ones (these should fail if not implemented)
        Tokenizer t2 = createTokenizer("\"\\a\\b\\f\\v\\?\"");
        // If not implemented, it might output 'a', 'b'... or error?
        // Tokenizer.java default case returns the char itself. So \a -> a.
        // But C++ expects \a -> bell (0x07).
        assertToken(t2, Tokenizer.TokenType.STRING, "\u0007\b\f\u000b?");

        Tokenizer t3 = createTokenizer("\"\\123\""); // Octal for 'S' (83)
        assertToken(t3, Tokenizer.TokenType.STRING, "S");

        Tokenizer t4 = createTokenizer("\"\\x41\""); // Hex for 'A'
        assertToken(t4, Tokenizer.TokenType.STRING, "A");

        Tokenizer t5 = createTokenizer("\"\\u0041\""); // Unicode for 'A'
        assertToken(t5, Tokenizer.TokenType.STRING, "A");
    }

    @Test
    public void testNumericLiterals() {
        Tokenizer t = createTokenizer("123 0x1A 012 1.23 .45 1.2e3 -1.2e-3");
        assertToken(t, Tokenizer.TokenType.INTEGER, "123");
        assertToken(t, Tokenizer.TokenType.INTEGER, "0x1A");
        assertToken(t, Tokenizer.TokenType.INTEGER, "012");
        assertToken(t, Tokenizer.TokenType.FLOAT, "1.23");
        assertToken(t, Tokenizer.TokenType.FLOAT, ".45");
        assertToken(t, Tokenizer.TokenType.FLOAT, "1.2e3");

        // Negative number is SYMBOL - then FLOAT/INT
        assertToken(t, Tokenizer.TokenType.SYMBOL, "-");
        assertToken(t, Tokenizer.TokenType.FLOAT, "1.2e-3");
    }
}
