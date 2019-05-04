package edu.lu.uni.serval.tbar.fixpatterns;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.lu.uni.serval.AST.ASTGenerator;
import edu.lu.uni.serval.AST.ASTGenerator.TokenType;
import edu.lu.uni.serval.jdt.tree.ITree;
import edu.lu.uni.serval.tbar.context.ContextReader;
import edu.lu.uni.serval.tbar.context.Method;
import edu.lu.uni.serval.tbar.fixtemplate.FixTemplate;
import edu.lu.uni.serval.tbar.utils.Checker;

/**
 * The method invocations are limited to all methods defined in the current program.
 * 
 * @author kui.liu
 *
 */
public abstract class AlterMethodInvocation extends FixTemplate {
	
	protected Map<ITree, Integer> suspMethodInvocations = new HashMap<>();
	ITree classDeclarationAst = null;
	String packageName = "";
	String className = null;
	
	protected List<MethodInvocationExpression> identifySuspiciousMethodInvocations() {
		ITree suspCodeTree = this.getSuspiciousCodeTree();
		ContextReader.readAllVariablesAndFields(suspCodeTree, allVarNamesMap, varTypesMap, allVarNamesList, this.sourceCodePath, null);
		
		if (this.classDeclarationAst == null) {
			readClassDeclaration(suspCodeTree);
		}
		if (this.classDeclarationAst == null) {
			return new ArrayList<MethodInvocationExpression>();
		}
		if (className == null) {
			readClassName(suspCodeTree);
		}
		if (className == null) {
			return new ArrayList<MethodInvocationExpression>();
		}
		readPackageName();
		if (packageName == null) {
//			return new ArrayList<MethodInvocationExpression>();
			packageName = "";
		}
		
		identifySuspiciousMethodInvocationExps(suspCodeTree);
		return identifySuspiciousMethodInvocations2();
	}

	private void identifySuspiciousMethodInvocationExps(ITree suspCodeTree) {
		int suspCodeTreeType = suspCodeTree.getType();
		if (Checker.isConstructorInvocation(suspCodeTreeType) || Checker.isSuperConstructorInvocation(suspCodeTreeType)) {
			// they are statements.
			suspMethodInvocations.put(suspCodeTree, 1);
		}
		
		List<ITree> children = suspCodeTree.getChildren();
		for (ITree child : children) {
			int childType = child.getType();
			if (Checker.isMethodInvocation(childType)) {
				if (Checker.isMethodInvocation(suspCodeTree.getType())) {
					int childIndex = suspCodeTree.getChildPosition(child);
					if (childIndex < (children.size() - 1)) {
						ITree lastChild = children.get(children.size() - 1);
						if (Checker.isSimpleName(lastChild.getType()) && lastChild.getLabel().startsWith("MethodName:")) {
							suspMethodInvocations.put(child, 3);
						} else {// It is a parameter of its parent method invocation.
							if (child.getChildren().isEmpty()){
								suspMethodInvocations.put(child, 1);
							} else {
								suspMethodInvocations.put(child.getChild(child.getChildren().size() - 1), 2);
							}
						}
					} else {// It is a parameter of its parent method invocation.
						if (child.getChildren().isEmpty()){
							suspMethodInvocations.put(child, 1);
						} else {
							suspMethodInvocations.put(child.getChild(child.getChildren().size() - 1), 2);
						}
					}
				} else if (child.getChildren().isEmpty()){
					suspMethodInvocations.put(child, 1);
				} else {
					suspMethodInvocations.put(child.getChild(child.getChildren().size() - 1), 2);
				}
				identifySuspiciousMethodInvocationExps(child);
			} else if (Checker.isSimpleName(childType)) {
				if (child.getLabel().startsWith("MethodName:")) {
//					boolean contained = false;
//					for (Map.Entry<ITree, Integer> entry : suspMethodInvocations.entrySet()) {
//						int size = entry.getKey().getChildren().size();
//						if (size == 0) continue;
//						if (entry.getKey().getChild(size - 1).equals(child)) {
//							contained = true;
//							break;
//						}
//					}
//					if (!contained) {
//						suspMethodInvocations.put(child, 2);
//					}
					identifySuspiciousMethodInvocationExps(child);
				}
			} else if (Checker.isClassInstanceCreation(childType)) {
				suspMethodInvocations.put(child, 1);
				identifySuspiciousMethodInvocationExps(child);
			} else if (Checker.isSuperMethodInvocation(childType)) {
				List<ITree> subChildren = child.getChildren();
				ITree subChild = subChildren.get(subChildren.size() - 1);
				suspMethodInvocations.put(subChild, 2);
				identifySuspiciousMethodInvocationExps(subChild);
			} else if (Checker.isStatement(childType)) {
				break;
			} else if (Checker.isComplexExpression(childType)) {
				identifySuspiciousMethodInvocationExps(child);
			}
		}
	}
	

	private boolean checkParameterTypes(List<String> paraTypeStrs, List<String> targetParaTypeList) {
		boolean matched = true;
		for (int i = 0, size = paraTypeStrs.size(); i < size; i ++) {
			String paraType = paraTypeStrs.get(i);
			String targetType = targetParaTypeList.get(i);
			matched = matchParameterType(paraType, targetType);
			if (!matched) break;
		}
		return matched;
	}
	
	protected boolean matchParameterType(String paraType, String targetType) {
		if (paraType.equals("Object")) {
			// fuzzy matching.
		} else if (paraType.equals(targetType)) {
		} else if ((paraType.equals("char") || paraType.equals("Character"))
				&& targetType.equals("char") || targetType.equals("Character")) {
		} else if ((paraType.equals("int") || paraType.equals("Integer"))
				&& targetType.equals("int") || targetType.equals("Integer")) {
		} else if (paraType.equalsIgnoreCase("boolean") && targetType.equalsIgnoreCase("boolean")) {
		} else if (paraType.equalsIgnoreCase("byte") && targetType.equalsIgnoreCase("byte")) {
		} else if (paraType.equalsIgnoreCase("short") && targetType.equalsIgnoreCase("short")) {
		} else if (paraType.equalsIgnoreCase("long") && targetType.equalsIgnoreCase("long")) {
		} else if (paraType.equalsIgnoreCase("float") && targetType.equalsIgnoreCase("float")) {
		} else if (paraType.equalsIgnoreCase("double") && targetType.equalsIgnoreCase("double")) {
		} else if (!paraType.contains("-" + targetType + "-")) {
		} else if (paraType.contains("," + targetType + ",")) {
		} else {
			return false;
		}
		return true;
	}

	/**
	 * Read the parameter types of the suspicious method invocation expression.
	 * 
	 * @param parameters
	 * @return Empty_ArrayList - zero parameter.
	 * 		   other - parameter types.
	 */
	private List<String> readMethodParameterTypes(List<ITree> parameters) {
		List<String> paraTypeStrs = new ArrayList<>();
		if (parameters == null || parameters.isEmpty()) {
			// no parameter.
		} else {
			for (ITree para : parameters) {
				paraTypeStrs.add(readParameterType(para));
			}
		}
		return paraTypeStrs;
	}
	
	private String readParameterType(ITree para) {
		int paraAstType = para.getType();
		String paraLabel = para.getLabel();
		if (Checker.isArrayAccess(paraAstType)) {
			return readArrayAccessParameterReturnType(para);
		} else if (Checker.isBooleanLiteral(paraAstType)){
			 return "boolean";
		} else if (Checker.isCastExpression(paraAstType)) {
			 return ContextReader.readType(para.getChild(0).getLabel());
		} else if (Checker.isCharacterLiteral(paraAstType)){
			 return "char";
		} else if (Checker.isClassInstanceCreation(paraAstType)) {
			if (Checker.isNewKeyword(para.getChild(0).getType())) {
				return ContextReader.readSimpleNameOfDataType(para.getChild(1).getLabel());
			} else {
				return ContextReader.readSimpleNameOfDataType(para.getChild(2).getLabel());
			}
		} else if (Checker.isFieldAccess(paraAstType)) {
			paraLabel = para.getChild(1).getLabel();
			String dataType = this.varTypesMap.get("this." + paraLabel);
			if(dataType!= null) return dataType;
			else {
//				ITree exp = para.getChild(0);
//				String type = readParameterType(exp);
//				if ("Object".equals(type)) {
//					return type;
//				} else {
//					// the field paraLabel. TODO
//					// how to get the class path of type.
//					return null;
//				}
				return "Object";
			}
		} else if (Checker.isInfixExpression(paraAstType)) {
			return infixExpressionReturnType(para);
		} else if (Checker.isInstanceofExpression(paraAstType)) {
			return "boolean";
		} else if (Checker.isMethodDeclaration(paraAstType)) {
		    // return type of method(...), var.method(...), 
			return "Object";// TODO
		} else if (Checker.isSimpleName(paraAstType)) {
			paraLabel = ContextReader.readVariableName(para);
			String dataType = varTypesMap.get(paraLabel);
			if (dataType == null) dataType = this.varTypesMap.get("this." + paraLabel);
			if (dataType == null) {
				// TODO
				return "Object";
			} else return dataType;
		} else if (Checker.isQualifiedName(paraAstType)) {
			String dataType = this.varTypesMap.get(paraLabel);
			if (dataType != null) {
				return dataType;
			} else {// TODO 
				return "Object";
			}
		} else if (Checker.isNullLiteral(paraAstType)){ // NullLiteral
			return "-int-short-byte-long-float-double-boolean-char-"; // it won't be one of this type.
//			return "Object"; // TODO
		} else if (Checker.isNumberLiteral(paraAstType)){ // NumberLiteral
			String lastChar = paraLabel.substring(paraLabel.length() - 1, paraLabel.length());
			if ("l".equalsIgnoreCase(lastChar)) {
				return "long";
			} else if ("f".equalsIgnoreCase(lastChar)) {
				return "float";
			} else if ("d".equalsIgnoreCase(lastChar)) {
				return "double";
			} else if (paraLabel.contains(".")) {
				return ",double,float,";
			} else { // Any possible number type.
				return ",int,Integer,long,Long,short,Short,char,Character,byte,Byte,float,Float,double,Double,";
			}
		} else if (Checker.isParenthesizedExpression(paraAstType)) {
			return readParameterType(para.getChild(0));
		} else if (Checker.isPostfixExpression(paraAstType)) {
			return ",int,Integer,long,Long,short,Short,char,Character,byte,Byte,float,Float,double,Double,"; //--, ++.
		} else if (Checker.isPrefixExpression(paraAstType)) {
			String op = para.getChild(0).getLabel();
			if ("!".equals(op)) {
				return "boolean";
			} else {// ~, -, +, --, ++.
				return ",int,Integer,long,Long,short,Short,char,Character,byte,Byte,float,Float,double,Double,";
			}
		} else if (Checker.isStringLiteral(paraAstType)){ // ThisExpression
			return "String";
		} else if (Checker.isSuperFieldAccess(paraAstType)) {
			paraLabel = para.getChildren().get(para.getChildren().size() - 1).getLabel();
			String dataType = this.varTypesMap.get("this." + paraLabel);
			return  dataType == null ? "Object" : dataType; // null seems impossible.
		} else if (Checker.isSuperMethodInvocation(paraAstType)) {
			return "Object";// TODO
		} else if (Checker.isThisExpression(paraAstType)){ 
			return className;
		} else {// Other Complex Expressions.
			// FIXME: Is it possible to get the data type of the return value of the complex expression?
			return "Object";
		}
	}

	private String readArrayAccessParameterReturnType(ITree para) {
		ITree exp = para.getChild(0);
		int i = 1;
		while (true) {
			if (!Checker.isArrayAccess(exp.getType())) break;
			exp = exp.getChild(0);
			i ++;
		}
		String dataType = readParameterType(exp);
		
		int index = dataType.indexOf("[");
		if (index > 0) dataType = dataType.substring(0, index);
		for (int a = 0; a < i; a ++) dataType += "[]";
		return dataType;
	}

	private String infixExpressionReturnType(ITree para) {
		String returnType;
		String op = para.getChild(1).getLabel();
		if ("&&".equals(op) || "||".equals(op) || "==".equals(op) || "!=".equals(op)
				|| "<".equals(op) || "<=".equals(op) || ">".equals(op) || ">=".equals(op)) {
			returnType = "boolean";
		} else if ("-".equals(op) || "*".equals(op) || "/".equals(op) || "%".equals(op)) {
			returnType = ",int,Integer,long,Long,short,Short,char,Character,byte,Byte,float,Float,double,Double,";
		} else if ("&".equals(op) || "|".equals(op) || "^".equals(op)
				|| ">>".equals(op) || "<<".equals(op) || ">>>".equals(op)) {
			// the integer types, long, int, short, char, and byte.
			returnType = ",int,Integer,long,Long,short,Short,char,Character,byte,Byte,";
		} else if ("+".equals(op)) {
			returnType = ",String,int,Integer,long,Long,short,Short,char,Character,byte,Byte,float,Float,double,Double,";
		} else {
			// TODO: +
			returnType = "Object";
		}
		return returnType;
	}
	

	/**
	 * Read the class declaration AST of the suspicious code.
	 * 
	 * @param suspCodeTree
	 */
	private void readClassDeclaration(ITree suspCodeTree) {
		ITree parent = suspCodeTree.getParent();
		while (true) {
			if (Checker.isTypeDeclaration(parent.getType())) {
				this.classDeclarationAst = parent;
			}
			parent = parent.getParent();
			if (parent == null) break;
		}
	}
	
	/**
	 * Read the class name of the suspicious code.
	 * @param suspCodeTree
	 */
	private void readClassName(ITree suspCodeTree) {
		if (this.classDeclarationAst == null) {
			readClassDeclaration(suspCodeTree);
		}
		if (this.classDeclarationAst == null) {
			// FIXME non-type declaration file.
			className = null;
			return;
		}
		List<ITree> classChildren = this.classDeclarationAst.getChildren();
		for (ITree classChild : classChildren) {
			if (Checker.isSimpleName(classChild.getType())) {
				className = classChild.getLabel().substring(10);
				break;
			}
		}
	}
	
	/**
	 * Read the package name of the suspicious code.
	 */
	private void readPackageName() {
		ITree parent = this.classDeclarationAst.getParent();
		while (true) {
			ITree packageDeclaration = parent.getChild(0);
			if (Checker.isPackageDeclaration(packageDeclaration.getType())) {
				this.packageName = packageDeclaration.getLabel();
				break;
			}
			parent = parent.getParent();
			if (parent == null) break;
		}
	}
	
	public class MethodInvocationExpression {
		
		/*
		 * PackageName,
		 * ClassName,
		 * ReturnType,
		 * MethodName,
		 * Parameter Types.
		 */
		
		private String codePath;
		private List<String> possibleReturnTypes = new ArrayList<>(); //"=CONSTRUCTOR="
		private String methodName;
		private List<String> possibleParameterTypes;
		private List<List<String>> parameterTypes = new ArrayList<>();
		private ITree codeAst;
		private Map<String, List<String>> couldBeReplacedMethods = null; // replace method name.
		private List<ITree> differentParaMethods = null;  // add or delete parameter(s);
		private List<Method> canBeReplacedMethods = null; // replace method name;
		private List<Method> diffParameterMethods = null; // add or delete parameter(s);

		public String getCodePath() {
			return codePath;
		}

		public void setCodePath(String codePath) {
			this.codePath = codePath;
		}

		public List<String> getPossibleReturnTypes() {
			return possibleReturnTypes;
		}

		public void setPossibleReturnTypes(List<String> possibleReturnTypes) {
			this.possibleReturnTypes = possibleReturnTypes;
		}

		public String getMethodName() {
			return methodName;
		}

		public void setMethodName(String methodName) {
			this.methodName = methodName;
		}

		public List<String> getPossibleParameterTypes() {
			return possibleParameterTypes;
		}

		public void setPossibleParameterTypes(List<String> possibleParameterTypes) {
			this.possibleParameterTypes = possibleParameterTypes;
		}

		public List<List<String>> getParameterTypes() {
			return parameterTypes;
		}

		public ITree getCodeAst() {
			return codeAst;
		}

		public void setCodeAst(ITree codeAst) {
			this.codeAst = codeAst;
		}

		public Map<String, List<String>> getCouldBeReplacedMethods() {
			return couldBeReplacedMethods;
		}

		public void setCouldBeReplacedMethods(Map<String, List<String>> couldBeReplacedMethods) {
			this.couldBeReplacedMethods = couldBeReplacedMethods;
		}

		public List<ITree> getDifferentParaMethods() {
			return differentParaMethods;
		}

		public void setDifferentParaMethods(List<ITree> differentParaMethods) {
			this.differentParaMethods = differentParaMethods;
		}

		public List<Method> getCanBeReplacedMethods() {
			return canBeReplacedMethods;
		}

		public void setCanBeReplacedMethods(List<Method> canBeReplacedMethods) {
			this.canBeReplacedMethods = canBeReplacedMethods;
		}

		public List<Method> getDiffParameterMethods() {
			return diffParameterMethods;
		}

		public void setDiffParameterMethods(List<Method> diffParameterMethods) {
			this.diffParameterMethods = diffParameterMethods;
		}
	}
	
	private Map<String, List<String>> couldBeReplacedMethods = null; // replace method name.
	private List<ITree> differentParaMethods = null;   // add or delete parameter(s);
	
	/**
	 * Read the information of suspicious method invocations.
	 * @return
	 */
	protected List<MethodInvocationExpression> identifySuspiciousMethodInvocations2() {
		List<MethodInvocationExpression> suspMethodInvocations = new ArrayList<>();
		
		for (Map.Entry<ITree, Integer> entry : this.suspMethodInvocations.entrySet()) {
			ITree suspMethodInv = entry.getKey();
			ITree methodNameNode = suspMethodInv;
			int type = suspMethodInv.getType();
			
			// Read method name and parameters.
			// Read the possible return types of the method.
			ITree rootTree = null;
			String varType = null;
			List<ITree> parameters = methodNameNode.getChildren();
			String methodName = null;
			if (Checker.isConstructorInvocation(type)) {
				methodName = "this=CONSTRUCTOR=";
				varType = "this=CONSTRUCTOR=";
				rootTree = this.classDeclarationAst;
			} else if (Checker.isSuperConstructorInvocation(type)) {
				methodName = "super=CONSTRUCTOR=";
				varType = "this+Super=CONSTRUCTOR=";
				rootTree = this.classDeclarationAst;
			} else if (Checker.isClassInstanceCreation(type)) {
				// new A().new B(). TODO
				int size = parameters.size();
				if (Checker.isAnonymousClassDeclaration(parameters.get(size - 1).getType())) continue;
				int i = 0;
				if (Checker.isNewKeyword(parameters.get(0).getType())) {
					methodName = parameters.get(1).getLabel(); 
					i = 2;
				} else {
					methodName = parameters.get(2).getLabel(); 
					i = 3;
				}
				parameters = parameters.subList(i, size);
				varType = ContextReader.readSimpleNameOfDataType(methodName) + "=CONSTRUCTOR=";
				rootTree = this.classDeclarationAst;
			} else {
//				int methodType = entry.getValue();
//				if (methodType == 2) {
//					List<ITree> children = suspMethodInv.getChildren();
//					methodNameNode = children.get(children.size() - 1);
//					parameters = methodNameNode.getChildren();
//				}
				methodName = methodNameNode.getLabel().substring(11);
				methodName = methodName.substring(0, methodName.indexOf(":"));
				
				if (Checker.isSuperMethodInvocation(methodNameNode.getParent().getType())) {
//					varType = "this+Super";
//					rootTree = this.classDeclarationAst;
				} else {
					ITree parentCodeAst = methodNameNode.getParent();
					int indexPos = parentCodeAst.getChildPosition(methodNameNode);
					if (indexPos == 0) { // the method belongs to the current class or its ancestral classes.
						rootTree = this.classDeclarationAst;
						varType = "this";
					} else { // the method belongs to the class of the return data type of its previous peer AST node..
						ITree prePeerCodeAst = parentCodeAst.getChild(indexPos - 1);
						/*
						 * Exp.method(...)
						 * 
						 * The previous peer AST node (Exp) can be:
						 * 		a. field
						 * 		b. qualified name.
						 * 		c. method invocation.
						 * 		d. other complex expressions.
						 */
						int prePeerCodeAstType = prePeerCodeAst.getType();
						if (Checker.isSimpleName(prePeerCodeAstType)) { // a variable.
							String varName = ContextReader.readVariableName(prePeerCodeAst);
							varType = this.varTypesMap.get(varName);
							if (varType == null) varType = this.varTypesMap.get("this." + varName);
							if (varType == null) varType = varName;
							rootTree = this.classDeclarationAst;
							/// varType will be treated as a class name. TODO
						} else if (Checker.isQualifiedName(prePeerCodeAstType)) { 
							// QualifiedName: T.var.get()
							String dataType = prePeerCodeAst.getLabel(); 
							if (this.varTypesMap.containsKey(dataType)) {
								varType = this.varTypesMap.get(dataType);
//								rootTree = this.classDeclarationAst;
								/// varType will be treated as a class name. TODO
							} else {
								int firstPointIndex = dataType.indexOf(".");
								int lastPointIndex = dataType.lastIndexOf(".");
								String fieldName = dataType.substring(lastPointIndex + 1); // field name.
								if (firstPointIndex == lastPointIndex) {
									dataType = dataType.substring(0, firstPointIndex); // Class Name.
									String dataTypeFile = this.identifyJavaFilePath(this.classDeclarationAst, dataType);
									if (dataTypeFile != null) { // field data type.
										if (dataTypeFile.endsWith("INNER-CLASS")) {
											dataTypeFile = dataTypeFile.substring(0, dataTypeFile.length() - 11);
											rootTree = new ASTGenerator().generateTreeForJavaFile(dataTypeFile, TokenType.EXP_JDT);
											rootTree = findClassTree(rootTree, dataType);
										} else {
											rootTree = new ASTGenerator().generateTreeForJavaFile(dataTypeFile, TokenType.EXP_JDT);
										}
										varType = readFieldType(rootTree, dataType, fieldName);
									}
								} else {
									String packageName = dataType;
									boolean isExisted = false;
									while (true) {
										if (new File(this.sourceCodePath + packageName.replace(".", "/") + ".java").exists()) {
											isExisted = true;
											break;
										}
										lastPointIndex = packageName.lastIndexOf(".");
										if (lastPointIndex == -1) break;
										packageName = packageName.substring(0, lastPointIndex);
									}
									if (!isExisted) {
										// TODO it is not defined in the buggy program.
										continue;
									}
									
									if (packageName.endsWith(fieldName)) {
										// defined static method.
										rootTree = new ASTGenerator().generateTreeForJavaFile(this.sourceCodePath + packageName.replace(".", "/") + ".java", TokenType.EXP_JDT);
										// TODO
									} else {
										// defined field.
										rootTree = new ASTGenerator().generateTreeForJavaFile(this.sourceCodePath + packageName.replace(".", "/") + ".java", TokenType.EXP_JDT);
										rootTree = findClassTree(rootTree, dataType);
										if (rootTree == null) continue;
										varType = readFieldType(rootTree, dataType, fieldName);
										// TODO
									}
								}
							}
						} else if (Checker.isThisExpression(prePeerCodeAstType)) {
							// this.method();
//							rootTree = this.classDeclarationAst;
							varType = "this";
						} else if (Checker.isFieldAccess(prePeerCodeAstType)) {
							// FieldAccess: this.var.get() or method().var.get().
							List<ITree> subChildren = prePeerCodeAst.getChildren();
							String dataType = subChildren.get(subChildren.size() - 1).getLabel();
							varType = this.varTypesMap.get("this." + dataType);
							if (varType == null) {
								// TODO: method1().var.method2()
								continue;
							} else {
//								rootTree = this.classDeclarationAst;
							}
						} else if (Checker.isSuperFieldAccess(prePeerCodeAstType)) {
							// SuperFieldAccess: super.var.get();
							List<ITree> subChildren = prePeerCodeAst.getChildren();
							String dataType = subChildren.get(subChildren.size() - 1).getLabel();
							varType = this.varTypesMap.get("this." + dataType);
//							rootTree = this.classDeclarationAst;
						} else if (Checker.isMethodInvocation(prePeerCodeAstType)) {
							// TODO the return type of the previous peer method invocation.
							continue;
						} else if (Checker.isArrayAccess(prePeerCodeAstType)) {
							// ArrayAccess: arrray[].method()
							ITree arrayVarTree = prePeerCodeAst.getChild(0);
							while (true) {
								if (!Checker.isArrayAccess(arrayVarTree.getType())) break;
								arrayVarTree = arrayVarTree.getChild(0);
							}
							int arrayVarType = arrayVarTree.getType();
							if (Checker.isSimpleName(arrayVarType)) {
								String varName = ContextReader.readVariableName(prePeerCodeAst);
								if (varName == null) continue; // It seems impossible.
								varType = this.varTypesMap.get(varName);
								if (varType == null) varType = this.varTypesMap.get("this." + varName);
								if (varType == null) continue; // It seems impossible as well.
//								rootTree = this.classDeclarationAst;
							} else if (Checker.isQualifiedName(arrayVarType)) {
							} else if (Checker.isFieldAccess(arrayVarType)) {
							} else if (Checker.isSuperFieldAccess(arrayVarType)) {
							} else {// MethodInvocation or SuperMethodInvocation
								// ParenthesizedExpression --> (CastExpression)
								// TODO
							}
						} else if (Checker.isParenthesizedExpression(prePeerCodeAstType)) {
							// ParenthesizedExpression: ((T) exp).method.
							if (prePeerCodeAst.getChildren().size() == 1 && Checker.isCastExpression(prePeerCodeAst.getChild(0).getType())) {
								varType = prePeerCodeAst.getChild(0).getChild(0).getLabel();
								varType = ContextReader.readType(varType);
								// TODO
							}
						} else if (Checker.isClassInstanceCreation(prePeerCodeAstType)) {
							// ClassInstanceCreation: new A().method().
						} else if (Checker.isSuperMethodInvocation(prePeerCodeAstType)) {
							// SuperMethodInvocation
						} else { // StringLiteral, 
							// FIXME
							continue;
						}
					}
				}
			}
			if (rootTree == null || varType == null) continue;
			
			// Read parameter data types.
			List<String> paraTypeStrs = readMethodParameterTypes(parameters);
			if (paraTypeStrs == null) continue; // Generate ERROR when reading its parameter types.
			
			// Identify possible return types of the method invocations.
			List<String> possibleReturnTypes = null;
			String methodClassPath = null;
			Map<List<String>, String> map = identifyPossibleReturnTypes(rootTree, varType, methodName, paraTypeStrs);
			if (map != null) {
				for (Map.Entry<List<String>, String> subEntry : map.entrySet()) {
					possibleReturnTypes = subEntry.getKey();
					methodClassPath = subEntry.getValue();
					break;
				}
			}
			
			if (possibleReturnTypes != null && !possibleReturnTypes.isEmpty()) {
				MethodInvocationExpression mi = new MethodInvocationExpression();
				mi.setCodePath(methodClassPath);
				mi.setMethodName(methodName);
				mi.setCodeAst(methodNameNode);
				mi.setCouldBeReplacedMethods(this.couldBeReplacedMethods);
				mi.setDifferentParaMethods(this.differentParaMethods);
				for (String possibleReturnType : possibleReturnTypes) {
					String[] elements = possibleReturnType.split("\\+");
					mi.getPossibleReturnTypes().add(elements[0]);
					paraTypeStrs = new ArrayList<>();
					for (int i = 1, length = elements.length; i < length; i = i + 2) {
						paraTypeStrs.add(elements[i]);
					}
					mi.getParameterTypes().add(paraTypeStrs);
				}
				suspMethodInvocations.add(mi);
				this.couldBeReplacedMethods = null;
				this.differentParaMethods = null;
			} else {
				// TODO
				if (Checker.isClassInstanceCreation(type)) {
					MethodInvocationExpression mi = new MethodInvocationExpression();
					mi.setCodePath(methodClassPath);
					mi.setMethodName(methodName);
					mi.setCodeAst(methodNameNode);
					mi.setCouldBeReplacedMethods(this.couldBeReplacedMethods);
					mi.setDifferentParaMethods(this.differentParaMethods);
					if (possibleReturnTypes != null) {
						for (String possibleReturnType : possibleReturnTypes) {
							String[] elements = possibleReturnType.split("\\+");
							mi.getPossibleReturnTypes().add(elements[0]);
							paraTypeStrs = new ArrayList<>();
							for (int i = 1, length = elements.length; i < length; i = i + 2) {
								paraTypeStrs.add(elements[i]);
							}
							mi.getParameterTypes().add(paraTypeStrs);
						}
					}
					suspMethodInvocations.add(mi);
				}
			}
		}
		return suspMethodInvocations;
	}
	
	private String readFieldType(ITree rootTree, String dataType, String fieldName) {
		String varType = null;
		List<ITree> children = rootTree.getChildren();
		children = children.get(children.size() - 1).getChildren();
		for (ITree child : children) {
			if (Checker.isFieldDeclaration(child.getType())) { // Field declaration
				List<ITree> subChildren = child.getChildren();
				boolean isFound = false;
				for (int i = 1, size = subChildren.size(); i < size; i ++) {
					ITree varDeclaration = subChildren.get(i);
					if (Checker.isVariableDeclarationFragment(varDeclaration.getType())) {
						if (varType == null) varType = subChildren.get(i - 1).getLabel();
						if (varDeclaration.getChild(0).getLabel().equals(fieldName)) {
							isFound = true;
							break;
						}
					}
				}
				if (isFound) break;
				varType = null;
			}
		}
		return varType;
	}
	
	/**
	 * Identify the java file path of a data type.
	 * 
	 * @param classDeclarationAst
	 * @param varType
	 * @return
	 */
	private String identifyJavaFilePath(ITree classDeclarationAst, String varType) {
		while (true) {
			if (classDeclarationAst == null) break;
			if (!Checker.isCompilationUnit(classDeclarationAst.getType())) {
				classDeclarationAst = classDeclarationAst.getParent();
			} else break;
		}
		if (classDeclarationAst == null) return null;
		
		List<ITree> rootTreeChildren = classDeclarationAst.getChildren();
		String path = null;
		String packageName = "";
		List<String> paths = new ArrayList<>();
		for (ITree child : rootTreeChildren) {
			int childType = child.getType();
			String childLabel = child.getLabel();
			if (Checker.isPackageDeclaration(childType)) { // package name.
				packageName = child.getLabel().replace(".", "/");
				path = this.sourceCodePath + packageName + "/" + varType + ".java";
				if (new File(path).exists()) break;
				else {
					path = null;
					paths.add(this.sourceCodePath + child.getLabel().replace(".", "/") + "/");
				}
			} else if (Checker.isImportDeclaration(childType)) { // import declarations.
				if (childLabel.endsWith("." + varType)) {
					path = this.sourceCodePath + child.getLabel().replace(".", "/") + ".java";
					if (!new File(path).exists()) path = null;
					break;
				} else {
					paths.add(this.sourceCodePath + child.getLabel().replace(".", "/") + "/");
				}
			} else if (Checker.isTypeDeclaration(childType)) {
				// Check its super class.
				String label = child.getLabel();
				int index = label.indexOf("@@SuperClass:");
				if (index > 0) {
					label = label.substring(index + 13);
					index = label.indexOf("@@Interface:");
					if (index > 0) {
						index = index - 2;
					} else index = label.length() - 2;
					String superClassName = ContextReader.readSimpleNameOfDataType(label.substring(0, index));
					if (!paths.isEmpty()) {
						String packagePath = paths.get(0);
						String superClassPath = packagePath + superClassName + ".java";
						if (!new File(superClassPath).exists()) {
							superClassPath = null;
							superClassName = "/" + superClassName;
							for (String p : paths) {
								if (p.endsWith(superClassName)) {
									superClassPath = packagePath + ".java";
									break;
								}
							}
							if (superClassPath != null && !new File(superClassPath).exists()) superClassPath = null;
						}
						
						if (superClassPath != null) {
							path = identifyJavaFilePath(new ASTGenerator().generateTreeForJavaFile(superClassPath, TokenType.EXP_JDT), varType);
						} else path = null;
					}
				}
				
				if (path == null) { // inner class.
					String className = child.getLabel();
					className = className.substring(className.indexOf("ClassName:") + 10);
					className = className.substring(0, className.indexOf(", "));
					List<ITree> children = child.getChildren();
					packageName += "/" + className + ".java";
					for (ITree tree : children) {
						if (Checker.isTypeDeclaration(tree.getType()) && tree.getLabel().contains("ClassName:" + varType + ", ")) {
							path = packageName + "INNER-CLASS";
							break;
						}
					}
					if (path != null) break;
				} else break;
			}
		}
		return path;
	}
	
	/**
	 * 
	 * @param classDeclarationAst
	 * @param varType
	 * @param methodName
	 * @param paraTypeStrs
	 * @return
	 */
	private Map<List<String>, String> identifyPossibleReturnTypes(ITree classDeclarationAst, String varType, String methodName, List<String> paraTypeStrs) {
		String path = null;
		int constructorIndex = varType.indexOf("=CONSTRUCTOR=");
		boolean isConstructor = false;
		boolean isSuperClass = false;
		if (constructorIndex > 0) {
			isConstructor = true;
			varType = varType.substring(0, constructorIndex);
		}
		int superIndex = varType.indexOf("+Super");
		if (superIndex > 0) {
			varType = varType.substring(0, superIndex);
			isSuperClass = true;
		}
		if ("this".equals(varType)) {
			path = this.sourceCodePath + this.packageName.replace(".", "/") + "/" + this.className + ".java";
		} else {
			path = identifyJavaFilePath(classDeclarationAst, varType);
			if (path == null) return null;
			if (path.endsWith("INNER-CLASS")) {
				path = path.substring(0, path.length() - 11);
			} else {
				
			}
			if (!new File(path).exists()) return null;
			classDeclarationAst = new ASTGenerator().generateTreeForJavaFile(path, TokenType.EXP_JDT);
			List<ITree> children = classDeclarationAst.getChildren();
			classDeclarationAst = children.get(children.size() - 1);
		}
		
		return identifyPossibleReturnTypes(classDeclarationAst, methodName, paraTypeStrs, path, isConstructor, isSuperClass);
	}
	
	/**
	 * 
	 * @param classDeclarationAst
	 * @param methodName
	 * @param paraTypeStrs
	 * @return
	 */
	private Map<List<String>, String> identifyPossibleReturnTypes(ITree classDeclarationAst, String methodName, List<String> paraTypeStrs, 
			String path, boolean isConstructor, boolean isSuperClass) {
		couldBeReplacedMethods = new HashMap<>(); // replace method name.
		differentParaMethods = new ArrayList<>();   // add or delete parameter(s);
		List<String> possibleReturnTypes = new ArrayList<>();
		String superConstructor = "";
		if (!isSuperClass) {
			List<ITree> children = classDeclarationAst.getChildren();
			for (ITree child : children) {
				if (Checker.isMethodDeclaration(child.getType())) {
					// Match method name.
					String label = child.getLabel();
					int indexOfMethodName = label.indexOf("MethodName:");
					int indexOrPara = label.indexOf("@@Argus:");
					String currentMethodName = label.substring(indexOfMethodName + 11, indexOrPara - 2);
					
					// Match parameter data types.
					String paraStr = label.substring(indexOrPara + 8);
					if (paraStr.startsWith("null")) {
						paraStr = null;
					} else {
						int indexExp = paraStr.indexOf("@@Exp:");
						if (indexExp > 0) paraStr = paraStr.substring(0, indexExp);
					}
					
					// Read return type.
					String returnType = label.substring(label.indexOf("@@") + 2, indexOfMethodName - 2);
					int index = returnType.indexOf("@@tp:");
					if (index > 0) returnType = returnType.substring(0, index - 2);
					
					if (isConstructor) {
						if (!"=CONSTRUCTOR=".equals(returnType)) {// Constructor.
							continue;
						}
//					} else if (!currentMethodName.equals(methodName)) continue;
					} else if ("=CONSTRUCTOR=".equals(returnType)) continue;
					else returnType = ContextReader.readType(returnType);
		
					// Match possible return types.
					if (paraTypeStrs.isEmpty()) {
						if (paraStr == null) {
							if (currentMethodName.equals(methodName)) {
								possibleReturnTypes.add(returnType);
							} else { // methods with same parameter and same(possible) return type
								List<String> methodNamesList = couldBeReplacedMethods.get(returnType);
								if (methodNamesList == null) methodNamesList = new ArrayList<String>();
								methodNamesList.add(currentMethodName);
								couldBeReplacedMethods.put(returnType, methodNamesList);
							}
						} else if (currentMethodName.equals(methodName)) {
							differentParaMethods.add(child);
						}
					} else if (paraStr != null) {
						List<String> targetParaTypeList = ContextReader.parseMethodParameterTypes(paraStr, "\\+");
						if (targetParaTypeList.size() == paraTypeStrs.size()) {
							boolean matched = checkParameterTypes(paraTypeStrs, targetParaTypeList);
							if (matched) {
								if (currentMethodName.equals(methodName)) {
									possibleReturnTypes.add(returnType + "+" + paraStr);
								} else { // methods with same parameter and same(possible) return type
									List<String> methodNamesList = couldBeReplacedMethods.get(returnType);
									if (methodNamesList == null) methodNamesList = new ArrayList<String>();
									methodNamesList.add(currentMethodName);
									couldBeReplacedMethods.put(returnType, methodNamesList);
								}
							}
						} else if (currentMethodName.equals(methodName)) {
							differentParaMethods.add(child);
						}
					} else if (currentMethodName.equals(methodName)) {
						differentParaMethods.add(child);
					}
				}
			}
		} else if (isConstructor) {
			superConstructor = "=CONSTRUCTOR=";
		}
		
		
		if (possibleReturnTypes.isEmpty()) {
			String label = classDeclarationAst.getLabel();
			int index = label.indexOf("@@SuperClass:");
			if (index > 0) {
				String superClassName = label.substring(index + 13);
				index = superClassName.indexOf("@@Interface:");
				if (index < 0) index = superClassName.length();
				superClassName = superClassName.substring(0, index - 2) + superConstructor;
				Map<List<String>, String> map = identifyPossibleReturnTypes(classDeclarationAst, superClassName, methodName, paraTypeStrs);
				if (map != null) {
					possibleReturnTypes = map.entrySet().iterator().next().getKey();
				}
			}
		}
		Map<List<String>, String> map = new HashMap<>();
		map.put(possibleReturnTypes, path);
		return map;
	}
	
	private ITree findClassTree(ITree rootTree, String dataType) {
		List<ITree> children = rootTree.getChildren();
		for (ITree child : children) {
			if (Checker.isTypeDeclaration(child.getType())) {
				if (child.getLabel().contains("ClassName:" + dataType + ", ")) {
					return child;
				} else {
					ITree tree = findClassTree(child, dataType);
					if (tree != null) return tree;
				}
			}
		}
		return null;
	}
	
}
