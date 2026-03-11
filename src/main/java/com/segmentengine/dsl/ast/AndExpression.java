package com.segmentengine.dsl.ast;

import java.util.Objects;

public record AndExpression(Expression left, Expression right) implements Expression {
    public AndExpression {
        Objects.requireNonNull(left, "left");
        Objects.requireNonNull(right, "right");
    }

    @Override
    public <R> R accept(ExpressionVisitor<R> visitor) {
        return visitor.visitAnd(this);
    }
}
