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

import java.util.Optional;

public class ComparisonSimplificationRule implements RewriteRule, ExpressionVisitor<Expression> {
    @Override
    public Expression rewrite(Expression expression) {
        return expression.accept(this);
    }

    @Override
    public Expression visitAnd(AndExpression expression) {
        Expression left = expression.left().accept(this);
        Expression right = expression.right().accept(this);

        Optional<ComparisonExpression> lc = ExpressionInspector.comparison(left);
        Optional<ComparisonExpression> rc = ExpressionInspector.comparison(right);
        if (lc.isPresent() && rc.isPresent()) {
            Optional<Expression> simplified = simplifyPair(lc.get(), rc.get());
            if (simplified.isPresent()) {
                return simplified.get();
            }
        }

        return new AndExpression(left, right);
    }

    @Override
    public Expression visitOr(OrExpression expression) {
        Expression left = expression.left().accept(this);
        Expression right = expression.right().accept(this);
        return new OrExpression(left, right);
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

    private Optional<Expression> simplifyPair(ComparisonExpression first, ComparisonExpression second) {
        if (!first.field().fieldName().equals(second.field().fieldName())) {
            return Optional.empty();
        }

        Optional<ComparisonExpression> lowerBound = stricterLowerBound(first, second);
        if (lowerBound.isPresent()) {
            return Optional.of(lowerBound.get());
        }

        Optional<ComparisonExpression> upperBound = stricterUpperBound(first, second);
        if (upperBound.isPresent()) {
            return Optional.of(upperBound.get());
        }

        return Optional.empty();
    }

    private Optional<ComparisonExpression> stricterLowerBound(ComparisonExpression first, ComparisonExpression second) {
        if (!isLowerBound(first.operator()) || !isLowerBound(second.operator())) {
            return Optional.empty();
        }

        double fv = first.literal().value();
        double sv = second.literal().value();
        if (fv > sv) {
            return Optional.of(first);
        }
        if (sv > fv) {
            return Optional.of(second);
        }

        if (first.operator() == ComparisonOperator.GREATER_THAN || second.operator() == ComparisonOperator.GREATER_THAN) {
            return Optional.of(new ComparisonExpression(first.field(), ComparisonOperator.GREATER_THAN, first.literal()));
        }
        return Optional.of(first);
    }

    private Optional<ComparisonExpression> stricterUpperBound(ComparisonExpression first, ComparisonExpression second) {
        if (!isUpperBound(first.operator()) || !isUpperBound(second.operator())) {
            return Optional.empty();
        }

        double fv = first.literal().value();
        double sv = second.literal().value();
        if (fv < sv) {
            return Optional.of(first);
        }
        if (sv < fv) {
            return Optional.of(second);
        }

        if (first.operator() == ComparisonOperator.LESS_THAN || second.operator() == ComparisonOperator.LESS_THAN) {
            return Optional.of(new ComparisonExpression(first.field(), ComparisonOperator.LESS_THAN, first.literal()));
        }
        return Optional.of(first);
    }

    private boolean isLowerBound(ComparisonOperator operator) {
        return operator == ComparisonOperator.GREATER_THAN || operator == ComparisonOperator.GREATER_OR_EQUAL;
    }

    private boolean isUpperBound(ComparisonOperator operator) {
        return operator == ComparisonOperator.LESS_THAN || operator == ComparisonOperator.LESS_OR_EQUAL;
    }
}
