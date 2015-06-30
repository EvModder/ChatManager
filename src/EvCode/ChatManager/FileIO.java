package EvCode.ChatManager;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Map;
import java.util.Set;

public class FileIO {
	public static void loadDefaultBlockedList(ChatManager plugin, Set<String> wordList, Map<String, String> subList){
		BufferedReader reader = null;
		reader = new BufferedReader(
				new InputStreamReader(
				plugin.getClass()
				.getResourceAsStream("/defaultblocked.txt")));
		
		if(reader != null){
			StringBuilder builder = new StringBuilder();
			String line = null;
			try{
				while((line = reader.readLine()) != null){
					if(!line.replace(" ", "").startsWith("//") && !line.replace(" ", "").startsWith("#")) builder.append(line);
				}reader.close();
			}
			catch(IOException e){plugin.getLogger().info(e.getMessage());}
			
			for(String word : builder.toString().replace("\", \"", "").replace("\",\"", "").replace("\"", "").split(",")){
				if(word.length() > 1 && !word.replace(" ", "").isEmpty()){
					wordList.add(word.split("=")[0]);
					if(word.contains("=")) subList.put(word.split("=")[0], word.split("=")[1]);
				}
			}
			
			if(wordList.isEmpty()){
				/** If unable to load from file, load a hard-coded list **/
//				for(int i = 0; i < defBadWords.length; i++) wordList.add(defBadWords[i]);
			}
		}
	}
	
	public static void loadCustomBlockedList(ChatManager plugin, Set<String> wordList, Map<String, String> subList){
		
		BufferedReader reader = null;
		try{reader = new BufferedReader(new FileReader("./plugins/EvFolder/blocked words list.txt"));}
		catch(FileNotFoundException e){
			//Create the file
			File blockedList = new File("./plugins/EvFolder/blocked words list.txt");
			try{
				//eww hardcoded text..
				BufferedWriter writer = new BufferedWriter(new FileWriter(blockedList));
				writer.write("thisisabadword,badword248,a@@ , word45 ,CRAPS**T,lolbutts,LMFAO=haha" +
					   "\n\n# Spaces are fine to use, but please realize that " +
						 "\n# they will be counted as a part of a word." +
					  "\n#\n# To set a custom replacement (sub) for a bad word, simply use" +
					     "\n# '='" +
					     "\n# For example, to have \"hell\" replaced with \"nether\", write it" +
					     "\n# like this:" +
					     "\n# thisisabadword,hell=nether,ass=donkey,f***=meep,a@@,etc.." +
					  "\n#\n# If you want to allow a word on the 'Default blocked list'," +
					     "\n# Then put it in like this: hell=hell,crap=crap,f***=meep" +
					     "\n# What this does is simply \"replace\" the badword with itself" +
					     "\n# (Note that you can also open/unzip the .jar file and directly" +
					     "\n# edit the default list there)");
				writer.flush();
				writer.close();
			}
			catch(IOException e1){plugin.getLogger().info(e1.getStackTrace().toString());}
			//Attempt again to load the file
			try{reader = new BufferedReader(new FileReader("./plugins/EvFolder/blocked words list.txt"));}
			catch(FileNotFoundException e2){plugin.getLogger().info(e2.getStackTrace().toString());reader = null;}
		}
		if(reader != null){
			StringBuilder builder = new StringBuilder();
			String line = null;
			try{
				while((line = reader.readLine()) != null){
					if(!line.replace(" ", "").startsWith("//") && !line.replace(" ", "").startsWith("#")) builder.append(line);
				}reader.close();
			}
			catch(IOException e){plugin.getLogger().info(e.getMessage());}
			
			for(String word : builder.toString().split(",")){
				if(word.length() > 1 && !word.replace(" ", "").isEmpty()){
					wordList.add(word.split("=")[0]);
					if(word.contains("=")) subList.put(word.split("=")[0], word.split("=")[1]);
				}
			}
		}
	}
	
	public static void loadBlockedBlockList(ChatManager plugin, Set<String> wordList){
		
		BufferedReader reader = null;
		try{reader = new BufferedReader(new FileReader("./plugins/EvFolder/blocked block-words list.txt"));}
		catch(FileNotFoundException e){
			//Create the file
			File blockedList = new File("./plugins/EvFolder/blocked block-words list.txt");
			try{
				//eww hardcoded text..
				BufferedWriter writer = new BufferedWriter(new FileWriter(blockedList));
				writer.write("#Put words in here that should be detected and removed when built in block form");
				writer.flush();
				writer.close();
			}
			catch(IOException e1){plugin.getLogger().info(e1.getStackTrace().toString());}
			//Attempt again to load the file
			try{reader = new BufferedReader(new FileReader("./plugins/EvFolder/blocked block-words list.txt"));}
			catch(FileNotFoundException e2){plugin.getLogger().info(e2.getStackTrace().toString());reader = null;}
		}
		if(reader != null){
			StringBuilder builder = new StringBuilder();
			String line = null;
			try{
				while((line = reader.readLine()) != null){
					line = line.replace(" ", "").replace("//", "");
					if(!line.startsWith("#")) builder.append(line);
				}reader.close();
			}
			catch(IOException e){plugin.getLogger().info(e.getMessage());}
			
			for(String word : builder.toString().split(",")){
				if(word.length() > 1) wordList.add(word);
			}
		}
	}
}
