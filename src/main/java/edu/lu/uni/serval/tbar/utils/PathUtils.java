package edu.lu.uni.serval.tbar.utils;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

public class PathUtils {

	public static ArrayList<String> getSrcPath(String bugProject) {
		ArrayList<String> path = new ArrayList<String>();
		String[] words = bugProject.split("_");
		String projectName = words[0];
		int bugId = Integer.parseInt(words[1]);
		if (projectName.equals("Math")) {
			if (bugId < 85) {
				path.add("/target/classes/");
				path.add("/target/test-classes/");
				path.add("/src/main/java/");
				path.add("/src/test/java/");
			} else {
				path.add("/target/classes/");
				path.add("/target/test-classes/");
				path.add("/src/java/");
				path.add("/src/test/");
			}
		} else if (projectName.equals("Time")) {
			if (bugId < 12) {
				path.add("/target/classes/");
				path.add("/target/test-classes/");
				path.add("/src/main/java/");
				path.add("/src/test/java/");
			} else {
				path.add("/build/classes/");
				path.add("/build/tests/");
				path.add("/src/main/java/");
				path.add("/src/test/java/");
			}
		} else if (projectName.equals("Lang")) {
			if (bugId <= 20) {
				path.add("/target/classes/");
				path.add("/target/tests/");
				path.add("/src/main/java/");
				path.add("/src/test/java/");
			} else if (bugId >= 21 && bugId <= 35) {
				path.add("/target/classes/");
				path.add("/target/test-classes/");
				path.add("/src/main/java/");
				path.add("/src/test/java/");
			} else if (bugId >= 36 && bugId <= 41) {
				path.add("/target/classes/");
				path.add("/target/test-classes/");
				path.add("/src/java/");
				path.add("/src/test/");
			} else if (bugId >= 42 && bugId <= 65) {
				path.add("/target/classes/");
				path.add("/target/tests/");
				path.add("/src/java/");
				path.add("/src/test/");
			}
		} else if (projectName.equals("Chart")) {
			path.add("/build/");
			path.add("/build-tests/");
			path.add("/source/");
			path.add("/tests/");

		} else if (projectName.equals("Closure")) {
			path.add("/build/classes/");
			path.add("/build/test/");
			path.add("/src/");
			path.add("/test/");
		} else if (projectName.equals("Mockito")) {
			if (bugId <= 11 || (bugId >= 18 && bugId <= 21)) {
				path.add("/build/classes/main/");
				path.add("/build/classes/test/");
				path.add("/src/");
				path.add("/test/");
			} else {
				path.add("/target/classes/");
				path.add("/target/test-classes/");
				path.add("/src/");
				path.add("/test/");
			}
		}
		return path;
	}

	public static String getJunitPath() {
		return System.getProperty("user.dir")+"/target/dependency/junit-4.12.jar";
	}
	
	private static String getHamcrestPath() {
		return System.getProperty("user.dir")+"/target/dependency/hamcrest-all-1.3.jar";
	}

	public static String buildCompileClassPath(List<String> additionalPath, String classPath, String testClassPath){
		String path = "\"";
		path += classPath;
		path += System.getProperty("path.separator");
		path += testClassPath;
		path += System.getProperty("path.separator");
		path += JunitRunner.class.getProtectionDomain().getCodeSource().getLocation().getFile();
		path += System.getProperty("path.separator");
		path += StringUtils.join(additionalPath,System.getProperty("path.separator"));
		path += "\"";
		return path;
	}
	
	public static String buildTestClassPath(String classPath, String testClassPath) {
		String path = "\"";
		path += classPath;
		path += System.getProperty("path.separator");
		path += testClassPath;
		path += System.getProperty("path.separator");
		path += JunitRunner.class.getProtectionDomain().getCodeSource().getLocation().getFile();
		path += System.getProperty("path.separator");
	    path += getJunitPath();
	    path += System.getProperty("path.separator");
	    path += getHamcrestPath();
	    path += System.getProperty("path.separator");
		path += "\"";
		return path;
    }

}
