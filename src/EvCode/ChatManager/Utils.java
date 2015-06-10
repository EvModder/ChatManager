package EvCode.ChatManager;

public class Utils {
	
	/** Strip punctuation **/
	public String removePunctuation(String oldChat){
		return oldChat.replaceAll("[^a-zA-Z0-9]", "");
	}
	
	/** leet converter **/
	public String convertFrom1337(String oldChat){
		char[] chars = oldChat.toCharArray();
		
		StringBuilder builder = new StringBuilder();
		builder.append(chars[0]);
		for(int i = 1; i < chars.length; i++){
			if(chars[i] == '1')builder.append('i');
			else if(chars[i] == '3')builder.append('e');
			else if(chars[i] == '4')builder.append('a');
			else if(chars[i] == '5')builder.append('s');
			else if(chars[i] == '6')builder.append('g');
			else if(chars[i] == '7')builder.append('t');
			else if(chars[i] == '0')builder.append('o');
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
}
