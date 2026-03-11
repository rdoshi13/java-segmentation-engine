package com.segmentengine.dsl.ast;

import java.util.Objects;

public record OrExpression(Expression left, Expression right) implements Expression {
    public OrExpression {
        Objects.requireNonNull(left, "left");
        Objects.requireNonNull(right, "right");
    }

    @Override
    public <R> R accept(ExpressionVisitor<R> visitor) {
        return visitor.visitOr(this);
    }
}
