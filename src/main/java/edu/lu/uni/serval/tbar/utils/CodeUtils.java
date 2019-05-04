package edu.lu.uni.serval.tbar.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.TypeDeclaration;

public class CodeUtils {

	public static String getPackageName(String code) {
		String[] lines = code.split("\n");
		for (String line : lines) {
			if (line.startsWith("package ")) {
				return line.split(" ")[1].substring(0, line.split(" ")[1].length() - 1).trim();
			}
		}
		return "";
	}

	public static String getClassNameOfImportClass(String code, String className) {
		List<String> packages = FileUtils.getPackageImportFromCode(code);
		for (String packageName : packages) {
			if (getClassNameFromPackage(packageName).equals(className)) {
				if (packageName.startsWith("import")) {
					packageName = packageName.substring(packageName.indexOf(" "));
				}
				if (packageName.endsWith(";")) {
					packageName = packageName.substring(0, packageName.length() - 1);
				}
				return packageName;
			}
		}
		return "";
	}

	private static String getClassNameFromPackage(String packageName) {
		String name = packageName.substring(packageName.lastIndexOf(".") + 1);
		if (name.endsWith(";")) {
			return name.substring(0, name.length() - 1);
		}
		return name;
	}

	public static List<Integer> getSingleMethodLine(String code, String methodName) {
		return getSingleMethodLine(code, methodName, -1);
	}

	private static List<Integer> getSingleMethodLine(String code, String methodName, int errorLine) {
		ASTParser parser = ASTParser.newParser(AST.JLS8);
		parser.setSource(code.toCharArray());
		CompilationUnit unit = (CompilationUnit) parser.createAST(null);
		List<MethodDeclaration> methodDec = getMethod(code, methodName, unit);
		for (MethodDeclaration method : methodDec) {
			if (method.getBody() == null || method.getBody().statements().size() == 0) {
				continue;
			}
			Statement firstStatement = (Statement) method.getBody().statements().get(0);
			int startLine = unit.getLineNumber(firstStatement.getStartPosition()) - 1;
			int endLine = unit.getLineNumber(firstStatement.getStartPosition() + method.getBody().getLength()) - 2;
			int position = firstStatement.getStartPosition() + method.getBody().getLength();
			while (endLine == -3) {
				endLine = unit.getLineNumber(--position) - 2;
			}
			if ((startLine <= errorLine && endLine >= errorLine) || errorLine == -1) {
				return Arrays.asList(startLine, endLine);
			}
		}
		return new ArrayList<>();
	}

	private static List<MethodDeclaration> getMethod(String code, String methodName, CompilationUnit unit) {
		methodName = methodName.trim();
		List<MethodDeclaration> result = new ArrayList<>();
		List<MethodDeclaration> allMethods = getAllMethod(code, true, unit);
		for (MethodDeclaration method : allMethods) {
			if (method.getName().getIdentifier().equals(methodName)) {
				result.add(method);
			}
		}
		return result;
	}

	private static List<MethodDeclaration> getAllMethod(String code, boolean getInnerClassMethod,
			CompilationUnit unit) {
		if (unit.types().size() == 0) {
			return new ArrayList<>();
		}
		TypeDeclaration declaration = (TypeDeclaration) unit.types().get(0);
		List<MethodDeclaration> methodDeclarations = new ArrayList<>();
		methodDeclarations.addAll(Arrays.asList(declaration.getMethods()));
		if (getInnerClassMethod) {
			TypeDeclaration[] typeDelcarations = declaration.getTypes();
			for (TypeDeclaration typeDeclaration : typeDelcarations) {
				methodDeclarations.addAll(Arrays.asList(typeDeclaration.getMethods()));
			}
		}
		return methodDeclarations;
	}

	public static Map<String, Integer> getAssertInTest(String code, String testMethodName, int methodStartLine) {
		Map<String, Integer> result = new HashMap<>();
		String methodCode = FileUtils.getTestFunctionBodyFromCode(code, testMethodName);
		int bracket = 0;
		String line = "";
		int assertStartLine = 0;
		for (String lineString : methodCode.split("\n")) {
			methodStartLine++;
			if (bracket != 0) {
				bracket += countChar(lineString, '(');
				bracket -= countChar(lineString, ')');
				line += lineString.trim();
				if (bracket == 0) {
					if (line.contains("fail(")) {
						line = line + assertStartLine;
					}
					result.put(line, assertStartLine);
				}
				continue;
			}
			if (AssertUtils.isAssertLine(lineString, code)) {
				line = lineString.trim();
				bracket += countChar(lineString, '(');
				bracket -= countChar(lineString, ')');
				if (bracket == 0) {
					if (line.contains("fail(")) {
						line = line + methodStartLine;
					}
					result.put(line, methodStartLine);
				} else {
					assertStartLine = methodStartLine;
				}
			}
		}
		return result;
	}

	public static int countChar(String s, char c) {
		int count = 0;
		for (int i = 0; i < s.length(); i++) {
			if (s.charAt(i) == c) {
				count++;
			}
		}
		return count;
	}

	public static String getLineFromCode(String code, int line) {
		int lineNum = 0;
		for (String lineString : code.split("\n")) {
			lineNum++;
			if (lineNum == line) {
				return lineString.trim();
			}
		}
		return "";
	}
}
