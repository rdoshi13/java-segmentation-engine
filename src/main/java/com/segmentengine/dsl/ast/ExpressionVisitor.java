package com.segmentengine.dsl.ast;

public interface ExpressionVisitor<R> {
    R visitAnd(AndExpression expression);

    R visitOr(OrExpression expression);

    R visitComparison(ComparisonExpression expression);

    R visitBooleanLiteral(BooleanLiteralExpression expression);

    R visitFieldReference(FieldReference reference);

    R visitNumericLiteral(NumericLiteral literal);
}
