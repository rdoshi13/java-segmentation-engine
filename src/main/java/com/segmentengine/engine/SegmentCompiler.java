package com.segmentengine.engine;

import com.segmentengine.dsl.Parser;
import com.segmentengine.dsl.ast.Expression;
import com.segmentengine.model.SegmentDefinition;
import com.segmentengine.optimizer.AstOptimizer;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

public class SegmentCompiler {
    private final Parser parser;
    private final AstOptimizer optimizer;
    private final Set<String> supportedFields;

    public SegmentCompiler(Parser parser, AstOptimizer optimizer, FieldAccessorRegistry fieldAccessorRegistry) {
        this.parser = parser;
        this.optimizer = optimizer;
        this.supportedFields = Set.copyOf(fieldAccessorRegistry.accessors().keySet());
    }

    public List<CompiledSegment> compile(List<SegmentDefinition> segments, boolean optimize) {
        List<CompiledSegment> compiled = new ArrayList<>();
        DependencyExtractionVisitor dependencyExtractionVisitor = new DependencyExtractionVisitor();
        for (SegmentDefinition segment : segments) {
            Expression original = parser.parseExpression(segment.getRule());
            validateSupportedFields(segment.getName(), original.accept(dependencyExtractionVisitor));
            Expression executable = optimize ? optimizer.optimize(original) : original;
            compiled.add(new CompiledSegment(segment.getName(), original, executable));
        }
        return compiled;
    }

    private void validateSupportedFields(String segmentName, Set<String> referencedFields) {
        Set<String> unsupported = new TreeSet<>(referencedFields);
        unsupported.removeAll(supportedFields);
        if (unsupported.isEmpty()) {
            return;
        }
        throw new IllegalArgumentException(
                "Segment '" + segmentName + "' references unsupported fields: " + unsupported
                        + ". Supported fields: " + new TreeSet<>(supportedFields)
        );
    }
}
