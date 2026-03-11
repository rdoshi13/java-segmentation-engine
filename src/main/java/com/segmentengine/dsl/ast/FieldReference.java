package com.segmentengine.dsl.ast;

import java.util.Objects;

public record FieldReference(String fieldName) implements Expression {
    public FieldReference {
        Objects.requireNonNull(fieldName, "fieldName");
    }

    @Override
    public <R> R accept(ExpressionVisitor<R> visitor) {
        return visitor.visitFieldReference(this);
    }
}
