package edu.lu.uni.serval.tbar;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.lu.uni.serval.jdt.tree.ITree;
import edu.lu.uni.serval.tbar.config.Configuration;
import edu.lu.uni.serval.tbar.context.ContextReader;
import edu.lu.uni.serval.tbar.fixpatterns.CNIdiomNoSuperCall;
import edu.lu.uni.serval.tbar.fixpatterns.ClassCastChecker;
import edu.lu.uni.serval.tbar.fixpatterns.ConditionalExpressionMutator;
import edu.lu.uni.serval.tbar.fixpatterns.DataTypeReplacer;
import edu.lu.uni.serval.tbar.fixpatterns.ICASTIdivCastToDouble;
import edu.lu.uni.serval.tbar.fixpatterns.LiteralExpressionMutator;
import edu.lu.uni.serval.tbar.fixpatterns.MethodInvocationMutator;
import edu.lu.uni.serval.tbar.fixpatterns.NPEqualsShouldHandleNullArgument;
import edu.lu.uni.serval.tbar.fixpatterns.NullPointerChecker;
import edu.lu.uni.serval.tbar.fixpatterns.OperatorMutator;
import edu.lu.uni.serval.tbar.fixpatterns.RangeChecker;
import edu.lu.uni.serval.tbar.fixpatterns.ReturnStatementMutator;
import edu.lu.uni.serval.tbar.fixpatterns.StatementInserter;
import edu.lu.uni.serval.tbar.fixpatterns.StatementMover;
import edu.lu.uni.serval.tbar.fixpatterns.StatementRemover;
import edu.lu.uni.serval.tbar.fixpatterns.VariableReplacer;
import edu.lu.uni.serval.tbar.fixtemplate.FixTemplate;
import edu.lu.uni.serval.tbar.info.Patch;
import edu.lu.uni.serval.tbar.utils.Checker;
import edu.lu.uni.serval.tbar.utils.FileHelper;
import edu.lu.uni.serval.tbar.utils.SuspiciousPosition;

/**
 * 
 * @author kui.liu
 *
 */
@SuppressWarnings("unused")
public class TBarFixer_NFL extends TBarFixer {
	
	public Granularity granularity = Granularity.FL;
	
	public enum Granularity {
		Line,
		File,
		FL
	}
	
	private static Logger log = LoggerFactory.getLogger(TBarFixer_NFL.class);
	
	public TBarFixer_NFL(String path, String projectName, int bugId, String defects4jPath) {
		super(path, projectName, bugId, defects4jPath);
	}
	
	public TBarFixer_NFL(String path, String metric, String projectName, int bugId, String defects4jPath) {
		super(path, metric, projectName, bugId, defects4jPath);
	}

	@Override
	public void fixProcess() {
		// Read paths of the buggy project.
		if (!dp.validPaths) return;
		
		// Read suspicious positions.
		List<SuspiciousPosition> suspiciousCodeList = null;
		suspiciousCodeList = readSuspiciousCodeFromFile();
		
		if (suspiciousCodeList == null) return;
		
		List<SuspCodeNode> triedSuspNode = new ArrayList<>();
		log.info("=======TBar: Start to fix suspicious code======");
		for (SuspiciousPosition suspiciousCode : suspiciousCodeList) {
			List<SuspCodeNode> scns = parseSuspiciousCode(suspiciousCode);
			if (scns == null) continue;

			for (SuspCodeNode scn : scns) {
//				log.debug(scn.suspCodeStr);
				if (triedSuspNode.contains(scn)) continue;
				triedSuspNode.add(scn);
				
				// Parse context information of the suspicious code.
				List<Integer> contextInfoList = readAllNodeTypes(scn.suspCodeAstNode);
				List<Integer> distinctContextInfo = new ArrayList<>();
				for (Integer contInfo : contextInfoList) {
					if (!distinctContextInfo.contains(contInfo) && !Checker.isBlock(contInfo)) {
						distinctContextInfo.add(contInfo);
					}
				}
//				List<Integer> distinctContextInfo = contextInfoList.stream().distinct().collect(Collectors.toList());
				
		        // Match fix templates for this suspicious code with its context information.
				fixWithMatchedFixTemplates(scn, distinctContextInfo);
		        
				if (!isTestFixPatterns && minErrorTest == 0) break;
//				if (this.patchId >= 10000) break;
			}
			if (!isTestFixPatterns && minErrorTest == 0) break;
//			if (this.patchId >= 10000) break;
        }
		log.info("=======TBar: Finish off fixing======");
		
		FileHelper.deleteDirectory(Configuration.TEMP_FILES_PATH + this.dataType + "/" + this.buggyProject);
	}

	private List<SuspiciousPosition> readKnownBugPositionsFromFile() {
		List<SuspiciousPosition> suspiciousCodeList = new ArrayList<>();
		
		String[] posArray = FileHelper.readFile(Configuration.knownBugPositions).split("\n");
		Boolean isBuggyProject = null;
		for (String pos : posArray) {
			if (isBuggyProject == null || isBuggyProject) {
				if (pos.startsWith(this.buggyProject + "@")) {
					isBuggyProject = true;
					
					String[] elements = pos.split("@");
	            	String[] lineStrArr = elements[2].split(",");
	            	String classPath = elements[1];
	            	String shortSrcPath = dp.srcPath.substring(dp.srcPath.indexOf(this.buggyProject) + this.buggyProject.length() + 1);
	            	classPath = classPath.substring(shortSrcPath.length(), classPath.length() - 5);

	            	for (String lineStr : lineStrArr) {
	    				if (lineStr.contains("-")) {
	    					String[] subPos = lineStr.split("-");
	    					for (int line = Integer.valueOf(subPos[0]), endLine = Integer.valueOf(subPos[1]); line <= endLine; line ++) {
	    						SuspiciousPosition sp = new SuspiciousPosition();
	    		            	sp.classPath = classPath;
	    		            	sp.lineNumber = line;
	    		            	suspiciousCodeList.add(sp);
	    					}
	    				} else {
	    					SuspiciousPosition sp = new SuspiciousPosition();
	    	            	sp.classPath = classPath;
	    	            	sp.lineNumber = Integer.valueOf(lineStr);
	    	            	suspiciousCodeList.add(sp);
	    				}
	    			}
				} else if (isBuggyProject!= null && isBuggyProject) isBuggyProject = false;
			} else if (!isBuggyProject) break;
		}
		return suspiciousCodeList;
	} 

	private List<String> readKnownFileLevelBugPositions() {
		List<String> buggyFileList = new ArrayList<>();
		
		String[] posArray = FileHelper.readFile(Configuration.knownBugPositions).split("\n");
		Boolean isBuggyProject = null;
		for (String pos : posArray) {
			if (isBuggyProject == null || isBuggyProject) {
				if (pos.startsWith(this.buggyProject + "@")) {
					isBuggyProject = true;
					
					String[] elements = pos.split("@");
	            	String classPath = elements[1];
	            	String shortSrcPath = dp.srcPath.substring(dp.srcPath.indexOf(this.buggyProject) + this.buggyProject.length() + 1);
	            	classPath = classPath.substring(shortSrcPath.length(), classPath.length() - 5).replace("/", ".");

	            	if (!buggyFileList.contains(classPath)) {
	            		buggyFileList.add(classPath);
	            	}
				} else if (isBuggyProject!= null && isBuggyProject) isBuggyProject = false;
			} else if (!isBuggyProject) break;
		}
		return buggyFileList;
	}
	
	public List<SuspiciousPosition> readSuspiciousCodeFromFile(List<String> buggyFileList) {
		File suspiciousFile = null;
		String suspiciousFilePath = "";
		if (this.suspCodePosFile == null) {
			suspiciousFilePath = Configuration.suspPositionsFilePath;
		} else {
			suspiciousFilePath = this.suspCodePosFile.getPath();
		}
		
		suspiciousFile = new File(suspiciousFilePath + "/" + this.buggyProject + "/" + this.metric + ".txt");
		if (!suspiciousFile.exists()) {
			System.out.println("Cannot find the suspicious code position file." + suspiciousFile.getPath());
			suspiciousFile = new File(suspiciousFilePath + "/" + this.buggyProject + "/" + this.metric.toLowerCase() + ".txt");
		}
		if (!suspiciousFile.exists()) {
			System.out.println("Cannot find the suspicious code position file." + suspiciousFile.getPath());
			suspiciousFile = new File(suspiciousFilePath + "/" + this.buggyProject + "/All.txt");
		}
		if (!suspiciousFile.exists()) return null;
		List<SuspiciousPosition> suspiciousCodeList = new ArrayList<>();
		try {
			FileReader fileReader = new FileReader(suspiciousFile);
            BufferedReader reader = new BufferedReader(fileReader);
            String line = null;
            while ((line = reader.readLine()) != null) {
            	String[] elements = line.split("@");
            	if (!buggyFileList.contains(elements[0])) continue;
            	SuspiciousPosition sp = new SuspiciousPosition();
            	sp.classPath = elements[0];
            	sp.lineNumber = Integer.valueOf(elements[1]);
            	suspiciousCodeList.add(sp);
            }
            reader.close();
            fileReader.close();
        }catch (Exception e){
        	e.printStackTrace();
        	log.debug("Reloading Localization Result...");
            return null;
        }
		if (suspiciousCodeList.isEmpty()) return null;
		return suspiciousCodeList;
	}


	public List<SuspiciousPosition> readSuspiciousCodeFromFile() {
		File suspiciousFile = null;
		if (this.suspCodePosFile == null) {
			suspiciousFile = new File(Configuration.suspPositionsFilePath);
		} else {
			suspiciousFile = this.suspCodePosFile;
		}
		
		List<SuspiciousPosition> suspiciousCodeList = new ArrayList<>();
		try {
			List<String> allTestCases = new ArrayList<>();
			BufferedReader reader = new BufferedReader(new FileReader(suspiciousFile.getPath() + "/" + this.buggyProject + "/sfl_tests.csv"));
			String line = reader.readLine();
			while ((line = reader.readLine()) != null) {
				String testCase = line.substring(0, line.indexOf(","));
				allTestCases.add(testCase);
			}
			reader.close();
			
			reader = new BufferedReader(new FileReader(suspiciousFile.getPath() + "/" + this.buggyProject + "/sfl_ochiai_ranking.csv"));
			line = reader.readLine();
			while ((line = reader.readLine()) != null) {
				int dollarIndex = line.indexOf("$");
				int sharpIndex = line.indexOf("#");
				int colonIndex = line.indexOf(":");
				int semicolonIndex = line.indexOf(";");
				String className = line.substring(0, dollarIndex) + "." + line.substring(dollarIndex + 1, sharpIndex);
				String methodName = line.substring(sharpIndex + 1);
				methodName = methodName.substring(0, methodName.indexOf("("));
				Integer buggyLine = Integer.parseInt(line.substring(colonIndex + 1, semicolonIndex));
				Double suspiciousness = Double.parseDouble(line.substring(semicolonIndex + 1));
				if (Double.compare(suspiciousness, 0.0d) == 0) break;
				if (allTestCases.contains(className + "#" + methodName)) continue;// Test case.
				
				dollarIndex = className.indexOf("$");
				if (dollarIndex > 0) className = className.substring(0, dollarIndex);
				SuspiciousPosition sp = new SuspiciousPosition();
            	sp.classPath = className;
            	sp.lineNumber = buggyLine;
            	suspiciousCodeList.add(sp);
			}
			reader.close();
        }catch (Exception e){
        	e.printStackTrace();
        	log.debug("Reloading Localization Result...");
            return null;
        }
		
		if (suspiciousCodeList.isEmpty()) return null;
		return suspiciousCodeList;
	}

}
