package EvCode.ChatManager;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;

import org.apache.commons.lang.StringUtils;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
public final class ChatManager extends JavaPlugin implements Listener{
	/** Config options **/
	boolean antiSpam = true, antiChatFilth = true, antiCmdFilth, removeCaps = true;
	boolean chatColor = true, chatFormat = true, signColor = true, signFormat = true;
	boolean ignoreAmperstand = true, antiSignFilth = true, checkWordsBackwards = false, autoUpdate = true;
	private String pluginPrefix = "§3<§aC§3>§f ";
	
	
	/** Anti-Spam configuration **/
	private String spamResultCmd = "kick %name% §cReceived TMC/TMS from client\\n§fEither lag or spam... :P";
	private int maxChatsPerMinute = 35, maxChatsPer10s = 15, maxChatsPerSecond = 2;
	private Map<UUID, List<Integer>> lastChats;
	
	
	/** Anti-Filth configuration **/
	private Set<String> badWords;
	
	private Map<String, String> subList;
	private String defaultSub = "[-]";
	Utils utils = new Utils();
	
	/** If fewer/more then this number are available, such as after a plugin update,
	 ** then the server's copy of the config will be updated. */
	final int AVAILABLE_SETTINGS = 14;
	
	final int projectID = 63180;//<-- Can be found at: https://api.curseforge.com/servermods/projects?search=ev-cleanchat
	@Override public void onEnable(){
		/** Load config **/
		loadConfig();
		
		/** Check for an update **/
		if(autoUpdate) new Updater(this, projectID, this.getFile(), Updater.UpdateType.DEFAULT, true);
		else  new Updater(this, projectID, this.getFile(), Updater.UpdateType.NO_DOWNLOAD, true);
		
		/** Initialize variables **/
		lastChats = new HashMap<UUID, List<Integer>>();
		badWords = new HashSet<String>();
		subList = new HashMap<String, String>();
		
		/** Load word lists **/
		FileIO.loadDefaultBlockedList(this, badWords, subList);
		FileIO.loadCustomBlockedList(this, badWords, subList);
		
		if(checkWordsBackwards){
			Set<String> badWordsBackwards = new HashSet<String>();
			String newBadWord;
			
			for(String badword : badWords){
				if(!badword.trim().contains(" ") && badword.length() > 3
						&& badword.equals(utils.removePunctuation(badword))){
					newBadWord = utils.reverse(badword);
					badWordsBackwards.add(newBadWord);
					if(subList.containsKey(badword)) subList.put(newBadWord, subList.get(badword));
				}
			}
			badWords.addAll(badWordsBackwards);
		}
		
		/** Runs every 1 second to update recored chats and clear chats with old timestamps **/
//		getServer().getScheduler().scheduleSyncRepeatingTask(this, new Runnable(){public void run(){updateLastChats();}}, 100, 20);
		
		/** Check for Vault **/
		new VaultHook(this);
		
		/** Register listener events with the server **/
		getServer().getPluginManager().registerEvents(this, this);
	}
	
	public boolean onCommand(CommandSender sender, Command cmd, String label, String args[]){
		if(cmd.getName().equalsIgnoreCase("evcm") && args.length > 0){
			args[0] = args[0].toLowerCase();
			//=================================================================================================================
			if(args[0].contains("spam") && VaultHook.hasPermission(sender, "evp.evcm.togglespam")){
				if(args.length >= 2){
					if(args[1].equalsIgnoreCase("on")) antiSpam = true;
					else antiSpam = false;
				}
				else antiSpam = !antiSpam;
				
				if(antiSpam) sender.sendMessage("SpamGuard Enabled.");
				else sender.sendMessage("SpamGuard Disabled.");
			}//-----------------------------------------------------------------------------------------------------------
			else if(args[0].contains("filth") && VaultHook.hasPermission(sender, "evp.evcm.togglefilth")){
				if(args.length >= 2){
					if(args[1].equalsIgnoreCase("on")) antiChatFilth = true;
					else antiChatFilth = false;
				}
				else antiChatFilth = !antiChatFilth;
				
				if(antiChatFilth) sender.sendMessage("FilthGuard Enabled.");
				else sender.sendMessage("FilthGuard Disabled.");
			}//-----------------------------------------------------------------------------------------------------------
			//~ ~ ~ ~ ~ ~ ~ ~
			else if(args[0].contains("color") && VaultHook.hasPermission(sender, "evp.evcm.togglecolor")){
				if(args.length >= 2){
					if(args[1].equalsIgnoreCase("on")) chatColor = true;
					else chatColor = false;
				}
				else chatColor = !chatColor;
				
				if(chatColor) sender.sendMessage("Chat-Colors Enabled.");
				else sender.sendMessage("Chat-Colors Disabled.");
			}//-----------------------------------------------------------------------------------------------------------
			else if(args[0].contains("format") && VaultHook.hasPermission(sender, "evp.evcm.toggleformat")){
				if(args.length >= 2){
					if(args[1].equalsIgnoreCase("on")) chatFormat = true;
					else chatFormat = false;
				}
				else chatFormat = !chatFormat;
				
				if(chatFormat) sender.sendMessage("Chat-Formats Enabled.");
				else sender.sendMessage("Chat-Formats Disabled.");
			}//-----------------------------------------------------------------------------------------------------------
			else if(args[0].equals("help") || args[0].equals("list") || args[0].equals("info") || args[0].equals("?")){
				sender.sendMessage("§a--- §6< §2§lEvCM Commands §6> §a---\n" +
							"§21§7. /evcm anti-spam [on/off/(toggle)]\n" +
							"§22§7. /evcm anti-filth [on/off/(toggle)]\n" +
							"§23§7. /evcm chatcolor [on/off/(toggle)]\n" +
							"§24§7. /evcm chatformat [on/off/(toggle)]\n" +
							"§24§7. /evcm reload   (or /evcm config)\n" +
							"§7§l----------------------------------");
			}
			else if((args[0].equals("reload") || args[0].equals("load") || args[0].equals("config"))
					&& VaultHook.hasPermission(sender, "evp.evcm.reload")){
				onEnable();
				sender.sendMessage("§aFiles Reloaded!");
			}
			else{
				sender.sendMessage("§Unknown evcm command!");
				sender.sendMessage("§7Type §5/cm ?§7 for a list of ChatManager commands.");
				
			}
			return true;
			//=============================================================================================================
		}
		else return false;
	}
	
	@EventHandler(priority = EventPriority.HIGHEST)
	public void onPlayerChat(AsyncPlayerChatEvent event){
		//if it is already cancelled or is something a plugin forced the player to say
		if(event.isCancelled() || !event.isAsynchronous())return;
		
		String chat = ' '+event.getMessage()+' ';
		String pName = event.getPlayer().getName();
		
		//-------------------------------------------------------------------------|
		if(antiChatFilth && !VaultHook.hasPermission(event.getPlayer(), "evp.evcm.chatfilter.exempt")){
			chat = filterOutBadWords(chat);
			
			String newChat = utils.removePunctuation(chat);
			if(hasBadWords(newChat)){
				chat = (defaultSub.length() > 1) ? defaultSub : StringUtils.repeat(defaultSub, chat.length()/2 +1);
				getLogger().info("De-Punc. Chat: "+newChat);
			}
			else{
				newChat = utils.combineRepeatedChars(utils.removeLowerCaseAndPunc(newChat));
				if(hasBadWords(newChat)){
					chat = (defaultSub.length() > 1) ? defaultSub : StringUtils.repeat(defaultSub, chat.length()/2 +1);
					getLogger().info("Uppercase-Only Chat: "+newChat);
				}
				else{
					newChat = utils.removePunctuation(utils.convertFrom1337(chat));
					
					if(newChat.length() > 0){
						newChat = utils.combineRepeatedChars(newChat);
						if(hasBadWords(newChat)){
							chat = (defaultSub.length() > 1) ? defaultSub : StringUtils.repeat(defaultSub, chat.length()/2 +1);
							getLogger().info("Abv. Chat: "+newChat);
						}
						else{
							newChat = utils.removeLowerCaseAndPunc(newChat);
							if(hasBadWords(newChat)){
								chat = (defaultSub.length() > 1) ? defaultSub : StringUtils.repeat(defaultSub, chat.length()/2 +1);
								getLogger().info("Uppercase-Only Chat: "+newChat);
							}
						}
					}
				}
			}
			
			if(chat.equalsIgnoreCase("<-> "+event.getMessage()+" <->") == false){
				// they've been naughty!
				// TODO: maybe add some punishment or something here
			}
		}
		//-------------------------------------------------------------------------|
		
		if(antiSpam && !event.getPlayer().hasPermission("evp.evcm.spamfilter.exempt")){
			try{
				lastChats.get(event.getPlayer().getUniqueId()).add(0);
			}
			catch(NullPointerException ex){
				lastChats.put(event.getPlayer().getUniqueId(), new ArrayList<Integer>());
				lastChats.get(event.getPlayer().getUniqueId()).add(0);
			}
			updateLastChats(true);
			
			// Chat timestamps are thrown out every minute or so
			int inLastSecond=0, inLast10s=0, inLastMinute=0;
			for(int timeElapsed : lastChats.get(event.getPlayer().getUniqueId())){
				
				if(timeElapsed <= 60){
					inLastMinute++;
					if(timeElapsed <= 10){
						inLast10s++;
						if(timeElapsed <= 1) inLastSecond++;
					}
				}
			}
			if(inLastSecond > maxChatsPerSecond || inLast10s > maxChatsPer10s || inLastMinute > maxChatsPerMinute){
				event.getPlayer().sendMessage(pluginPrefix + "Please slow down chat a little.");
				
				//If they continue to spam after the warning..
				if(inLastSecond > maxChatsPerSecond+2 || inLast10s > maxChatsPer10s+3 || inLastMinute > maxChatsPerMinute+5){
//					getServer().dispatchCommand(getServer().getConsoleSender(), spamResultCmd);
					final String name = pName;
					getServer().getScheduler().scheduleSyncDelayedTask(this, new Runnable(){@Override public void run(){
						getServer().dispatchCommand(getServer().getConsoleSender(), spamResultCmd.replace("%name%", name));
					}});
				}
			}
			
			//Keep players from repeating messages --------------------------------------------------------------
			int chatLength =
				ignoreAmperstand ? chat.replace("&", "").replace(defaultSub, "").length() : chat.replace(defaultSub, "").length();
					
			String noPuncChat = utils.removePunctuation(chat);
			if(noPuncChat.length() > 4 || noPuncChat.equals(noPuncChat.toUpperCase())){
				
			}
			//---------------------------------------------------------------------------------------------------
			//if more then 55% of the chat is non-alphanumerical, remove the excess punctuation
			if((chatLength > 7 && chatLength*.55 < chatLength-noPuncChat.length())){
				chat = noPuncChat;
				event.getPlayer().sendMessage(pluginPrefix+"§cSpam Detect. Perhaps try with less punctuation.");
			}
			
			//If more then 55% of the chat is uppercase, make the chat lowercase
			if(removeCaps && noPuncChat.length() > 13 && chatLength*.55 < noPuncChat.replaceAll("[^A-Z]", "").length()){
				chat = chat.toLowerCase();
				event.getPlayer().sendMessage(pluginPrefix+"§cPlease don't shout. My ears are hurting xP");
			}
		}
		
		if(chatFormat && event.getPlayer().hasPermission("evp.evcm.chatformat")){
			chat = utils.determineFormats(chat);
			
			if(chatColor && event.getPlayer().hasPermission("evp.evcm.chatcolor")){
				chat = utils.determineColors(chat);
			}
			else{
				// Look for a default message color, and if found, prevent them from changing the color with &f or &r
				String totalMessage = event.getFormat()+chat;
				int lastColorIndex = totalMessage.substring(0, totalMessage.length()-1).lastIndexOf('§');
				ChatColor lastColor = null;
				
				while(lastColorIndex != -1 && !(lastColor = ChatColor.getByChar(totalMessage.charAt(lastColorIndex+1))).isColor()){
					lastColorIndex = totalMessage.lastIndexOf('§', lastColorIndex);
				}
				if(lastColorIndex != -1){
					chat = chat.replace("§f", lastColor.toString()).replace("§r", lastColor.toString());
				}
			}
		}
		else if(chatColor && event.getPlayer().hasPermission("evp.evcm.chatcolor")){
			chat = utils.determineColors(chat);
		}
		
		chat = chat.trim();
		// If the new chat does not match the original message, log the original to the console
		if(event.getMessage().equals(chat) == false){
			getLogger().info("Original Chat: "+pName+": "+event.getMessage());
			event.setMessage(chat);
		}
	}
	
	@EventHandler
	public void preCommand(PlayerCommandPreprocessEvent evt){
		if(evt.getMessage().contains(" ") && antiCmdFilth && evt.isCancelled() == false){
			String[] args = evt.getMessage().split(" ");
			String chat = " ";
			for(int i = 1; i < args.length; i++)chat+=args[i]+' ';
			
			if(hasBadWords(chat)) evt.setCancelled(true);
			else{
				chat = utils.convertFrom1337(utils.removePunctuation(chat));
				if(hasBadWords(chat)) evt.setCancelled(true);
				else if(hasBadWords(utils.combineRepeatedChars(chat))) evt.setCancelled(true);
				else return;
			}
			evt.getPlayer().sendMessage("§7Really now?");
		}
	}
	
	@EventHandler
	public void signPlaceEvent(SignChangeEvent evt){
		if(!antiSignFilth || evt.isCancelled()) return;
		
		StringBuilder builder = new StringBuilder(' ');
		builder.append(evt.getLine(0)); builder.append(" \n ");
		builder.append(evt.getLine(1)); builder.append(" \n ");
		builder.append(evt.getLine(2)); builder.append(" \n ");
		builder.append(evt.getLine(3)); builder.append(' ');
		
		String signText = builder.toString();
		String filteredText = filterOutBadWords(signText);
		
		if(signText.equals(filteredText) == false){
			String[] signLines = filteredText.replace(" \n", "\n").replace("\n ", "\n").split("\n");
			for(int i = 0; i < 4; i++) evt.setLine(i, signLines[i]);
		}
		
		if(hasBadWords(filteredText)) evt.setCancelled(true);
		else{
			filteredText = utils.convertFrom1337(utils.removePunctuation(filteredText));
			if(hasBadWords(filteredText)) evt.setCancelled(true);
			else if(hasBadWords(utils.combineRepeatedChars(filteredText))) evt.setCancelled(true);
			else return;
		}
		evt.getPlayer().sendMessage(pluginPrefix+"§7Really now?");
	}
	
	public boolean hasBadWords(String chat){
		chat = chat.toLowerCase();
//		for(int i = 0; i < badWords.length; i++){
//			if(chat.contains(badWords[i])){
//				//If this bad word has a replacement to use and its replacement is itself, then it's not considered a bad word
//				if(!subs.containsKey(badWords[i]) || !subs.get(badWords[i]).equals(badWords[i])) return true;
//			}
//		}
		for(String badword : badWords){
			if(chat.contains(badword)){
				//If this bad word does not have a custom replacement or 
				if(!subList.containsKey(badword) || !subList.get(badword).equals(badword)) return true;
			}
		}
		return false;
	}
	
	public String filterOutBadWords(String chat){
		String lowerCaseChat = chat.toLowerCase();
		
		for(String badword : badWords){
			if(chat.contains(badword)) chat = chat.replaceAll(badword.trim(),
					(subList.containsKey(badword)) ? subList.get(badword) :
					((defaultSub.length() != 1) ? defaultSub : StringUtils.repeat(defaultSub, badword.trim().length()-1)));
			
			if(lowerCaseChat.contains(badword)) chat = utils.replaceIgnoreCase(chat, badword.trim(),
					(subList.containsKey(badword)) ? subList.get(badword) :
					((defaultSub.length() != 1) ? defaultSub : StringUtils.repeat(defaultSub, badword.trim().length()-1)));
		}
		return chat;
	}
	
	@EventHandler
	public void onPlayerQuit(PlayerQuitEvent evt){
		lastChats.remove(evt.getPlayer().getUniqueId());
	}
	
	private boolean updateLastChatsLoopRunning;
	private void updateLastChats(boolean setActive){
		if(setActive == false){updateLastChatsLoopRunning = false; return;}
		else if(updateLastChatsLoopRunning) return;
		else{
			getServer().getScheduler().scheduleSyncDelayedTask(this, new Runnable(){
				public void run(){
					boolean remainingChats = false;
					for(UUID uuid : lastChats.keySet()){
						List<Integer> chats = lastChats.get(uuid);
//						chats.forEach(new Consumer<Integer>(){
//							@Override public void accept(Integer chat){chat++;}
//						});
						for(int i = 0; i < chats.size(); i++) chats.set(i, chats.get(i)+1);
						
						chats.removeIf(new Predicate<Integer>(){
							@Override
							public boolean test(Integer timeStamp) {
								return timeStamp > 60;
							}
						});
						lastChats.put(uuid, chats);
						if(!chats.isEmpty()) remainingChats = true;
					}
					
					//check wether to continue checking chats
					if(updateLastChatsLoopRunning && remainingChats){
						updateLastChatsLoopRunning = false;
						updateLastChats(true);
					}
				}
			}, 20);//every 1 second
		}
	}
	
	private void loadConfig(){
		BufferedReader reader = null;
		try{reader = new BufferedReader(new FileReader("./plugins/EvFolder/chatmanager config.txt"));}
		catch(FileNotFoundException e){
			
			//Create Directory
			File dir = new File("./plugins/EvFolder");
			if(!dir.exists()){dir.mkdir(); getLogger().info("Directory Created!");}
			
			//Create the config file
			writeConfig();
			return;
		}
		if(reader != null){
			String line = null;
			int settings = 0;
			try{
				while((line = reader.readLine()) != null){
					if(line.split(":").length < 2) continue;
					String[] split = line.toLowerCase().split(":");
					String tag = split[0].replace(" ", ""); String value = split[1].trim();
					
					if(tag.equals("blockspam")){//1
						antiSpam = (value.equals("true") || value.equals("yes") || value.equals("yup"));
					}
					else if(tag.contains("ignore'&'")){//2
						ignoreAmperstand = (value.equals("true") || value.equals("yes") || value.equals("yup"));
					}
					else if(tag.equals("spamresult")){//3
						spamResultCmd = value;
						if(!spamResultCmd.startsWith("/")) spamResultCmd = '/'+spamResultCmd;
					}
					else if(tag.equals("sanitizechat")){//4
						antiChatFilth = (value.equals("true") || value.equals("yes") || value.equals("yup"));
					}
					else if(tag.equals("sanitizecommands")){//5
						antiCmdFilth = (value.equals("true") || value.equals("yes") || value.equals("yup"));
					}
					else if(tag.equals("sanitizesign")){//6
						antiSignFilth = (value.equals("true") || value.equals("yes") || value.equals("yup"));
					}
					else if(tag.contains("backwards")){//7
						checkWordsBackwards = (value.equals("true") || value.equals("yes") || value.equals("yup"));
					}
					else if(tag.contains("replacement")){//8
						defaultSub = value;
					}
					else if(tag.equals("chatcolors")){//9
						chatColor = (value.equals("true") || value.equals("yes") || value.equals("yup"));
					}
					else if(tag.equals("chatformats")){//10
						chatFormat = (value.equals("true") || value.equals("yes") || value.equals("yup"));
					}
					else if(tag.equals("signcolors")){//11
						signColor = (value.equals("true") || value.equals("yes") || value.equals("yup"));
					}
					else if(tag.equals("signformats")){//12
						signFormat = (value.equals("true") || value.equals("yes") || value.equals("yup"));
					}
					else if(tag.contains("update")){//13
						autoUpdate = (value.equals("true") || value.equals("yes") || value.equals("yup"));
					}
					else if(tag.contains("prefix")){//14
						pluginPrefix = line.split(":")[1].trim()+' ';
					}
					else continue;
					settings++;
				}
				reader.close();
			}
			catch(IOException e){getLogger().info(e.getMessage());}
			if(settings < AVAILABLE_SETTINGS){
				writeConfig();
			}
		}
	}
	
	private void writeConfig(){
		File conf = new File("./plugins/EvFolder/chatmanager config.txt");
		try{
			conf.createNewFile();
			BufferedWriter writer = new BufferedWriter(new FileWriter(conf));
			writer.write(
					 "Block Spam: " + String.valueOf(antiSpam) +//1
					 "\nIgnore '&' as Punctuation: " + ignoreAmperstand +//2
					 "\nSpam Result Console Command: " + spamResultCmd +//3
					 
					 "\n\nSanitize Chat: " + String.valueOf(antiChatFilth) +//4
					 "\nSanitize Commands: " + String.valueOf(antiCmdFilth) +//5
					 "\nSanitize Sign Text: "+ String.valueOf(antiSignFilth) +//6
					 "\n\n#Warning: the following configuration option can cause harmless words to be blocked " +
					 "\n#When their backwards spelling matches a blocked word." +
					 "\nCheck words backwards in antiFilth: " + checkWordsBackwards +//7
					 "\nDefault Badword Replacement: " + defaultSub +//8
					 
					 "\n\nIt is generally good to disable these if you have another plugin" +
					 "\nthat handles sign/chat color (such as EssentialsChat)" +
					 "\nChat Colors: " + String.valueOf(chatColor) +//9
					 "\nChat Formats: " + String.valueOf(chatFormat) +//10
					 "\nSign Colors: " + String.valueOf(signColor) +//11
					 "\nSign Formats: " + String.valueOf(signFormat) +//12
					 
					 "\n\nAutomatic update check: " + String.valueOf(autoUpdate) + //13
					 "\nPlugin Prefix (before plugin->player messages): " + pluginPrefix);//14
			writer.close();
		}
		catch(IOException e1){getLogger().info(e1.getStackTrace().toString());}
	}
}
