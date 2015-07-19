package EvCode.ChatManager;

public class Utils {
	
	/** Strip punctuation, NOTE: Does not remove spaces **/
	public String removePunctuation(String oldChat){
		return oldChat.replaceAll("[^a-zA-Z\\s]", "");
	}
	
	/** Strip all lowercase characters and punctuation **/
	public String removeLowerCaseAndPunc(String oldChat){
		return oldChat.replaceAll("[^A-Z\\s]", "");
	}
	
	/** Reverse the order of chars in a string **/
	public String reverse(String oldChat){
		char[] chars = oldChat.toCharArray();
		
		StringBuilder builder = new StringBuilder();
		for(int i = chars.length-1; i >= 0; i--) builder.append(chars[i]);
		return builder.toString();
	}
	
	/** leet converter **/
	public String convertFrom1337(String oldChat){
		char[] chars = oldChat.toCharArray();
		
		StringBuilder builder = new StringBuilder();
		for(int i = 0; i < chars.length; i++){
			if(chars[i] == '1')builder.append('i');
			else if(chars[i] == '3')builder.append('E');
			else if(chars[i] == '4' || chars[i] == '@')builder.append('A');
			else if(chars[i] == '5' || chars[i] == '$')builder.append('S');
			else if(chars[i] == '6')builder.append('G');
			else if(chars[i] == '7')builder.append('T');
			else if(chars[i] == '0')builder.append('O');
			else if(chars[i] == '#')builder.append('H');
			else builder.append(chars[i]);
		}
		return builder.toString();
	}

	/** Combines repeated characters, except for the letter 'o'.
	  * This is useful for detecting bad words written like such: "niggggger" **/
	public String combineRepeatedChars(String oldChat){
		char[] chars = oldChat.toCharArray();
		
		StringBuilder builder = new StringBuilder();
		builder.append(chars[0]);
		for(int i = 1; i < chars.length; i++){
			if(chars[i] != chars[i-1] || chars[i] == 'o')builder.append(chars[i]);
		}
		return builder.toString();
	}
	
	/** Decipher colors in a string **/
	public String determineColors(String str){
		return str
			.replace("&1", "§1").replace("&2", "§2").replace("&3", "§3")
			.replace("&4", "§4").replace("&5", "§5").replace("&6", "§6")
			.replace("&7", "§7").replace("&8", "§8").replace("&9", "§9")
			.replace("&0", "§0").replace("&r", "§r")
			.replace("&a", "§a").replace("&b", "§b").replace("&c", "§c")
			.replace("&d", "§d").replace("&e", "§e").replace("&f", "§f")
			.replace("§§", "&").replace("\\§", "&");
	}
	
	/** Decipher formats in a string **/
	public String determineFormats(String str){
		return str
			.replace("&k", "§k").replace("&l", "§l").replace("&m", "§m")
			.replace("&n", "§n").replace("&o", "§o").replace("&r", "§r")
			.replace("&f", "§f").replace("§§", "&").replace("\\§", "&");
	}
	
	/** Replace all occurances of the regex with the replacement **/
	public String replaceIgnoreCase(String string, String regex, String replacement){
		return string.replaceAll("(?i)"+regex, replacement);
	}
}
