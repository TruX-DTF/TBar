package edu.lu.uni.serval.tbar.code.analyser;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.lu.uni.serval.AST.ASTGenerator;
import edu.lu.uni.serval.AST.ASTGenerator.TokenType;
import edu.lu.uni.serval.jdt.tree.ITree;
import edu.lu.uni.serval.tbar.context.ContextReader;
import edu.lu.uni.serval.tbar.context.Field;
import edu.lu.uni.serval.tbar.context.Method;
import edu.lu.uni.serval.tbar.context.ModifierType;
import edu.lu.uni.serval.tbar.utils.Checker;
import edu.lu.uni.serval.tbar.utils.SetUtils;

/**
 * 
 * @author kui.liu
 *
 */
public class JavaCodeFileParser {
	
	private List<String> classNamesInSamePackage = new ArrayList<>();
	private String packageName = null;
	private Map<ITree, String> classNames = new HashMap<>();
	public List<String> importDeclarations = new ArrayList<>();
	
	// Key: class path, List<T>: objects.
	public Map<String, List<String>> importMaps = new HashMap<>();
	public Map<String, List<Method>> methods = new HashMap<>();
	public Map<String, List<Field>> fields = new HashMap<>();
	public Map<String, String> superClassNames = new HashMap<>();

	public JavaCodeFileParser(File javaFile) {
		File[] files = javaFile.getParentFile().listFiles();
		for (File file : files) {
			String fileName = file.getName();
			if (file.isDirectory()) continue;
			if (fileName.equals(javaFile.getName())) continue;
			fileName = fileName.substring(0, fileName.length() - 5);
			classNamesInSamePackage.add(fileName);
		}
		ITree rootTree = new ASTGenerator().generateTreeForJavaFile(javaFile, TokenType.EXP_JDT);
		identifyInfo(rootTree, null);
	}
	
	private void identifyInfo(ITree tree, ITree classDeclarationTree) {
		List<ITree> children = tree.getChildren();
		for (ITree child : children) {
			int astNodeType = child.getType();
			if (Checker.isMethodDeclaration(astNodeType)) { // MethodDeclaration.
				String className = this.classNames.get(classDeclarationTree);
				readMethodInfo(child, className);
			} else if (Checker.isFieldDeclaration(astNodeType)) {
				readField(child, this.classNames.get(classDeclarationTree));
			} else if (Checker.isImportDeclaration(astNodeType)) {
				String importDecl = child.getLabel().trim();
				importDeclarations.add(importDecl);
			} else if (Checker.isPackageDeclaration(astNodeType)) {
				packageName = child.getLabel().trim();
				int index = packageName.lastIndexOf("package ");
				if (index >= 0) packageName = packageName.substring(index + 8);
				for (String classFileName : classNamesInSamePackage) {
					importDeclarations.add(packageName + "." + classFileName);
				}
			} else if (Checker.isTypeDeclaration(astNodeType)) { // TypdeDeclaration.
				readClassName(child);
				if (importMaps.isEmpty()) {
					importMaps.put(this.classNames.get(child), importDeclarations);
				}
				identifyInfo(child, child);
			} else {
			}
 		}
	}
	
	private void readField(ITree fieldDeclaration, String classPathAndName) {
		List<ITree> children = fieldDeclaration.getChildren();
		boolean readVar = false;
		boolean isStatic = false;
		ModifierType mt = ModifierType.PROTECTED;
		String varType = null;
		String varName = null;
		String className = classPathAndName.substring(classPathAndName.lastIndexOf(".") + 1);
		for (ITree child : children) {
			if (readVar) {
				if (isStatic) {
					varName = child.getChild(0).getLabel();
					if ("serialVersionUID".equals(varName)) {
						varName = null;
						break;
					}
					if (mt != ModifierType.PRIVATE) {
						Field field = new Field(classPathAndName, mt, varType, varName, isStatic);
						SetUtils.addToMap(this.fields, classPathAndName, field);
						varName = className + "." + varName;
					}
				} else {
					varName = "this." + child.getChild(0).getLabel();
				}
				break;
			} else if (!Checker.isModifier(child.getType())) {
				varType = ContextReader.readType(child.getLabel());
				readVar = true;
			} else {
				String modifier = child.getLabel();
				if ("public".equals(modifier)) {
					mt = ModifierType.PUBLIC;
				} else if ("private".equals(modifier)) {
					mt = ModifierType.PRIVATE;
				} else if ("static".equals(modifier)) {
					isStatic = true;
				}
			}
		}
		
		if (varName != null) {
			Field field = new Field(classPathAndName, mt, varType, varName, isStatic);
			SetUtils.addToMap(this.fields, classPathAndName, field);
		}
	}

	private void readMethodInfo(ITree methodDeclarationTree, String className) {
		String methodNameInfo = methodDeclarationTree.getLabel();
		int indexOfMethodName = methodNameInfo.indexOf("MethodName:");
		String methodName = methodNameInfo.substring(indexOfMethodName);
		methodName = methodName.substring(11, methodName.indexOf(", "));
		
		if ("main".equals(methodName)) return;
		
		boolean isConstructor = false;
		String returnType = ContextReader.readReturnType(methodDeclarationTree);

		if ("=CONSTRUCTOR=".equals(returnType)) {// Constructor.
			isConstructor = true;
		}
		
		List<String> parameterTypes = this.readParameterTypes(methodNameInfo);
		
		ModifierType mm = readMethodModifier(methodDeclarationTree);
		
		Method m = new Method(className, mm, returnType, methodName, parameterTypes, isConstructor);
		SetUtils.addToMap(this.methods, className, m);
	}
	
	private ModifierType readMethodModifier(ITree methodDeclarationTree) {
		List<ITree> children = methodDeclarationTree.getChildren();
		for (ITree child : children) {
			if (Checker.isModifier(child.getType())) {
				if ("public".equals(child.getLabel())) {
					return ModifierType.PUBLIC;
				} else if ("protected".equals(child.getLabel())) {
					return ModifierType.PROTECTED;
				} else if ("private".equals(child.getLabel())) {
					return ModifierType.PRIVATE;
				}
			} else break;
		}
		return ModifierType.PROTECTED;
	}

	private List<String> readParameterTypes(String methodDeclarationLabel) {
		List<String> parameterTypes = new ArrayList<>();
		if (methodDeclarationLabel.endsWith("@@Argus:null")) {
			return parameterTypes;
		} else {
			String argus = methodDeclarationLabel.substring(methodDeclarationLabel.indexOf("@@Argus:") + 8, methodDeclarationLabel.length() - 1).replace(" ", "");
			int expIndex = argus.indexOf("@@Exp:");
			if (expIndex > 0) {
				argus = argus.substring(0, expIndex - 1);
			}
			if (argus.endsWith("@@Argus:null")) {
				return parameterTypes;
			}
			String[] argusArray = argus.split("\\+");
			for (int index = 0, length = argusArray.length; index < length; index = index + 2) {
				String arguType = ContextReader.readType(argusArray[index]);
				parameterTypes.add(arguType);
			}
			
			return parameterTypes;
		}
	}
	
	private void readClassName(ITree classNameTree) {
		String className = ContextReader.readClassName(classNameTree);
		
		ITree parentTree = classNameTree.getParent();
		String parentClassName = this.classNames.get(parentTree);
		if (parentTree == null) {
			className = (this.packageName == null ? "" : this.packageName + ".") + className;
		} else {
			if (parentClassName == null) {
				className = (this.packageName == null ? "" : this.packageName + ".") + className;
			} else {
				className = parentClassName + "." + className;
				this.importDeclarations.add(className);
			}
		}
		this.classNames.put(classNameTree, className);
		
		String classNameLabel = classNameTree.getLabel();
		int indexOfSuperClassName = classNameLabel.indexOf("@@SuperClass:");
		if (indexOfSuperClassName > 0) {
			indexOfSuperClassName += 13;
			classNameLabel = classNameLabel.substring(indexOfSuperClassName);
			String superClassName = classNameLabel.substring(0, classNameLabel.indexOf(", "));
			String superClassName_ = "." + ContextReader.readSimpleNameOfDataType(superClassName);
			String superClassPackage = null;
			for (String importDecl : this.importDeclarations) {
				if (importDecl.endsWith(superClassName_)) {
					superClassPackage = importDecl;
					break;
				}
			}
			if (superClassPackage == null) {
				superClassName = (this.packageName == null ? "" : this.packageName + ".") + superClassName;
			} else {
				superClassName = superClassPackage;
			}
			this.superClassNames.put(className, superClassName);
		}
	}

}
