package edu.lu.uni.serval.tbar.utils;

public class LineUtils {

	public static boolean isLineInFailBlock(String code, int lineNum){
        int braceCount = 0;
        for (int i= lineNum; i< code.split("\n").length; i++){
            String lineString = CodeUtils.getLineFromCode(code, i);
            braceCount += CodeUtils.countChar(lineString, '{');
            braceCount -= CodeUtils.countChar(lineString, '}');
            if (braceCount < 0){
                return false;
            }
            if (lineString.contains("fail(") && braceCount == 0){
                return true;
            }
        }
        return false;
	}

}
