package edu.lu.uni.serval.jdt.visitor;


import org.eclipse.jdt.core.dom.ASTNode;

/**
 * Create AbstractRowTokenJdtVisitor by extending AbstractJdtVisitor and overriding pushNode method.
 * 
 * Remove the ASTNode type in trees.
 * 
 * @author kui.liu
 *
 */
public abstract class AbstractRawTokenJdtVisitor extends AbstractJdtVisitor {

    public AbstractRawTokenJdtVisitor() {
        super();
    }

    @Override
    protected void pushNode(ASTNode n, String label) {
        push(0, "", label, n.getStartPosition(), n.getLength());
    }

}
