package com.segmentengine.dsl.ast;

import java.util.Objects;

public record ComparisonExpression(FieldReference field, ComparisonOperator operator, NumericLiteral literal)
        implements Expression {
    public ComparisonExpression {
        Objects.requireNonNull(field, "field");
        Objects.requireNonNull(operator, "operator");
        Objects.requireNonNull(literal, "literal");
    }

    @Override
    public <R> R accept(ExpressionVisitor<R> visitor) {
        return visitor.visitComparison(this);
    }
}
