package edu.lu.uni.serval.tbar.fixpatterns;

import java.util.ArrayList;
import java.util.List;

import edu.lu.uni.serval.entity.Pair;
import edu.lu.uni.serval.jdt.tree.ITree;
import edu.lu.uni.serval.tbar.context.ContextReader;
import edu.lu.uni.serval.tbar.fixtemplate.FixTemplate;
import edu.lu.uni.serval.tbar.utils.Checker;
import edu.lu.uni.serval.tbar.utils.ListSorter;

/**
 * Fix patterns of checking null pointer.
 * 
 * @author kui.liu
 *
 */
public class NullPointerChecker extends FixTemplate {
	
	/*
	 * Null Pointer Checker: 
	 * 
	 * NPEFix, 
	 * ELIXIR-T3_NullPointer,
	 * FindBugs- NP_NULL_ON_SOME_PATH, NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE, NP_NULL_ON_SOME_PATH_EXCEPTION.
	 * PAR	NullChecker
	 * FixMiner IfNullChecker
	 * 
	 * Fuzzy fix patterns:
	 * SketchFix	If-ConditionTransform
	 * SimFix	InsIfStmt
	 * SOFix	IfChecker
	 * 
	 */

	@Override
	public void generatePatches() {
		ITree suspCodeTree = this.getSuspiciousCodeTree();
		List<String> vars = new ArrayList<>();
		List<SuspNullExpStr> suspNullExpStrs = new ArrayList<>();
		identifySuspiciousVariables(suspCodeTree, vars, suspNullExpStrs);
		ContextReader.readAllVariablesAndFields(suspCodeTree, allVarNamesMap, varTypesMap, allVarNamesList, this.sourceCodePath, dic);
		String returnType = readReturnType(suspCodeTree);
		if (!vars.isEmpty()) {
			// Only check whether a variable is null or not.
			List<String> nullCheckedVars = identifyNullCheckedVariables(suspCodeTree);
			if (nullCheckedVars != null) {
				vars.removeAll(nullCheckedVars);
				for (String nullCheckedVar : nullCheckedVars) {
					for (SuspNullExpStr suspNullExpStr : suspNullExpStrs) {
						if (suspNullExpStr.expStr.equals(nullCheckedVar)) {
							suspNullExpStrs.remove(suspNullExpStr);
							break;
						}
					}
				}
			}
			
			if (!vars.isEmpty()) {
				for (String var : vars) {
					String varType = varTypesMap.get(var);
					if (varType == null) {
						varType = varTypesMap.get("this." + var);
					}
					if (varType != null) {
						if ("int".equals(varType) || "long".equals(varType) || "short".equals(varType) || "byte".equals(varType)
								|| "float".equals(varType) || "double".equals(varType) || "char".equals(varType) || "boolean".equals(varType)) continue;
					}
					if (this.getSuspiciousCodeStr().replace(" ", "").contains(var + "!=null") 
							|| this.getSuspiciousCodeStr().replace(" ", "").contains(var + "==null")) continue;
					// FindBugs NP_NULL_ON_SOME_PATH Patch 1. // NPEFix : S3
					String fixedCodeStr1 = "if (" + var + " != null) {\n\t";
					String fixedCodeStr2 = "\n\t}\n";
					int suspCodeEndPos = this.suspCodeEndPos;
					suspCodeEndPos = ContextReader.identifyRelatedStatements(suspCodeTree, var, this.suspCodeEndPos);
					this.generatePatch(suspCodeStartPos, suspCodeEndPos, fixedCodeStr1, fixedCodeStr2);
					
					if (returnType != null) {
						// FindBugs NP_NULL_ON_SOME_PATH Patch 3.
						fixedCodeStr1 = "if (" + var + " == null) {\n\t    throw new IllegalArgumentException(\"Null '" + var + "' argument.\");\n\t}\n";
						this.generatePatch(suspCodeStartPos, fixedCodeStr1);
						
						// FindBugs NP_NULL_ON_SOME_PATH Patch 2.
						fixedCodeStr1 = "if (" + var + " == null) {\n\t    return";
						if ("void".equals(returnType)) {
							// NPEFix S4a
						} else if ("float".equals(returnType) || "double".equals(returnType)) {
							fixedCodeStr1 += " 0.0";
						} else if ("int".equals(returnType) || "long".equals(returnType)) {
							fixedCodeStr1 += " 0";
						} else if ("boolean".equalsIgnoreCase(returnType)) {
							fixedCodeStr1 += " false;\n\t}\n";
							this.generatePatch(suspCodeStartPos, fixedCodeStr1);
							fixedCodeStr1 = "if (" + var + " == null) {\n\t    return true";
						} else {
							fixedCodeStr1 += " null"; // NPEFix S4a
						}
						fixedCodeStr1 += ";\n\t}\n";
						this.generatePatch(suspCodeStartPos, fixedCodeStr1);
					}
					
					// NPEFix S1b
					List<String> varsWithSameType = allVarNamesMap.get(varType);
					if (varsWithSameType != null) {
						for (String v : varsWithSameType) {
							// NPEFix : S1
							if (v.equals(var) || v.equals("this." + var)) continue;
							if (vars.contains(v)) continue;
							fixedCodeStr1 = "if (" + var + " == null) " + var + " = " + v + ";\n ";
							this.generatePatch(suspCodeStartPos, fixedCodeStr1);
						}
					}
					
					// NPEFix : S2
					fixedCodeStr1 = "if (" + var + " == null) " + var + " = new " + varType + "();\n ";
					this.generatePatch(suspCodeStartPos, fixedCodeStr1);
					
					if (returnType == null) continue;
					// FNPEFix P4
					if (!"void".equals(returnType) && !"boolean".equalsIgnoreCase(returnType)) {
						if (!"int".equals(returnType) && !"Integer".equals(returnType) && !"long".equals(returnType) && !"Long".equals(returnType)
								&& !"double".equals(returnType) && !"Double".equals(returnType) && !"float".equals(returnType) && !"Float".equals(returnType)
								&& !"char".equals(returnType) && !"Character".equals(returnType) && !"short".equals(returnType) && !"Short".equals(returnType)
								&& !"byte".equals(returnType) && !"Byte".equals(returnType)) {
							// NPEFix S4b
							fixedCodeStr1 = "if (" + var + " == null) return new "+ returnType + "();\n\t ";
							this.generatePatch(suspCodeStartPos, fixedCodeStr1);
						}
						// NPEFix S4c
						varsWithSameType = allVarNamesMap.get(returnType);
						if (varsWithSameType == null) continue;
						for (String v : varsWithSameType) {
							if (v.equals(var) || v.equals("this." + var)) continue;
							if (vars.contains(v)) continue;
							fixedCodeStr1 = "if (" + var + " == null) return " + v + ";\n\t ";
							this.generatePatch(suspCodeStartPos, fixedCodeStr1);
						}
					}
				}
			}
		}
		
		if (!suspNullExpStrs.isEmpty()) {
			ListSorter<SuspNullExpStr> sorter = new ListSorter<SuspNullExpStr>(suspNullExpStrs);
			suspNullExpStrs = sorter.sortAscending();
			
			Pair<String, Boolean> parentContext = identifyParentContext(suspCodeTree);
			if (parentContext != null && returnType != null) {
				/*
				 * Fix pattern for NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE violations
				 * -   …exp1.method()…
				 * +   T v = exp1.method();
				 * +   if (v == null) { return null; }
				 * +   …v…
				 */
				boolean isLoopStmt = parentContext.getSecond().booleanValue();
				
				String fixedCodeStr1 = "";
				String expStr = "";
				
				for (int index = 0, size = suspNullExpStrs.size(); index < size; index ++) {
					SuspNullExpStr buggyExp = suspNullExpStrs.get(index);
					expStr = buggyExp.expStr;
					
					String varType = varTypesMap.get(expStr);
					if (varType == null) {
						varType = varTypesMap.get("this." + expStr);
					}
					if (varType != null) {
						if ("int".equals(varType) || "long".equals(varType) || "short".equals(varType) || "byte".equals(varType)
								|| "float".equals(varType) || "double".equals(varType) || "char".equals(varType) || "boolean".equals(varType)) continue;
					}
					
					fixedCodeStr1 += "if (" + expStr + " == null)";
					fixedCodeStr1 += isLoopStmt ? " continue;\n\t" : " return null;\n\t";
				}
				if (!vars.contains(expStr) || isLoopStmt) {
					this.generatePatch(suspCodeEndPos, fixedCodeStr1);
				}
			}
			
			// PAR, ELIXIR
			SuspNullExpStr snes = suspNullExpStrs.get(0);
			String expStr = snes.expStr;
			int suspCodeEndPos = ContextReader.identifyRelatedStatements(suspCodeTree, expStr, this.suspCodeEndPos);
			String varType = this.varTypesMap.get(expStr);
			
			int a = 0;
			StringBuilder fixedCodeStr1 = new StringBuilder("(");
			if (!"int".equals(varType) && !"long".equals(varType) 
					&& !"double".equals(varType) && !"float".equals(varType)
					&& !"char".equals(varType) && !"short".equals(varType) && !"byte".equals(varType)) {
				fixedCodeStr1.append(expStr).append(" != null");
				a ++;
			}
			for (int index = 1, size = suspNullExpStrs.size(); index < size; index ++) {
				snes = suspNullExpStrs.get(index);
				expStr = snes.expStr;
				varType = this.varTypesMap.get(expStr);
				if (!"int".equals(varType) && !"long".equals(varType) 
						&& !"double".equals(varType) && !"float".equals(varType)
						&& !"char".equals(varType) && !"short".equals(varType) && !"byte".equals(varType)) {
					if (fixedCodeStr1.length() == 1) fixedCodeStr1.append(expStr).append(" != null");
					else fixedCodeStr1.append(" && ").append(expStr).append(" != null");
					a ++;
					if (!vars.contains(expStr)) {
						String nullCheckStr = "if (" + expStr + " != null) {";
						this.generatePatch(suspCodeStartPos, suspCodeEndPos, nullCheckStr, "\n\t}\n");
					}
				}
			}
			if (a > 1) {
				fixedCodeStr1.append(")");
				if (Checker.isReturnStatement(suspCodeTree.getType())) {
					// PAR Patch 2.
					if (returnType != null && !returnType.isEmpty()) {
						fixedCodeStr1 = fixedCodeStr1.append(") return");
						if ("void".equals(returnType)) {
							fixedCodeStr1.append(";\n");
						} else if ("float".equals(returnType) || "double".equals(returnType)) {
							fixedCodeStr1.append(" 0.0;\n");
						} else if ("int".equals(returnType) || "long".equals(returnType)) {
							fixedCodeStr1.append(" 0;\n");
						} else if ("boolean".equalsIgnoreCase(returnType)) {
							fixedCodeStr1.append(" false;\n");
							this.generatePatch(suspCodeStartPos, "if (!" + fixedCodeStr1.toString());
							fixedCodeStr1.replace(fixedCodeStr1.length() - 9, fixedCodeStr1.length(), " true;\n");
						} else {
							fixedCodeStr1.append(" null;\n\t");
						}
						this.generatePatch(suspCodeStartPos, "if (!" + fixedCodeStr1.toString());
					}
				} else {
					// PAR Patch 1.
					fixedCodeStr1.append(" {\n\t");
					String fixedCodeStr2 = "\n\t}\n";
					this.generatePatch(suspCodeStartPos, suspCodeEndPos, "if " + fixedCodeStr1.toString(), fixedCodeStr2);
				}
			}
		}
		
		/*
		 * Fix pattern for NP_NULL_ON_SOME_PATH_EXCEPTION violations.
		 * +  if (input/outputSteam != null) {
		 *        input/outputSteam.close();
		 * +  }
		 */
		if (Checker.isExpressionStatement(suspCodeTree.getType())) {
			if (suspCodeTree.getChildren().size() == 1) {
				ITree exp = suspCodeTree.getChild(0);
				if (Checker.isMethodInvocation(exp.getType())) {
					String label = exp.getLabel();
					if (label.endsWith(".close()")) {
						String varExp = label.substring(0, label.length() - 8);
						ITree parentTree = suspCodeTree.getParent();
						if (Checker.isIfStatement(parentTree.getType())) {
							String parentLabel = parentTree.getLabel().replaceAll(" ", "");
							if (parentLabel.contains(varExp.replace(" ", "") + "!=null")) varExp = null;
						}
						if (varExp != null) {
							String fixedCodeStr1 = "if (" + varExp + " != null) \n\t";
							this.generatePatch(this.suspCodeEndPos, fixedCodeStr1);	
						}
					}
				}
			}
		}
		
		// FixMiner ifNullChecker, IfStatement, ReturnStatement.
		int type = suspCodeTree.getType();
		if (!Checker.isIfStatement(type) && !Checker.isReturnStatement(type)) return;
		if (Checker.isReturnStatement(type) && !isBooleanReturnType(suspCodeTree)) return;
		ITree suspExpTree = suspCodeTree.getChild(0);
		if (Checker.isMethodInvocation(suspExpTree.getType())) {
			List<ITree> variables = identifyVariables(suspExpTree);
			int startPos = suspExpTree.getPos();
			String codePart1 = this.getSubSuspiciouCodeStr(suspCodeStartPos, startPos);
			String codePart2 = this.getSubSuspiciouCodeStr(startPos, suspCodeEndPos);
			for (ITree var : variables) {
				String label = var.getLabel();
				if (label.startsWith("Name:")) label = label.substring(5);
				String fixedCodeStr1 = codePart1 + label + " != null && " + codePart2;
				this.generatePatch(fixedCodeStr1);
				fixedCodeStr1 = codePart1 + label + " != null || " + codePart2;
				this.generatePatch(fixedCodeStr1);
			}
		}
		clear();
	}
	
	private boolean isBooleanReturnType(ITree suspCodeTree) {
		ITree parentTree = suspCodeTree.getParent();
		while (true) {
			if (Checker.isMethodDeclaration(parentTree.getType()))
				break;
			if (Checker.isTypeDeclaration(parentTree.getType())) {
				parentTree = null;
				break;
			}
			parentTree = parentTree.getParent();
		}
		
		if (parentTree == null) return false;
		
		String label = parentTree.getLabel();
		int indexOfMethodName = label.indexOf("MethodName:");
		
		// Read return type.
		String returnType = label.substring(label.indexOf("@@") + 2, indexOfMethodName - 2);
		int index = returnType.indexOf("@@tp:");
		if (index > 0) returnType = returnType.substring(0, index - 2);
		
		returnType = ContextReader.readType(returnType);

		return returnType.equalsIgnoreCase("boolean");
	}
	
	private List<ITree> identifyVariables(ITree codeAst) {
		List<ITree> variables = new ArrayList<>();
		List<ITree> children = codeAst.getChildren();
		for (ITree child : children) {
			int type = child.getType();
			if (Checker.isComplexExpression(type)) 
				variables.addAll(identifyVariables(child));
			else if (Checker.isSimpleName(type)) {
				String label = child.getLabel();
				if (label.startsWith("MethodName:")) continue;
				variables.add(child);
			} else if (Checker.isStatement(type) || Checker.isMethodDeclaration(type))
				break;
		}
		return variables;
	}
	
	private void identifySuspiciousVariables(ITree suspCodeAst, List<String> allSuspVariables, List<SuspNullExpStr> suspNullExpStrs) {
		List<ITree> children = suspCodeAst.getChildren();
		for (ITree child : children) {
			int childType = child.getType();
			if (Checker.isSimpleName(childType)) {
				int parentType = suspCodeAst.getType();
				if ((Checker.isAssignment(parentType) || Checker.isVariableDeclarationFragment(parentType))
						&& suspCodeAst.getChildPosition(child) == 0) {
					continue;
				}
				if ((Checker.isQualifiedName(parentType) || Checker.isFieldAccess(parentType) || Checker.isSuperFieldAccess(parentType)) &&
						suspCodeAst.getChildPosition(child) == children.size() - 1) {
					continue;
				}
				
				String varName = ContextReader.readVariableName(child);
				if (varName != null && !varName.endsWith(".length")) {
					int startPos = child.getPos();
					int endPos = startPos + child.getLength();
					SuspNullExpStr snes = new SuspNullExpStr(varName, startPos, endPos);
					if (!suspNullExpStrs.contains(snes) && !Checker.isAssignment(suspCodeAst.getType())
							&& !Checker.isVariableDeclarationFragment(suspCodeAst.getType())) suspNullExpStrs.add(snes);
					if (!allSuspVariables.contains(varName)) allSuspVariables.add(varName);
				}
				else identifySuspiciousVariables(child, allSuspVariables, suspNullExpStrs);
			} else if (Checker.isMethodInvocation(childType)) {
				List<ITree> subChildren = child.getChildren();
				if (subChildren.size() > 2) {
					int startPos = child.getPos();
					ITree subChild = subChildren.get(subChildren.size() - 2);
					int endPos = subChild.getPos() + subChild.getLength();
					String suspExpStr = this.getSubSuspiciouCodeStr(startPos, endPos);
					SuspNullExpStr snes = new SuspNullExpStr(suspExpStr, startPos, endPos);
					if (!suspNullExpStrs.contains(snes) && !Checker.isAssignment(suspCodeAst.getType())
							&& !Checker.isVariableDeclarationFragment(suspCodeAst.getType())) suspNullExpStrs.add(snes);
				}
				identifySuspiciousVariables(child, allSuspVariables, suspNullExpStrs);
			} else if (Checker.isArrayAccess(childType)) {
				int startPos = child.getPos();
				int endPos = startPos + child.getLength();
				String suspExpStr = this.getSubSuspiciouCodeStr(startPos, endPos);
				SuspNullExpStr snes = new SuspNullExpStr(suspExpStr, startPos, endPos);
				if (!suspNullExpStrs.contains(snes) && !Checker.isAssignment(suspCodeAst.getType())
						&& !Checker.isVariableDeclarationFragment(suspCodeAst.getType())) suspNullExpStrs.add(snes);
				identifySuspiciousVariables(child, allSuspVariables, suspNullExpStrs);
			} else if (Checker.isQualifiedName(childType) || Checker.isFieldAccess(childType) || Checker.isSuperFieldAccess(childType)) {
				int parentType = suspCodeAst.getType();
				if ((Checker.isAssignment(parentType) || Checker.isVariableDeclarationFragment(parentType))
						&& suspCodeAst.getChildPosition(child) == 0) {
					continue;
				}
				int startPos = child.getPos();
				int endPos = startPos + child.getLength();
				String suspExpStr = this.getSubSuspiciouCodeStr(startPos, endPos);
				
				if (!suspExpStr.endsWith(".length")) {
					SuspNullExpStr snes = new SuspNullExpStr(suspExpStr, startPos, endPos);
					if (!suspNullExpStrs.contains(snes) && !Checker.isAssignment(suspCodeAst.getType())
							&& !Checker.isVariableDeclarationFragment(suspCodeAst.getType())) suspNullExpStrs.add(snes);
					if (!allSuspVariables.contains(suspExpStr)) allSuspVariables.add(suspExpStr);
				}
				int index1 = suspExpStr.indexOf(".");
				int index2 = suspExpStr.lastIndexOf(".");
				if (index1 != index2) identifySuspiciousVariables(child, allSuspVariables, suspNullExpStrs);
			} else if (Checker.isFieldAccess(childType) || Checker.isSuperFieldAccess(childType)) {
				int parentType = suspCodeAst.getType();
				if ((Checker.isAssignment(parentType) || Checker.isVariableDeclarationFragment(parentType))
						&& suspCodeAst.getChildPosition(child) == 0) {
					continue;
				}
				String nameStr = child.getLabel(); // "this."/"super." + varName
				if (!allSuspVariables.contains(nameStr)) allSuspVariables.add(nameStr);
			} else if (Checker.isComplexExpression(childType)) {
				identifySuspiciousVariables(child, allSuspVariables, suspNullExpStrs);
			} else if (Checker.isStatement(childType)) break;
		}
	}

	private String readReturnType(ITree suspCodeTree) {
		String returnType = ContextReader.readMethodReturnType(suspCodeTree);
		if (returnType == null) return null;
		if ("=CONSTRUCTOR=".equals(returnType)) return null;
		return returnType;
	}

	private List<String> identifyNullCheckedVariables(ITree suspCodeTree) {
		ITree parent = suspCodeTree;
		List<String> vars = new ArrayList<>();
		
		while (true) {
			if (parent == null) return null;
			int index = parent.getChildPosition(suspCodeTree);
			for (int i = 0; i <= index; i ++) {
				ITree peerStmt = parent.getChild(i);
				if (Checker.isStatement(peerStmt.getType())) {
					vars.addAll(findNullCheckedVars(peerStmt));
				}
			}
			if (Checker.isMethodDeclaration(parent.getType())) break;
			if (Checker.isTypeDeclaration(parent.getType())) break;
			suspCodeTree = parent;
			parent = parent.getParent();
		}
		
		return vars;
	}

	private List<String> findNullCheckedVars(ITree tree) {
		List<String> vars = new ArrayList<>();
		List<ITree> children = tree.getChildren();
		for (ITree child : children) {
			int childType = child.getType();
			if (Checker.isStatement(childType)) break;
			else if (Checker.isComplexExpression(childType)) {
				if (Checker.isInfixExpression(childType)) {
					ITree subChild = child.getChild(0);
					if (Checker.isSimpleName(subChild.getType()) && !subChild.getLabel().startsWith("MethodName:")
							&& Checker.isNullLiteral(child.getChild(2).getType())
							&& ("==".equals(child.getChild(1).getLabel()) || "!=".equals(child.getChild(1).getLabel()))) {
						vars.add(subChild.getLabel());
						continue;
					}
				}
				vars.addAll(findNullCheckedVars(child));
			} else if (Checker.isSimpleName(childType) && child.getLabel().startsWith("MethodName:")) {
				vars.addAll(findNullCheckedVars(child));
			}
		}
		return vars;
	}

	private Pair<String, Boolean> identifyParentContext(ITree suspCodeTree) {
		ITree parent = suspCodeTree.getParent();
		Boolean isLoopStmt = Boolean.valueOf(false);
		while (true) {
			if (parent == null) return null;
			int type = parent.getType();
			if (!isLoopStmt.booleanValue() && 
					(Checker.isForStatement(type) || Checker.isEnhancedForStatement(type)
					|| Checker.isWhileStatement(type) || Checker.isDoStatement(type))) {
				isLoopStmt = Boolean.valueOf(true);
			} else if (Checker.isMethodDeclaration(type)) break;
			parent = parent.getParent();
		}
		
		String methodLabel = parent.getLabel();
		methodLabel = methodLabel.substring(methodLabel.indexOf("@@") + 2);
		int index = methodLabel.indexOf("MethodName:");
		if (index == -1) return null;
		String returnType = methodLabel.substring(0, index - 2);
		if ("byte".equals(returnType) || "char".equals(returnType) || "short".equals(returnType) || 
				"int".equals(returnType) || "long".equals(returnType) || "double".equals(returnType) || "float".equals(returnType))
			return null;
		
		return new Pair<String, Boolean>(returnType, isLoopStmt);
	}
	
	class SuspNullExpStr implements Comparable<SuspNullExpStr> {
		public String expStr;
		public Integer startPos;
		public Integer endPos;
		
		public SuspNullExpStr(String expStr, Integer startPos, Integer endPos) {
			this.expStr = expStr;
			this.startPos = startPos;
			this.endPos = endPos;
		}

		@Override
		public int compareTo(SuspNullExpStr o) {
			int result = this.startPos.compareTo(o.startPos);
			 if (result == 0) {
				 result = this.endPos.compareTo(o.endPos);
			 }
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (!(obj instanceof SuspNullExpStr)) return false;
			return this.expStr.equals(((SuspNullExpStr) obj).expStr);
		}
		
	}
	
	/**
	 * Fix pattern for NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE violations.
	 * 
	 * @author kui.liu
	 *
	 */
	public class NPNullOnSomePathFromReturnValue extends FixTemplate {
		
		/*
		 * -   …exp1.method()…
		 * +   T v = exp1.method();
		 * +   if (v == null) { return null; }
		 * +   …v…
		 */

		@Override
		public void generatePatches() {
			ITree suspCodeTree = this.getSuspiciousCodeTree();
			
			List<ExpStr> buggyExps = identifyBuggyExpressions(suspCodeTree);
			
			if (buggyExps.isEmpty()) return; 
			
			Pair<String, Boolean> parentContext = identifyParentContext(suspCodeTree);
			if (parentContext == null) return;
			
			boolean isLoopStmt = parentContext.getSecond().booleanValue();
			
			buggyExps = new ListSorter<ExpStr>(buggyExps).sortAscending();
			
			ExpStr buggyExp = buggyExps.get(0);
			String expStr = buggyExp.expStr;
			String fixedCodeStr1 = "if (" + expStr + " == null)";
			fixedCodeStr1 += isLoopStmt ? " continue;\n" : " return null;\n";
			for (int index = 1, size = buggyExps.size(); index < size; index ++) {
				buggyExp = buggyExps.get(index);
				expStr = buggyExp.expStr;
				fixedCodeStr1 += "if (" + expStr + " == null)";
				fixedCodeStr1 += isLoopStmt ? " continue;\n" : " return null;\n";
			}
			
			this.generatePatch(suspCodeEndPos, fixedCodeStr1);
		}

		private List<ExpStr> identifyBuggyExpressions(ITree suspCodeTree) {
			List<ExpStr> buggyExps = new ArrayList<>();
			List<ITree> children = suspCodeTree.getChildren();
			for (ITree child : children) {
				int childType = child.getType();
				if (Checker.isComplexExpression(childType)) {
					if (Checker.isArrayAccess(childType) || Checker.isFieldAccess(childType) || Checker.isSuperFieldAccess(childType)) {
						int startPos = child.getPos();
						int endPos = startPos + child.getLength();
						String expStr = this.getSubSuspiciouCodeStr(startPos, endPos);
						buggyExps.add(new ExpStr(expStr, startPos, endPos));
					} else if (Checker.isMethodInvocation(childType)) {
						List<ITree> subChildren = child.getChildren();
						if (subChildren.size() == 2) {
							if (Checker.isComplexExpression(subChildren.get(0).getType())) {
								int startPos = subChildren.get(0).getPos();
								int endPos = startPos + subChildren.get(0).getLength();
								String expStr = this.getSubSuspiciouCodeStr(startPos, endPos);
								buggyExps.add(new ExpStr(expStr, startPos, endPos));
							}
						} else if (subChildren.size() >= 2) {
							int startPos = child.getPos();
							ITree subChild = subChildren.get(subChildren.size() - 2);
							int endPos = subChild.getPos() + subChild.getLength();
							String expStr = this.getSubSuspiciouCodeStr(startPos, endPos);
							buggyExps.add(new ExpStr(expStr, startPos, endPos));
						}
					}
					buggyExps.addAll(identifyBuggyExpressions(child));
				} else if (Checker.isStatement(childType)) break;
			}
			return buggyExps;
		}

		private Pair<String, Boolean> identifyParentContext(ITree suspCodeTree) {
			ITree parent = suspCodeTree.getParent();
			Boolean isLoopStmt = Boolean.valueOf(false);
			while (true) {
				if (parent == null) return null;
				int type = parent.getType();
				if (!isLoopStmt.booleanValue() && 
						(Checker.isForStatement(type) || Checker.isEnhancedForStatement(type)
						|| Checker.isWhileStatement(type) || Checker.isDoStatement(type))) {
					isLoopStmt = Boolean.valueOf(true);
				} else if (Checker.isMethodDeclaration(type)) break;
				parent = parent.getParent();
			}
			
			String methodLabel = parent.getLabel();
			methodLabel = methodLabel.substring(methodLabel.indexOf("@@") + 2);
			int index = methodLabel.indexOf("MethodName:");
			if (index == -1) return null;
			String returnType = methodLabel.substring(0, index - 2);
			if ("byte".equals(returnType) || "char".equals(returnType) || "short".equals(returnType) || 
					"int".equals(returnType) || "long".equals(returnType) || "double".equals(returnType) || "float".equals(returnType))
				return null;
			
			return new Pair<String, Boolean>(returnType, isLoopStmt);
		}

		class ExpStr implements Comparable<ExpStr> {
			public String expStr;
			public Integer startPos;
			public Integer endPos;
			
			public ExpStr(String expStr, Integer startPos, Integer endPos) {
				this.expStr = expStr;
				this.startPos = startPos;
				this.endPos = endPos;
			}

			@Override
			public int compareTo(ExpStr o) {
				int result = this.startPos.compareTo(o.startPos);
				 if (result == 0) {
					 result = this.endPos.compareTo(o.endPos);
				 }
				return result;
			}
			
		}
	}

}
