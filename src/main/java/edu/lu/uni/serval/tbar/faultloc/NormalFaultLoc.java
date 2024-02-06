package edu.lu.uni.serval.tbar.faultloc;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

import java.util.ArrayList;
import java.util.List;

import edu.lu.uni.serval.tbar.config.Configuration;
import edu.lu.uni.serval.tbar.dataprepare.DataPreparer;
import edu.lu.uni.serval.tbar.utils.SuspiciousPosition;

public class NormalFaultLoc extends AbstractFaultLoc {
    
    public NormalFaultLoc(DataPreparer d, String dataType, String buggyProject, String filePath, String metric) {
        super(d,dataType,buggyProject);

        File suspiciousFile = new File(filePath + "/" +  buggyProject + "/" + metric + ".txt");
		if (!suspiciousFile.exists()) {
			System.out.println("Cannot find the suspicious code position file." + suspiciousFile.getPath());
			suspiciousFile = new File(filePath + "/" + this.buggyProject + "/" + metric.toLowerCase() + ".txt");
		}
		if (!suspiciousFile.exists()) {
			System.out.println("Cannot find the suspicious code position file." + suspiciousFile.getPath());
			suspiciousFile = new File(filePath + "/" + this.buggyProject + "/All.txt");
		}
		if (!suspiciousFile.exists()) return;
		try {
			FileReader fileReader = new FileReader(suspiciousFile);
            BufferedReader reader = new BufferedReader(fileReader);
            String line = null;
            while ((line = reader.readLine()) != null) {
            	String[] elements = line.split("@");
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
            return ;
        }
		return;
    }
    
}
