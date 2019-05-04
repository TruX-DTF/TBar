package edu.lu.uni.serval.tbar.fixpatterns;

import java.util.ArrayList;
import java.util.List;

import edu.lu.uni.serval.entity.Pair;
import edu.lu.uni.serval.jdt.tree.ITree;
import edu.lu.uni.serval.tbar.fixtemplate.FixTemplate;
import edu.lu.uni.serval.tbar.utils.Checker;

/**
 * Fix patterns for ICAST_IDIV_CAST_TO_DOUBLE violations.
 * 
 * Context: InfixExpression and the operator is "/".
 * 
 * @author kui.liu
 *
 */
public class ICASTIdivCastToDouble extends FixTemplate {

	/*
	 * Fix pattern 1:
	 * - intVarExp / 10;
     * + intVarExp / 10d(or f);
     * 
     * Fix pattern 2:
     * -   1 / var
     * +   1.0 / var
     * 
     * Fix pattern 3:
     * -    dividend / divisor;
     * +    dividend / (float or double) divisor;
     * 
     * Fix pattern 4:
     * -   dividend / divisor;
     * +   (double or float) dividend / divisor;
     * 
     * Fix pattern 5:
     * - intVarExp / 2;
     * + 0.5 * intVarExp;
	 */
	
	@Override
	public void generatePatches() {
		ITree suspCodeTree = this.getSuspiciousCodeTree();
		List<Pair<ITree, Integer>> buggyExps = identifyBuggyExpressions(suspCodeTree);
		
		for (Pair<ITree, Integer> buggyExp : buggyExps) {
			ITree buggyExpTree = buggyExp.getFirst();
			int startPos = buggyExpTree.getPos();
			int endPos = startPos + buggyExpTree.getLength();
			String codePart1 = this.getSubSuspiciouCodeStr(suspCodeStartPos, startPos);
			String codePart2 = this.getSubSuspiciouCodeStr(endPos, suspCodeEndPos);
			int fixPatternType = buggyExp.getSecond();
			
			switch (fixPatternType) {
			case 1:
				//FP1
				String code = this.getSubSuspiciouCodeStr(startPos, endPos);
				this.generatePatch(codePart1 + code + "d" + codePart2);
				this.generatePatch(codePart1 + code + "f" + codePart2);
				
				//FP5
				endPos = buggyExpTree.getChild(0).getPos() + buggyExpTree.getChild(0).getLength();
				String numberLiteral = buggyExpTree.getChild(2).getLabel();
				code = "(1.0 / " + numberLiteral + ") * " + this.getSubSuspiciouCodeStr(startPos, endPos);
				this.generatePatch(codePart1 + code + codePart2);
				break;
			case 2://FP2
				numberLiteral = buggyExpTree.getChild(0).getLabel();
				startPos = buggyExpTree.getChild(0).getPos() + buggyExpTree.getChild(0).getLength();
				code = this.getSubSuspiciouCodeStr(startPos, endPos);
				code = numberLiteral + ".0" + code;
				this.generatePatch(codePart1 + code + codePart2);
				break;
			case 3:
				//FP3
				ITree rightHandExp = buggyExpTree.getChild(2);
				int startPos1 = rightHandExp.getPos();
				int endPos1 = startPos1 + rightHandExp.getLength();
				String rightHandExpCode = this.getSubSuspiciouCodeStr(startPos1, endPos1);
				code = this.getSubSuspiciouCodeStr(startPos, startPos1);
				this.generatePatch(codePart1 + code + "(double)" + rightHandExpCode + codePart2);
				this.generatePatch(codePart1 + code + "(float)" + rightHandExpCode + codePart2);
				
				//FP4
				code = this.getSubSuspiciouCodeStr(startPos, endPos);
				this.generatePatch(codePart1 + "(double)" + code + codePart2);
				this.generatePatch(codePart1 + "(float)" + code + codePart2);
				break;
			default:
				break;
			}
		}
	}

	private List<Pair<ITree, Integer>> identifyBuggyExpressions(ITree suspCodeTree) {
		List<Pair<ITree, Integer>> buggyExps = new ArrayList<>();
		List<ITree> children = suspCodeTree.getChildren();
		for (ITree child : children) {
			int childType = child.getType();
			if (Checker.isComplexExpression(childType)) {
				if (Checker.isInfixExpression(childType)) {
					String operator = child.getChild(1).getLabel();
					if ("/".equals(operator)) {
						if (Checker.isNumberLiteral(child.getChild(0).getType())) {
							String leftHandExpLabel = child.getChild(0).getLabel();
							if (!leftHandExpLabel.endsWith("f") && !leftHandExpLabel.endsWith("d") && !leftHandExpLabel.endsWith("l")) {
								if (!child.getChild(2).getLabel().startsWith("(double)") && !child.getChild(2).getLabel().startsWith("(float)")) {
									buggyExps.add(new Pair<ITree, Integer>(child, Integer.valueOf(2)));
								}
							}
						} else if (Checker.isNumberLiteral(child.getChild(2).getType())) {
							String rightHandExpLabel = child.getChild(2).getLabel();
							if (!rightHandExpLabel.endsWith("f") && !rightHandExpLabel.endsWith("d") && !rightHandExpLabel.endsWith("l")) {
								if (!child.getLabel().startsWith("(double)") && !child.getLabel().startsWith("(float)")) {
									buggyExps.add(new Pair<ITree, Integer>(child, Integer.valueOf(1)));
								}
							}
						} else {
							if (!child.getLabel().startsWith("(double)") && !child.getLabel().startsWith("(float)") &&
									!child.getChild(2).getLabel().startsWith("(double)") && !child.getChild(2).getLabel().startsWith("(float)")) {
								buggyExps.add(new Pair<ITree, Integer>(child, Integer.valueOf(3)));
							}
						}
					}
				}
				buggyExps.addAll(identifyBuggyExpressions(child));
			} else if (Checker.isSimpleName(childType) && child.getLabel().startsWith("MethodName:")) {
				buggyExps.addAll(identifyBuggyExpressions(child));
			} else if (Checker.isStatement(childType)) {
				break;
			}
		}
		return buggyExps;
	}

}
