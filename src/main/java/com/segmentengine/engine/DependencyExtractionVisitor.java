package com.segmentengine.engine;

import com.segmentengine.dsl.ast.AndExpression;
import com.segmentengine.dsl.ast.BooleanLiteralExpression;
import com.segmentengine.dsl.ast.ComparisonExpression;
import com.segmentengine.dsl.ast.ExpressionVisitor;
import com.segmentengine.dsl.ast.FieldReference;
import com.segmentengine.dsl.ast.NumericLiteral;
import com.segmentengine.dsl.ast.OrExpression;

import java.util.LinkedHashSet;
import java.util.Set;

public class DependencyExtractionVisitor implements ExpressionVisitor<Set<String>> {
    @Override
    public Set<String> visitAnd(AndExpression expression) {
        return merge(expression.left().accept(this), expression.right().accept(this));
    }

    @Override
    public Set<String> visitOr(OrExpression expression) {
        return merge(expression.left().accept(this), expression.right().accept(this));
    }

    @Override
    public Set<String> visitComparison(ComparisonExpression expression) {
        return expression.field().accept(this);
    }

    @Override
    public Set<String> visitBooleanLiteral(BooleanLiteralExpression expression) {
        return new LinkedHashSet<>();
    }

    @Override
    public Set<String> visitFieldReference(FieldReference reference) {
        Set<String> fields = new LinkedHashSet<>();
        fields.add(reference.fieldName());
        return fields;
    }

    @Override
    public Set<String> visitNumericLiteral(NumericLiteral literal) {
        return new LinkedHashSet<>();
    }

    private Set<String> merge(Set<String> first, Set<String> second) {
        Set<String> merged = new LinkedHashSet<>(first);
        merged.addAll(second);
        return merged;
    }
}
