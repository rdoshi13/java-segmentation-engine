package com.segmentengine.optimizer;

import com.segmentengine.dsl.ast.Expression;

public interface RewriteRule {
    Expression rewrite(Expression expression);
}
