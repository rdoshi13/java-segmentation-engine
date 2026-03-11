package com.segmentengine.dsl;

import com.segmentengine.dsl.ast.AndExpression;
import com.segmentengine.dsl.ast.BooleanLiteralExpression;
import com.segmentengine.dsl.ast.ComparisonExpression;
import com.segmentengine.dsl.ast.ComparisonOperator;
import com.segmentengine.dsl.ast.Expression;
import com.segmentengine.dsl.ast.FieldReference;
import com.segmentengine.dsl.ast.NumericLiteral;
import com.segmentengine.dsl.ast.OrExpression;

import java.util.List;

public class Parser {
    private final Tokenizer tokenizer = new Tokenizer();
    private String source;
    private List<Token> tokens;
    private int index;

    public Expression parseExpression(String input) {
        String normalized = normalizeSegmentSyntax(input);
        this.source = normalized;
        this.tokens = tokenizer.tokenize(normalized);
        this.index = 0;
        Expression expression = parseOrExpression();
        expect(TokenType.EOF, "Expected end of expression");
        return expression;
    }

    private String normalizeSegmentSyntax(String input) {
        String trimmed = input.trim();
        String lower = trimmed.toLowerCase();
        if (lower.startsWith("segment ")) {
            int colon = trimmed.indexOf(':');
            if (colon < 0 || colon == trimmed.length() - 1) {
                throw new ParseException("Invalid segment declaration. Expected 'segment <name>: <expr>'");
            }
            return trimmed.substring(colon + 1).trim();
        }
        return trimmed;
    }

    private Expression parseOrExpression() {
        Expression left = parseAndExpression();
        while (match(TokenType.OR)) {
            Expression right = parseAndExpression();
            left = new OrExpression(left, right);
        }
        return left;
    }

    private Expression parseAndExpression() {
        Expression left = parsePrimary();
        while (match(TokenType.AND)) {
            Expression right = parsePrimary();
            left = new AndExpression(left, right);
        }
        return left;
    }

    private Expression parsePrimary() {
        if (match(TokenType.LPAREN)) {
            Expression inner = parseOrExpression();
            expect(TokenType.RPAREN, "Expected ')' after group");
            return inner;
        }
        if (match(TokenType.TRUE)) {
            return new BooleanLiteralExpression(true);
        }
        if (match(TokenType.FALSE)) {
            return new BooleanLiteralExpression(false);
        }
        return parseComparison();
    }

    private Expression parseComparison() {
        Token identifier = expect(TokenType.IDENTIFIER, "Expected field identifier");
        Token operator = expect(TokenType.OPERATOR, "Expected comparison operator");
        Token value = expect(TokenType.NUMBER, "Expected numeric literal");
        return new ComparisonExpression(
                new FieldReference(identifier.text()),
                ComparisonOperator.fromSymbol(operator.text()),
                new NumericLiteral(Double.parseDouble(value.text()))
        );
    }

    private boolean match(TokenType type) {
        if (peek().type() == type) {
            index++;
            return true;
        }
        return false;
    }

    private Token expect(TokenType type, String message) {
        Token token = peek();
        if (token.type() != type) {
            String found = token.type() == TokenType.EOF ? "<EOF>" : token.text();
            throw new ParseException(
                    message + " at position " + token.position()
                            + ", found '" + found + "' (" + token.type() + ")"
                            + " in expression: " + source
            );
        }
        index++;
        return token;
    }

    private Token peek() {
        return tokens.get(index);
    }
}
