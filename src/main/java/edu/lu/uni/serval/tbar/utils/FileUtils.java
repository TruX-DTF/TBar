package edu.lu.uni.serval.tbar.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;

import edu.lu.uni.serval.tbar.config.Configuration;

public class FileUtils {

	public static String getMD5(String s) {
		char hexDigits[] = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F' };
		try {
			byte[] btInput = s.getBytes("utf-8");
			MessageDigest mdInst = MessageDigest.getInstance("MD5");
			mdInst.update(btInput);
			byte[] md = mdInst.digest();
			int j = md.length;
			char str[] = new char[j * 2];
			int k = 0;
			for (int i = 0; i < j; i++) {
				byte byte0 = md[i];
				str[k++] = hexDigits[byte0 >>> 4 & 0xf];
				str[k++] = hexDigits[byte0 & 0xf];
			}
			return new String(str);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	public static String getFileAddressOfJava(String srcPath, String className) {
		if (className.contains("<") && className.contains(">")) {
			className = className.substring(0, className.indexOf("<"));
		}
		return srcPath.trim() + System.getProperty("file.separator")
				+ className.trim().replace('.', System.getProperty("file.separator").charAt(0)) + ".java";
	}

	public static String getFileAddressOfClass(String classPath, String className) {
		if (className.contains("<") && className.contains(">")) {
			className = className.substring(0, className.indexOf("<"));
		}
		return classPath.trim() + System.getProperty("file.separator")
				+ className.trim().replace('.', System.getProperty("file.separator").charAt(0)) + ".class";
	}

	public static String tempJavaPath(String classname, String identifier) {
		new File(Configuration.TEMP_FILES_PATH + identifier).mkdirs();
		return Configuration.TEMP_FILES_PATH + identifier + "/" + classname.substring(classname.lastIndexOf(".") + 1) + ".java";
	}

	public static String tempClassPath(String classname, String identifier) {
		new File(Configuration.TEMP_FILES_PATH + identifier).mkdirs();
		return Configuration.TEMP_FILES_PATH + identifier + "/" + classname.substring(classname.lastIndexOf(".") + 1) + ".class";
	}

	public static File copyFile(File src, File dst) {
		return copyFile(src.getAbsolutePath(), dst.getAbsolutePath());
	}

	public static File copyFile(String srcPath, String dstPath) {
		FileInputStream input = null;
		FileOutputStream output = null;
		try {
			input = new FileInputStream(srcPath);
			output = new FileOutputStream(dstPath);
			int in = input.read();
			while (in != -1) {
				output.write(in);
				in = input.read();
			}
		} catch (IOException e) {
			System.out.println(e.toString());
		} finally {
			if (input != null) {
				try {
					input.close();
				} catch (IOException e) {
				}
			}
			if (output != null) {
				try {
					output.close();
				} catch (IOException e) {
				}
			}
		}
		return new File(dstPath);
	}

	public static String getCodeFromFile(String srcPath, String className) {
		return getCodeFromFile(getFileAddressOfJava(srcPath, className));
	}

	public static String getCodeFromFile(String fileaddress) {
		FileInputStream stream = null;
		try {
			stream = new FileInputStream(new File(fileaddress));
			byte[] b = new byte[stream.available()];
			int len = stream.read(b);
			if (len <= 0) {
				throw new IOException("Source code file " + fileaddress + " read fail!");
			}
			stream.close();
			return new String(b);
		} catch (Exception e) {
			System.out.println(e.getMessage());
			return "";
		} finally {
			if (stream != null) {
				try {
					stream.close();
				} catch (IOException e) {
				}
			}
		}
	}

	public static String getCodeFromFile(File file) {
		return getCodeFromFile(file.getAbsolutePath());
	}

	public static List<String> getPackageImportFromCode(String code) {
		List<String> result = new ArrayList<>();
		for (String line : code.split("\n")) {
			if (line.startsWith("import")) {
				result.add(line);
			}
		}
		return result;
	}

	public static String getTestFunctionCodeFromCode(String code, String targetFunctionName, String testSrcPath) {
		String result = getTestFunctionCodeFromCode(code, targetFunctionName);
		if (result.equals("")) {
			if (code.contains(" extends ")) {
				String extendsClass = code.split(" extends ")[1].substring(0, code.split(" extends ")[1].indexOf("{"));
				String className = CodeUtils.getClassNameOfImportClass(code, extendsClass);
				if (className.equals("")) {
					className = CodeUtils.getPackageName(code) + "." + extendsClass;
				}
				String extendsCode = FileUtils.getCodeFromFile(testSrcPath, className.trim());
				if (!extendsCode.equals("")) {
					return getTestFunctionCodeFromCode(extendsCode, targetFunctionName, testSrcPath);
				}
			}
		}
		return result;
	}

	private static String getTestFunctionCodeFromCode(String code, String targetFunctionName) {
		if (code.contains("@Test")) {
			String[] tests = code.split("@Test");
			for (String test : tests) {
				if (test.contains("public void " + targetFunctionName + "()")) {
					if (test.contains("private void ")) {
						test = test.split("private void ")[0];
					}
					return test;
				}
			}
		} else {
			List<String> tests = divideTestFunction(code);
			for (String test : tests) {
				if (test.trim().startsWith(targetFunctionName + "()")) {
					return "public void" + test.trim();
				}
			}
		}
		return "";
	}

	private static List<String> divideTestFunction(String code) {
		List<String> result = new ArrayList<String>();
		code = code.replaceAll("private void", "public void");
		String[] items = code.split("public void");
		for (int j = 1; j < items.length; j++) {
			String item = items[j];
			int startPoint = item.indexOf('{') + 1;
			int braceCount = 1;
			for (int i = startPoint; i < item.length(); i++) {
				if (item.charAt(i) == '}') {
					if (--braceCount == 0) {
						result.add(item.substring(0, i + 1));
						break;
					}
				}
				if (item.charAt(i) == '{') {
					braceCount++;
				}
			}
		}
		return result;
	}

	public static String getTestFunctionBodyFromCode(String code, String targetFunctionName) {
		String methodString = getTestFunctionCodeFromCode(code, targetFunctionName);
		if (!methodString.contains("{") || !methodString.contains("}")) {
			return "";
		}
		methodString = methodString.substring(methodString.indexOf("{") + 1, methodString.lastIndexOf("}"));
		while (methodString.startsWith("\n")) {
			methodString = methodString.substring(1);
		}
		while (methodString.endsWith("\n")) {
			methodString = methodString.substring(0, methodString.length() - 1);
		}
		if (!methodString.contains("\n")) {
			return methodString;
		}
		while (methodString.split("\n")[0].trim().equals("") || methodString.split("\n")[0].trim().startsWith("//")) {
			methodString = methodString.substring(methodString.indexOf("\n") + 1);
		}
		return methodString;
	}

}
