package com.segmentengine.engine;

import com.segmentengine.dsl.Parser;
import com.segmentengine.dsl.ast.Expression;
import com.segmentengine.model.SegmentDefinition;
import com.segmentengine.optimizer.AstOptimizer;

import java.util.ArrayList;
import java.util.List;

public class SegmentCompiler {
    private final Parser parser;
    private final AstOptimizer optimizer;

    public SegmentCompiler(Parser parser, AstOptimizer optimizer) {
        this.parser = parser;
        this.optimizer = optimizer;
    }

    public List<CompiledSegment> compile(List<SegmentDefinition> segments, boolean optimize) {
        List<CompiledSegment> compiled = new ArrayList<>();
        for (SegmentDefinition segment : segments) {
            Expression original = parser.parseExpression(segment.getRule());
            Expression executable = optimize ? optimizer.optimize(original) : original;
            compiled.add(new CompiledSegment(segment.getName(), original, executable));
        }
        return compiled;
    }
}
