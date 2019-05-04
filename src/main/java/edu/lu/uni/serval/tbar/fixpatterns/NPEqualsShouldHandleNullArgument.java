package edu.lu.uni.serval.tbar.fixpatterns;

import java.util.ArrayList;
import java.util.List;

import edu.lu.uni.serval.entity.Pair;
import edu.lu.uni.serval.jdt.tree.ITree;
import edu.lu.uni.serval.tbar.fixtemplate.FixTemplate;
import edu.lu.uni.serval.tbar.utils.Checker;

/**
 * Fix pattern for NP_EQUALS_SHOULD_HANDLE_NULL_ARGUMENT violations.
 * 
 * @author kui.liu
 *
 */
public class NPEqualsShouldHandleNullArgument extends FixTemplate {
	/*
	 * Fix pattern 1:
	 * -  other.getClass() == getClass() && equals((T) other);
	 * +  this == other || other != null && other.getClass() == getClass() && equals((T) other);
	 * 
	 * Fix pattern 2:
	 * -   toString().equals(obj.toString());
	 * +   obj instanceof T && toString().equals(obj.toString());
	 */
	
	private List<Pair<ITree, String>> buggyExps1 = new ArrayList<>();
	private List<Pair<ITree, String>> buggyExps2 = new ArrayList<>();

	@Override
	public void generatePatches() {
		ITree suspCodeTree = this.getSuspiciousCodeTree();
		identifyBuggyExpressions(suspCodeTree);
		
		for (Pair<ITree, String> buggyExp : buggyExps1) {
			ITree buggyExpTree = buggyExp.getFirst();
			String expStr = buggyExp.getSecond();
			int pos = buggyExpTree.getPos();
			String codePart1 = this.getSubSuspiciouCodeStr(suspCodeStartPos, pos);
			String codePart2 = this.getSubSuspiciouCodeStr(pos, suspCodeEndPos);
			String fixedCodeStr1 = codePart1 + "this == " + expStr + " || " + expStr + " != null && " + codePart2;
			this.generatePatch(fixedCodeStr1);
		}
		
		if (!buggyExps2.isEmpty()) {
			String className = readClassName(suspCodeTree);
			if (className == null) return;
			for (Pair<ITree, String> buggyExp : buggyExps2) {
				ITree buggyExpTree = buggyExp.getFirst();
				String expStr = buggyExp.getSecond();
				int pos = buggyExpTree.getPos();
				String codePart1 = this.getSubSuspiciouCodeStr(suspCodeStartPos, pos);
				String codePart2 = this.getSubSuspiciouCodeStr(pos, suspCodeEndPos);
				String fixedCodeStr1 = codePart1 + expStr + " instanceof " + className + " && " + codePart2;
				this.generatePatch(fixedCodeStr1);
			}
		}
	}

	private void identifyBuggyExpressions(ITree suspCodeTree) {
		List<ITree> children = suspCodeTree.getChildren();
		for (ITree child : children) {
			int childType = child.getType();
			if (Checker.isComplexExpression(childType)) {
				if (Checker.isInfixExpression(childType)) {
					if ("==".equals(child.getChild(1).getLabel())) {
						String leftHandExpStr = child.getChild(0).getLabel();
						String rightHandExpStr = child.getChild(2).getLabel();
						String expStr = null;
						if ("MethodName:getClass:[]".equals(leftHandExpStr) && rightHandExpStr.endsWith(".getClass()")) {
							expStr = rightHandExpStr.substring(0, rightHandExpStr.lastIndexOf("."));
						} else if ("MethodName:getClass:[]".equals(rightHandExpStr) && leftHandExpStr.endsWith(".getClass()")) {
							expStr = leftHandExpStr.substring(0, leftHandExpStr.lastIndexOf("."));
						}
						if (expStr != null) {
							buggyExps1.add(new Pair<ITree, String>(child, expStr));
							continue;
						}
					}
				} else if (Checker.isMethodInvocation(childType)) {
					String expStr = null;
					if (child.getLabel().startsWith("toString().equals(") && child.getChildren().size() == 2) {
						ITree subChild = child.getChild(1);
						if (subChild.getChildren().size() == 1) {
							subChild = subChild.getChild(0);
							String label = subChild.getLabel();
							if (label.endsWith(".toString()")) {
								expStr = label.substring(0, 11);
							}
						}
					} else if (child.getLabel().endsWith(".toString().equals(toString())")) {
						expStr = child.getLabel();
						expStr = expStr.substring(0, expStr.length() - 30);
					}
					if (expStr != null) {
						buggyExps2.add(new Pair<ITree, String>(child, expStr));
						continue;
					}
				} 
				identifyBuggyExpressions(child);
			} else if (Checker.isStatement(childType)) break;
		}
	}

	private String readClassName(ITree suspCodeTree) {
		ITree parent = suspCodeTree.getParent();
		while (true) {
			if (parent == null) return null;
			if (Checker.isTypeDeclaration(parent.getType())) {
				break;
			}
			parent = parent.getParent();
		}
		
		String classLabel = parent.getLabel();
		String className = classLabel.substring(classLabel.indexOf("ClassName:") + 10);
		className = className.substring(0, className.indexOf(", "));
		
		return className;
	}
}
