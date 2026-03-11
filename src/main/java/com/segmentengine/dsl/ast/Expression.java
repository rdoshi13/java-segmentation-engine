package com.segmentengine.dsl.ast;

public interface Expression {
    <R> R accept(ExpressionVisitor<R> visitor);
}
