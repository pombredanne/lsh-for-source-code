package org.weborganic.furi;

import java.util.ArrayList;
import java.util.List;
import org.weborganic.furi.Parameters;
import org.weborganic.furi.TestUtils;
import org.weborganic.furi.Token;
import org.weborganic.furi.TokenFactory;
import org.weborganic.furi.TokenLiteral;
import org.weborganic.furi.TokenVariable;
import org.weborganic.furi.URIParameters;
import org.weborganic.furi.URITemplate;
import junit.framework.TestCase;

/**
 * Test class for the <code>URITemplate</code> class.
 * <p>
 * Some test cases are built directly from examples in the specifications
 * 
 * @see <a
 *      href="http://bitworking.org/projects/URI-Templates/spec/draft-gregorio-uritemplate-03.html#examples">URI
 *      Template (Draft 3) - examples</a>
 * 
 * @author Christophe Lauret
 * @version 21 October 2009
 */
public class URITemplateTest extends TestCase {

    /**
   * Parameters examples from the specifications.
   */
    private final Parameters vars = new URIParameters();

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        vars.set("foo", new String[] { "ϓ" });
        vars.set("bar", new String[] { "fred" });
        vars.set("baz", new String[] { "10,20,30" });
        vars.set("qux", new String[] { "10", "20", "30" });
        vars.set("corge", new String[] {});
        vars.set("grault", new String[] { "" });
        vars.set("garply", new String[] { "a/b/c" });
        vars.set("waldo", new String[] { "ben & jerrys" });
        vars.set("fred", new String[] { "fred", "", "wilma" });
        vars.set("plugh", new String[] { "ẛ", "ṡ" });
        vars.set("1-a_b.c", new String[] { "200" });
    }

    /**
   * Test that a null pointer exception is thrown by the constructor.
   */
    public void testNew_Null() {
        boolean nullThrown = false;
        try {
            new URITemplate(null);
        } catch (NullPointerException ex) {
            nullThrown = true;
        } finally {
            assertTrue(nullThrown);
        }
    }

    /**
   * Test that it can construct a template from an empty string.
   */
    public void testNew_EmptyString() {
        new URITemplate("");
    }

    /**
   * Test that the <code>digest</code> method returns an empty token list for an empty string.
   */
    public void testDigest_EmptyString() {
        List<Token> tokens = new ArrayList<Token>();
        assertEquals(tokens, URITemplate.digest(""));
    }

    /**
   * Test that the <code>digest</code> method returns one literal token list for simple text.
   */
    public void testDigest_OneTokenLiteral() {
        List<Token> tokens = new ArrayList<Token>();
        tokens.add(new TokenLiteral("http://acme.com/"));
        assertEquals(tokens, URITemplate.digest("http://acme.com/"));
    }

    /**
   * Test that the <code>digest</code> method returns a variable token for a variable expression.
   */
    public void testDigest_OneTokenVariable() {
        List<Token> tokens = new ArrayList<Token>();
        tokens.add(new TokenVariable("x"));
        assertEquals(tokens, URITemplate.digest("{x}"));
    }

    /**
   * Test that the <code>digest</code> method returns an operator token for an operator expression.
   */
    public void testDigest_OneTokenOperator() {
        List<Token> tokens = new ArrayList<Token>();
        tokens.add(TokenFactory.getInstance().newToken("{-opt|x|y}"));
        assertEquals(tokens, URITemplate.digest("{-opt|x|y}"));
    }

    /**
   * Test that the <code>digest</code> method returns the appropriate tokens for text followed by
   * one variable.
   */
    public void testDigest_TwoToken() {
        List<Token> tokens = new ArrayList<Token>();
        tokens.add(new TokenLiteral("http://acme.com/"));
        tokens.add(new TokenVariable("x"));
        assertEquals(tokens, URITemplate.digest("http://acme.com/{x}"));
    }

    /**
   * Test that the <code>digest</code> method returns the appropriate tokens for text with one
   * variable in the middle.
   */
    public void testDigest_OneTokenInTheMiddle() {
        List<Token> tokens = new ArrayList<Token>();
        tokens.add(new TokenLiteral("http://acme.com/"));
        tokens.add(new TokenVariable("x"));
        tokens.add(new TokenLiteral("/text"));
        assertEquals(tokens, URITemplate.digest("http://acme.com/{x}/text"));
    }

    /**
   * Test that the <code>digest</code> method returns the appropriate tokens for text including two
   * variables.
   */
    public void testDigest_TwoTokens() {
        List<Token> tokens = new ArrayList<Token>();
        tokens.add(new TokenLiteral("http://acme.com/"));
        tokens.add(new TokenVariable("x"));
        tokens.add(new TokenLiteral("/"));
        tokens.add(new TokenVariable("y"));
        assertEquals(tokens, URITemplate.digest("http://acme.com/{x}/{y}"));
    }

    /**
   * Test that the <code>digest</code> method returns the appropriate tokens for two consecutive
   * variables.
   */
    public void testDigest_TwoConsecutiveTokens() {
        List<Token> tokens = new ArrayList<Token>();
        tokens.add(new TokenLiteral("http://acme.com/"));
        tokens.add(new TokenVariable("x"));
        tokens.add(new TokenVariable("y"));
        assertEquals(tokens, URITemplate.digest("http://acme.com/{x}{y}"));
    }

    /**
   * Test the <code>equals</code> method.
   */
    public void testEquals_Contract() {
        URITemplate x = new URITemplate("http://ps.com/{X}");
        URITemplate y = new URITemplate("http://ps.com/{X}");
        URITemplate z = new URITemplate("http://ps.com/{Y}");
        TestUtils.satisfyEqualsContract(x, y, z);
    }

    /**
   * Test the <code>expand</code> method using test cases in the specifications.
   * <p>
   * This method tests:
   * 
   * <pre>
   *   http://example.org/?q={bar}
   *   http://example.org/?q=fred
   * </pre>
   */
    public void testExpand_Spec1() {
        assertExpand("http://example.org/?q={bar}", vars, "http://example.org/?q=fred");
    }

    /**
   * Test the <code>expand</code> method using test cases in the specifications.
   * <p>
   * This method tests:
   * 
   * <pre>
   * /{xyzzy}
   * /
   * </pre>
   */
    public void testExpand_Spec2() {
        assertExpand("/{xyzzy}", vars, "/");
    }

    /**
   * Test the <code>expand</code> method using test cases in the specifications.
   * <p>
   * This method tests:
   * 
   * <pre>
   * http://example.org/?{-join|&amp;|foo,bar,xyzzy,baz}
   * http://example.org/?foo=%CE%8E&amp;bar=fred&amp;baz=10%2C20%2C30
   * </pre>
   */
    public void testExpand_Spec3() {
        assertExpand("http://example.org/?{-join|&|foo,bar,xyzzy,baz}", vars, "http://example.org/?foo=%CE%8E&bar=fred&baz=10%2C20%2C30");
    }

    /**
   * Test the <code>expand</code> method using test cases in the specifications.
   * <p>
   * This method tests:
   * 
   * <pre>
   * http://example.org/?d={-list|,|qux}
   * http://example.org/?d=10,20,30
   * </pre>
   */
    public void testExpand_Spec4() {
        assertExpand("http://example.org/?d={-list|,|qux}", vars, "http://example.org/?d=10,20,30");
    }

    /**
   * Test the <code>expand</code> method using test cases in the specifications.
   * <p>
   * This method tests:
   * 
   * <pre>
   * http://example.org/?d={-list|&amp;d=|qux}
   * http://example.org/?d=10&amp;d=20&amp;d=30
   * </pre>
   */
    public void testExpand_Spec5() {
        assertExpand("http://example.org/?d={-list|&d=|qux}", vars, "http://example.org/?d=10&d=20&d=30");
    }

    /**
   * Test the <code>expand</code> method using test cases in the specifications.
   * <p>
   * This method tests:
   * 
   * <pre>
   * http://example.org/{bar}{bar}/{garply}
   * http://example.org/fredfred/a%2Fb%2Fc
   * </pre>
   */
    public void testExpand_Spec6() {
        assertExpand("http://example.org/{bar}{bar}/{garply}", vars, "http://example.org/fredfred/a%2Fb%2Fc");
    }

    /**
   * Test the <code>expand</code> method using test cases in the specifications.
   * <p>
   * This method tests:
   * 
   * <pre>
   * http://example.org/{bar}{-prefix|/|fred}
   * http://example.org/fred/fred//wilma
   * </pre>
   */
    public void testExpand_Spec7() {
        assertExpand("http://example.org/{bar}{-prefix|/|fred}", vars, "http://example.org/fred/fred//wilma");
    }

    /**
   * Test the <code>expand</code> method using test cases in the specifications.
   * <p>
   * This method tests:
   * 
   * <pre>
   * {-neg|:|corge}{-suffix|:|plugh}
   * :%E1%B9%A1:%E1%B9%A1:
   * </pre>
   */
    public void testExpand_Spec8() {
        assertExpand("{-neg|:|corge}{-suffix|:|plugh}", vars, ":%E1%B9%A1:%E1%B9%A1:");
    }

    /**
   * Test the <code>expand</code> method using test cases in the specifications.
   * <p>
   * This method tests:
   * 
   * <pre>
   * ../{waldo}/
   * ../ben%20%26%20jerrys/
   * </pre>
   */
    public void testExpand_Spec9() {
        assertExpand("../{waldo}/", vars, "../ben%20%26%20jerrys/");
    }

    /**
   * Test the <code>expand</code> method using test cases in the specifications.
   * <p>
   * This method tests:
   * 
   * <pre>
   * telnet:192.0.2.16{-opt|:80|grault}
   * telnet:192.0.2.16:80
   * </pre>
   */
    public void testExpand_Spec10() {
        assertExpand("telnet:192.0.2.16{-opt|:80|grault}", vars, "telnet:192.0.2.16:80");
    }

    /**
   * Test the <code>expand</code> method using test cases in the specifications.
   * <p>
   * This method tests:
   * 
   * <pre>
   * :{1-a_b.c}:
   * :200:
   * </pre>
   */
    public void testExpand_Spec11() {
        assertExpand(":{1-a_b.c}:", vars, ":200:");
    }

    /**
   * Test the <code>expand</code> method when a type is in use.
   * <p>
   * This method tests:
   * 
   * <pre>
   * /type/{x:bar}
   * /type/fred
   * </pre>
   */
    public void testExpand_Type() {
        assertExpand("/type/{x:bar}", vars, "/type/fred");
    }

    /**
   * Expand the specified template with the given parameters and checks that it matches the
   * specified URL.
   * 
   * @param template The template to expand.
   * @param parameters The parameters to use.
   * @param url The expected URL.
   */
    private void assertExpand(String template, Parameters parameters, String url) {
        assertEquals(url, URITemplate.expand(template, parameters));
    }
}
