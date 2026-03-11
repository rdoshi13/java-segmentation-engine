package com.segmentengine.dsl.ast;

public record NumericLiteral(double value) implements Expression {
    @Override
    public <R> R accept(ExpressionVisitor<R> visitor) {
        return visitor.visitNumericLiteral(this);
    }
}
