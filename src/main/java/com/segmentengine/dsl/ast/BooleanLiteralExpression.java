package com.segmentengine.dsl.ast;

public record BooleanLiteralExpression(boolean value) implements Expression {
    @Override
    public <R> R accept(ExpressionVisitor<R> visitor) {
        return visitor.visitBooleanLiteral(this);
    }
}
