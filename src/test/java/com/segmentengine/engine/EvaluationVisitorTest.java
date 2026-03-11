package com.segmentengine.engine;

import com.segmentengine.dsl.Parser;
import com.segmentengine.dsl.ast.Expression;
import com.segmentengine.model.Profile;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EvaluationVisitorTest {
    private final Parser parser = new Parser();
    private final SegmentEngine engine = new SegmentEngine(new FieldAccessorRegistry());

    @Test
    void evaluatesAllComparisonOperators() {
        Profile profile = new Profile(1, 30, 1200.0, 5);

        assertTrue(engine.evaluate(parser.parseExpression("age > 25"), profile));
        assertTrue(engine.evaluate(parser.parseExpression("age >= 30"), profile));
        assertTrue(engine.evaluate(parser.parseExpression("last_login_days < 10"), profile));
        assertTrue(engine.evaluate(parser.parseExpression("last_login_days <= 5"), profile));
        assertTrue(engine.evaluate(parser.parseExpression("total_spent == 1200"), profile));
        assertTrue(engine.evaluate(parser.parseExpression("total_spent != 1000"), profile));
    }

    @Test
    void shortCircuitsAndOnFalse() {
        Profile profile = new Profile(1, 20, 0, 100);
        Expression expression = parser.parseExpression("age > 30 AND unknown_field > 1");
        assertFalse(engine.evaluate(expression, profile));
    }

    @Test
    void shortCircuitsOrOnTrue() {
        Profile profile = new Profile(1, 50, 0, 100);
        Expression expression = parser.parseExpression("age > 30 OR unknown_field > 1");
        assertTrue(engine.evaluate(expression, profile));
    }
}
