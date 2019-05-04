package edu.lu.uni.serval.tbar.fixpatterns;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.lu.uni.serval.entity.Pair;
import edu.lu.uni.serval.jdt.tree.ITree;
import edu.lu.uni.serval.tbar.context.ContextReader;
import edu.lu.uni.serval.tbar.context.Method;
import edu.lu.uni.serval.tbar.fixtemplate.FixTemplate;
import edu.lu.uni.serval.tbar.utils.Checker;

/**
 * Insert a statement.
 * 
 * @author kui.liu
 *
 */
public class StatementInserter extends FixTemplate {

	@Override
	public void generatePatches() {
		ITree suspCodeTree = this.getSuspiciousCodeTree();
		if (Checker.isBreakStatement(suspCodeTree.getType()) || Checker.isContinueStatement(suspCodeTree.getType())) return;
		
		Pair<String, ITree> equalsMethod = findEqualsMethodDeclaration(suspCodeTree);
		if (equalsMethod != null) {
			/*
			 * public boolean equals(Object o) {
			 * +  if (! (o instanceof T)) return false;
			 *    ......
			 * }
			 * 
			 */
//			ITree method = equalsMethod.getSecond();
//			String parameterName = equalsMethod.getFirst();
//			ITree classTree = method.getParent();
//			if (classTree == null) return;
//			if (!Checker.isTypeDeclaration(classTree.getType())) return;
//			
//			String classLabel = classTree.getLabel();
//			String className = classLabel.substring(classLabel.indexOf("ClassName:") + 10);
//			className = className.substring(0, className.indexOf(", "));
//			
//			String fixedCodeStr1 = "if (!(" + parameterName + " instanceof " + className + ")) return false;\n";
//			this.generatePatch(this.suspCodeEndPos, fixedCodeStr1);
		}
		
		
		// Insert a return statement.
		if (!Checker.isReturnStatement(suspCodeTree.getType())) {
			// we infer that inserting the return statement at the end of the block which contains the buggy statement.
			ITree parentTree = suspCodeTree.getParent();
			int parentNodeType = parentTree.getType();
			List<ITree> peerStmts = suspCodeTree.getParent().getChildren();
			ITree lastPeerStmt = peerStmts.get(peerStmts.size() - 1);
			
			if (Checker.isSwitchStatement(parentNodeType)) {
//				if (!Checker.isSwitchCase(suspCodeTree.getType())) {
//				} else {
//				}
				int toIndex = -1;
				int index = parentTree.getChildPosition(suspCodeTree);
				for (int i = index + 1; i < peerStmts.size(); i ++) {
					if (Checker.isSwitchCase(peerStmts.get(i).getType())) {
						toIndex = i - 1;
						break;
					}
				}
				
				if (toIndex == -1) toIndex = index;
				lastPeerStmt = peerStmts.get(toIndex);
				if (!Checker.isBreakStatement(lastPeerStmt.getType()) && !Checker.isContinueStatement(lastPeerStmt.getType()) && !Checker.isReturnStatement(lastPeerStmt.getType())) {
					if (Checker.isBlock(lastPeerStmt.getType())) {
						peerStmts = lastPeerStmt.getChildren();
						lastPeerStmt = peerStmts.get(peerStmts.size() - 1);
					}
					String returnType = ContextReader.readMethodReturnType(suspCodeTree);
					int pos = lastPeerStmt.getPos() + lastPeerStmt.getLength();
					String code = suspJavaFileCode.substring(suspCodeStartPos, pos);
					
					String fixedCodeStr;
					if ("char".equals(returnType) || "short".equals(returnType) ||
							"byte".equals(returnType) || "int".equals(returnType) || 
							"long".equals(returnType) || "float".equals(returnType)|| "double".equals(returnType)) {
						fixedCodeStr = code + "\n\t return 0;\n";
					} else if ("void".equals(returnType)) {
						fixedCodeStr = code + "\n\t return;\n";
					} else if ("boolean".equalsIgnoreCase(returnType)) {
						fixedCodeStr = code + "\n\t return false;\n";
						generatePatch(pos, pos, fixedCodeStr, null);
						fixedCodeStr = code + "\n\t return true;\n";
					} else {
						fixedCodeStr = code + "\n\t return null;\n";
					}
					generatePatch(pos, pos, fixedCodeStr, null);
					
					if ("void".equals(returnType)) {
						generatePatch(suspCodeEndPos, "return;\n");
					} else {
						ContextReader.readAllVariablesAndFields(suspCodeTree, allVarNamesMap, varTypesMap, allVarNamesList, sourceCodePath, dic);
						List<String> vars = readVariables(returnType);
						if (vars != null) {
							for (String var : vars) {
//								generatePatch(suspCodeEndPos, "return " + var + ";\n");
								generatePatch(pos, pos, "return " + var + ";\n", null);
							}
						}
					}
				}
//			} else if (Checker.isTryStatement(parentNodeType)) {
//				if (Checker.isCatchClause(suspCodeTree.getType()) || 
//						(Checker.isBlock(suspCodeTree.getType()) && "FinallyBody".equals(suspCodeTree.getLabel()))) return;
//				int toIndex = -1;
//				int index = parentTree.getChildPosition(suspCodeTree);
//				for (int i = index + 1; i < peerStmts.size(); i ++) {
//					ITree peerStmt = peerStmts.get(i);
//					if (Checker.isCatchClause(peerStmt.getType()) || 
//							(Checker.isBlock(peerStmt.getType()) && "FinallyBody".equals(peerStmt.getLabel()))) {
//						toIndex = i - 1;
//						break;
//					}
//				}
//				
//				if (toIndex == -1) toIndex = index + 1;
//				lastPeerStmt = peerStmts.get(toIndex);
//				return;
			} 
		}
		
		List<String> varStrList = new ArrayList<>();
		ContextReader.identifySuspiciousVariables(suspCodeTree, new ArrayList<ITree>(), varStrList);
		ContextReader.readAllVariablesAndFields(suspCodeTree, allVarNamesMap, varTypesMap, allVarNamesList, sourceCodePath, dic);
		if (varStrList.isEmpty()) return;
		
		/* 
		 * SketchFix
		 * FP2: M_if
		 * Insert a new if statement.
		 */
//		for (String var : varStrList) {
//			String varType = varTypesMap.get(var);
//			if (varType == null) continue;
//			List<String> varList = allVarNamesMap.get(varType);
//			varList.remove(var);
//			String[] operators = selectOperators(varType);
//			int suspCodeEndPos = ContextReader.identifyRelatedStatements(suspCodeTree, var, this.suspCodeEndPos);
//			for (String v_ : varList) {
//				for (String op : operators) {
//					generatePatch(suspCodeStartPos, suspCodeEndPos, "if (" + var + op + v_ + ") {\n\t", "\t}\n");
//				}
//			}
//		}
		
		/*
		 * This is a new schema in ELIXIR. It synthesizes MIs, 
		 * and inserts them as a part of an expression or as a complete statement.
		 */
		// TODO Insert a method invocation as a part of an expression, how?
		// Insert a method invocation as a complete statement.
		ITree parent = suspCodeTree.getParent();
		while (true) {
			if (Checker.isTypeDeclaration(parent.getType())) break;
			parent = parent.getParent();
			if (parent == null) return;
		}
		String currentClassPath = ContextReader.readClassNameAndPath(parent);
		
		List<String> types = new ArrayList<>();
		Map<String, List<String>> varMaps = new HashMap<>();
		for (int index = varStrList.size() - 1; index >= 0; index --) {
			String var = varStrList.get(index);
			String varType = this.varTypesMap.get(var);
			if (varType == null) varStrList.remove(index);
			else {
				types.add(0, varType);
				List<String> vars = varMaps.get(varType);
				if (vars == null) {
					vars = new ArrayList<>();
					varMaps.put(varType, vars);
				}
				vars.add(var);
			}
		}
		
		List<Method> methods = dic.findAllAvailableMethodsOfThisClass(currentClassPath, true, true);
		for (Method method : methods) {
			if (!method.getReturnType().equals("void")) continue; // 
			List<String> parameterTypes = method.getParameterTypes();
			if (parameterTypes.isEmpty()) {
				String fixedCodeStr1 = method.getMethodName() + "();";
				this.generatePatch(fixedCodeStr1 + "\n\t" + this.getSuspiciousCodeStr());
				this.generatePatch(this.getSuspiciousCodeStr() + "\n\t" + fixedCodeStr1);
			} else {
				if (types.isEmpty()) continue;
				if (parameterTypes.size() > 1) continue;
				
				boolean isMatched = true;
				List<Map<String, String>> maps = new ArrayList<>();
				for (String parameterType : parameterTypes) {
					List<String> vars = varMaps.get(parameterType);
					if (vars == null) {
						isMatched = false;
						break;
					}
					
					// n * m * l: a set of variable groups used to synthesis patches with this method.
					maps = ContextReader.arrangeVariableGroups(maps, parameterType, vars);
				}
				if (!isMatched) continue;
				
				// Generate patches.
				for (Map<String, String> map : maps) {
					String fixedCodeStr1 = method.getMethodName() + "(";
					Map<String, String> usedVars = new HashMap<>();
					boolean isFailed = false;
					for (String parameterType : parameterTypes) {
						String var = map.get(parameterType);
						String prevParaType = usedVars.get(var);
						if (parameterType.equals(prevParaType)) {
							isFailed = true;
							break;
						}
						if (!fixedCodeStr1.endsWith("(")) {
							fixedCodeStr1 += ",";
						}
						fixedCodeStr1 += var;
					}

					if (isFailed) continue;
					fixedCodeStr1 += ");";
					this.generatePatch(fixedCodeStr1);
					this.generatePatch(fixedCodeStr1 + "\n\t" + this.getSuspiciousCodeStr());
					this.generatePatch(this.getSuspiciousCodeStr() + "\n\t" + fixedCodeStr1);
				}
			}
		}
		clear();
	}

	private Pair<String, ITree> findEqualsMethodDeclaration(ITree suspCodeTree) {
		ITree parent = suspCodeTree.getParent();
		while (true) {
			if (parent == null) return null;
			if (Checker.isMethodDeclaration(parent.getType())) {
				break;
			} else if (Checker.isTypeDeclaration(parent.getType())) {
				return null;
			}
			parent = parent.getParent();
		}
		String methodLabel = parent.getLabel();
		methodLabel = methodLabel.substring(methodLabel.indexOf("@@") + 2);
		int index = methodLabel.indexOf("MethodName:");
		String returnType = methodLabel.substring(0, index - 2);
		if (!"boolean".equals(returnType)) return null;
		methodLabel = methodLabel.substring(index + 11);
		if (!methodLabel.startsWith("equals, ")) return null;
		methodLabel = methodLabel.substring(methodLabel.indexOf("@@Argus:") + 8);
		String[] parameters = methodLabel.split("\\+");
		if (parameters.length != 2 || !"Object".equals(parameters[0])) return null;
		Pair<String, ITree> equalsMethod = new Pair<>(parameters[1], parent);
		return equalsMethod;
	}
	
	@SuppressWarnings("unused")
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
