package com.segmentengine.optimizer;

import com.segmentengine.dsl.ast.AndExpression;
import com.segmentengine.dsl.ast.BooleanLiteralExpression;
import com.segmentengine.dsl.ast.ComparisonExpression;
import com.segmentengine.dsl.ast.ComparisonOperator;
import com.segmentengine.dsl.ast.Expression;
import com.segmentengine.dsl.ast.ExpressionVisitor;
import com.segmentengine.dsl.ast.FieldReference;
import com.segmentengine.dsl.ast.NumericLiteral;
import com.segmentengine.dsl.ast.OrExpression;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class PredicateOrderingRule implements RewriteRule, ExpressionVisitor<Expression> {
    @Override
    public Expression rewrite(Expression expression) {
        return expression.accept(this);
    }

    @Override
    public Expression visitAnd(AndExpression expression) {
        List<Expression> terms = new ArrayList<>(ExpressionInspector.flattenAnd(expression));
        List<Expression> rewritten = terms.stream().map(term -> term.accept(this)).toList();
        List<Expression> sorted = rewritten.stream()
                .sorted(Comparator.<Expression>comparingInt(this::score).reversed())
                .toList();
        return buildAnd(sorted);
    }

    @Override
    public Expression visitOr(OrExpression expression) {
        List<Expression> terms = new ArrayList<>(ExpressionInspector.flattenOr(expression));
        List<Expression> rewritten = terms.stream().map(term -> term.accept(this)).toList();
        List<Expression> sorted = rewritten.stream()
                .sorted(Comparator.<Expression>comparingInt(this::score).reversed())
                .toList();
        return buildOr(sorted);
    }

    @Override
    public Expression visitComparison(ComparisonExpression expression) {
        return expression;
    }

    @Override
    public Expression visitBooleanLiteral(BooleanLiteralExpression expression) {
        return expression;
    }

    @Override
    public Expression visitFieldReference(FieldReference reference) {
        return reference;
    }

    @Override
    public Expression visitNumericLiteral(NumericLiteral literal) {
        return literal;
    }

    private int score(Expression expression) {
        return expression.accept(new SelectivityScoreVisitor());
    }

    private Expression buildAnd(List<Expression> terms) {
        if (terms.isEmpty()) {
            return new BooleanLiteralExpression(true);
        }
        Expression current = terms.get(0);
        for (int i = 1; i < terms.size(); i++) {
            current = new AndExpression(current, terms.get(i));
        }
        return current;
    }

    private Expression buildOr(List<Expression> terms) {
        if (terms.isEmpty()) {
            return new BooleanLiteralExpression(false);
        }
        Expression current = terms.get(0);
        for (int i = 1; i < terms.size(); i++) {
            current = new OrExpression(current, terms.get(i));
        }
        return current;
    }

    private static final class SelectivityScoreVisitor implements ExpressionVisitor<Integer> {
        @Override
        public Integer visitAnd(AndExpression expression) {
            return 1;
        }

        @Override
        public Integer visitOr(OrExpression expression) {
            return 1;
        }

        @Override
        public Integer visitComparison(ComparisonExpression expression) {
            return switch (expression.operator()) {
                case EQUAL, NOT_EQUAL -> 100;
                case GREATER_THAN, LESS_THAN -> 80;
                case GREATER_OR_EQUAL, LESS_OR_EQUAL -> 70;
            };
        }

        @Override
        public Integer visitBooleanLiteral(BooleanLiteralExpression expression) {
            return expression.value() ? 0 : 200;
        }

        @Override
        public Integer visitFieldReference(FieldReference reference) {
            return 0;
        }

        @Override
        public Integer visitNumericLiteral(NumericLiteral literal) {
            return 0;
        }
    }
}
