package com.segmentengine.architecture;

import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

class ArchitectureBoundariesTest {
    @Test
    void corePackagesMustNotDependOnCliOrBenchmark() {
        ArchRule rule = noClasses()
                .that().resideInAnyPackage(
                        "com.segmentengine.dsl..",
                        "com.segmentengine.engine..",
                        "com.segmentengine.optimizer..",
                        "com.segmentengine.incremental..",
                        "com.segmentengine.model..",
                        "com.segmentengine.metrics.."
                )
                .should().dependOnClassesThat()
                .resideInAnyPackage("com.segmentengine.cli..", "com.segmentengine.benchmark..");

        rule.check(new ClassFileImporter().importPackages("com.segmentengine"));
    }
}
