package edu.lu.uni.serval.tbar.fixpatterns;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.lu.uni.serval.jdt.tree.ITree;
import edu.lu.uni.serval.tbar.context.ContextReader;
import edu.lu.uni.serval.tbar.fixtemplate.FixTemplate;
import edu.lu.uni.serval.tbar.utils.Checker;

public class ReturnStatementMutator extends FixTemplate {
	
	/**
	 * SOFix      ReturnAdder
	 * SketchFix  ReturnStatementTransform
	 * ELIXIR     T2_ReturnStmt
	 */
	private String returnType;
	
	public ReturnStatementMutator(String returnType) {
		this.returnType = returnType;
	}

	@Override
	public void generatePatches() {
		ITree suspCodeTree = getSuspiciousCodeTree();
		if (Checker.isReturnStatement(suspCodeTree.getType())) {
			if (suspCodeTree.getChildren().isEmpty()) return;
			/*
			 * How to select the another compatible expression?
			 * We only consider the variables in this implementation.
			 */
			ContextReader.readAllVariablesAndFields(suspCodeTree, allVarNamesMap, varTypesMap, allVarNamesList, sourceCodePath, dic);
			ITree returnExp = suspCodeTree.getChild(0);
//			if (Checker.isBooleanLiteral(returnExp.getType())) {
//				if ("true".equals(returnExp.getLabel())) this.generatePatch("return false;\n");
//				else this.generatePatch("return true;\n");
//			}
			List<String> vars = readVariables(returnType);
			if (vars == null) return;
//			String returnExpStr = returnExp.getLabel();
			int returnExpType = returnExp.getType();
			if (!Checker.isSimpleName(returnExpType) && !Checker.isStringLiteral(returnExpType)
					&& !Checker.isNumberLiteral(returnExpType) && !Checker.isBooleanLiteral(returnExpType)
					&& !Checker.isMethodInvocation(returnExpType) && !Checker.isConditionalExpression(returnExpType)) {
				for (String var : vars) {
//					if (var.equals(returnExpStr)) continue;
					generatePatch("return " + var + ";\n");
				}
			}
			
			// Parse the local file to get any possible expression.
			// we only focus on boolean return type expressions.
			if ("boolean".equalsIgnoreCase(returnType)) {
				List<ITree> donorExpList = searchForExpressions(suspCodeTree);
				Map<String, List<String>> buggyVariablesMap = new HashMap<>();
				buggyVariablesMap.putAll(allVarNamesMap);
				List<String> fixedCodeStrList = new ArrayList<>();
				for (ITree donorExp : donorExpList) {
					List<ITree> donorVarTrees = new ArrayList<>();
					List<String> donorVarNames = new ArrayList<>();
					ContextReader.identifySuspiciousVariables(donorExp, donorVarTrees, donorVarNames);
					int donorStartPos = donorExp.getPos();
					int dornorEndPos = donorStartPos + donorExp.getLength();
					String donorCode = suspJavaFileCode.substring(donorStartPos, dornorEndPos);
					
					if (donorVarTrees.isEmpty() ) {
						if (fixedCodeStrList.contains(donorCode)) {
							generatePatch("return " + donorCode + ";\n");
							fixedCodeStrList.add(donorCode);
						}
					} else {
						allVarNamesList.clear();
						varTypesMap.clear();
						allVarNamesMap.clear();
						ContextReader.readAllVariablesAndFields(donorExp, allVarNamesMap, varTypesMap, allVarNamesList, sourceCodePath, dic);
						boolean isFailed = false;
						
						List<Map<String, String>> maps = new ArrayList<>();
						for (String donorVarName : donorVarNames) {
							if (donorVarName.startsWith("Name:")) donorVarName = donorVarName.substring(5);
							String donorVarType = varTypesMap.get(donorVarName);
							if (donorVarType == null) {
								isFailed = true;
								break;
							}
							List<String> exchangableVars = buggyVariablesMap.get(donorVarType);
							if (exchangableVars == null) {
								isFailed = true;
								break;
							}
							
							// n * m * l: a set of variable groups used to synthesis patches by replacing the original variables in 'donorCode'.
							maps = ContextReader.arrangeVariableGroups(maps, donorVarName, exchangableVars);
						}
						if (isFailed) continue;
						
						// Generate patches.
						for (Map<String, String> map : maps) {
							int startPos2 = 0;
							StringBuilder fixedCodeStr1 = new StringBuilder();
							Map<String, String> exchangedVarMap = new HashMap<>();
							for (ITree donorVarTree : donorVarTrees) {
								int pos = donorVarTree.getPos() - donorStartPos;
								fixedCodeStr1.append(donorCode.substring(startPos2, pos));
								startPos2 = pos + donorVarTree.getLength();
								
								String donorVarName = donorVarTree.getLabel();
								if (donorVarName.startsWith("Name:")) donorVarName = donorVarName.substring(5);
								String exchangedVar = map.get(donorVarName);
								String prevDonorVarName = exchangedVarMap.get(exchangedVar);
								if (!donorVarName.equals(prevDonorVarName)) { // different variables will not be replaced by the same other variables.
									isFailed = true;
									break;
								}
								fixedCodeStr1.append(exchangedVar);
							}
							
							if (isFailed) {
								isFailed = false;
								continue;
							}
							
							fixedCodeStr1.append(donorCode.substring(startPos2));
							if (!fixedCodeStrList.contains(fixedCodeStr1.toString())) {
								generatePatch("return " + fixedCodeStr1.toString() + ";\n");
								fixedCodeStrList.add(fixedCodeStr1.toString());
							}
						}
					}
				}
				clear();
			}
		}
	}
	
	private List<ITree> searchForExpressions(ITree suspCodeTree) {
		List<ITree> expList = new ArrayList<>();
		ITree classTree = suspCodeTree.getParent();
		while (true) {
			if (classTree == null) return expList;
			if (Checker.isTypeDeclaration(classTree.getType())) break;
			classTree = classTree.getParent();
		}
		List<ITree> children = classTree.getChildren();
		for (ITree child : children) {
			searchForExpressions(child, expList);
		}
		return expList;
	}
	
	private List<String> condExpStrList = new ArrayList<>();
	private void searchForExpressions(ITree tree, List<ITree> expList) {
		int treeType = tree.getType();
		ITree exp = null;
		if (Checker.isIfStatement(treeType) || Checker.isWhileStatement(treeType)) {
			exp = tree.getChild(0);
		} else if (Checker.isDoStatement(treeType)) {
			exp = tree.getChildren().get(tree.getChildren().size() - 1);
		} else if (Checker.isReturnStatement(treeType)) {
			String methodReturnType = ContextReader.readMethodReturnType(tree);
			if ("boolean".equalsIgnoreCase(methodReturnType)) {
				exp = tree.getChild(0);
			}
		} else if (Checker.isInfixExpression(treeType)) {
			String op = tree.getChild(1).getLabel();
			if ("&&".equals(op) || "||".equals(op)) {
				exp = tree.getChild(0);
				String expLabel = exp.getLabel();
				if (!condExpStrList.contains(expLabel)) {
					expList.add(exp);
					condExpStrList.add(expLabel);
				}
				exp = tree.getChild(2);
			}
		} else if (Checker.isConditionalExpression(treeType)) {
			exp = tree.getChild(0);
		} else if (Checker.isPrefixExpression(treeType)) {
			String op = tree.getChild(0).getLabel();
			if ("!".equals(op)) {
				exp = tree.getChild(1);
			}
		}
		if (exp != null) {
			String expLabel = exp.getLabel();
			if (!condExpStrList.contains(expLabel)) {
				expList.add(exp);
				condExpStrList.add(expLabel);
			}
		}
		List<ITree> children = tree.getChildren();
		for (ITree child : children) {
			searchForExpressions(child, expList);
		}
	}

	private List<String> readVariables(String returnType) {
		List<String> vars;
		if ("boolean".equalsIgnoreCase(returnType)) {
			vars = allVarNamesMap.get("boolean");
			List<String> vars2 = allVarNamesMap.get("Boolean");
			if (vars == null) vars = vars2;
			else if (vars2 != null) vars.addAll(vars2);
		} else if ("char".equals(returnType) | "Character".equals(returnType)) {
			vars = allVarNamesMap.get("char");
			List<String> vars2 = allVarNamesMap.get("Character");
			if (vars == null) vars = vars2;
			else if (vars2 != null) vars.addAll(vars2);
		} else if ("byte".equalsIgnoreCase(returnType)) {
			vars = allVarNamesMap.get("byte");
			List<String> vars2 = allVarNamesMap.get("Byte");
			if (vars == null) vars = vars2;
			else if (vars2 != null) vars.addAll(vars2);
		} else if ("short".equalsIgnoreCase(returnType)) {
			vars = allVarNamesMap.get("short");
			List<String> vars2 = allVarNamesMap.get("Short");
			if (vars == null) vars = vars2;
			else if (vars2 != null) vars.addAll(vars2);
		} else if ("int".equals(returnType) || "Integer".equals(returnType)) {
			vars = allVarNamesMap.get("int");
			List<String> vars2 = allVarNamesMap.get("Integer");
			if (vars == null) vars = vars2;
			else if (vars2 != null) vars.addAll(vars2);
		} else if ("long".equalsIgnoreCase(returnType)) {
			vars = allVarNamesMap.get("long");
			List<String> vars2 = allVarNamesMap.get("Long");
			if (vars == null) vars = vars2;
			else if (vars2 != null) vars.addAll(vars2);
		} else if ("float".equalsIgnoreCase(returnType)) {
			vars = allVarNamesMap.get("float");
			List<String> vars2 = allVarNamesMap.get("Float");
			if (vars == null) vars = vars2;
			else if (vars2 != null) vars.addAll(vars2);
		} else if ("double".equalsIgnoreCase(returnType)) {
			vars = allVarNamesMap.get("double");
			List<String> vars2 = allVarNamesMap.get("Double");
			if (vars == null) vars = vars2;
			else if (vars2 != null) vars.addAll(vars2);
		} else {
			vars = allVarNamesMap.get(returnType);
		}
		return vars;
	}
}
