package com.segmentengine.optimizer;

import com.segmentengine.dsl.ast.AndExpression;
import com.segmentengine.dsl.ast.BooleanLiteralExpression;
import com.segmentengine.dsl.ast.ComparisonExpression;
import com.segmentengine.dsl.ast.Expression;
import com.segmentengine.dsl.ast.ExpressionVisitor;
import com.segmentengine.dsl.ast.FieldReference;
import com.segmentengine.dsl.ast.NumericLiteral;
import com.segmentengine.dsl.ast.OrExpression;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

final class ExpressionInspector {
    private ExpressionInspector() {
    }

    static Optional<Boolean> booleanLiteral(Expression expression) {
        return expression.accept(new BooleanLiteralFinder());
    }

    static Optional<ComparisonExpression> comparison(Expression expression) {
        return expression.accept(new ComparisonFinder());
    }

    static List<Expression> flattenAnd(Expression expression) {
        return expression.accept(new AndFlattener());
    }

    static List<Expression> flattenOr(Expression expression) {
        return expression.accept(new OrFlattener());
    }

    private static final class BooleanLiteralFinder implements ExpressionVisitor<Optional<Boolean>> {
        @Override
        public Optional<Boolean> visitAnd(AndExpression expression) {
            return Optional.empty();
        }

        @Override
        public Optional<Boolean> visitOr(OrExpression expression) {
            return Optional.empty();
        }

        @Override
        public Optional<Boolean> visitComparison(ComparisonExpression expression) {
            return Optional.empty();
        }

        @Override
        public Optional<Boolean> visitBooleanLiteral(BooleanLiteralExpression expression) {
            return Optional.of(expression.value());
        }

        @Override
        public Optional<Boolean> visitFieldReference(FieldReference reference) {
            return Optional.empty();
        }

        @Override
        public Optional<Boolean> visitNumericLiteral(NumericLiteral literal) {
            return Optional.empty();
        }
    }

    private static final class ComparisonFinder implements ExpressionVisitor<Optional<ComparisonExpression>> {
        @Override
        public Optional<ComparisonExpression> visitAnd(AndExpression expression) {
            return Optional.empty();
        }

        @Override
        public Optional<ComparisonExpression> visitOr(OrExpression expression) {
            return Optional.empty();
        }

        @Override
        public Optional<ComparisonExpression> visitComparison(ComparisonExpression expression) {
            return Optional.of(expression);
        }

        @Override
        public Optional<ComparisonExpression> visitBooleanLiteral(BooleanLiteralExpression expression) {
            return Optional.empty();
        }

        @Override
        public Optional<ComparisonExpression> visitFieldReference(FieldReference reference) {
            return Optional.empty();
        }

        @Override
        public Optional<ComparisonExpression> visitNumericLiteral(NumericLiteral literal) {
            return Optional.empty();
        }
    }

    private static final class AndFlattener implements ExpressionVisitor<List<Expression>> {
        @Override
        public List<Expression> visitAnd(AndExpression expression) {
            List<Expression> nodes = new ArrayList<>(expression.left().accept(this));
            nodes.addAll(expression.right().accept(this));
            return nodes;
        }

        @Override
        public List<Expression> visitOr(OrExpression expression) {
            return List.of(expression);
        }

        @Override
        public List<Expression> visitComparison(ComparisonExpression expression) {
            return List.of(expression);
        }

        @Override
        public List<Expression> visitBooleanLiteral(BooleanLiteralExpression expression) {
            return List.of(expression);
        }

        @Override
        public List<Expression> visitFieldReference(FieldReference reference) {
            return List.of(reference);
        }

        @Override
        public List<Expression> visitNumericLiteral(NumericLiteral literal) {
            return List.of(literal);
        }
    }

    private static final class OrFlattener implements ExpressionVisitor<List<Expression>> {
        @Override
        public List<Expression> visitAnd(AndExpression expression) {
            return List.of(expression);
        }

        @Override
        public List<Expression> visitOr(OrExpression expression) {
            List<Expression> nodes = new ArrayList<>(expression.left().accept(this));
            nodes.addAll(expression.right().accept(this));
            return nodes;
        }

        @Override
        public List<Expression> visitComparison(ComparisonExpression expression) {
            return List.of(expression);
        }

        @Override
        public List<Expression> visitBooleanLiteral(BooleanLiteralExpression expression) {
            return List.of(expression);
        }

        @Override
        public List<Expression> visitFieldReference(FieldReference reference) {
            return List.of(reference);
        }

        @Override
        public List<Expression> visitNumericLiteral(NumericLiteral literal) {
            return List.of(literal);
        }
    }
}
