package edu.lu.uni.serval.tbar.fixpatterns;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.lu.uni.serval.jdt.tree.ITree;
import edu.lu.uni.serval.tbar.context.ContextReader;
import edu.lu.uni.serval.tbar.utils.Checker;

/**
 * Mutate the conditional expressions in a suspicious statement.
 * 
 * Context: conditional expressions.
 * 
 * @author kui.liu
 *
 */
public class ConditionalExpressionMutator extends ChangeCondition {
	
	/*
	 * PAR	ExpressionAdder
	 * PAR	ExpressionRemover
	 * PAR	ExpressionReplacer
	 * SketchFix	ConditionTransform:M_con ==> insert new conditional expression.
	 *
	 * Fuzzy fix patterns:
	 * SimFix	InsIfStmt
	 * SOFix	IfChecker
	 * 
	 */
	
	private int type = 0;
	public ConditionalExpressionMutator(int type) {
		this.type = type;
	}
	
	@Override
	public void generatePatches() {
		Map<ITree, Integer> allSuspPredicateExps = readAllSuspiciousPredicateExpressions(this.getSuspiciousCodeTree());
		if (type == 1) { // Do, If, While statements.
			new ExpressionRemover().generatePatches(allSuspPredicateExps);
		} else if (type == 2) {
			Map<ITree, Integer> allPredicateExpressions = identifyPredicateExpressions();
			new ExpressionAdder().generatePatches(allPredicateExpressions);
//			new ExpressionReplacer().generatePatches(allPredicateExpressions, allSuspPredicateExps);
		} else {
			// Conditional expression.
			new ExpressionRemover().generatePatches(allSuspPredicateExps);
		}
	}
	
	public class ExpressionAdder {
		/*
		 * a -> a || c;
		 * a -> a && c;
		 */
		List<String> triedExpCands = new ArrayList<>();
		
		public void generatePatches(Map<ITree, Integer> allPredicateExpressions) {
			//TODO: this expression adder is just simply adding expression candidates. To be improved.
			ITree suspStmtAst = getSuspiciousCodeTree();
			ITree suspPredicateExp = null;
			if (Checker.isDoStatement(suspStmtAst.getType())) {
				List<ITree> children = suspStmtAst.getChildren();
				suspPredicateExp = children.get(children.size() - 1);
			} else {// If, while statement.
				suspPredicateExp = suspStmtAst.getChild(0);
			}
			int suspPredicateExpStartPos = suspPredicateExp.getPos();
			int suspPredicateExpEndPos = suspPredicateExpStartPos + suspPredicateExp.getLength();
			String suspPredicateExpStr = getSubSuspiciouCodeStr(suspPredicateExpStartPos, suspPredicateExpEndPos);
			
			String suspCodeStr = getSuspiciousCodeStr();
			int suspStmtStartPos = suspStmtAst.getPos();
			String codePart1 = suspCodeStr.substring(0, suspPredicateExpStartPos - suspStmtStartPos);
			String codePart2 = suspCodeStr.substring(suspPredicateExpEndPos - suspStmtStartPos);
			
			List<String> variables = readVariables(suspPredicateExp);
			
			for (Map.Entry<ITree, Integer> entry : allPredicateExpressions.entrySet()) {
				// same expression problem.
				ITree predicateExpCandidate = entry.getKey();
				int predicateExpStartPos = predicateExpCandidate.getPos();
				int predicateExpEndPos = predicateExpStartPos + predicateExpCandidate.getLength();
				
				String predicateExpCandidateStr = getPredicateExpCandidate(predicateExpStartPos, predicateExpEndPos);
				if (suspPredicateExpStr.contains(predicateExpCandidateStr) || predicateExpCandidateStr.equals(suspPredicateExpStr)) continue;
				if (predicateExpCandidateStr.contains("==null") || predicateExpCandidateStr.contains("== null")
						|| predicateExpCandidateStr.contains("!=null") || predicateExpCandidateStr.contains("!= null")) continue;
				if (triedExpCands.contains(predicateExpCandidateStr)) continue;
				triedExpCands.add(predicateExpCandidateStr);
				
				List<String> vars = readVariables(predicateExpCandidate);
				vars.retainAll(variables);
				if (vars.isEmpty()) continue;
				/*
				 * TODO: use the context information to limit the search space of predicate expression candidates. 
				 */
				String fixedCodeStr1 = codePart1 + "(" + suspPredicateExpStr + ") || (" + predicateExpCandidateStr + ")" + codePart2;
				generatePatch(fixedCodeStr1);
				
				fixedCodeStr1 = codePart1 + "(" + suspPredicateExpStr + ") && (" + predicateExpCandidateStr + ")" + codePart2;
				generatePatch(fixedCodeStr1);
				
				fixedCodeStr1 = codePart1 + "(" + suspPredicateExpStr + ") || !(" + predicateExpCandidateStr + ")" + codePart2;
				generatePatch(fixedCodeStr1);
				
				fixedCodeStr1 = codePart1 + "(" + suspPredicateExpStr + ") && !(" + predicateExpCandidateStr + ")" + codePart2;
				generatePatch(fixedCodeStr1);
			}
			
			if (!Checker.isIfStatement(suspStmtAst.getType())) return;
			
			List<String> vars = new ArrayList<>();
			ContextReader.identifySuspiciousVariables(suspStmtAst, null, vars);
			if (vars.isEmpty()) return;
			
			/* SketchFix
			 * 
			 * FP1: M_con
			 * Insert new conditional Expression.
			 */
			ContextReader.readAllVariablesAndFields(suspStmtAst, allVarNamesMap, varTypesMap, allVarNamesList, sourceCodePath, dic);
			int pos1 = suspStmtAst.getChild(0).getPos();
			int pos2 = pos1 + suspStmtAst.getChild(0).getLength();
			codePart1 = getSubSuspiciouCodeStr(suspCodeStartPos, pos1);
			String conditionalExpStr = getSubSuspiciouCodeStr(pos1, pos2);
			codePart2 = getSubSuspiciouCodeStr(pos2, suspCodeEndPos);
			for (String var : vars) {
				String varType = varTypesMap.get(var);
				if (varType == null) continue;
				List<String> varList = allVarNamesMap.get(varType);
				varList.remove(var);
				if (varList.isEmpty()) continue;
				String[] operators = selectOperators(varType);
				for (String v_ : varList) {
					for (String op : operators) {
						// FP1.
						String fixedCodeStr1 = codePart1 + var + op + v_ + " && (" + conditionalExpStr + ")" + codePart2;
						generatePatch(fixedCodeStr1);
						fixedCodeStr1 = codePart1 + var + op + v_ + " || (" + conditionalExpStr + ")" + codePart2;
						generatePatch(fixedCodeStr1);
					}
				}
			}
			clear();
		}
		
		private List<String> readVariables(ITree predicateExp) {
			List<String> vars = new ArrayList<>();
			List<ITree> children = predicateExp.getChildren();
			for (ITree child : children) {
				if (Checker.isSimpleName(child.getType())) {
					String var = ContextReader.readVariableName(child);
					if (var == null) vars.addAll(readVariables(child));
					else vars.add(var);
				} else vars.addAll(readVariables(child));
			}
			return vars;
		}

		private String[] selectOperators(String varType) {
			if ("byte".equals(varType) || "Byte".equals(varType)
					|| "short".equals(varType) || "Short".equals(varType)
					|| "int".equals(varType) || "Integer".equals(varType)
					|| "long".equals(varType) || "Long".equals(varType)
					|| "double".equals(varType) || "Double".equals(varType)
					|| "float".equals(varType) || "Float".equals(varType)) {
				return new String[]{" != ", " == ", " < ", " <= ", " > ", " >= "};
			}
			return new String[]{" != ", " == "};
		}
	}

	public class ExpressionRemover {

		/*
		 * a || b -> a;
		 * a && b -> a;
		 */
		
		public void generatePatches(Map<ITree, Integer> allPredicateExps) {
			for (Map.Entry<ITree, Integer> entry : allPredicateExps.entrySet()) {
				ITree predicateExp = entry.getKey();
				
				if (Checker.isConditionalExpression(predicateExp.getType())) {
					int startPos = predicateExp.getPos();
					int endPos = startPos + predicateExp.getLength();
					String fixedCodeStr1 = getSubSuspiciouCodeStr(suspCodeStartPos, startPos);
					String fixedCodeStr2 = getSubSuspiciouCodeStr(endPos, suspCodeEndPos);
					
					ITree thenExp = predicateExp.getChild(1);
					startPos = thenExp.getPos();
					endPos = startPos + thenExp.getLength();
					String fixedCodeStr = fixedCodeStr1 + getSubSuspiciouCodeStr(startPos, endPos) + fixedCodeStr2;
					generatePatch(fixedCodeStr);
					
					ITree elseExp = predicateExp.getChild(2);
					startPos = elseExp.getPos();
					endPos = startPos + elseExp.getLength();
					fixedCodeStr = fixedCodeStr1 + getSubSuspiciouCodeStr(startPos, endPos) + fixedCodeStr2;
					generatePatch(fixedCodeStr);
					
					continue;
				}
				
				int pos = entry.getValue();
				int predicateExpStartPos = predicateExp.getPos();
				String fixedCodeStr1;
				if (pos == 0) {
					continue;
				} else if (pos > predicateExpStartPos) {
					fixedCodeStr1 = getSubSuspiciouCodeStr(suspCodeStartPos, predicateExpStartPos);
					fixedCodeStr1 += getSubSuspiciouCodeStr(pos, suspCodeEndPos);
				} else {
					fixedCodeStr1 = getSubSuspiciouCodeStr(suspCodeStartPos, pos);
					fixedCodeStr1 += getSubSuspiciouCodeStr(predicateExpStartPos + predicateExp.getLength(), suspCodeEndPos);
				}
				generatePatch(fixedCodeStr1);
			}
		}

	}
	
	public class ExpressionReplacer {
		
		/*
		 * a || b -> a || c
		 */
		Map<ITree, List<String>> triedExpCandsMap = new HashMap<>();
		
		public void generatePatches(Map<ITree, Integer> allPredicateExpressions, Map<ITree, Integer> allSuspPredicateExps) {
			ITree suspStmtAst = getSuspiciousCodeTree();
			
			ITree suspPredicateExp = null;
			if (Checker.isDoStatement(suspStmtAst.getType())) {
				List<ITree> children = suspStmtAst.getChildren();
				suspPredicateExp = children.get(children.size() - 1);
			} else {
				suspPredicateExp = suspStmtAst.getChild(0);
			}
			int suspPredicateExpStartPos = suspPredicateExp.getPos();
			int suspPredicateExpEndPos = suspPredicateExpStartPos + suspPredicateExp.getLength();
			String suspPredicateExpStr = getSubSuspiciouCodeStr(suspPredicateExpStartPos, suspPredicateExpEndPos);
			
			for (Map.Entry<ITree, Integer> entry : allSuspPredicateExps.entrySet()) {
				ITree suspExpAst = entry.getKey();
				int suspExpStartPos = suspExpAst.getPos();
				int suspExpEndPos = suspExpStartPos + suspExpAst.getLength();
				String suspCodePart1 = getSubSuspiciouCodeStr(suspCodeStartPos, suspExpStartPos);
				String suspCodePart2 = getSubSuspiciouCodeStr(suspExpEndPos,suspCodeEndPos);
				
				String buggyExpStr = getSubSuspiciouCodeStr(suspExpStartPos, suspExpEndPos);
				for (Map.Entry<ITree, Integer> entry2 : allPredicateExpressions.entrySet()) {
					ITree expCandidate = entry2.getKey();
					int expCandidateStartPos = expCandidate.getPos();
					int expCandidateEndPos = expCandidateStartPos + expCandidate.getLength();
					String expCandidateStr = getPredicateExpCandidate(expCandidateStartPos, expCandidateEndPos);
					if (expCandidateStr.contains("==null") || expCandidateStr.contains("== null")
							|| expCandidateStr.contains("!=null") || expCandidateStr.contains("!= null")) continue;
					if (suspPredicateExpStr.contains(expCandidateStr) || expCandidateStr.equals(buggyExpStr)) continue;
					List<String> triedExpCands = triedExpCandsMap.get(suspExpAst);
					if (triedExpCands == null) {
						triedExpCands = new ArrayList<>();
						triedExpCandsMap.put(suspExpAst, triedExpCands);
					}
					else if (triedExpCands.contains(expCandidateStr)) continue;
					triedExpCands.add(expCandidateStr);
					//FIXME: Whether the candidate expression is related to the suspicious code?
					generatePatch(suspCodePart1 + "(" + expCandidateStr + ")" + suspCodePart2);
				}
			}
		}
	}
	
}
