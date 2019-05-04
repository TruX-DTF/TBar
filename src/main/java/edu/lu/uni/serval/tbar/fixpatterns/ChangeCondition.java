package edu.lu.uni.serval.tbar.fixpatterns;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import edu.lu.uni.serval.jdt.tree.ITree;
import edu.lu.uni.serval.tbar.context.ContextReader;
import edu.lu.uni.serval.tbar.fixtemplate.FixTemplate;
import edu.lu.uni.serval.tbar.utils.Checker;

/**
 * Pattern: Changing a branch condition. 
 * Example: if(a == b) → if(a == b && c != 0).
 * Description: This pattern modifies a branch condition in conditional statements or in ternary operators. 
 * 				Patches in this pattern often just add a term to a predicate or remove a term from a predicate.
 * 
 * @author kui.liu
 *
 */
public abstract class ChangeCondition extends FixTemplate {

	private boolean ignoreOtherMethods = false; // FIXME do not ignore other methods.
	
	/**
	 * Map<ITree, Integer>: ITree - Predicate Exp AST, Integer - distance to suspicious stmt.
	 * @return
	 */
	protected Map<ITree, Integer> identifyPredicateExpressions() {
		Map<ITree, Integer> predicateExps = new HashMap<>();
		ITree suspStmtAst = this.getSuspiciousCodeTree();
		ITree parent = suspStmtAst.getParent();
		int suspIndex = parent.getChildPosition(suspStmtAst);
		List<ITree> peerStmts = parent.getChildren();
		int size = peerStmts.size();
		
		for (int index = 0; index < suspIndex; index ++) {
			ITree peerStmt = peerStmts.get(index);
			predicateExps.putAll(identifyPredicateExpressions(peerStmt, 1, true));
		}
		
		List<ITree> children = suspStmtAst.getChildren();
		for (ITree child : children) {
			if (Checker.isStatement(child.getType())) {
				predicateExps.putAll(identifyPredicateExpressions(child, 1, false));
			}
		}
		
		for (int index = suspIndex; index < size; index ++) {
			ITree peerStmt = peerStmts.get(index);
			predicateExps.putAll(identifyPredicateExpressions(peerStmt, 1, false));
		}
		
		identifyPredicateExpressionsInParentTree(parent, 1, predicateExps);

		return sortByValueAscending(predicateExps);
	}

	private void identifyPredicateExpressionsInParentTree(ITree tree, int distance, Map<ITree, Integer> predicateExps) {
		int treeType = tree.getType();
		if (Checker.isTypeDeclaration(treeType)) {
		} else if (Checker.isMethodDeclaration(treeType) && !ignoreOtherMethods) {
			ITree parent = tree.getParent();
			int suspIndex = parent.getChildPosition(tree);
			List<ITree> peerMethods = parent.getChildren();
			int size = peerMethods.size();
			
			for (int index = 0; index < size; index ++) {
				if (index != suspIndex) {
					List<ITree> children = peerMethods.get(index).getChildren();
					for (ITree child : children) {
						if (Checker.isStatement(child.getType())) {
							predicateExps.putAll(identifyPredicateExpressions(child, distance + 1, false));
						}
					}
				}
			}
			identifyPredicateExpressionsInParentTree(tree.getParent(), distance + 1, predicateExps);
		} else {
			ITree parent = tree.getParent();
			if (parent == null) return;
			int suspIndex = parent.getChildPosition(tree);
			List<ITree> peerStmts = parent.getChildren();
			int size = peerStmts.size();
			
			for (int index = 0; index < suspIndex; index ++) {
				ITree peerStmt = peerStmts.get(index);
				if (Checker.isStatement(peerStmt.getType())) {
					predicateExps.putAll(identifyPredicateExpressions(peerStmt, distance + 1, true));
				}
			}
			List<ITree> children = tree.getChildren();
			for (ITree child : children) {
				if (!Checker.isStatement(child.getType())) {
					predicateExps.putAll(identifyPredicateExpressions(child, distance + 1, false));
				}
			}
			for (int index = suspIndex; index < size; index ++) {
				ITree peerStmt = peerStmts.get(index);
				if (Checker.isStatement(peerStmt.getType())) {
					predicateExps.putAll(identifyPredicateExpressions(peerStmt, distance + 1, false));
				}
			}
			identifyPredicateExpressionsInParentTree(parent, distance + 1, predicateExps);
		}
	}

	private Map<ITree, Integer> identifyPredicateExpressions(ITree codeAst, int distance, boolean considerVarDec) {
		Map<ITree, Integer> predicateExps = new HashMap<>();
		List<String> varNames = new ArrayList<>();
		int codeAstType = codeAst.getType();
		List<ITree> children = codeAst.getChildren();
		int size = children.size();
		if (Checker.isStatement(codeAstType)) {
			if (Checker.isDoStatement(codeAstType)) {
				ITree exp = children.get(size - 1);
				identifySinglePredicateExpressions(exp, distance + 1, predicateExps, varNames);
				for (int index = 0; index < size - 1; index ++) {
					ITree child = children.get(index);
					predicateExps.putAll(identifyPredicateExpressions(child, distance + 1, !Checker.isStatement(child.getType())));
				}
			} else if (Checker.isIfStatement(codeAstType) || Checker.isWhileStatement(codeAstType)) {
				ITree exp = children.get(0);
				identifySinglePredicateExpressions(exp, distance + 1, predicateExps, varNames);
				for (int index = 1; index < size; index ++) {
					ITree child = children.get(index);
					predicateExps.putAll(identifyPredicateExpressions(child, distance + 1, !Checker.isStatement(child.getType())));
				}
			} else {// Other Statements.
				if (Checker.isVariableDeclarationStatement(codeAstType) && !considerVarDec) {
					// get the variable name.
					String varName = ContextReader.identifyVariableName(codeAst);
					varNames.add(varName);
				}
				for (ITree child : children) {
					predicateExps.putAll(identifyPredicateExpressions(child, distance + 1, !Checker.isStatement(child.getType())));
				}
			}
		} else if (Checker.isComplexExpression(codeAstType)) {
			if (Checker.isConditionalExpression(codeAstType)) {// ConditionalExpression
				identifySinglePredicateExpressions(codeAst.getChild(0), distance + 1, predicateExps, varNames);
			}
			for (ITree child : children) {
				predicateExps.putAll(identifyPredicateExpressions(child, distance + 1, false));
			}
		}
		
		return predicateExps;
	}

	private void identifySinglePredicateExpressions(ITree expAst, int distance, Map<ITree, Integer> predicateExps, List<String> varNames) {
		if (Checker.isInfixExpression(expAst.getType())) { // InfixExpression
			String operator = expAst.getChild(1).getLabel();
			if ("||".equals(operator) || "&&".equals(operator)) {
				identifySinglePredicateExpressions(expAst.getChild(0), distance + 1, predicateExps, varNames);
				List<ITree> children = expAst.getChildren();
				int size = children.size();
				for (int index = 2; index < size; index ++) {
					identifySinglePredicateExpressions(children.get(index), distance + 1, predicateExps, varNames);
				}
			} else {
				if (!ContextReader.containsVar(expAst, varNames)) {
					predicateExps.put(expAst, distance);
				}
			}
//		} else { // if (Checker.isValidExpression(expAst.getType()))
//			if (this.containsVar(expAst, varNames)) {
//				predicateExps.put(expAst, distance);
//			}
		}
		if (!ContextReader.containsVar(expAst, varNames)) {
			predicateExps.put(expAst, distance);
		}
	}
	
	private <K, V extends Comparable<? super V>> Map<K, V> sortByValueAscending(Map<K, V> unsortMap) {

        // 1. Convert Map to List of Map
        List<Map.Entry<K, V>> list =
                new LinkedList<Map.Entry<K, V>>(unsortMap.entrySet());

        // 2. Sort list with Collections.sort(), provide a custom Comparator
        //    Try switch the o1 o2 position for a different order
        Collections.sort(list, new Comparator<Map.Entry<K, V>>() {
            public int compare(Map.Entry<K, V> o1,
                               Map.Entry<K, V> o2) {
                return o1.getValue().compareTo(o2.getValue());
            }
        });

        // 3. Loop the sorted list and put it into a new insertion order Map LinkedHashMap
        Map<K, V> sortedMap = new LinkedHashMap<K, V>();
        for (Map.Entry<K, V> entry : list) {
            sortedMap.put(entry.getKey(), entry.getValue());
        }

        return sortedMap;
    }
	
	protected String getPredicateExpCandidate(int predicateExpStartPos, int predicateExpEndPos) {
		return suspJavaFileCode.substring(predicateExpStartPos, predicateExpEndPos);
	}
	
	/**
	 * Map<ITree, Integer>: ITree - predicate exp ast, Integer - start or end pos of the key value.
	 * @param suspStmtAst
	 * @return
	 */
	protected Map<ITree, Integer> readAllSuspiciousPredicateExpressions(ITree suspStmtAst) {
		Map<ITree, Integer> predicateExps = new HashMap<>();
		ITree suspExpTree;
		if (Checker.isDoStatement(suspStmtAst.getType())) {
			List<ITree> children = suspStmtAst.getChildren();
			suspExpTree = children.get(children.size() - 1);
		} else if (Checker.withBlockStatement(suspStmtAst.getType())) {
			suspExpTree = suspStmtAst.getChild(0);
		} else {
			suspExpTree = suspStmtAst;
		}
		int suspExpTreeType = suspExpTree.getType();
		
		if (!Checker.isInfixExpression(suspExpTreeType)) {
			if (Checker.isStatement(suspExpTreeType)) {
				predicateExps.putAll(readConditionalExpressions(suspExpTree));
			} else {
				predicateExps.put(suspExpTree, 0);
			}
			return predicateExps;
		}
		predicateExps.put(suspExpTree, 0);
		
		List<ITree> subExps = suspExpTree.getChildren();
		predicateExps.putAll(readSubPredicateExpressions(subExps));
		return predicateExps;
	}
	
	private Map<ITree, Integer> readConditionalExpressions(ITree suspExpTree) {
		Map<ITree, Integer> predicateExps = new HashMap<>();
		List<ITree> children = suspExpTree.getChildren();
		for (ITree child : children) {
			int childType = child.getType();
			if (Checker.isComplexExpression(childType)) {
				if (Checker.isConditionalExpression(child.getType())) {
					predicateExps.put(child, child.getPos());
				}
				predicateExps.putAll(readConditionalExpressions(child));
			} else if (Checker.isSimpleName(childType) && child.getLabel().startsWith("MethodName:")) {
				predicateExps.putAll(readConditionalExpressions(child));
			} else if (Checker.isStatement(childType)) break;
		}
		return predicateExps;
	}

	private Map<ITree, Integer> readSubPredicateExpressions(List<ITree> subExps) {
		Map<ITree, Integer> predicateExps = new HashMap<>();
		ITree operator = subExps.get(1);
		String op = operator.getLabel();
		if ("||".equals(op) || "&&".equals(op)) {
			ITree leftExp = subExps.get(0);
			ITree rightExp = subExps.get(2);
			predicateExps.put(leftExp, rightExp.getPos());
			if (Checker.isInfixExpression(leftExp.getType())) {
				predicateExps.putAll(readSubPredicateExpressions(leftExp.getChildren()));
			}
			predicateExps.put(rightExp, operator.getPos());
			if (Checker.isInfixExpression(rightExp.getType())) {
				predicateExps.putAll(readSubPredicateExpressions(rightExp.getChildren()));
			}
			for (int index = 3, size = subExps.size(); index < size; index ++) {
				ITree subExp = subExps.get(index);
				ITree prevExp = subExps.get(index - 1);
				int pos = prevExp.getPos() + prevExp.getLength();
				predicateExps.put(subExp, pos);
				if (Checker.isInfixExpression(subExp.getType())) {
					predicateExps.putAll(readSubPredicateExpressions(subExp.getChildren()));
				}
			}
		}
		return predicateExps;
	}

}
