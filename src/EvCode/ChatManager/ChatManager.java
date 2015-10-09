package EvCode.ChatManager;

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
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
public final class ChatManager extends JavaPlugin implements Listener{
	/** Config options **/
	private boolean antiSpam = true, antiChatFilth = true, antiCmdFilth, removeCaps = true;
	private boolean chatColor = true, chatFormat = true, signColor = true, signFormat = true;
	private boolean ignoreAmperstand = true, antiSignFilth = true, checkWordsBackwards = false, autoUpdate = true;
	private String pluginPrefix = "§3<§aC§3>§f ";
	
	
	/** Anti-Spam configuration **/
	private String spamResultCmd = "kick %name% §cReceived TMC/TMS from client\\n§fEither lag or spam... :P";
	private int maxChatsPerMinute = 35, maxChatsPer10s = 15, maxChatsPerSecond = 3;
	private Map<UUID, List<Integer>> lastChats;
	
	
	/** Anti-Filth configuration **/
	private List<String> badWords;
	private boolean useDefaultBlockedList;
	private int minWordLengthToCheckBackwards = 4;
	
	private Map<String, String> subList;
	private String defaultSub = "[-]";
	private Utils utils = new Utils();
	
	/** If fewer/more then this number are available, such as after a plugin update,
	 ** then the server's copy of the config will be updated. */
	final int AVAILABLE_SETTINGS = 18;
	
	final int projectID = 63180;//<-- Can be found at: https://api.curseforge.com/servermods/projects?search=ev-cleanchat
	@Override public void onEnable(){
		/** Load config **/
		loadConfig();
		
		/** Check for an update **/
		if(autoUpdate) new Updater(this, projectID, this.getFile(), Updater.UpdateType.DEFAULT, true);
		else  new Updater(this, projectID, this.getFile(), Updater.UpdateType.NO_DOWNLOAD, true);
		
		/** Initialize variables **/
		lastChats = new HashMap<UUID, List<Integer>>();
		badWords = new ArrayList<String>();
		subList = new HashMap<String, String>();
		
		/** Load word lists **/
		if(useDefaultBlockedList) FileIO.loadDefaultBlockedList(this, badWords, subList);
		FileIO.loadCustomBlockedList(this, badWords, subList);
		
		if(checkWordsBackwards){
			Set<String> badWordsBackwards = new HashSet<String>();
			String newBadWord;
			
			for(String badword : badWords){
				if(!badword.trim().contains(" ") && badword.length() >= minWordLengthToCheckBackwards
						&& badword.equals(utils.removePunctuation(badword)))
				{
					newBadWord = utils.reverse(badword);
					if(subList.containsKey(newBadWord)){
						//method by which users can remove a backwards bad word from the blocked list
						if(subList.get(newBadWord).equals(newBadWord)) continue;
					}
					else if(subList.containsKey(badword)) subList.put(newBadWord, subList.get(badword));
					badWordsBackwards.add(newBadWord);
				}
			}
			badWords.addAll(badWordsBackwards);
		}
		//loop through the subList and remove instances of "sameword=sameword"
		for(String badword : subList.keySet()){
			if(subList.get(badword).equals(badword)) subList.put(badword, null);
		}
		subList.remove(null);
		
		/** Check for Vault **/
		new VaultHook(this);
		
		/** Register listener events with the server **/
		getServer().getPluginManager().registerEvents(this, this);
	}
	
	public boolean onCommand(CommandSender sender, Command cmd, String label, String args[]){
		if(cmd.getName().equalsIgnoreCase("evcm") && args.length > 0){
			args[0] = args[0].toLowerCase();
			//=================================================================================================================
			if(args[0].contains("spam") && VaultHook.hasPermission(sender, "evp.chatmanager.togglespam")){
				if(args.length >= 2){
					if(args[1].equalsIgnoreCase("on")) antiSpam = true;
					else antiSpam = false;
				}
				else antiSpam = !antiSpam;
				
				if(antiSpam) sender.sendMessage("SpamGuard Enabled.");
				else sender.sendMessage("SpamGuard Disabled.");
			}//-----------------------------------------------------------------------------------------------------------
			else if(args[0].contains("filth") && VaultHook.hasPermission(sender, "evp.chatmanager.togglefilth")){
				if(args.length >= 2){
					if(args[1].equalsIgnoreCase("on")) antiChatFilth = true;
					else antiChatFilth = false;
				}
				else antiChatFilth = !antiChatFilth;
				
				if(antiChatFilth) sender.sendMessage("FilthGuard Enabled.");
				else sender.sendMessage("FilthGuard Disabled.");
			}//-----------------------------------------------------------------------------------------------------------
			//~ ~ ~ ~ ~ ~ ~ ~
			else if(args[0].contains("color") && VaultHook.hasPermission(sender, "evp.chatmanager.togglecolor")){
				if(args.length >= 2){
					if(args[1].equalsIgnoreCase("on")) chatColor = true;
					else chatColor = false;
				}
				else chatColor = !chatColor;
				
				if(chatColor) sender.sendMessage("Chat-Colors Enabled.");
				else sender.sendMessage("Chat-Colors Disabled.");
			}//-----------------------------------------------------------------------------------------------------------
			else if(args[0].contains("format") && VaultHook.hasPermission(sender, "evp.chatmanager.toggleformat")){
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
					&& VaultHook.hasPermission(sender, "evp.chatmanager.reload")){
				HandlerList.unregisterAll((Plugin)this);
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
		if(antiChatFilth && !VaultHook.hasPermission(event.getPlayer(), "evp.chatmanager.chatfilter.exempt")){
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
		
		if(antiSpam && !VaultHook.hasPermission(event.getPlayer(), "evp.chatmanager.spamfilter.exempt")){
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
			
			int chatLength =
				ignoreAmperstand ? chat.replace("&", "").replace(defaultSub, "").length() : chat.replace(defaultSub, "").length();
					
			String noPuncChat = utils.removePunctuation(chat);
			int noPuncChatLength = noPuncChat.length();
			
			//if more then 55% of the chat is non-alphanumerical, remove the excess punctuation
			if((chatLength > 7 && chatLength*.55 < chatLength-noPuncChatLength)){
				chat = noPuncChat;
				event.getPlayer().sendMessage(pluginPrefix+"§cSpam Detect. Perhaps try with less punctuation.");
			}
			
			//If more then 55% of the chat is uppercase, make the chat lowercase
			if(removeCaps && noPuncChatLength > 13 && chatLength*.55 < noPuncChat.replaceAll("[^A-Z]", "").length()){
				chat = chat.toLowerCase();
				event.getPlayer().sendMessage(pluginPrefix+"§cPlease don't shout. My ears are hurting xP");
			}
			
			//Keep players from repeating messages --------------------------------------------------------------
			/** TODO: fill this **/
			//---------------------------------------------------------------------------------------------------
		}
		
		if(chatFormat && VaultHook.hasPermission(event.getPlayer(), "evp.chatmanager.chatformat")){
			chat = utils.determineFormatsByPermission(chat, event.getPlayer());
			
			if(chatColor && VaultHook.hasPermission(event.getPlayer(), "evp.chatmanager.chatcolor")){
				chat = utils.determineColorsByPermission(chat, event.getPlayer());
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
					chat = chat.replace("§f", "§r").replace("§r", lastColor.toString());
				}
			}
		}
		else if(chatColor && VaultHook.hasPermission(event.getPlayer(), "evp.chatmanager.chatcolor")){
			chat = utils.determineColorsByPermission(chat, event.getPlayer());
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
		if(evt.getMessage().contains(" ") && antiCmdFilth && evt.isCancelled() == false
				&& !VaultHook.hasPermission(evt.getPlayer(), "evp.chatmanager.chatfilter.exempt")){
			StringBuilder builder = new StringBuilder(' ');
			String[] args = evt.getMessage().split(" ");
			for(int i = 1; i < args.length; i++){
				builder.append(args[i]);
				builder.append(' ');
			}
			String chat = filterOutBadWords(builder.toString());
			if(!chat.equals(builder.toString())) evt.setMessage(evt.getMessage().split(" ")[0]+' '+chat.trim());
			
			chat = utils.convertFrom1337(utils.removePunctuation(chat));
			if(hasBadWords(chat)) evt.setCancelled(true);
			else if(hasBadWords(utils.combineRepeatedChars(chat))) evt.setCancelled(true);
			else return;
			evt.getPlayer().sendMessage(pluginPrefix+"§7Really now?");
		}
	}
	
	@EventHandler
	public void signPlaceEvent(SignChangeEvent evt){
		if(!antiSignFilth || evt.isCancelled() || VaultHook.hasPermission(evt.getPlayer(), "evp.chatmanager.chatfilter.exempt")) return;
		
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
	
	//
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
		String[] lines = FileIO.loadFile("chatmanager config").split("\n");
		int settings = 0;
		boolean invalidConfig = false;
		
		for(String line : lines){
			if(line.contains(":") == false || line.trim().replaceFirst("//", "#").startsWith("#")) continue;
			String[] split = line.toLowerCase().split(":");
			String tag = split[0].replace(" ", ""); String value = split[1].trim();
			
			if(tag.equals("blockspam")){//1
				antiSpam = (value.equals("true") || value.equals("yes") || value.equals("yup"));
			}
			else if(tag.contains("ignore'&'")){//2
				ignoreAmperstand = (value.equals("true") || value.equals("yes") || value.equals("yup"));
			}
			else if(tag.equals("spamresult")){//3
				spamResultCmd = line.split(":")[1].trim();
				if(!spamResultCmd.startsWith("/")) spamResultCmd = '/'+spamResultCmd;
			}
			else if(tag.contains("perminute")){//4
				try{maxChatsPerMinute = Integer.parseInt(value);}
				catch(NumberFormatException ex){invalidConfig = true;}
			}
			else if(tag.contains("per10s")){//5
				try{maxChatsPer10s = Integer.parseInt(value);}
				catch(NumberFormatException ex){invalidConfig = true;}
			}
			else if(tag.contains("persecond")){//6
				try{maxChatsPerSecond = Integer.parseInt(value);}
				catch(NumberFormatException ex){invalidConfig = true;}
			}
			else if(tag.equals("sanitizechat")){//7
				antiChatFilth = (value.equals("true") || value.equals("yes") || value.equals("yup"));
			}
			else if(tag.equals("sanitizecommands")){//8
				antiCmdFilth = (value.equals("true") || value.equals("yes") || value.equals("yup"));
			}
			else if(tag.equals("sanitizesign")){//9
				antiSignFilth = (value.equals("true") || value.equals("yes") || value.equals("yup"));
			}
			else if(tag.contains("backwards")){//10
				checkWordsBackwards = (value.equals("true") || value.equals("yes") || value.equals("yup"));
			}
			else if(tag.contains("replacement")){//11
				defaultSub = value;
			}
			else if(tag.contains("defaultblocked")){//12
				useDefaultBlockedList = (value.equals("true") || value.equals("yes") || value.equals("yup"));
			}
			else if(tag.equals("chatcolors")){//13
				chatColor = (value.equals("true") || value.equals("yes") || value.equals("yup"));
			}
			else if(tag.equals("chatformats")){//14
				chatFormat = (value.equals("true") || value.equals("yes") || value.equals("yup"));
			}
			else if(tag.equals("signcolors")){//15
				signColor = (value.equals("true") || value.equals("yes") || value.equals("yup"));
			}
			else if(tag.equals("signformats")){//16
				signFormat = (value.equals("true") || value.equals("yes") || value.equals("yup"));
			}
			else if(tag.contains("update")){//17
				autoUpdate = (value.equals("true") || value.equals("yes") || value.equals("yup"));
			}
			else if(tag.contains("prefix")){//18
				pluginPrefix = line.split(":")[1].trim()+' ';
			}
			else continue;
			settings++;
		}
		if(settings < AVAILABLE_SETTINGS || invalidConfig){
			writeConfig();
		}
	}
	
	private void writeConfig(){
		StringBuilder config = new StringBuilder();
		config.append("Block Spam: "); config.append(antiSpam);//1
		config.append("\n Ignore '&' as Punctuation: "); config.append(ignoreAmperstand);//2
		config.append("\n Spam Result Console Command: "); config.append(spamResultCmd);//3
		config.append("\n MaxChatsPerPlayerPerMinute: "); config.append(maxChatsPerMinute );//4
		config.append("\n MaxChatsPerPlayerPer10s: "); config.append(maxChatsPer10s);//5
		config.append("\n MaxChatsPerPlayerPerSecond: "); config.append(maxChatsPerSecond );//6
		
		config.append("\n\nSanitize Chat: "); config.append(antiChatFilth);//7
		config.append("\nSanitize Commands: "); config.append(antiCmdFilth);//8
		config.append("\nSanitize Sign Text: "); config.append(antiSignFilth);//9
		config.append("\n\n#Warning: the following configuration option can cause harmless ");
		config.append("\n#words to be blocked if their reversed spelling matches a bad word.");
		config.append("\nCheck words backwards in antiFilth: "); config.append(checkWordsBackwards);//10
		config.append("\nDefault Badword Replacement: "); config.append(defaultSub);//11
		config.append("\n\n#It is highly recommended that you enable this feature unless");
		config.append("\n#you want to add every existing obscenity yourself, or unless");
		config.append("\n#you are merely using this plugin to fix grammer mistakes.");
		config.append("\nUse default blocked word list: "); config.append(useDefaultBlockedList);//12
					 
		config.append("\n\nIt is generally good to disable these if you have another plugin");
		config.append("\nthat handles sign/chat color (such as EssentialsChat)");
		config.append("\nChat Colors: "); config.append(chatColor);//13
		config.append("\nChat Formats: "); config.append(chatFormat);//14
		config.append("\nSign Colors: "); config.append(signColor);//15
		config.append("\nSign Formats: "); config.append(signFormat);//16
		
		config.append("\n\nAutomatic update check: "); config.append(autoUpdate); //17
		config.append("\nPlugin Prefix (for plugin->player messages): "); config.append(pluginPrefix);//18
		
		FileIO.saveFile("chatmanager config", config.toString());
	}
}
