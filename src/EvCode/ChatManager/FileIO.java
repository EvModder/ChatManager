package EvCode.ChatManager;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Map;

public class FileIO {
	public static void loadDefaultBlockedList(ChatManager plugin, List<String> wordList, Map<String, String> subList){
		BufferedReader reader = null;
		reader = new BufferedReader(new InputStreamReader(plugin.getClass().getResourceAsStream("/defaultblocked.txt")));
		
		if(reader != null){
			StringBuilder builder = new StringBuilder();
			String line = null;
			try{
				while((line = reader.readLine()) != null){
					if(!line.replace(" ", "").replace("#", "//").startsWith("//")) builder.append(line);
				}reader.close();
			}
			catch(IOException e){plugin.getLogger().info(e.getMessage());}
			
			for(String word : builder.toString().split(",")){
				if(!word.trim().isEmpty()){
					word = word.replace("\\=", "<equals>").replace("=", "<split>").replace("<equals>", "=");
					if(word.contains("<split>")){
						String[] pair = word.split("<split>");
						subList.put(pair[0].trim(), pair[1].trim());
						if(!wordList.contains(pair[0])) wordList.add(pair[0]);
					}
					else if(!wordList.contains(word)) wordList.add(word);
				}
			}
			
			if(wordList.isEmpty()){
				/** If unable to load from file, load a hard-coded list **/
//				for(int i = 0; i < defBadWords.length; i++) wordList.add(defBadWords[i]);
			}
		}
	}
	
	public static void loadCustomBlockedList(ChatManager plugin, List<String> wordList, Map<String, String> subList){
		BufferedReader reader = null;
		try{reader = new BufferedReader(new FileReader("./plugins/EvFolder/blocked words list.txt"));}
		catch(FileNotFoundException e){
			//Create the file
			File blockedList = new File("./plugins/EvFolder/blocked words list.txt");
			try{
				//eww hardcoded text..
				BufferedWriter writer = new BufferedWriter(new FileWriter(blockedList));
				writer.write("thisisabadword,badword248,a@@ , word45 ,CRAPS**T,lolbutts,LMFAO=haha,flycoder=qua-112" +
					   "\n\n# Spaces are fine to use, but please realize that " +
						 "\n# they will be counted as a part of a word." +
					  "\n#\n# To set a custom replacement (sub) for a bad word, simply use" +
					     "\n# '='" +
					     "\n# For example, to have \"hell\" replaced with \"nether\", write it" +
					     "\n# like this:" +
					     "\n# thisisabadword,hell=nether,ass=donkey,f***=meep,a@@,etc.." +
					  "\n#\n# If you want to allow a word on the 'Default blocked list'," +
					     "\n# Then put it in like this: hell=hell,crap=crap," +
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
					line = line.replace("//", "#").trim();
					if(!line.startsWith("#")) builder.append(line);
				}reader.close();
			}
			catch(IOException e){plugin.getLogger().info(e.getMessage());}
			
			for(String word : builder.toString().split(",")){
				if(!word.trim().isEmpty()){
					word = word.replace("\\=", "<equals>").replace("=", "<split>").replace("<equals>", "=");
					if(word.contains("<split>")){
						String[] pair = word.split("<split>");
						
						if(pair[0].equals(pair[1]) == false){
							subList.put(pair[0].trim(), pair[1].trim());
							if(!wordList.contains(pair[0])) wordList.add(pair[0]);
						}
						else wordList.remove(pair[0]);//If they did "damn=damn", they want to unblock the default.
					}
					else if(!wordList.contains(word)) wordList.add(word);
				}
			}
		}
	}
	
/*	public static void loadBlockedBlockList(ChatManager plugin, Set<String> wordList){
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
	}*/
	
	public static String loadFile(String filename) {
		BufferedReader reader = null;
		try{reader = new BufferedReader(new FileReader("./plugins/EvFolder/"+filename));}
		catch(FileNotFoundException e){
			return "";
		}
		StringBuilder file = new StringBuilder();
		if(reader != null){
			String line = null;
			try{
				while((line = reader.readLine()) != null){file.append(line);file.append('\n');}
				reader.close();
			}catch(IOException e){}
		}
		if(file.length() > 0) file.substring(0, file.length()-1);
		return file.toString();
	}

	public static boolean saveFile(String filename, String content) {
		//Create Directory
		File dir = new File("./plugins/EvFolder");
		if(!dir.exists()){dir.mkdir(); System.out.println("Directory Created!");}
		
		//Create the file
		try{
			BufferedWriter writer = new BufferedWriter(new FileWriter("./plugins/EvFolder/"+filename));
			writer.write(content); writer.close();
			return true;
		}
		catch(IOException e){return false;}
	}
}
