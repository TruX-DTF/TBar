package edu.lu.uni.serval.tbar.context;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.lu.uni.serval.AST.ASTGenerator;
import edu.lu.uni.serval.AST.ASTGenerator.TokenType;
import edu.lu.uni.serval.jdt.tree.ITree;
import edu.lu.uni.serval.tbar.utils.Checker;
import edu.lu.uni.serval.tbar.utils.SetUtils;

public class ContextReader {
	
	/**
	 * Read the all variables by traversing the ancestral tree of the suspicious code AST.
	 * 
	 * @param codeAst
	 * @param allVarNamesMap
	 * @param varTypesMap
	 * @param allVarNamesList
	 */
	public static void readAllVariablesAndFields(ITree codeAst, Map<String, List<String>> allVarNamesMap, Map<String, String> varTypesMap, List<String> allVarNamesList, String srcCodePath, Dictionary dic) {
		if (dic == null) {
			while (true) {
				int parentTreeType = codeAst.getType();
				if (Checker.isStatement(parentTreeType)) {
					// read local variables.
					readVariableDeclaration(codeAst, parentTreeType, allVarNamesMap, varTypesMap, allVarNamesList);
					parentTreeType = codeAst.getParent().getType();
					if (Checker.isStatement2(parentTreeType) || Checker.isMethodDeclaration(parentTreeType)) {
						List<ITree> children = codeAst.getParent().getChildren();
						int index = children.indexOf(codeAst) - 1;
						for (; index >= 0; index --) {
							ITree child = children.get(index);
							int childType = child.getType();
							if (!Checker.isStatement(childType)) break;
							readVariableDeclaration(child, childType, allVarNamesMap, varTypesMap, allVarNamesList);
						}
					}
				} else if (Checker.isMethodDeclaration(parentTreeType)) { 
					// read parameters.
					List<ITree> children = codeAst.getChildren();
					for (ITree child : children) {
						int childType = child.getType();
						if (Checker.isStatement(childType)) break;
						readSingleVariableDeclaration(child, childType, allVarNamesMap, varTypesMap, allVarNamesList);
					}
				} else if (Checker.isTypeDeclaration(parentTreeType)) {
					// read fields.
					readFields(codeAst, allVarNamesMap, varTypesMap, allVarNamesList);
					readFiledsInSuperClass(codeAst, srcCodePath, allVarNamesMap, varTypesMap, allVarNamesList);
					
					// read variables in related java files.
					//TODO
					break;
				}
				
				codeAst = codeAst.getParent();
				if (codeAst == null) break;
			}
		} else {
			while (true) {
				int parentTreeType = codeAst.getType();
				if (Checker.isStatement(parentTreeType)) {// variable
					readVariableDeclaration(codeAst, parentTreeType, allVarNamesMap, varTypesMap, allVarNamesList);
					parentTreeType = codeAst.getParent().getType();
					if (Checker.isStatement2(parentTreeType) || Checker.isMethodDeclaration(parentTreeType)) {
						List<ITree> children = codeAst.getParent().getChildren();
						int index = children.indexOf(codeAst) - 1;
						for (; index >= 0; index --) {
							ITree child = children.get(index);
							int childType = child.getType();
							if (!Checker.isStatement(childType)) break;
							readVariableDeclaration(child, childType, allVarNamesMap, varTypesMap, allVarNamesList);
						}
					}
				} else if (Checker.isMethodDeclaration(parentTreeType)) { // parameter type.
					List<ITree> children = codeAst.getChildren();
					for (ITree child : children) {
						int childType = child.getType();
						if (Checker.isStatement(childType)) break;
						readSingleVariableDeclaration(child, childType, allVarNamesMap, varTypesMap, allVarNamesList);
					}
				} else if (Checker.isTypeDeclaration(parentTreeType)) {// Field
					String classNameAndPath = readClassNameAndPath(codeAst);
					List<Field> fields = dic.findFieldsByClassPath(classNameAndPath);
					addFieldsToVars(fields, allVarNamesMap, varTypesMap, allVarNamesList, classNameAndPath, true);
					addSuperFieldsToVars(classNameAndPath, allVarNamesMap, varTypesMap, allVarNamesList, dic);
					addStaticFieldsFromDependencies(allVarNamesMap, varTypesMap, allVarNamesList, classNameAndPath, dic, "");
				}
				
				codeAst = codeAst.getParent();
				if (codeAst == null) break;
			}
		}
	}
	
	private static void addFieldsToVars(List<Field> fields, Map<String, List<String>> allVarNamesMap,
			Map<String, String> varTypesMap, List<String> allVarNamesList, String className, boolean privateNeeded) {
		if (fields == null) return;
		
		for (Field field : fields) {
			ModifierType mt = field.getModifier();
			if (!privateNeeded && mt == ModifierType.PRIVATE) continue;
			
			String dataType = field.getDataType();
			String varName = field.getVarName();
			if (!allVarNamesList.contains(varName)) allVarNamesList.add(varName);
			if (!varTypesMap.containsKey(varName)) varTypesMap.put(varName, dataType);
			SetUtils.addToMap(allVarNamesMap, dataType, varName);
		}
	}
	
	private static void addSuperFieldsToVars(String classNameAndPath, Map<String, List<String>> allVarNamesMap,
			Map<String, String> varTypesMap, List<String> allVarNamesList, Dictionary dic) {
		String superClassNameAndPath = dic.findSuperClassName(classNameAndPath);
		if (superClassNameAndPath == null) return;
		
		List<Field> fields = dic.findFieldsByClassPath(superClassNameAndPath);
		addFieldsToVars(fields, allVarNamesMap, varTypesMap, allVarNamesList, superClassNameAndPath, false);
		addSuperFieldsToVars(superClassNameAndPath, allVarNamesMap, varTypesMap, allVarNamesList, dic);
	}

	private static void addStaticFieldsFromDependencies(Map<String, List<String>> allVarNamesMap,
			Map<String, String> varTypesMap, List<String> allVarNamesList, String classNameAndPath, Dictionary dic, String codePath) {
		List<String> dependencies = dic.findImportedDependencies(classNameAndPath);
		if (dependencies == null) return;
		
		String packageName = classNameAndPath.substring(0, classNameAndPath.lastIndexOf("."));
		for (String dependency : dependencies) {
			if (dependency.equals(dic.findSuperClassName(classNameAndPath))) continue;
			
			List<Field> fields = dic.findFieldsByClassPath(dependency);
			if (fields == null) {
//				File javaFile = new File(codePath + dependency.replace(".", "/") + ".java");
//				if (!javaFile.exists()) {
//					String classPath = dependency.substring(0, dependency.lastIndexOf("."));
//					javaFile = new File(codePath + classPath.replace(".", "/") + ".java");
//					if (!javaFile.exists()) continue;
//				}
				continue;
			}
			
			boolean isSamePackage = false;
			int index = dependency.lastIndexOf(".");
			String packageName_ = dependency.substring(0, index);
			if (packageName_.equals(packageName)) isSamePackage = true;
			String className = dependency.substring(index + 1);
			for (Field field : fields) {
				if (field.isStatic() && 
						(field.getModifier() == ModifierType.PUBLIC || 
								(isSamePackage && field.getModifier() == ModifierType.PROTECTED))) {
					String dataType = field.getDataType();
					String varName =  field.getVarName();
					if (!varName.startsWith(className + ".")) {
						varName = className + "." + varName;
					}
					if (!allVarNamesList.contains(varName)) allVarNamesList.add(varName);
					if (!varTypesMap.containsKey(varName)) varTypesMap.put(varName, dataType);
					SetUtils.addToMap(allVarNamesMap, dataType, varName);
				}
			}
		}
	}
	
	public static String readClassNameAndPath(ITree classDeclarationTree) {
		String className = readClassName(classDeclarationTree);
		ITree parentTree = classDeclarationTree.getParent();
		while (true) {
			if (Checker.isCompilationUnit(parentTree.getType())) break;
			if (Checker.isTypeDeclaration(parentTree.getType())) {
				className = readClassName(parentTree) + "." + className;
			}
			parentTree = parentTree.getParent();
			if (parentTree == null) break;
		}
		
		if (parentTree == null) return className;
		
		ITree packageTree = parentTree.getChild(0);
		if (!Checker.isPackageDeclaration(packageTree.getType())) return className;
		
		String packageName = packageTree.getLabel().trim();
		int index = packageName.lastIndexOf("package ");
		if (index >= 0) packageName = packageName.substring(index + 8);
		className = packageName + "." + className;
		
		return className;
	}
	
	public static String readClassName(ITree classDeclarationTree) {
		String classNameLabel = classDeclarationTree.getLabel();
		int indexOfClassName = classNameLabel.indexOf("ClassName:") + 10;
		classNameLabel = classNameLabel.substring(indexOfClassName);
		String className = classNameLabel.substring(0, classNameLabel.indexOf(", "));
		return readSimpleNameOfDataType(className);
	}
	
	public static String readSuperClassName(ITree classDecTree) {
		String classLabel = classDecTree.getLabel();
		int superClassIndex = classLabel.indexOf("@@SuperClass:");
		
		String superClassName = null;
		if (superClassIndex > 0) {
			superClassName = classLabel.substring(superClassIndex + 13);
			superClassName = superClassName.substring(0, superClassName.indexOf(", "));
		}
		
		if (superClassName == null) return null;
		
		return readSimpleNameOfDataType(superClassName);
	}
	
	public static String readReturnType(ITree methodDeclarationTree) {
		String label = methodDeclarationTree.getLabel();
		int indexOfMethodName = label.indexOf("MethodName:");

		// Read return type.
		String returnType = label.substring(label.indexOf("@@") + 2, indexOfMethodName - 2);
		int index = returnType.indexOf("@@tp:");
		if (index > 0) returnType = returnType.substring(0, index - 2);
		return readType(returnType);
	}
	
	public static String readMethodReturnType(ITree suspCodeTree) {
		ITree parent = suspCodeTree.getParent();
		while (true) {
			if (parent == null) return null;
			if (Checker.isMethodDeclaration(parent.getType())) {
				break;
			}
			parent = parent.getParent();
		}
		
		return readReturnType(parent);
	}
	
	private static void readFiledsInSuperClass(ITree codeAst, String srcCodePath, Map<String, List<String>> allVarNamesMap, Map<String, String> varTypesMap, List<String> allVarNamesList) {
		String superClassName = readSuperClassName(codeAst);
		if (superClassName != null) {
			String superClassPath = findJavaFilePath(codeAst, srcCodePath, superClassName);;
			if (superClassPath != null && new File(superClassPath).exists()) {
				ITree superClassTree = new ASTGenerator().generateTreeForJavaFile(superClassPath, TokenType.EXP_JDT);
				List<ITree> superClassChildren = superClassTree.getChildren();
				for (ITree superClassChild : superClassChildren) {
					if (Checker.isTypeDeclaration(superClassChild.getType())) {
						readFields(superClassChild, allVarNamesMap, varTypesMap, allVarNamesList);
						readFiledsInSuperClass(superClassChild, srcCodePath, allVarNamesMap, varTypesMap, allVarNamesList);
						break;
					}
				}
			}
		}
	}

	public static String findJavaFilePath(ITree classDecTree, String srcCodePath, String className) {
		List<ITree> importDeclarations = classDecTree.getParent().getChildren().subList(0, classDecTree.getParent().getChildPosition(classDecTree));
		String superClassPath = null;
		
		String packageName = null;
		if (Checker.isPackageDeclaration(importDeclarations.get(0).getType())) {
			packageName = importDeclarations.get(0).getLabel().trim().replace(".", "/");
		}
		if (packageName != null && new File(srcCodePath + packageName + "/" + className + ".java").exists()) {
			superClassPath = srcCodePath + packageName + "/" + className + ".java"; // return this path.
		} // TODO: default empty package.
		
		if (superClassPath == null) {
			for (ITree importDec : importDeclarations) {
				if (Checker.isImportDeclaration(importDec.getType())) {
					String importLabel = importDec.getLabel().trim();
					if (importLabel.endsWith("." + className)) {
						superClassPath = srcCodePath + importLabel.replace(".", "/") + ".java";
						break;
					}
				}
			}
		}
		
		return superClassPath;
	}
	
	private static void readFields(ITree typeDeclarationTree, Map<String, List<String>> allVarNamesMap, Map<String, String> varTypesMap, List<String> allVarNamesList) {
		List<ITree> children = typeDeclarationTree.getChildren();
		String className = ContextReader.readClassName(typeDeclarationTree);
		for (ITree child : children) {
			int childType = child.getType();
			if (Checker.isFieldDeclaration(childType)) {
				List<ITree> subChildren = child.getChildren();
				boolean readVar = false;
				boolean isStatic = false;
				String varType = null;
				boolean isPrivate = false;
				for (ITree subChild : subChildren) {
					if (readVar) {
						String varName;
						if (isStatic) {
							varName = subChild.getChild(0).getLabel();
							if ("serialVersionUID".equals(varName)) break;
							if (!isPrivate) {
								addVarialbeToSet(varName, varType, allVarNamesMap, varTypesMap, allVarNamesList);
								varName = className + "." + varName;
							}
						} else {
							varName = "this." + subChild.getChild(0).getLabel();
						}
						addVarialbeToSet(varName, varType, allVarNamesMap, varTypesMap, allVarNamesList);
					} else if (!Checker.isModifier(subChild.getType())) {
						varType = readType(subChild.getLabel());
						readVar = true;
					} else {
						if (subChild.getLabel().equals("static")) {
							isStatic = true;
//							break;
						} else if (subChild.getLabel().equals("private")) {
							isPrivate = true;
						}
					}
				}
			}
		}
	}

	private static void addVarialbeToSet(String varName, String varType, Map<String, List<String>> allVarNamesMap, Map<String, String> varTypesMap, List<String> allVarNamesList) {
		List<String> varNames = allVarNamesMap.get(varType);
		if (varNames == null) {
			varNames = new ArrayList<>();
		}
		varNames.add(varName);
		allVarNamesList.add(varName);
		allVarNamesMap.put(varType, varNames);
		varTypesMap.put(varName, varType);
	}

	/**
	 * Read the information of a variable in the variable declaration nodes.
	 * @param stmtTree
	 * @param stmtType
	 * @param varNamesMap
	 * @param varTypesMap
	 * @param allVarNamesList
	 */
	private static void readVariableDeclaration(ITree stmtTree, int stmtType, Map<String, List<String>> varNamesMap, Map<String, String> varTypesMap, List<String> allVarNamesList) {
		String varType = null;
		if (Checker.isVariableDeclarationStatement(stmtType)) {
			List<ITree> children = stmtTree.getChildren();
			boolean readVar = false;
			for (ITree child : children) {
				if (readVar) {
					String varName = child.getChild(0).getLabel();
					List<String> varNames = varNamesMap.get(varType);
					if (varNames == null) {
						varNames = new ArrayList<>();
					}
					varNames.add(varName);
					varNamesMap.put(varType, varNames);
					varTypesMap.put(varName, varType);
					allVarNamesList.add(varName);
				} else if (!Checker.isModifier(child.getType())) {
					varType = readType(child.getLabel());
					readVar = true;
				}
			}
		} else if (Checker.isForStatement(stmtType)) {
			ITree varDecFrag = stmtTree.getChild(0);
			if (Checker.isVariableDeclarationExpression(varDecFrag.getType())) {
				List<ITree> children = varDecFrag.getChildren();
				varType = readType(children.get(0).getLabel());
				for (int i = 1, size = children.size(); i < size; i ++) {
					ITree child = children.get(i);
					String varName = child.getChild(0).getLabel();
					List<String> varNames = varNamesMap.get(varType);
					if (varNames == null) {
						varNames = new ArrayList<>();
					}
					varNames.add(varName);
					varNamesMap.put(varType, varNames);
					varTypesMap.put(varName, varType);
					allVarNamesList.add(varName);
				}
			}
		} else if (Checker.isEnhancedForStatement(stmtType)) {
			ITree singleVarDec = stmtTree.getChild(0);
			readSingleVariableDeclaration(singleVarDec, singleVarDec.getType(), varNamesMap, varTypesMap, allVarNamesList);
		}
	}
	
	private static void readSingleVariableDeclaration(ITree codeTree, int treeType, Map<String, List<String>> varNamesMap, Map<String, String> varTypesMap, List<String> allVarNamesList) {
		if (Checker.isSingleVariableDeclaration(treeType)) {
			List<ITree> children = codeTree.getChildren();
			int size = children.size();
			String varType = readType(children.get(size - 2).getLabel());
			String varName = children.get(size - 1).getLabel();
			List<String> varNames = varNamesMap.get(varType);
			if (varNames == null) {
				varNames = new ArrayList<>();
			}
			varNames.add(varName);
			varNamesMap.put(varType, varNames);
			varTypesMap.put(varName, varType);
			allVarNamesList.add(varName);
		}
	}
	
	/**
	 * Remove the type parameters of the data type.
	 * e.g., List<T> --> List.
	 * 
	 * @param returnType
	 * @return
	 */
	public static String readType(String returnType) {
		if (returnType.endsWith("[]")) {
			return readType(returnType.substring(0, returnType.length() - 2)) + "[]";
		}
		
		return readSimpleNameOfDataType(returnType);
	}
	
	/**
	 * Remove the type parameters of the data type (class name).
	 * e.g., List<T> --> List.
	 * 
	 * @param className
	 * @return
	 */
	public static String readSimpleNameOfDataType(String className) {
		int index = className.indexOf("<");
		if (index != -1) {
			if (index == 0) {
				while (index == 0) {
					className = className.substring(className.indexOf(">") + 1).trim();
					index = className.indexOf(">");
				}
				index = className.indexOf("<");
				if (index == -1) index = className.length();
			}
			className = className.substring(0, index);
		}
		index = className.lastIndexOf(".");
		if (index != -1) { // && returnType.startsWith("java.")) {
			className = className.substring(index + 1);
		}

		return className;
	}

	public static void identifySuspiciousVariables(ITree suspCodeAst, List<ITree> varTrees, List<String> allSuspVariables) {
		List<ITree> children = suspCodeAst.getChildren();
		for (ITree child : children) {
			int childType = child.getType();
			if (Checker.isSimpleName(childType)) {
				int parentType = suspCodeAst.getType();
				if ((Checker.isAssignment(parentType) || Checker.isVariableDeclarationFragment(parentType))
						&& suspCodeAst.getChildPosition(child) == 0) {
					continue;
				}
				String varName = readVariableName(child);
				if (varName != null) {
					if (varTrees != null) varTrees.add(child);
					if (!allSuspVariables.contains(varName)) allSuspVariables.add(varName);
				}
				else identifySuspiciousVariables(child, varTrees, allSuspVariables);
			} else if (Checker.isQualifiedName(childType)) {
				int parentType = suspCodeAst.getType();
				if ((Checker.isAssignment(parentType) || Checker.isVariableDeclarationFragment(parentType))
						&& suspCodeAst.getChildPosition(child) == 0) {
					continue;
				}
				if (varTrees != null) varTrees.add(child);
				String qualifiedName = child.getLabel();
				if (!allSuspVariables.contains(qualifiedName)) allSuspVariables.add(qualifiedName);
			} else if (Checker.isFieldAccess(childType) || Checker.isSuperFieldAccess(childType)) {
				int parentType = suspCodeAst.getType();
				if ((Checker.isAssignment(parentType) || Checker.isVariableDeclarationFragment(parentType))
						&& suspCodeAst.getChildPosition(child) == 0) {
					continue;
				}
				String nameStr = child.getLabel(); // "this."/"super." + varName
				if (varTrees != null) varTrees.add(child);
				if (!allSuspVariables.contains(nameStr)) allSuspVariables.add(nameStr);
			} else if (Checker.isComplexExpression(childType)) {
				identifySuspiciousVariables(child, varTrees, allSuspVariables);
			} else if (Checker.isStatement(childType)) break;
		}
	}
	
	public static String readVariableName(ITree simpleNameAst) {
		String label = simpleNameAst.getLabel();
		if (label.startsWith("MethodName:") || label.startsWith("ClassName:")) {
			return null;
		} else if (label.startsWith("Name:")) {
			label = label.substring(5);
			if (!label.contains(".")) {
				char firstChar = label.charAt(0);
				if (Character.isUpperCase(firstChar)) {
					return null;
				}
			}
		}
		return label;
	}

	public static int identifyRelatedStatements(ITree suspStmtTree, String varName, int endPosition) {
		List<String> varNames = new ArrayList<>();
		varNames.add(varName);
		List<ITree> peerStmts = suspStmtTree.getParent().getChildren();
		boolean isFollowingPeerStmt = false;
		for (ITree peerStmt : peerStmts) {
			if (isFollowingPeerStmt) {
				boolean isRelatedStmt = containsVar(peerStmt, varNames);
				if (isRelatedStmt) {
					endPosition = peerStmt.getPos() + peerStmt.getLength();
					int peerStmtType = peerStmt.getType();
					if (Checker.isVariableDeclarationStatement(peerStmtType) ||
								(Checker.isExpressionStatement(peerStmtType) && Checker.isAssignment(peerStmt.getChild(0).getType()))) {
						varNames.add(identifyVariableName(peerStmt));
					}
				}
			} else {
				if (peerStmt == suspStmtTree) {
					isFollowingPeerStmt = true;
					int suspStmtType = suspStmtTree.getType();
					if (Checker.isVariableDeclarationStatement(suspStmtType) ||
							(Checker.isExpressionStatement(suspStmtType) && Checker.isAssignment(suspStmtTree.getChild(0).getType()))) {
						varNames.add(identifyVariableName(suspStmtTree));
					}
				}
			}
		}
		return endPosition;
	}

	public static boolean containsVar(ITree codeAst, List<String> varNames) {
		if (varNames.contains(codeAst.getLabel())) return true;
		List<ITree> children = codeAst.getChildren();
		if (children == null || children.isEmpty()) return false;
		for (ITree child : children) {
			int childType = child.getType();
			if (Checker.isSimpleName(childType)) {
				if (child.getLabel().startsWith("MethodName:")) {
					return containsVar(child, varNames);
				} else {
					String var = readVariableName(child);
					if (var != null && varNames.contains(var)) return true;
				}
			} else if (Checker.isComplexExpression(childType)) {
				return containsVar(child, varNames);
			} else if (Checker.isStatement(childType)) break;
		}
		return false;
	}

	public static String identifyVariableName(ITree stmtAst) {
		List<ITree> children = stmtAst.getChildren();
		int stmtAstType = stmtAst.getType();
		if (Checker.isVariableDeclarationStatement(stmtAstType)) {
			for (int index = 0, size = children.size(); index < size; index ++) {
				if (!Checker.isModifier(children.get(index).getType())) {
					return children.get(index + 1).getChild(0).getLabel();
				}
			}
		} else if (Checker.isExpressionStatement(stmtAstType)) {
			return children.get(0).getChild(0).getLabel();
		} else if (Checker.isSingleVariableDeclaration(stmtAstType)) {
			for (int index = 0, size = children.size(); index < size; index ++) {
				if (!Checker.isModifier(children.get(index).getType())) {
					return children.get(index + 1).getLabel();
				}
			}
		}
		
		return null;
	}
	
	/**
	 * Parse the parameter data types of a method declaration.
	 * 
	 * @param paraStr
	 * @return
	 */
	public static List<String> parseMethodParameterTypes(String paraStr, String splitStr) {
		List<String> parameterTypes = new ArrayList<>();
		String[] paraArray = paraStr.split(splitStr);
		for (int i = 0, length = paraArray.length; i < length; i = i + 2) {
			parameterTypes.add(ContextReader.readType(paraArray[i]));
		}
		return parameterTypes;
	}

	public static List<Map<String, String>> arrangeVariableGroups(List<Map<String, String>> maps, String keyStr, List<String> vars) {
		// n * m * l: a set of variable groups used to synthesis patches by replacing the original variables.
		if (maps.isEmpty()) {
			for (String var : vars) {
				Map<String, String> m = new HashMap<>();
				m.put(keyStr, var);
				maps.add(m);
			}
		} else {
			List<Map<String, String>> maps2 = new ArrayList<>();
			maps2.addAll(maps);
			maps.clear();
			for (String var : vars) {
				List<Map<String, String>> maps3 = new ArrayList<>();
				maps3.addAll(maps2);
				for (Map<String, String> mm : maps3) {
					Map<String, String> m = new HashMap<>();
					m.put(keyStr, var);
					m.putAll(mm);
					maps.add(m);
				}
				maps3.clear();
			}
			maps2.clear();
		}
		return maps;
	}

	public static String readVariableType(ITree codeAst, String varName) {
		ITree parentTree = codeAst.getParent();
		while (true) {
			int parentTreeType = parentTree.getType();
			if (Checker.isStatement(parentTreeType) || Checker.isMethodDeclaration(parentTreeType)) {
				// read local variables.
				List<ITree> peerStmts = parentTree.getChildren();
				int index = parentTree.getChildPosition(codeAst) - 1;
				for (; index >= 0; index --) {
					ITree peerStmt = peerStmts.get(index);
					int stmtType = peerStmt.getType();
					if (Checker.isVariableDeclarationStatement(stmtType)) {
						List<ITree> children = peerStmt.getChildren();
						boolean readVar = false;
						String varType = null;
						for (ITree child : children) {
							if (readVar) {
								String varName_ = child.getChild(0).getLabel();
								if (varName.equals(varName_)) return varType;
								break;
							} else if (!Checker.isModifier(child.getType())) {
								varType = readType(child.getLabel());
								readVar = true;
							}
						}
					} else if (Checker.isForStatement(stmtType)) {
						ITree varDecFrag = peerStmt.getChild(0);
						if (Checker.isVariableDeclarationExpression(varDecFrag.getType())) {
							List<ITree> children = varDecFrag.getChildren();
							String varType = readType(children.get(0).getLabel());
							ITree child = children.get(1);
							String varName_ = child.getChild(0).getLabel();
							if (varName.equals(varName_)) return varType;
							break;
						}
					} else if (Checker.isEnhancedForStatement(stmtType)) {
						ITree singleVarDec = peerStmt.getChild(0);
						List<ITree> children = singleVarDec.getChildren();
						int size = children.size();
						String varType = readType(children.get(size - 2).getLabel());
						String varName_ = children.get(size - 1).getLabel();
						if (varName.equals(varName_)) return varType;
					} else if (Checker.isSingleVariableDeclaration(stmtType)) {
						List<ITree> children = peerStmt.getChildren();
						int size = children.size();
						String varType = readType(children.get(size - 2).getLabel());
						String varName_ = children.get(size - 1).getLabel();
						if (varName.equals(varName_)) return varType;
					}
				}
			} else if (Checker.isTypeDeclaration(parentTreeType)) {
				// read fields.
				List<ITree> children = parentTree.getChildren();
				for (ITree child : children) {
					int childType = child.getType();
					if (Checker.isFieldDeclaration(childType)) {
						List<ITree> subChildren = child.getChildren();
						boolean readVar = false;
						String varType = null;
						for (ITree subChild : subChildren) {
							if (readVar) {
								String varName_ = subChild.getChild(0).getLabel();;
								if (varName.equals(varName_)) return varType;
							} else if (!Checker.isModifier(subChild.getType())) {
								varType = readType(subChild.getLabel());
								readVar = true;
							}
						}
					}
				}
				break;
			}
			
			codeAst = parentTree;
			parentTree = parentTree.getParent();
			if (parentTree == null) break;
		}
		return null;
	}

	public static String readClassPath(ITree suspCodeTree) {
		ITree parent = suspCodeTree.getParent();
		String className = "";
		while (true) {
			if (Checker.isTypeDeclaration(parent.getType())) {
				className = "".equals(className) ? ContextReader.readClassName(parent) : ContextReader.readClassName(parent) + "." + className;
				break;
			}
			parent = parent.getParent();
			if (parent == null) break;
		}
		if (parent == null) return null;
		List<ITree> children = parent.getParent().getChildren();
		for (ITree child : children) {
			if (Checker.isPackageDeclaration(child.getType())) {
				className = child.getLabel() + "." + className;
				break;
			}
		}
		return className;
	}

	public static void readLocalVariables(ITree codeAst, Map<String, List<String>> allVarNamesMap, Map<String, String> varTypesMap, List<String> allVarNamesList) {
		while (true) {
			int parentTreeType = codeAst.getType();
			if (Checker.isStatement(parentTreeType)) {
				// read local variables.
				readVariableDeclaration(codeAst, parentTreeType, allVarNamesMap, varTypesMap, allVarNamesList);
				parentTreeType = codeAst.getParent().getType();
				if (Checker.isStatement(parentTreeType) || Checker.isMethodDeclaration(parentTreeType)) {
					List<ITree> children = codeAst.getParent().getChildren();
					int index = children.indexOf(codeAst) - 1;
					for (; index >= 0; index --) {
						ITree child = children.get(index);
						int childType = child.getType();
						if (!Checker.isStatement(childType)) break;
						readVariableDeclaration(child, childType, allVarNamesMap, varTypesMap, allVarNamesList);
					}
				}
			} else if (Checker.isMethodDeclaration(parentTreeType)) { 
				// read parameters.
				List<ITree> children = codeAst.getChildren();
				for (ITree child : children) {
					int childType = child.getType();
					if (Checker.isStatement(childType)) break;
					readSingleVariableDeclaration(child, childType, allVarNamesMap, varTypesMap, allVarNamesList);
				}
				break;
			}
			
			codeAst = codeAst.getParent();
			if (codeAst == null) break;
		}
	}

	public static Map<String, String> readPublicStaticFields(File fileName) {
		Map<String, String> publicStaticFields = new HashMap<>();
		ITree tree = new ASTGenerator().generateTreeForJavaFile(fileName, TokenType.EXP_JDT);
		ITree classTree = null;
		List<ITree> children = tree.getChildren();
		for (ITree child : children) {
			if (Checker.isTypeDeclaration(child.getType())) {
				classTree = child;
				break;
			}
		}
		if (classTree != null) {
			children = classTree.getChildren();
			for (ITree child : children) {
				if (Checker.isFieldDeclaration(child.getType())) {
					List<ITree> subChildren = child.getChildren();
					boolean readVar = false;
					boolean isPrivate = false;
					boolean isStatic = false;
					String varType = null;
					String varName = null;
					for (ITree subChild : subChildren) {
						if (readVar) {
							if (!isStatic || isPrivate) break;
							varName = subChild.getChild(0).getLabel();
							if ("serialVersionUID".equals(varName)) break;
							publicStaticFields.put(varName, varType);
						} else if (!Checker.isModifier(subChild.getType())) {
							varType = ContextReader.readType(subChild.getLabel());
							readVar = true;
						} else {
							String modifier = subChild.getLabel();
							if ("private".equals(modifier)) {
								isPrivate = true;
							} else if ("static".equals(modifier)) {
								isStatic = true;
							}
						}
					}
						
				}
			}
		}
		return publicStaticFields;
	}
	
}
