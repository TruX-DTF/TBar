package edu.lu.uni.serval.tbar.faultloc;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.lu.uni.serval.entity.Pair;
import edu.lu.uni.serval.jdt.tree.ITree;
import edu.lu.uni.serval.tbar.dataprepare.DataPreparer;
import edu.lu.uni.serval.tbar.utils.FileUtils;
import edu.lu.uni.serval.tbar.utils.SuspiciousCodeParser;
import edu.lu.uni.serval.tbar.utils.SuspiciousPosition;

public abstract class AbstractFaultLoc {
    protected DataPreparer dp = null;
    protected String dataType = "";
    protected String buggyProject = "";
	protected static Logger log = LoggerFactory.getLogger(AbstractFaultLoc.class);

    public AbstractFaultLoc(DataPreparer d, String dt, String project) {
        dp = d;
        dataType = dt;
        project= buggyProject;
    }

    public List<SuspCodeNode> getSuspiciousCode(SuspiciousPosition suspiciousCode) {
		String suspiciousClassName = suspiciousCode.classPath;
		int buggyLine = suspiciousCode.lineNumber;
		
		log.debug(suspiciousClassName + " ===" + buggyLine);
		if (suspiciousClassName.contains("$")) {
			suspiciousClassName = suspiciousClassName.substring(0, suspiciousClassName.indexOf("$"));
		}
		String suspiciousJavaFile = suspiciousClassName.replace(".", "/") + ".java";
		
		suspiciousClassName = suspiciousJavaFile.substring(0, suspiciousJavaFile.length() - 5).replace("/", ".");
		
		String filePath = dp.srcPath + suspiciousJavaFile;
		if (!new File(filePath).exists()) return null;
		File suspCodeFile = new File(filePath);
		if (!suspCodeFile.exists()) return null;
		SuspiciousCodeParser scp = new SuspiciousCodeParser();
		scp.parseSuspiciousCode(new File(filePath), buggyLine);
		
		List<Pair<ITree, String>> suspiciousCodePairs = scp.getSuspiciousCode();
		if (suspiciousCodePairs.isEmpty()) {
			log.debug("Failed to identify the buggy statement in: " + suspiciousClassName + " --- " + buggyLine);
			return null;
		}
		
		File targetJavaFile = new File(FileUtils.getFileAddressOfJava(dp.srcPath, suspiciousClassName));
        File targetClassFile = new File(FileUtils.getFileAddressOfClass(dp.classPath, suspiciousClassName));
        File javaBackup = new File(FileUtils.tempJavaPath(suspiciousClassName,  this.dataType + "/" + this.buggyProject));
        File classBackup = new File(FileUtils.tempClassPath(suspiciousClassName, this.dataType + "/" + this.buggyProject));
        try {
        	if (!targetClassFile.exists()) return null;
        	if (javaBackup.exists()) javaBackup.delete();
        	if (classBackup.exists()) classBackup.delete();
			Files.copy(targetJavaFile.toPath(), javaBackup.toPath());
			Files.copy(targetClassFile.toPath(), classBackup.toPath());
		} catch (IOException e) {
			e.printStackTrace();
		}
        
        List<SuspCodeNode> scns = new ArrayList<>();
		for (Pair<ITree, String> suspCodePair : suspiciousCodePairs) {
			ITree suspCodeAstNode = suspCodePair.getFirst(); //scp.getSuspiciousCodeAstNode();
			String suspCodeStr = suspCodePair.getSecond(); //scp.getSuspiciousCodeStr();
			log.debug("Suspicious Code: \n" + suspCodeStr);
			
			int startPos = suspCodeAstNode.getPos();
			int endPos = startPos + suspCodeAstNode.getLength();
			SuspCodeNode scn = new SuspCodeNode(javaBackup, classBackup, targetJavaFile, targetClassFile, 
	        		startPos, endPos, suspCodeAstNode, suspCodeStr, suspiciousJavaFile, buggyLine);
			scns.add(scn);
		}
        return scns;
    }
}
