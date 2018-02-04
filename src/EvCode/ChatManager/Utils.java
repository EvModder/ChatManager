package EvCode.ChatManager;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

public class Utils {
	
	/** Strip punctuation, NOTE: Does not remove spaces **/
	public String removePunctuation(String oldChat){
		return oldChat.replaceAll("[^a-zA-Z\\s]", "");
	}
	
	/** Strip non-alphanumeric charactes, NOTE: Does not remove spaces **/
	public String removeNonAlphanumeric(String oldChat){
		return oldChat.replaceAll("[^a-zA-Z0-9\\s]", "");
	}
	
	/** Strip all lowercase characters and punctuation **/
	public String removeLowerCaseAndPunc(String oldChat){
		return oldChat.replaceAll("[^A-Z\\s]", "");
	}
	
	/** Strip all uppercase characters and punctuation **/
	public String removeUpperCaseAndPunc(String oldChat){
		return oldChat.replaceAll("[^a-z\\s]", "");
	}
	
	/** Strip all lowercase characters **/
	public String removeLowerCase(String oldChat){
		return oldChat.replaceAll("[a-z]", "");
	}
	
	/** Strip all uppercase characters **/
	public String removeUpperCase(String oldChat){
		return oldChat.replaceAll("[A-Z]", "");
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
			if(chars[i] == '1' || chars[i] == '|')builder.append('L');
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
	
	/** Decipher colors in a string and place them based on permissions **/
	char[] chatColors = {'1','2','3','4','5','6','7','8','9','0','a','b','c','d','e','f','r'};
	public String determineColorsByPermission(String str, Player player){
		if(str.replace("&&", "").replace("\\&", "").contains("&") == false) return str;
		
		for(char color : chatColors){
			if(str.contains("&") == false) break;
			if(str.contains("&"+color) && (VaultHook.hasPermission(player, "chatmanager.color."+color)
					|| VaultHook.hasPermission(player, "chatmanager.color."+ChatColor.getByChar(color).name().toLowerCase()))){
				str = str.replace("&"+color, "§"+color);
			}
		}
		return str.replace("&§", "&").replace("\\§", "&");
	}
	
	/** Decipher formats in a string and place them based on permissions **/
	char[] chatFomats = {'k','l','m','n','o','r','f'};
	public String determineFormatsByPermission(String str, Player player){
		if(str.replace("&&", "").replace("\\&", "").contains("&") == false) return str;
		
		for(char format : chatColors){
			if(str.contains("&") == false) break;
			if(str.contains("&"+format) && (VaultHook.hasPermission(player, "chatmanager.format."+format)
					|| VaultHook.hasPermission(player, "chatmanager.format."+ChatColor.getByChar(format).name().toLowerCase()))){
				str = str.replace("&"+format, "§"+format);
			}
		}
		return str.replace("&§", "&").replace("\\§", "&");
	}
	
	/** Replace all occurances of the regex with the replacement **/
	public String replaceIgnoreCase(String string, String regex, String replacement){
		return string.replaceAll("(?i)"+regex, replacement);
	}
}
