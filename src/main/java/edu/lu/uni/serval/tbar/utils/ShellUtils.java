package edu.lu.uni.serval.tbar.utils;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import edu.lu.uni.serval.tbar.config.Configuration;

public class ShellUtils {

	public static String shellRun(List<String> asList, String buggyProject, int type) throws IOException {
		String fileName;
        String cmd;
        if (System.getProperty("os.name").toLowerCase().startsWith("win")){
            fileName = Configuration.TEMP_FILES_PATH + buggyProject + ".bat";
            cmd = Configuration.TEMP_FILES_PATH + buggyProject + ".bat";
        }
        else {
            fileName = Configuration.TEMP_FILES_PATH + buggyProject + ".sh";
            cmd = "bash " + fileName;
        }
        File batFile = new File(fileName);
        if (!batFile.exists()){
        	if (!batFile.getParentFile().exists()) {
        		batFile.getParentFile().mkdirs();
        	}
            boolean result = batFile.createNewFile();
            if (!result){
                throw new IOException("Cannot Create bat file:" + fileName);
            }
        }
        FileOutputStream outputStream = null;
        try {
            outputStream = new FileOutputStream(batFile);
            for (String arg: asList){
                outputStream.write(arg.getBytes());
            }
        } catch (IOException e){
            if (outputStream != null){
                outputStream.close();
            }
        }
        batFile.deleteOnExit();
        
        Process process= Runtime.getRuntime().exec(cmd);
        String results = ShellUtils.getShellOut(process, type);
        batFile.delete();
        return results;
	}

	private static String getShellOut(Process process, int type) {
		ExecutorService service = Executors.newSingleThreadExecutor();
        Future<String> future = service.submit(new ReadShellProcess(process));
        String returnString = "";
        try {
            if (type == 2)
            	returnString = future.get(Configuration.TEST_SHELL_RUN_TIMEOUT, TimeUnit.SECONDS);
            else 
            	returnString = future.get(Configuration.SHELL_RUN_TIMEOUT, TimeUnit.SECONDS);
        } catch (InterruptedException e){
            future.cancel(true);
//            e.printStackTrace();
            shutdownProcess(service, process);
            return "";
        } catch (TimeoutException e){
            future.cancel(true);
//            e.printStackTrace();
            shutdownProcess(service, process);
            return "";
        } catch (ExecutionException e){
            future.cancel(true);
//            e.printStackTrace();
            shutdownProcess(service, process);
            return "";
        } finally {
            shutdownProcess(service, process);
        }
        return returnString;
	}

	private static void shutdownProcess(ExecutorService service, Process process) {
		service.shutdownNow();
        try {
			process.getErrorStream().close();
			process.getInputStream().close();
	        process.getOutputStream().close();
		} catch (IOException e) {
			e.printStackTrace();
		}
        process.destroy();
	}
}

class ReadShellProcess implements Callable<String> {
    public Process process;

    public ReadShellProcess(Process p) {
        this.process = p;
    }

    public synchronized String call() {
        StringBuilder sb = new StringBuilder();
        BufferedInputStream in = null;
        BufferedReader br = null;
        try {
            String s;
            in = new BufferedInputStream(process.getInputStream());
            br = new BufferedReader(new InputStreamReader(in));
            while ((s = br.readLine()) != null && s.length()!=0) {
                if (sb.length() < 1000000){
                    if (Thread.interrupted()){
                        return sb.toString();
                    }
                    sb.append(System.getProperty("line.separator"));
                    sb.append(s);
                }
            }
            in = new BufferedInputStream(process.getErrorStream());
            br = new BufferedReader(new InputStreamReader(in));
            while ((s = br.readLine()) != null && s.length()!=0) {
                if (Thread.interrupted()){
                    return sb.toString();
                }
                if (sb.length() < 1000000){
                    sb.append(System.getProperty("line.separator"));
                    sb.append(s);
                }
            }
        } catch (IOException e){
//            e.printStackTrace();
        } finally {
            if (br != null){
                try {
                    br.close();
                } catch (IOException e){
                }
            }
            if (in != null){
                try {
                    in.close();
                } catch (IOException e){
                }
            }
            process.destroy();
        }
        FileHelper.outputToFile("logs/compile_log.log", sb, true);
        return sb.toString();
    }
}