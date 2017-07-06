package com.ozan.stromtwitter;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ozan.stromtwitter.model.HashtagCount;

@Controller
@SpringBootApplication
public class StormTwitterAnalysisApplication {
	
	@Value("${executeCommand}")
	private String executeCommand;
	
	@Value("${resultPath}")
	private String resultPath;

	public static void main(String[] args) {
		SpringApplication.run(StormTwitterAnalysisApplication.class, args);
		
		openBrowser();
	}
	
	public static void openBrowser(){
		String url = "http://localhost:8181/index";
		String os = System.getProperty("os.name").toLowerCase();
	    Runtime rt = Runtime.getRuntime();
		try{
		    if (os.indexOf( "win" ) >= 0) {
		        // this doesn't support showing urls in the form of "page.html#nameLink" 
		        rt.exec( "rundll32 url.dll,FileProtocolHandler " + url);
		    }else if (os.indexOf( "mac" ) >= 0) {
		        rt.exec( "open " + url);
	        }else if (os.indexOf( "nix") >=0 || os.indexOf( "nux") >=0) {
		        // Do a best guess on unix until we get a platform independent way
		        // Build a list of browsers to try, in this order.
		        String[] browsers = {"epiphany", "firefox", "mozilla", "konqueror",
		       			             "netscape","opera","links","lynx"};		       
		        // Build a command string which looks like "browser1 "url" || browser2 "url" ||..."
		        StringBuffer cmd = new StringBuffer();
		        for (int i=0; i<browsers.length; i++)
		            cmd.append( (i==0  ? "" : " || " ) + browsers[i] +" \"" + url + "\" ");		        	
		        rt.exec(new String[] { "sh", "-c", cmd.toString() });
	           } else {
	                return;
	           }
	       }catch (Exception e){
	    	   return;
	       }
			return;
	}
	
	@RequestMapping("/index")
    public String index(Locale locale, Model model) {
        return "index";
    }
	
	@RequestMapping(value = "/makeAnalysis", method = RequestMethod.POST)
    public String makeAnalysis(Locale locale, Model model,
    		@RequestParam(value = "runDuration", required = true) int runDuration,
    		@RequestParam(value = "keywords", required = true) String keywords) {
		
		deleteFiles();
		
		String executeCmd = executeCommand 
				+ Integer.toString(runDuration) + " "
				+ keywords;
		
		Runtime rt = Runtime.getRuntime();
		
        try {
        	
			Process proc = rt.exec(executeCmd);
			
			StreamGobbler outputGobbler = new 
	                StreamGobbler(proc.getInputStream(), "OUTPUT", resultPath+"output.txt");
			
			outputGobbler.start();
			
			int exitVal = proc.waitFor();
	        System.out.println("ExitValue: " + exitVal);
        
        } catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
        
        ObjectMapper mapper = new ObjectMapper();
        
        List<HashtagCount> hashtags = new ArrayList<HashtagCount>();
        
        try{
        	
        	File hastagFile = new File(resultPath+"hashtag.txt");
        	BufferedReader br = new BufferedReader(new FileReader(hastagFile));
        	
			String line;
            while ((line = br.readLine()) != null) {
            	line = line.trim();
            	HashtagCount hashtagCount = mapper.readValue(line, HashtagCount.class);
            	hashtags.add(hashtagCount);
            }
        
            br.close();
        }catch (Exception e) {
			
		}
        
        model.addAttribute("hashtags", hashtags);
        return "index";
	}
	
	class StreamGobbler extends Thread
	{
	    InputStream is;
	    String type;
	    String backupPath;
	    
	    StreamGobbler(InputStream is, String type, String backupPath)
	    {
	        this.is = is;
	        this.type = type;
	        this.backupPath = backupPath;
	    }
	    
	    public void run()
	    {
	        try
	        {
	            InputStreamReader isr = new InputStreamReader(is);
	            BufferedReader br = new BufferedReader(isr);
	            String line=null;
	            
	            if(backupPath != null){
	            	PrintWriter writer = new PrintWriter(backupPath, "UTF-8");
	        	    while ( (line = br.readLine()) != null){
	        	    	writer.println(line);
	        	    }
	        	    writer.close();
	            }else{
	            	while ( (line = br.readLine()) != null){
	        	    	System.out.println(line);
	        	    }
	            }
	        } catch (IOException ioe){
	        	ioe.printStackTrace();  
	        }
	    }
	}
	
	private void deleteFiles(){
		
		try{
    		File file = new File(resultPath+"hashtag.txt");
    		file.delete();
    	}catch(Exception e){
    		e.printStackTrace();
    	}
		
		try{
    		File file = new File(resultPath+"links.txt");
    		file.delete();
    	}catch(Exception e){
    		e.printStackTrace();
    	}
		
		try{
    		File file = new File(resultPath+"media.txt");
    		file.delete();
    	}catch(Exception e){
    		e.printStackTrace();
    	}
		
		try{
    		File file = new File(resultPath+"output.txt");
    		file.delete();
    	}catch(Exception e){
    		e.printStackTrace();
    	}
		
	}
}
