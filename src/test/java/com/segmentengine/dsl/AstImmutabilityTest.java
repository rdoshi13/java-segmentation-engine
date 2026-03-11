package com.segmentengine.dsl;

import com.segmentengine.dsl.ast.AndExpression;
import com.segmentengine.dsl.ast.BooleanLiteralExpression;
import com.segmentengine.dsl.ast.ComparisonExpression;
import com.segmentengine.dsl.ast.FieldReference;
import com.segmentengine.dsl.ast.NumericLiteral;
import com.segmentengine.dsl.ast.OrExpression;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class AstImmutabilityTest {
    @Test
    void astNodesUseFinalInstanceFields() {
        List<Class<?>> astClasses = List.of(
                AndExpression.class,
                OrExpression.class,
                ComparisonExpression.class,
                BooleanLiteralExpression.class,
                FieldReference.class,
                NumericLiteral.class
        );

        for (Class<?> clazz : astClasses) {
            for (Field field : clazz.getDeclaredFields()) {
                if (Modifier.isStatic(field.getModifiers())) {
                    continue;
                }
                assertTrue(
                        Modifier.isFinal(field.getModifiers()),
                        "Expected final field in " + clazz.getSimpleName() + ": " + field.getName()
                );
            }
        }
    }
}
