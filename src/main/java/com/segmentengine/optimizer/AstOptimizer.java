package com.segmentengine.optimizer;

import com.segmentengine.dsl.ast.Expression;

import java.util.List;

public class AstOptimizer {
    private final List<RewriteRule> rules;

    public AstOptimizer(List<RewriteRule> rules) {
        this.rules = List.copyOf(rules);
    }

    public Expression optimize(Expression expression) {
        Expression current = expression;
        boolean changed;
        do {
            changed = false;
            for (RewriteRule rule : rules) {
                Expression next = rule.rewrite(current);
                if (!next.equals(current)) {
                    changed = true;
                    current = next;
                }
            }
        } while (changed);
        return current;
    }
}
