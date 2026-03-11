package com.segmentengine.optimizer;

import com.segmentengine.dsl.ast.AndExpression;
import com.segmentengine.dsl.ast.BooleanLiteralExpression;
import com.segmentengine.dsl.ast.ComparisonExpression;
import com.segmentengine.dsl.ast.Expression;
import com.segmentengine.dsl.ast.ExpressionVisitor;
import com.segmentengine.dsl.ast.FieldReference;
import com.segmentengine.dsl.ast.NumericLiteral;
import com.segmentengine.dsl.ast.OrExpression;

public class DuplicatePredicateEliminationRule implements RewriteRule, ExpressionVisitor<Expression> {
    @Override
    public Expression rewrite(Expression expression) {
        return expression.accept(this);
    }

    @Override
    public Expression visitAnd(AndExpression expression) {
        Expression left = expression.left().accept(this);
        Expression right = expression.right().accept(this);
        if (left.equals(right)) {
            return left;
        }
        return new AndExpression(left, right);
    }

    @Override
    public Expression visitOr(OrExpression expression) {
        Expression left = expression.left().accept(this);
        Expression right = expression.right().accept(this);
        if (left.equals(right)) {
            return left;
        }
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
}
