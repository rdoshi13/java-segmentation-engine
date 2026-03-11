package com.segmentengine.engine;

import com.segmentengine.dsl.Parser;
import com.segmentengine.dsl.ast.Expression;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DependencyExtractionVisitorTest {
    @Test
    void extractsAllReferencedFields() {
        Parser parser = new Parser();
        Expression expression = parser.parseExpression("age > 25 AND (total_spent > 100 OR last_login_days < 7)");

        DependencyExtractionVisitor visitor = new DependencyExtractionVisitor();
        Set<String> fields = expression.accept(visitor);

        assertEquals(Set.of("age", "total_spent", "last_login_days"), fields);
    }
}
