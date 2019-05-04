package edu.lu.uni.serval.tbar.fixpatterns;

import java.util.List;

import edu.lu.uni.serval.jdt.tree.ITree;
import edu.lu.uni.serval.tbar.fixtemplate.FixTemplate;
import edu.lu.uni.serval.tbar.utils.Checker;

public class StatementRemover extends FixTemplate {

	/*
	 * SOFix	StateRemover
	 * Fix pattern for UC_USELESS_OBJECT violations. delete the variable declaration statement.
	 * Fix pattern for DL_SYNCHRONIZATION_ON_SHARED_CONSTANT violations.
	 * FB	UCFUselessControlFlow
	 * FB	UPMUncalledPrivateMethod
	 * FixMiner DeleteStatement
	 * 
	 * Delete a statement or a method.
	 */
	
	@Override
	public void generatePatches() {
		ITree suspCodeTree = this.getSuspiciousCodeTree();
		int stmtType = suspCodeTree.getType();
		
		if (Checker.isSynchronizedStatement(suspCodeTree.getType())) {
			List<ITree> children = suspCodeTree.getChildren();
			ITree firstStatement = null;
			ITree lastStatement = null;
			for (ITree child : children) {
				if (Checker.isStatement(child.getType())) {
					if (firstStatement == null) firstStatement = child;
					lastStatement = child;
				}
			}
			if (firstStatement == null) return;
			int startPos = firstStatement.getPos();
			int endPos = lastStatement.getPos() + lastStatement.getLength();
			String code = this.getSubSuspiciouCodeStr(startPos, endPos);
			this.generatePatch(code);
//		} else {
//			ITree parent = suspCodeTree.getParent();
//			if (Checker.isSynchronizedStatement(parent.getType())) {
//				
//			}
		} else if (Checker.isIfStatement(suspCodeTree.getType())) {
			/*
			 * FB	UCFUselessControlFlow
			 * 
			 * Fix Pattern:
			 * 1. DEL IfStatement@...
			 * 2. DEL SwithStatement@...
			 * 
			 */
			int endPos1 = 0;
			List<ITree> children = this.getSuspiciousCodeTree().getChildren();
			int size = children.size();
			ITree lastChild = children.get(size - 1);
			
			if ("ElseBody".equals(lastChild.getLabel())) {
				// Remove the control flow, but keep the statements in else block.
				List<ITree> subChildren = lastChild.getChildren();
				if (!subChildren.isEmpty()) {
					endPos1 = subChildren.get(0).getPos();
					ITree lastStmt = subChildren.get(subChildren.size() - 1);
					int endPos2 = lastStmt.getPos() + lastStmt.getLength();
					
					String fixedCodeStr1 = this.getSubSuspiciouCodeStr(endPos1, endPos2);
					this.generatePatch(fixedCodeStr1);
				}
				lastChild = children.get(size - 2);
			}
			if ("ThenBody".equals(lastChild.getLabel())) {// Then block
				// Remove the control flow, but keep the statements in then block.
				List<ITree> subChildren = lastChild.getChildren();
				if (!subChildren.isEmpty()) {
					endPos1 = subChildren.get(0).getPos();
					ITree lastStmt = subChildren.get(subChildren.size() - 1);
					int endPos2 = lastStmt.getPos() + lastStmt.getLength();
					
					String fixedCodeStr1 = this.getSubSuspiciouCodeStr(endPos1, endPos2);
					this.generatePatch(fixedCodeStr1); 
				}
			}
			
			if (endPos1 == 0) {
				// No Statement in the control flow.
				return;
			}
		}
		
		if (!Checker.isReturnStatement(stmtType) && !Checker.isContinueStatement(stmtType) 
				&& !Checker.isBreakStatement(stmtType) && !Checker.isSwitchCase(stmtType)) {
			this.generatePatch("");
		}
		
		/*
		 * FB	UPMUncalledPrivateMethod
		 * 
		 * Fix Pattern:
		 * DEL MethodDeclaration@...
		 * 
		 */
		ITree parentTree = suspCodeTree;
		while (true) {
			if (Checker.isMethodDeclaration(parentTree.getType())) break;
			parentTree = parentTree.getParent();
			if (parentTree == null) break;
		}
		if (parentTree == null) return;
		
		int startPos = parentTree.getPos();
		int endPos = startPos + parentTree.getLength();
		this.generatePatch(startPos, endPos, "", "");
	}

}
