package net.evmodder.ChatManager;

import java.util.HashMap;
import org.apache.commons.lang.StringUtils;
import net.evmodder.EvLib.FileIO;

public class ProfanityFilter{
	// key=badword, value=replaceword, if value=null, replaces using default
	private HashMap<String, String> badWords;
	private String defaultSub = "*";//"[-]";
	final boolean CHECK_WORDS_BACKWARDS = false;
	final int MIN_WORD_LENGTH_TO_CHECK_BACKWARDS = 4;
	
	ProfanityFilter(ChatManager pl){
		badWords = new HashMap<String, String>();

		loadBadWords(pl, badWords, FileIO.loadResource(pl, "defaultblocked.txt").replace("\n", ""));
		loadBadWords(pl, badWords, FileIO.loadFile("blocked-words.txt",
					"thisisabadword,badword248,a@@ , word45 ,CRAPS**T,lolbutts,LMFAO=haha,flycoder=qua-112" +
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
				     "\n# edit the default list there)"));

		if(CHECK_WORDS_BACKWARDS){
			HashMap<String, String> badWordsBackwards = new HashMap<>();
			badWords.entrySet().stream()
					.filter(e -> !e.getKey().trim().contains(" ") &&
							e.getKey().length() >= MIN_WORD_LENGTH_TO_CHECK_BACKWARDS &&
							e.getKey().equals(ChatUtils.removePunctuation(e.getKey())) &&
							badWords.get(ChatUtils.reverse(e.getKey())) == null)
					.forEach(e -> badWordsBackwards.put(ChatUtils.reverse(e.getKey()), e.getValue()));
		}

		//loop through the subList and remove instances of "sameword=sameword"
		badWords.entrySet().removeIf(e -> e.getKey().equals(e.getValue()));
	}
	
	void loadBadWords(ChatManager pl, HashMap<String, String> badWords, String rawBlockedWordsFileContent){
		String[] blockedWords = FileIO.loadResource(pl, "defaultblocked.txt").replace("\n", "").split(",");
		for(String word : blockedWords){
			if(!word.trim().isEmpty()){
				word = word.replace("\\=", "<equals>").replace("=", "<split>").replace("<equals>", "=");
				if(word.contains("<split>")){
					String[] pair = word.split("<split>");
					if(!pair[0].equals(pair[1])){
						if(!badWords.containsKey(pair[0])) badWords.put(pair[0], null);
						badWords.put(pair[0].trim(), pair[1].trim());
					}
					else{//If they did "damn=damn", they want to unblock the default.
						badWords.remove(pair[0]);
					}
				}
				else if(!badWords.containsKey(word)) badWords.put(word, null);
			}
		}
	}

	public String filterOutBadWords(String chat){
		String lowerCaseChat = chat.toLowerCase();
		for(String badword : badWords.keySet()){
			if(chat.contains(badword)){
				String trimmed = badword.trim();
				String subword = badWords.get(trimmed);
				chat = chat.replace(badword,
					(badword.startsWith(" ") ? " " : "") +
					(subword != null ? subword :
					(defaultSub.length() != 1 ? defaultSub : StringUtils.repeat(defaultSub, trimmed.length()-1)) +
					(badword.endsWith(" ") ? " " : "")));
			}
			else if(lowerCaseChat.contains(badword)){
				String trimmed = badword.trim();
				String subword = badWords.get(trimmed);
				chat = ChatUtils.replaceIgnoreCase(chat, badword,
					(badword.startsWith(" ") ? " " : "") +
					(subword != null ? subword :
					(defaultSub.length() != 1 ? defaultSub : StringUtils.repeat(defaultSub, trimmed.length()-1)) +
					(badword.endsWith(" ") ? " " : "")));
			}
		}
		return chat;
	}

	public boolean hasBadWords(String chat){
		return badWords.keySet().stream().anyMatch(badword -> chat.contains(badword));
	}
}
