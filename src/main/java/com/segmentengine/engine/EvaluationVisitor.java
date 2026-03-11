package com.segmentengine.engine;

import com.segmentengine.dsl.ast.AndExpression;
import com.segmentengine.dsl.ast.BooleanLiteralExpression;
import com.segmentengine.dsl.ast.ComparisonExpression;
import com.segmentengine.dsl.ast.ExpressionVisitor;
import com.segmentengine.dsl.ast.FieldReference;
import com.segmentengine.dsl.ast.NumericLiteral;
import com.segmentengine.dsl.ast.OrExpression;
import com.segmentengine.model.Profile;

public class EvaluationVisitor implements ExpressionVisitor<Boolean> {
    private final Profile profile;
    private final FieldAccessorRegistry fieldAccessorRegistry;

    public EvaluationVisitor(Profile profile, FieldAccessorRegistry fieldAccessorRegistry) {
        this.profile = profile;
        this.fieldAccessorRegistry = fieldAccessorRegistry;
    }

    @Override
    public Boolean visitAnd(AndExpression expression) {
        boolean left = expression.left().accept(this);
        if (!left) {
            return false;
        }
        return expression.right().accept(this);
    }

    @Override
    public Boolean visitOr(OrExpression expression) {
        boolean left = expression.left().accept(this);
        if (left) {
            return true;
        }
        return expression.right().accept(this);
    }

    @Override
    public Boolean visitComparison(ComparisonExpression expression) {
        Object raw = fieldAccessorRegistry.read(expression.field().fieldName(), profile);
        double left = ((Number) raw).doubleValue();
        double right = expression.literal().value();

        return switch (expression.operator()) {
            case GREATER_THAN -> left > right;
            case GREATER_OR_EQUAL -> left >= right;
            case LESS_THAN -> left < right;
            case LESS_OR_EQUAL -> left <= right;
            case EQUAL -> left == right;
            case NOT_EQUAL -> left != right;
        };
    }

    @Override
    public Boolean visitBooleanLiteral(BooleanLiteralExpression expression) {
        return expression.value();
    }

    @Override
    public Boolean visitFieldReference(FieldReference reference) {
        throw new UnsupportedOperationException("Field references are not directly evaluable as booleans");
    }

    @Override
    public Boolean visitNumericLiteral(NumericLiteral literal) {
        throw new UnsupportedOperationException("Numeric literals are not directly evaluable as booleans");
    }
}
