package edu.lu.uni.serval.tbar.context;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Dictionary {
	
	private Map<String, List<Field>> allFields = new HashMap<>();
	private Map<String, String> superClasses = new HashMap<>();
	private Map<String, List<String>> importedDependencies = new HashMap<>();
	private Map<String, List<Method>> methods = new HashMap<>();
	
	public Map<String, List<Field>> getAllFields() {
		return allFields;
	}
	
	public Map<String, String> getSuperClasses() {
		return superClasses;
	}
	
	public Map<String, List<String>> getImportedDependencies() {
		return importedDependencies;
	}
	
	public Map<String, List<Method>> getMethods() {
		return methods;
	}
	
	public List<Field> findFieldsByClassPath(String currentClassPath) {
		return allFields.get(currentClassPath);
	}
	
	public String findSuperClassName(String currentClassPath) {
		return superClasses.get(currentClassPath);
	}

	public List<String> findImportedDependencies(String currentClassPath) {
		return importedDependencies.get(currentClassPath);
	}

	/**
	 * Find the class path of an imported class with its name.
	 * 
	 * @param currentClassPath
	 * @param className
	 * @return
	 */
	public String findDependencyByClassName(String currentClassPath, String className) {
		List<String> importedDependencies = this.findImportedDependencies(currentClassPath);
		if (importedDependencies == null) return null;
		
		String className_ = "." + className;
		String classPath = null;
		for (String dependency : importedDependencies) {
			if (dependency.endsWith(className_)) {
				classPath = dependency;
				break;
			}
		}
		
		if (classPath == null) {
			String superClassPath = this.findSuperClassName(currentClassPath);
			if (superClassPath != null) {
				classPath = this.findDependencyByClassName(superClassPath, className);
			}
		}
		return classPath;
	}

	/**
	 * Find the class name and path of the data type of a field in the current class or its super classes.
	 * 
	 * @param currentClassPath
	 * @param varName
	 * @return
	 */
	public String findDataTypeClassPathOfField(String currentClassPath, String varName) {
		List<Field> fields = findFieldsByClassPath(currentClassPath);
		String classPathOfFieldType = null;
		if (fields != null && !fields.isEmpty()) {
			for (Field field : fields) {
				if (varName.endsWith("." + field.getVarName())) {
					classPathOfFieldType = field.getClassName();
					break;
				}
			}
		}
		if (classPathOfFieldType == null) {
			String superClassPath = findSuperClassName(currentClassPath);
			if (superClassPath != null) {
				classPathOfFieldType = findDataTypeClassPathOfField(superClassPath, varName);
			}
		}
		return classPathOfFieldType;
	}

	/**
	 * Find all constructors of the current class.
	 * 
	 * @param currentClassPath
	 * @return
	 */
	public List<Method> findConstructors(String currentClassPath) {
		List<Method> methodList = this.methods.get(currentClassPath);
		if (methodList == null) return new ArrayList<Method>();
		List<Method> constructors = new ArrayList<>();
		for (Method m : methodList) {
			if (m.isConstructor()) constructors.add(m);
		}
		return constructors;
	}

	/**
	 * Find all super constructors of the super class of the current one.
	 * 
	 * @param currentClassPath
	 * @return
	 */
	public List<Method> findSuperConstructors(String currentClassPath) {
		String superClass = this.findSuperClassName(currentClassPath);
		if (superClass == null) return new ArrayList<Method>();
		return this.findConstructors(superClass);
	}
	
	/**
	 * Find all constructors of which class instance is created in the current class.
	 * 
	 * @param currentClassPath
	 * @param constructorName
	 * @return
	 */
	public List<Method> findOtherConstructors(String currentClassPath, String constructorName) {
		String classPath = this.findDependencyByClassName(currentClassPath, constructorName);
		if (classPath == null) return new ArrayList<Method>();
		return this.findConstructors(classPath);
	}
	
	/**
	 * Find all methods that can be accessed by the current class, including the protected methods and public ones of its super class.
	 * 
	 * @param currentClassPath
	 * @return
	 */
	public List<Method> findAllAvailableMethodsOfThisClass(String currentClassPath, boolean needsPrivateMethods, boolean needsProtectedMethods) {
		List<Method> methods = this.methods.get(currentClassPath);
		if (methods == null) {
			methods = new ArrayList<>();
		}
		if (!needsProtectedMethods) needsPrivateMethods = false;
		
		for (int index = methods.size() - 1; index >= 0; index --) {
			if (methods.get(index).isConstructor()) {
				methods.remove(index);
				continue;
			}
			if (methods.get(index).getModifier() == ModifierType.PRIVATE && !needsPrivateMethods) {
				methods.remove(index);
			} else if (methods.get(index).getModifier() == ModifierType.PROTECTED && !needsProtectedMethods) {
				methods.remove(index);
			}
		}
		
		String superClass = this.findSuperClassName(currentClassPath);
		if (superClass != null) {
			methods.addAll(this.findAllAvailableMethodsOfThisClass(superClass, false, needsProtectedMethods));
		}
		return methods;
	}

	public List<String> findDependenciesByClassNames(String currentClassPath, List<String> classNames) {
		List<String> classPaths = new ArrayList<>();
		List<String> importedDependencies = this.findImportedDependencies(currentClassPath);
		for (String dependency : importedDependencies) {
			String className = dependency.substring(dependency.lastIndexOf(".") + 1);
			if (classNames.contains(className)) {
				classPaths.add(dependency);
			}
		}
		String superClass = this.findSuperClassName(currentClassPath);
		if (superClass != null) {
			classPaths.addAll(findDependenciesByClassNames(superClass, classNames));
		}
		return classPaths;
	}

	public void setAllFields(Map<String, List<Field>> allFields) {
		this.allFields.putAll(allFields);
	}

	public void setSuperClasses(Map<String, String> superClasses) {
		this.superClasses.putAll(superClasses);
	}

	public void setImportedDependencies(Map<String, List<String>> importedDependencies) {
		this.importedDependencies.putAll(importedDependencies);
	}

	public void setMethods(Map<String, List<Method>> methods) {
		this.methods.putAll(methods);
	}

	public void clear() {
		this.allFields.clear();
		this.superClasses.clear();
		this.importedDependencies.clear();
		this.methods.clear();
	}
}
