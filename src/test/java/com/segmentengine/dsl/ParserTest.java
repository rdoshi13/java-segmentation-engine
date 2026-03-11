package com.segmentengine.dsl;

import com.segmentengine.dsl.ast.Expression;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ParserTest {
    private final Parser parser = new Parser();
    private final AstPrettyPrinter printer = new AstPrettyPrinter();

    @Test
    void parsesOperatorPrecedence() {
        Expression expression = parser.parseExpression("age > 25 OR total_spent > 1000 AND last_login_days < 30");
        assertEquals("(age > 25 OR (total_spent > 1000 AND last_login_days < 30))", expression.accept(printer));
    }

    @Test
    void parsesParentheses() {
        Expression expression = parser.parseExpression("(age > 25 OR total_spent > 1000) AND last_login_days < 30");
        assertEquals("((age > 25 OR total_spent > 1000) AND last_login_days < 30)", expression.accept(printer));
    }

    @Test
    void failsOnInvalidSyntax() {
        assertThrows(ParseException.class, () -> parser.parseExpression("age >"));
    }
}
