package edu.lu.uni.serval.tbar.dataprepare;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class TestFinder {
	static final int CLASS_SUFFIX_LENGTH = ".class".length();
	static final int JAVA_SUFFIX_LENGTH = ".java".length();
	private final ClassFilter tester;
	private final ClassFinder finder;

	public TestFinder(ClassFinder finder, ClassFilter tester) {
		this.tester = tester;
		this.finder = finder;
	}

	public Class<?>[] process() {
		List<Class<?>> classes = new ArrayList<>();
		String[] finderClasses = finder.getClasses();
		int length = finderClasses.length;

		for (int index = 0; index < length; ++index) {
			String fileName = finderClasses[index];
			String className;
			if (isJavaFile(fileName)) {//
				className = classNameFromJava(fileName);
			} else if (isClassFile(fileName)) {//
				className = classNameFromFile(fileName);//
			} else
				continue;
			if (!tester.acceptClassName(className))//
				continue;
			if (!tester.acceptInnerClass() && isInnerClass(className))//
				continue;

			if (!isInnerClass(className)) {
				try {
					Class<?> clazz = Class.forName(className);
					if (clazz.isLocalClass() || clazz.isAnonymousClass())
						continue;
					if (tester.acceptClass(clazz)) {
						classes.add(clazz);
					}
				} catch (ClassNotFoundException cnfExp1) {
					try {
						ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
						Class<?> clazz = Class.forName(className, false, classLoader);
						if (clazz.isLocalClass() || clazz.isAnonymousClass())
							continue;
						if (tester.acceptClass(clazz)) {
							classes.add(clazz);
						}
					} catch (ClassNotFoundException cnfExp2) {
						cnfExp2.printStackTrace();
					} catch (NoClassDefFoundError ncefErr) {
					}
				} catch (NoClassDefFoundError ncefErr) {
				}
			}
		}
		Collections.sort(classes, new Comparator<Class<?>>() {
			@Override
			public int compare(Class<?> o1, Class<?> o2) {
				return o1.getName().compareTo(o2.getName());
			}
		});
		return classes.toArray(new Class[classes.size()]);
	}

	private String classNameFromJava(String fileName) {
		String className = replaceFileSeparators(cutOffExtension(fileName, JAVA_SUFFIX_LENGTH));
		while (className.startsWith("."))
			className = className.substring(1);
		return className;
	}

	private boolean isJavaFile(String fileName) {
		return fileName.endsWith(".java");
	}

	private boolean isInnerClass(String className) {
		return className.contains("$");
	}

	private boolean isClassFile(String classFileName) {
		return classFileName.endsWith(".class");
	}

	private String classNameFromFile(String classFileName) {
		String className = replaceFileSeparators(cutOffExtension(classFileName, CLASS_SUFFIX_LENGTH));
		while (className.startsWith("."))
			className = className.substring(1);
		return className;
	}

	private String replaceFileSeparators(String s) {
		String result = s.replace(File.separatorChar, '.');
		if (File.separatorChar != '/') {
			result = result.replace('/', '.');
		}

		return result;
	}

	private String cutOffExtension(String classFileName, int length) {
		return classFileName.substring(0, classFileName.length() - length);
	}

}
