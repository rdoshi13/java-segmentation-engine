package com.segmentengine.dsl;

import com.segmentengine.dsl.ast.AndExpression;
import com.segmentengine.dsl.ast.BooleanLiteralExpression;
import com.segmentengine.dsl.ast.ComparisonExpression;
import com.segmentengine.dsl.ast.ExpressionVisitor;
import com.segmentengine.dsl.ast.FieldReference;
import com.segmentengine.dsl.ast.NumericLiteral;
import com.segmentengine.dsl.ast.OrExpression;

public class AstPrettyPrinter implements ExpressionVisitor<String> {
    @Override
    public String visitAnd(AndExpression expression) {
        return "(" + expression.left().accept(this) + " AND " + expression.right().accept(this) + ")";
    }

    @Override
    public String visitOr(OrExpression expression) {
        return "(" + expression.left().accept(this) + " OR " + expression.right().accept(this) + ")";
    }

    @Override
    public String visitComparison(ComparisonExpression expression) {
        return expression.field().accept(this)
                + " "
                + expression.operator().symbol()
                + " "
                + expression.literal().accept(this);
    }

    @Override
    public String visitBooleanLiteral(BooleanLiteralExpression expression) {
        return Boolean.toString(expression.value());
    }

    @Override
    public String visitFieldReference(FieldReference reference) {
        return reference.fieldName();
    }

    @Override
    public String visitNumericLiteral(NumericLiteral literal) {
        if (literal.value() % 1 == 0) {
            return Long.toString((long) literal.value());
        }
        return Double.toString(literal.value());
    }
}
