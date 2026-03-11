package com.segmentengine.optimizer;

import com.segmentengine.dsl.AstPrettyPrinter;
import com.segmentengine.dsl.Parser;
import com.segmentengine.dsl.ast.Expression;
import com.segmentengine.engine.FieldAccessorRegistry;
import com.segmentengine.engine.SegmentEngine;
import com.segmentengine.model.Profile;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OptimizerTest {
    private final Parser parser = new Parser();
    private final AstPrettyPrinter printer = new AstPrettyPrinter();
    private final AstOptimizer optimizer = OptimizerFactory.defaultOptimizer();

    @Test
    void foldsConstants() {
        Expression optimized = optimizer.optimize(parser.parseExpression("true AND age > 25"));
        assertEquals("age > 25", optimized.accept(printer));
    }

    @Test
    void removesDuplicatePredicates() {
        Expression optimized = optimizer.optimize(parser.parseExpression("age > 25 AND age > 25"));
        assertEquals("age > 25", optimized.accept(printer));
    }

    @Test
    void simplifiesComparableBounds() {
        Expression optimized = optimizer.optimize(parser.parseExpression("age > 18 AND age > 25"));
        assertEquals("age > 25", optimized.accept(printer));
    }

    @Test
    void optimizedExpressionKeepsSemantics() {
        Expression original = parser.parseExpression("age > 18 AND age > 25 AND total_spent > 1000");
        Expression optimized = optimizer.optimize(original);

        SegmentEngine engine = new SegmentEngine(new FieldAccessorRegistry());
        List<Profile> profiles = List.of(
                new Profile(1, 20, 1200, 2),
                new Profile(2, 30, 900, 4),
                new Profile(3, 32, 5000, 1)
        );

        for (Profile profile : profiles) {
            assertEquals(engine.evaluate(original, profile), engine.evaluate(optimized, profile));
        }
    }
}
