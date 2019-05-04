package edu.lu.uni.serval.tbar.fixpatterns;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.lu.uni.serval.entity.Pair;
import edu.lu.uni.serval.jdt.tree.ITree;
import edu.lu.uni.serval.tbar.context.ContextReader;
import edu.lu.uni.serval.tbar.context.Method;
import edu.lu.uni.serval.tbar.fixtemplate.FixTemplate;
import edu.lu.uni.serval.tbar.utils.Checker;

public class VariableReplacer extends FixTemplate {


	@Override
	public void generatePatches() {
		/*
		 * SOFix	VarReplacer
		 * SimFix	RepName_Name
		 * FB	DLSDeadLocalStore
		 * Par  Castee Mutator 
		 * 
		 * Fuzzy fix pattern.
		 * SketchFix	ExpressionTransform
		 *
		 * Update Variable with anther one.
		 * -  ...v1...
		 * +  ...v2...
		 * ---UPD Variable
		 */
		int stmtType = this.getSuspiciousCodeTree().getType();
		if (Checker.isForStatement(stmtType) || Checker.isEnhancedForStatement(stmtType)
				|| Checker.isWhileStatement(stmtType) || Checker.isDoStatement(stmtType)) return;
		List<ITree> suspVars = new ArrayList<>();
		ContextReader.identifySuspiciousVariables(this.getSuspiciousCodeTree(), suspVars, new ArrayList<String>());
		if (suspVars.isEmpty()) return;
		ContextReader.readAllVariablesAndFields(this.getSuspiciousCodeTree(), allVarNamesMap, varTypesMap, allVarNamesList, this.sourceCodePath, dic);
		
		for (ITree suspVar : suspVars) {
			if (isForStatementVar(suspVar.getLabel())) continue;//
		
			String codePart1;
			if (Checker.isFieldAccess(suspVar.getParent().getType())) {
				codePart1 = this.getSubSuspiciouCodeStr(suspCodeStartPos, suspVar.getParent().getPos());
			} else {
				codePart1 = this.getSubSuspiciouCodeStr(suspCodeStartPos, suspVar.getPos());
			}
			String codePart2 = this.getSubSuspiciouCodeStr(suspVar.getPos() + suspVar.getLength(), suspCodeEndPos);
			
			String suspVarName = suspVar.getLabel();
			if (suspVarName.startsWith("Name:"))
				suspVarName = suspVarName.substring(5);
			boolean isQualifiedName = Checker.isQualifiedName(suspVar.getType()) || Character.isUpperCase(suspVarName.charAt(0));
			String suspVarType = varTypesMap.get(suspVarName);
			if (suspVarType == null) {
				suspVarType = varTypesMap.get("this." + suspVarName);
			}
			if ("boolean".equals(suspVarType)) {
				List<String> booleanVars = allVarNamesMap.get("boolean");
				List<String> booleanVars_ = allVarNamesMap.get("Boolean");
				if (booleanVars == null) {
					if (booleanVars_ != null) booleanVars = allVarNamesMap.get("Boolean");
					else continue;
				} else if (booleanVars_ != null) {
					booleanVars.addAll(booleanVars_);
				}
				for (String var : booleanVars) {
					if (var.equals(suspVarName) || var.equals("this." + suspVarName)) continue;
					this.generatePatch(codePart1 + var + codePart2);
				}
			} else {
				List<String> sameTypeVars = allVarNamesMap.get(suspVarType);
				if (sameTypeVars != null) {
					List<Pair<String, Double>> similarVarPairs = sortCompatibleVars(sameTypeVars, suspVarName);
					MyFor: for (Pair<String, Double> var : similarVarPairs) {
						for (ITree suspV : suspVars) {
							if (suspV.getLabel().equals(var.getFirst())) continue MyFor;
						}
						if (!isQualifiedName && Character.isUpperCase(var.getFirst().charAt(0))) continue MyFor; //var.getFirst().contains(".") ||
						this.generatePatch(codePart1 + var.getFirst() + codePart2);
					}
				} else sameTypeVars = new ArrayList<>();
				
//				List<String> variables = new ArrayList<>();
//				variables.addAll(allVarNamesList);
//				variables.removeAll(sameTypeVars);
//				variables.remove(suspVarName);
//				List<Pair<String, Double>> sortVariables = sortCompatibleVars(variables, suspVarName);
//				for (Pair<String, Double> var : sortVariables) {
//					this.generatePatch(codePart1 + var.getFirst() + codePart2);
//				}
			}
			
			// Qualified Name.
			if (Checker.isQualifiedName(suspVar.getType())) {
				String varName = suspVar.getLabel();
				String className = "." + varName.substring(0, varName.indexOf("."));
				String currentClassPath = ContextReader.readClassPath(this.getSuspiciousCodeTree());
				if (currentClassPath == null || currentClassPath.isEmpty()) continue;
				List<String> importedDependencies = dic.getImportedDependencies().get(currentClassPath);
				if (importedDependencies == null) continue;
				String classPath = null;
				for (String importedDependency : importedDependencies) {
					if (importedDependency.endsWith(className)) {
						classPath = importedDependency;
						break;
					}
				}
				if (classPath == null) continue;
				File fileName = new File(this.sourceCodePath + classPath.replace(".", "/") + ".java");
				if (!fileName.exists()) continue;
				Map<String, String> publicStaticFields = ContextReader.readPublicStaticFields(fileName);
				if (publicStaticFields.isEmpty()) continue;
				className = className.substring(1);
				for (Map.Entry<String, String> entity : publicStaticFields.entrySet()) {
					String publicStaticField = entity.getKey();
					String varType = entity.getValue();
					publicStaticField = className + "." + publicStaticField;
					if (publicStaticField.equals(suspVarName)) {
						if (suspVarType == null) {
							suspVarType = varType;
							List<String> sameTypeVars = allVarNamesMap.get(suspVarType);
							if (sameTypeVars != null) {
								for (String var : sameTypeVars) {
									this.generatePatch(codePart1 + var + codePart2);
								}
							}
						}
						continue;
					}
					if (varType.equals(suspVarType)) this.generatePatch(codePart1 + publicStaticField + codePart2);
				}
				
				ITree sameValueSuspVarTree = sameValueSuspVarTrees.get(suspVarName);
				if (sameValueSuspVarTree == null) sameValueSuspVarTrees.put(suspVarName, suspVar);
				else {
					int startPos1 = suspVar.getPos();
					int endPos1 = startPos1 + suspVar.getLength();
					int startPos2 = sameValueSuspVarTree.getPos();
					int endPos2 = startPos2 + sameValueSuspVarTree.getLength();
					String codePart_1, codePart_2, codePart_3;
					if (startPos1 < startPos2) {
						codePart_1 = this.getSubSuspiciouCodeStr(suspCodeStartPos, startPos1);
						codePart_2 = this.getSubSuspiciouCodeStr(endPos1, startPos2);
						codePart_3 = this.getSubSuspiciouCodeStr(endPos2, suspCodeEndPos);
					} else {
						codePart_1 = this.getSubSuspiciouCodeStr(suspCodeStartPos, startPos2);
						codePart_2 = this.getSubSuspiciouCodeStr(endPos2, startPos1);
						codePart_3 = this.getSubSuspiciouCodeStr(endPos1, suspCodeEndPos);
					}
					for (Map.Entry<String, String> entity : publicStaticFields.entrySet()) {
						String publicStaticField = entity.getKey();
						String varType = entity.getValue();
						if (publicStaticField.equals(suspVarName) || !varType.equals(suspVarType)) continue;
						this.generatePatch(codePart_1 + publicStaticField + codePart_2 + publicStaticField + codePart_3);
					}
				}
			}
		}
		
		/*
		 * SOFix	VarToInvo
		 * SimFix	RepName_MI
		 * SketchFix	ExpressionTransform	
		 *
		 *
		 * Replace a variable with a method invocation.
		 * 
		 * methods whose names are similar to variable names.
		 * methods without parameters.
		 */
		String currentClassPath = ContextReader.readClassPath(this.getSuspiciousCodeTree());
		if (currentClassPath == null || currentClassPath.isEmpty()) return;

		List<Method> methods = dic.findAllAvailableMethodsOfThisClass(currentClassPath, true, true);
		for (ITree varTree : suspVars) {
			if (!Checker.isSimpleName(varTree.getType())) continue;
			String varName = varTree.getLabel();
			if (varName.startsWith("Name:")) varName = varName.substring(5);
			String varDataType = varTypesMap.get(varName);
			if (varDataType == null) varDataType = varTypesMap.get("this." + varName);
			if (varDataType == null) continue;
			
			int pos1 = varTree.getPos();
			int pos2 = pos1 + varTree.getLength();
			String codePart1 = getSubSuspiciouCodeStr(suspCodeStartPos, pos1);
			String codePart2 = getSubSuspiciouCodeStr(pos2, suspCodeEndPos);
			for (Method method : methods) {
				if (method.isConstructor()) continue;
//				if (method.getParameterTypes().isEmpty()) {
//					if (!"void".equals(method.getReturnType())) {
//						String fixedCodeStr1 = codePart1 + method.getMethodName() + "()" + codePart2;
//						generatePatch(fixedCodeStr1);
//					}
//				} else 
//				if (varDataType.equals(method.getReturnType())) {
					List<String> parameterTypes = method.getParameterTypes();
					if (parameterTypes.isEmpty()) {
						generatePatch(codePart1 + method.getMethodName() + "()" + codePart2);
						continue;
					}
//					List<Map<String, String>> maps = new ArrayList<>();
//					boolean isFailed = false; // failed to find useful variables as parameters of this method.
//					for (String parameterType : parameterTypes) {
//						List<String> vars = allVarNamesMap.get(parameterType);
//						if (vars == null) {
//							isFailed = true;
//							break;
//						}
//						maps = ContextReader.arrangeVariableGroups(maps, parameterType, vars);
//					}
//					if (isFailed) continue;
//					
//					for (Map<String, String> map : maps) {
//						String fixedCodeStr1 = method.getMethodName() + "(";
//						for (String parameterType : parameterTypes) {
//							if (!fixedCodeStr1.endsWith("(")) fixedCodeStr1 += ", ";
//							fixedCodeStr1 += map.get(parameterType);
//						}
//						fixedCodeStr1 = codePart1 + fixedCodeStr1 + ")" + codePart2;
//						generatePatch(fixedCodeStr1);
//					}
//				}
			}
		}
		clear();
	}

	private boolean isForStatementVar(String suspVarName) {
		ITree parentStmt = this.getSuspiciousCodeTree().getParent();
		while (true) {
			if (parentStmt == null) return false;
			if (!Checker.isStatement(parentStmt.getType())) return false;
			if (Checker.isForStatement(parentStmt.getType()) || Checker.isEnhancedForStatement(parentStmt.getType())) break;
			parentStmt = parentStmt.getParent();
		}
		
		if (Checker.isForStatement(parentStmt.getType())) {
			ITree varDecFrag = parentStmt.getChild(0);
			if (Checker.isVariableDeclarationExpression(varDecFrag.getType())) {
				List<ITree> children = varDecFrag.getChildren();
				for (int i = 1, size = children.size(); i < size; i ++) {
					ITree child = children.get(i);
					String varName = child.getChild(0).getLabel();
					if (suspVarName.equals(varName)) return true;
				}
			}
		} else {
			ITree singleVarDec = parentStmt.getChild(0);
			List<ITree> children = singleVarDec.getChildren();
			int size = children.size();
			String varName = children.get(size - 1).getLabel();
			if (suspVarName.equals(varName)) return true;
		}
		
		return false;
	}

	private List<Pair<String, Double>> sortCompatibleVars(List<String> sameTypeVars, String suspVarName) {
		List<Pair<String, Double>> similarVarPairs = new ArrayList<>();
		int index = suspVarName.lastIndexOf(".");
		if (index >= 0) suspVarName = suspVarName.substring(index + 1);
		for (String var1 : sameTypeVars) {
			String var = var1;
			if (var.equals(suspVarName) || var.equals("this." + suspVarName)) continue;
			index = var.lastIndexOf(".");
			if (index >= 0) var = var.substring(index + 1);
			double similarity = new LongestCommonSubsequence().similarity(suspVarName, var);
			similarVarPairs.add(new Pair<String, Double>(var1, similarity));
		}
		Collections.sort(similarVarPairs, new Comparator<Pair<String, Double>>() {
			@Override
			public int compare(Pair<String, Double> p1, Pair<String, Double> p2) {
				return p2.secondElement.compareTo(p1.secondElement);
			}
		});
		return similarVarPairs;
	}

//	private List<Pair<String, Double>> identifySimilarVariableNames(String suspVarName, List<String> vars) {
//		List<Pair<String, Double>> similarVars = new ArrayList<>();
//		int index = suspVarName.lastIndexOf(".");
//		if (index >= 0) suspVarName = suspVarName.substring(index + 1);
//		for (String var : vars) {
//			index = var.lastIndexOf(".");
//			if (index >= 0) var = var.substring(index + 1);
//			double distance = new LongestCommonSubsequence().similarity(suspVarName, var);
////			if (distance > 0.5) {
//				similarVars.add(new Pair<String, Double>(var, distance));
////			}
//			
//		}
//		Collections.sort(similarVars, new Comparator<Pair<String, Double>>() {
//			@Override
//			public int compare(Pair<String, Double> o1, Pair<String, Double> o2) {
//				return o2.secondElement.compareTo(o1.secondElement);
//			}
//		});
//		return similarVars;
//	}

	private Map<String, ITree> sameValueSuspVarTrees = new HashMap<>();
	
	private class LongestCommonSubsequence {

		public Double similarity(String str1, String str2) {
			if (str1 == null || str2 == null) {
				return Double.NaN;
	        }

	        if (str1.equals(str2)) {
	            return 1d; // 0d
	        }

	        // str1.length() + str2.length() - 2 * lcs(str1, str2)
	        int distance = lcs(str1, str2);
	        return 2d * distance / (str1.length() + str2.length());
		}

		int lcs(String s1, String s2) {
	        int lengthOfS1 = s1.length();
	        int lengthOfS2 = s2.length();
	        char[] x = s1.toCharArray();
	        char[] y = s2.toCharArray();

	        int[][] c = new int[lengthOfS1 + 1][lengthOfS2 + 1];

	        for (int i = 1; i <= lengthOfS1; i++) {
	            for (int j = 1; j <= lengthOfS2; j++) {
	                if (x[i - 1] == y[j - 1]) {
	                    c[i][j] = c[i - 1][j - 1] + 1;

	                } else {
	                    c[i][j] = Math.max(c[i][j - 1], c[i - 1][j]);
	                }
	            }
	        }

	        return c[lengthOfS1][lengthOfS2];
		}

	}

}
