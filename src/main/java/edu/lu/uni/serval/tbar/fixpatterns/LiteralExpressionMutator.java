package edu.lu.uni.serval.tbar.fixpatterns;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.math.NumberUtils;

import edu.lu.uni.serval.jdt.tree.ITree;
import edu.lu.uni.serval.tbar.fixtemplate.FixTemplate;
import edu.lu.uni.serval.tbar.utils.Checker;

public class LiteralExpressionMutator extends FixTemplate {
	
	/*
	 * SimFix: Replacement (BLIT, BLIT). 
	 *  
	 */
	private List<ITree> suspCons = new ArrayList<>();
	
	public void generatePatches() {
		ITree suspCodeTree = getSuspiciousCodeTree();
		identifyPotentialBuggyExpressions(suspCodeTree);
		if (suspCons.isEmpty()) return;
		
//		ContextReader.readAllVariablesAndFields(suspCodeTree, allVarNamesMap, varTypesMap, allVarNamesList, sourceCodePath, dic);
		
		for (ITree suspCon : suspCons) {
			List<String> varCandidates = new ArrayList<>();;
			int suspConType = suspCon.getType();
			if (Checker.isStringLiteral(suspConType)) {
				// TODO: How to select other string literals?
//				List<String> vars = allVarNamesMap.get("String");
//				if (vars != null) varCandidates.addAll(vars);
			} else if (Checker.isNumberLiteral(suspConType)) {
				// TODO: How to select other number literals?
				String num = suspCon.getLabel();
				if (NumberUtils.isDigits(num)) {
					boolean isTrue = true;
					if (Checker.isInfixExpression(suspCon.getParent().getType())) {
						String op = suspCon.getParent().getChild(1).getLabel();
						if ("==".equals(op) || "!=".equals(op) || ">=".equals(op) || "<=".equals(op) || ">".equals(op) || "<".equals(op)) isTrue = false;
					}
					if (isTrue) {
						varCandidates.add(num + "d");
						varCandidates.add(num + "f");
					}
				}
//				List<String> vars = allVarNamesMap.get("int");
//				if (vars != null) varCandidates.addAll(vars);
//				vars = allVarNamesMap.get("Integer");
//				if (vars != null) varCandidates.addAll(vars);
//				vars = allVarNamesMap.get("short");
//				if (vars != null) varCandidates.addAll(vars);
//				vars = allVarNamesMap.get("Short");
//				if (vars != null) varCandidates.addAll(vars);
//				vars = allVarNamesMap.get("long");
//				if (vars != null) varCandidates.addAll(vars);
//				vars = allVarNamesMap.get("Long");
//				if (vars != null) varCandidates.addAll(vars);
//				vars = allVarNamesMap.get("float");
//				if (vars != null) varCandidates.addAll(vars);
//				vars = allVarNamesMap.get("Float");
//				if (vars != null) varCandidates.addAll(vars);
//				vars = allVarNamesMap.get("double");
//				if (vars != null) varCandidates.addAll(vars);
//				vars = allVarNamesMap.get("Double");
//				if (vars != null) varCandidates.addAll(vars);
			} else if (Checker.isBooleanLiteral(suspConType)) {
				if ("true".equals(suspCon.getLabel())) varCandidates.add("false");
				else varCandidates.add("true");
//				List<String> vars = allVarNamesMap.get("boolean");
//				if (vars != null) varCandidates.addAll(vars);
//				vars = allVarNamesMap.get("Boolean");
//				if (vars != null) varCandidates.addAll(vars);
			} else {
				// TODO: How to select other character literals?
//				List<String> vars = allVarNamesMap.get("char");
//				if (vars != null) varCandidates.addAll(vars);
//				vars = allVarNamesMap.get("Character");
//				if (vars != null) varCandidates.addAll(vars);
			}
			
			int pos1 = suspCon.getPos();
			int pos2 = pos1 + suspCon.getLength();
			String codePart1 = getSubSuspiciouCodeStr(suspCodeStartPos, pos1);
			String codePart2 = getSubSuspiciouCodeStr(pos2, suspCodeEndPos);
			
			for (String v : varCandidates) {
				if (this.getSuspiciousCodeStr().contains(v)) continue;
				String fixedCodeStr1 = codePart1 + v + codePart2;
				generatePatch(fixedCodeStr1);
			}
		}
		clear();
	}

	private void identifyPotentialBuggyExpressions(ITree suspCodeTree) {
		List<ITree> children = suspCodeTree.getChildren();
		for (ITree child : children) {
			int childType = child.getType();
			if (Checker.isSimpleName(childType) && child.getLabel().startsWith("MethodName:")) {
				identifyPotentialBuggyExpressions(child);
			} else if (Checker.isStringLiteral(childType) || Checker.isBooleanLiteral(childType) || Checker.isCharacterLiteral(childType)) {
				suspCons.add(child);
			} else if (Checker.isNumberLiteral(childType)) {
				if (Checker.isMethodInvocation(suspCodeTree.getType())) continue;
				if (Checker.isArrayAccess(suspCodeTree.getType())) continue;
				suspCons.add(child);
			} else if (Checker.isComplexExpression(childType)) {
				identifyPotentialBuggyExpressions(child);
			} else if (Checker.isStatement(childType)) break;
		}
	}

}
