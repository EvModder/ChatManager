package net.evmodder.ChatManager;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

public final class ChatUtils{

//	/** Strip punctuation, NOTE: Does not remove spaces **/
//	public static String removePunctuation(String oldChat){return oldChat.replaceAll("[^a-zA-Z0\\s]", "");}

	/** Strip non-alphanumeric charactes, NOTE: Does not remove spaces **/
	public static String removeNonAlphanumeric(String oldChat){return oldChat.replaceAll("[^a-zA-Z0-9\\s]", "");}

	/** Strip all lowercase characters and punctuation **/
	public static String removeLowerCaseAndPunc(String oldChat){return oldChat.replaceAll("[^A-Z\\s]", "");}

	/** Strip all uppercase characters and punctuation **/
	public static String removeUpperCaseAndPunc(String oldChat){return oldChat.replaceAll("[^a-z\\s]", "");}

	/** Strip all lowercase characters **/
	public static String removeLowerCase(String oldChat){return oldChat.replaceAll("[a-z]", "");}

	/** Strip all uppercase characters **/
	public static String removeUpperCase(String oldChat){return oldChat.replaceAll("[A-Z]", "");}

	/** Reverse the order of chars in a string **/
	public static String reverse(String oldChat){return new StringBuilder(oldChat).reverse().toString();}

	/** leet converter **/
	public static String convertFrom1337(String oldChat){
		char[] chars = oldChat.toCharArray();
		StringBuilder builder = new StringBuilder();
		for(int i = 0; i < chars.length; ++i){
			switch(chars[i]){
				case ChatColor.COLOR_CHAR: case '&':
					builder.append(chars[i]).append(chars[++i]);
					break;
				case '1': case '|': builder.append('L'); break;
				case '4': case '@': builder.append('A'); break;
				case '5': case '$': builder.append('S'); break;
				case '3': builder.append('E'); break;
				case '6': builder.append('G'); break;
				case '7': builder.append('T'); break;
				case '0': builder.append('O'); break;
				case '#': builder.append('H'); break;
				default: builder.append(chars[i]);
			}
		}
		return builder.toString();
	}

	/** Combines repeated characters, except for the letter 'o'.
	  * This is useful for detecting bad words written like this: "dammmnn" **/
	public static String combineRepeatedChars(String oldChat){
		char[] chars = oldChat.toCharArray();
		StringBuilder builder = new StringBuilder();
		builder.append(chars[0]);
		for(int i = 1; i < chars.length; ++i){
			if(chars[i] != chars[i-1] || chars[i] == 'o')builder.append(chars[i]);
		}
		return builder.toString();
	}

	/** Decipher colors in a string **/
	public static String determineColors(String str){
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
	public static String determineFormats(String str){
		return str
			.replace("&k", "§k").replace("&l", "§l").replace("&m", "§m")
			.replace("&n", "§n").replace("&o", "§o").replace("&r", "§r")
			.replace("&f", "§f").replace("§§", "&").replace("\\§", "&");
	}
	
	/** Decipher colors in a string and place them based on permissions **/
	static char[] chatColors = {'1','2','3','4','5','6','7','8','9','0','a','b','c','d','e','f','r'};
	public static String determineColorsByPermission(String str, Player player){
		if(str.replace("&&", "").replace("\\&", "").contains("&") == false) return str;
		
		for(char color : chatColors){
			if(str.contains("&") == false) break;
			if(str.contains("&"+color) && (player.hasPermission("chatmanager.color."+color)
					|| player.hasPermission("chatmanager.color."+ChatColor.getByChar(color).name().toLowerCase()))){
				str = str.replace("&"+color, "§"+color);
			}
		}
		return str.replace("&§", "&").replace("\\§", "&");
	}
	
	/** Decipher formats in a string and place them based on permissions **/
	static char[] chatFomats = {'k','l','m','n','o','r','f'};
	public static String determineFormatsByPermission(String str, Player player){
		if(str.replace("&&", "").replace("\\&", "").contains("&") == false) return str;
		
		for(char format : chatColors){
			if(str.contains("&") == false) break;
			if(str.contains("&"+format) && (player.hasPermission("chatmanager.format."+format)
					|| player.hasPermission("chatmanager.format."+ChatColor.getByChar(format).name().toLowerCase()))){
				str = str.replace("&"+format, "§"+format);
			}
		}
		return str.replace("&§", "&").replace("\\§", "&");
	}
	
	/** Replace all occurances of the regex with the replacement **/
	public static String replaceIgnoreCase(String string, String regex, String replacement){
		return string.replaceAll("(?i)"+regex, replacement);
	}
}
