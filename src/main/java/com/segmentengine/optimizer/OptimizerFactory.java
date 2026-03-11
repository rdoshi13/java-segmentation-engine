package com.segmentengine.optimizer;

import java.util.List;

public final class OptimizerFactory {
    private OptimizerFactory() {
    }

    public static AstOptimizer defaultOptimizer() {
        return new AstOptimizer(List.of(
                new ConstantFoldingRule(),
                new DuplicatePredicateEliminationRule(),
                new ComparisonSimplificationRule(),
                new PredicateOrderingRule()
        ));
    }
}
