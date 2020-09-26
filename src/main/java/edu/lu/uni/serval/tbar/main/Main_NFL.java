package edu.lu.uni.serval.tbar.main;

import java.io.File;

import edu.lu.uni.serval.tbar.AbstractFixer;
import edu.lu.uni.serval.tbar.TBarFixer_NFL;
import edu.lu.uni.serval.tbar.config.Configuration;

/**
 * Fix bugs with Fault Localization results.
 * 
 * @author kui.liu
 *
 */
public class Main_NFL {
	
	public static void main(String[] args) {
//		if (args.length != 5) {
//			System.err.println("Arguments: \n" 
//					+ "\t<Bug_Data_Path>: the directory of checking out Defects4J bugs. \n"
//					+ "\t<Bug_ID>: bug id of each Defects4J bug, such as Chart_1. \n"
//					+ "\t<Suspicious_Code_Positions_File_Path>: \n"
//					 +"\t<Failed_Test_Cases_File_Path>: \n"
//					+ "\t<defects4j_Home>: the directory of defects4j git repository.\n");
//			System.exit(0);
//		}
		String bugDataPath = args[0];// "/Users/kui.liu/Public/Defects4J_Data/";//
		String bugId = args[1]; // "Chart_1"
		Configuration.suspPositionsFilePath = args[2];//"/Users/kui.liu/Public/Defects4J_Backup/gzoltar_results/";//
		Configuration.failedTestCasesFilePath = args[3];//"/Users/kui.liu/eclipse-fault-localization/FL-VS-APR/data/FailedTestCases/";//
		String defects4jHome = args[4]; // "/Users/kui.liu/Public/GitRepos/defects4j/";//
		System.out.println(bugId);
		fixBug(bugDataPath, defects4jHome, bugId);
	}

	public static void fixBug(String bugDataPath, String defects4jHome, String bugIdStr) {
		Configuration.outputPath += "NFL/";
		String suspiciousFileStr = Configuration.suspPositionsFilePath;
		
		String[] elements = bugIdStr.split("_");
		String projectName = elements[0];
		int bugId;
		try {
			bugId = Integer.valueOf(elements[1]);
		} catch (NumberFormatException e) {
			System.err.println("Please input correct buggy project ID, such as \"Chart_1\".");
			return;
		}
		
		AbstractFixer fixer = new TBarFixer_NFL(bugDataPath, projectName, bugId, defects4jHome);
		fixer.dataType = "TBar";
		fixer.metric = Configuration.faultLocalizationMetric;
		fixer.suspCodePosFile = new File(suspiciousFileStr);
		if (Integer.MAX_VALUE == fixer.minErrorTest) {
			System.out.println("Failed to defects4j compile bug " + bugIdStr);
			return;
		}
		
		fixer.fixProcess();
		
		int fixedStatus = fixer.fixedStatus;
		switch (fixedStatus) {
		case 0:
			System.out.println("Failed to fix bug " + bugIdStr);
			break;
		case 1:
			System.out.println("Succeeded to fix bug " + bugIdStr);
			break;
		case 2:
			System.out.println("Partial succeeded to fix bug " + bugIdStr);
			break;
		}
	}

}
