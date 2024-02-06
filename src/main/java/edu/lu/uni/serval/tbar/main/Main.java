package edu.lu.uni.serval.tbar.main;

import java.io.File;

import edu.lu.uni.serval.tbar.config.Configuration;
import edu.lu.uni.serval.tbar.faultloc.AbstractFaultLoc;
import edu.lu.uni.serval.tbar.fixers.AbstractFixer;
import edu.lu.uni.serval.tbar.fixers.TBarFixer;
import edu.lu.uni.serval.tbar.faultloc.PerfectFaultLoc;
import edu.lu.uni.serval.tbar.faultloc.NormalFaultLoc;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
/**
 * Fix bugs with Fault Localization results.
 * 
 * @author kui.liu
 *
 */

 // previously, NormalFL runner called Main
 // perfect FLT runner called MainPerfectFL
 // Main made a TBarFixer
 // MainPerfectFL also made a TBarFixer
 // so the big mystery is, how does TBar know which FL to use

public class Main {
	
	public static Options buildOptions() {
		Options options = new Options();

		options.addOption(Option.builder("bugDataPath")
		.argName("bug-data-path")
		.hasArg()
		.desc("parent directory of checked out defects")
		.required()
		.build());

		options.addOption(Option.builder("bugId")
		.argName("bug ID")
		.hasArg()
		.desc("Defects$J bug ID, <project_name>_<num>") // TODO make this reasonable
		.required()
		.build());
		
		options.addOption(Option.builder("d4jHome")
		.argName("d4jHome")
		.hasArg()
		.desc("path to defects4j repository")
		.required()
		.build());

		options.addOption(Option.builder("faultLoc")
		.argName("faultLoc")
		.hasArg()
		.desc("Fault localization strategies, options: perfect, normal. Default: normal")
		.build());

		options.addOption(Option.builder("susPositions")
		.argName("susPositions")
		.hasArg()
		.desc("File path to suspicious positions file. Dunno if we need this.")
		.build());

		options.addOption(Option.builder("failedTests")
		.argName("failedTests")
		.hasArg()
		.desc("File path to failed Test Cases. Dunno if we need this.")
		.build());

		options.addOption(Option.builder("knownBugPositions")
		.argName("knownBugPositions")
		.hasArg()
		.desc("File path to file listing known bug positions. Dunno if we need this.")
		.build());

		options.addOption(Option.builder("isTestFixPatterns")
		.argName("isTestFixPatterns")
		.desc("Not sure what this is but it exists.")
		.build());

        // --help
        options.addOption("help", false, "Prints this help message.");
		return options;

	}


	public static void main(String[] args) {

		CommandLineParser parser = new DefaultParser();
		Options options = buildOptions();
        // automatically generate the help statement
        HelpFormatter formatter = new HelpFormatter();
		AbstractFixer fixer = null;
		AbstractFaultLoc faultloc = null;

		String bugId = "";
		try {
            CommandLine line = parser.parse(options, args);
			if (line.hasOption("help")) {
                formatter.printHelp("tbar", options);
                System.exit(0);
            }
			Configuration.bugDataPath = line.getOptionValue("bugDataPath"); // "/Users/kui.liu/Public/Defects4J_Data/";//
			bugId = line.getOptionValue("bug-id"); //  "Chart_1" 
			String[] elements = bugId.split("_"); //FIXME fix this
			String projectName = elements[0];
			int bugNum;
			try {
				bugNum = Integer.valueOf(elements[1]);
			} catch (NumberFormatException e) {
				System.err.println("Please input correct buggy project ID, such as \"Chart_1\".");
				return;
			}
			if (line.hasOption("susPositions")) {
				Configuration.suspPositionsFilePath = line.getOptionValue("susPositions");
			}
			if (line.hasOption("failedTests")) {
				Configuration.failedTestCasesFilePath = line.getOptionValue("failedTests"); //"/Users/kui.liu/eclipse-fault-localization/FL-VS-APR/data/FailedTestCases/";//
			}
			if (line.hasOption("knownBugPositions")) {
				Configuration.knownBugPositions = line.getOptionValue("knownBugPositions"); 
			}
			Configuration.defects4j_home = line.getOptionValue("d4j-home");
			fixer = new TBarFixer(Configuration.bugDataPath, projectName, bugNum, Configuration.defects4j_home);
			fixer.dataType = "TBar";
			fixer.isTestFixPatterns = line.hasOption("isTestFixPatterns");
			fixer.metric = Configuration.faultLocalizationMetric;


			// FIXME: fix the design here because the data preparer thing is shared weirdly 

			if(line.hasOption("faultloc") && line.getOptionValue("faultloc").equals("perfect")) {
				// claire cut configuration of granularity since it looks like they only use Line
				 faultloc =  new PerfectFaultLoc(fixer.getDataPreparer(), fixer.dataType, projectName, Configuration.knownBugPositions); 
				if (Integer.MAX_VALUE == fixer.minErrorTest) {
					System.out.println("Failed to defects4j compile bug " + bugId);
					return;
				}
				fixer.metric = Configuration.faultLocalizationMetric;
		
			if (line.hasOption("isTestFixPatterns")) {
				Configuration.outputPath += "FixPatterns/";
			} else {
				Configuration.outputPath += "PerfectFL/";
			}
			} else {
				// fixme: there is code to do line-level vs. file-level localization for some reason 
				faultloc = new NormalFaultLoc(fixer.getDataPreparer(), fixer.dataType, projectName, Configuration.suspPositionsFilePath, Configuration.faultLocalizationMetric);
				Configuration.outputPath += "NormalFL/";
				fixer.suspCodePosFile = new File(Configuration.suspPositionsFilePath);
				if (Integer.MAX_VALUE == fixer.minErrorTest) {
					System.out.println("Failed to defects4j compile bug " + bugId);
					return;
				}
				
		
			}
			fixer.setFaultLoc(faultloc);

		} catch (ParseException exp) {
            System.out.println("Unexpected parser exception:" + exp.getMessage());
        }
		if(fixer != null) {
		fixer.fixProcess();
		
		int fixedStatus = fixer.fixedStatus;
		switch (fixedStatus) {
		case 0:
			System.out.println("Failed to fix bug " + bugId);
			break;
		case 1:
			System.out.println("Succeeded to fix bug " + bugId);
			break;
		case 2:
			System.out.println("Partial succeeded to fix bug " + bugId);
			break;
		}
	}
}

}
