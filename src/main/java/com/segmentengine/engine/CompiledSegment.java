package com.segmentengine.engine;

import com.segmentengine.dsl.ast.Expression;

public record CompiledSegment(String name, Expression originalExpression, Expression executableExpression) {
}
