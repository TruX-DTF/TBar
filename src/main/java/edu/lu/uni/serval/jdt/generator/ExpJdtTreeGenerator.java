package edu.lu.uni.serval.jdt.generator;

import edu.lu.uni.serval.jdt.visitor.AbstractJdtVisitor;
import edu.lu.uni.serval.jdt.visitor.ExpJdtVisitor;

@Register(id = "java-jdt-exp")
public class ExpJdtTreeGenerator extends AbstractJdtTreeGenerator {
    @Override
    protected AbstractJdtVisitor createVisitor() {
        return new ExpJdtVisitor();
    }
}
