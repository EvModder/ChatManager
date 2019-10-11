package EvCode.ChatManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.ListIterator;
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
import org.bukkit.scheduler.BukkitRunnable;

public final class ChatManager extends JavaPlugin implements Listener{
	/** Config options **/
	private boolean antiSpam = false, antiChatFilth = true, antiCmdFilth = true, removeCaps = true;
	private boolean chatColor = true, chatFormat = true, signColor = true, signFormat = true;
	private boolean filterSelf = true;
	private int antiCmdFilthStartArg = 2;
	private boolean antiSignFilth = true, checkWordsBackwards = false, autoUpdate = true;
	private String pluginPrefix = "§3<§aC§3>§f ";

	/** Anti-Spam configuration **/
	private String spamResultCmd = "kick %name% §cReceived TMC/TMS from client\\n§fEither lag or spam... :P";
	private int maxChatsPerMinute = 35, maxChatsPer10s = 15, maxChatsPerSecond = 4;
	private Map<UUID, List<Integer>> lastChats;

	//Feature TODO: Word variations, ex: "..., ass { @ss,azz,a s s, a$$ ,pussy}=cat,..."
	/** Anti-Filth configuration **/
	private String filthResultCmd = "";
	private List<String> badWords;
	private int minWordLengthToCheckBackwards = 4;

	private Map<String, String> subList;
	private String defaultSub = "*";//"[-]";
	private Utils utils = new Utils();

	/** If fewer/more then this number are available, such as after a plugin update,
	 ** then the server's copy of the config will be updated. */
	final int AVAILABLE_SETTINGS = 17;

	final int projectID = 63180;//<-- Can be found at: https://api.curseforge.com/servermods/projects?search=ev-cleanchat
	@Override public void onEnable(){
		/** Load config **/
		loadConfig();

		/** Check for an update **/
		if(autoUpdate) new Updater(this, projectID, this.getFile(), Updater.UpdateType.DEFAULT, true);
		//else new Updater(this, projectID, this.getFile(), Updater.UpdateType.NO_DOWNLOAD, true);

		/** Initialize variables **/
		lastChats = new HashMap<UUID, List<Integer>>();
		badWords = new ArrayList<String>();
		subList = new HashMap<String, String>();

		/** Load word lists **/
		FileIO.loadDefaultBlockedList(this, badWords, subList);
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
					else if(subList.containsKey(badword.trim())) subList.put(newBadWord.trim(), subList.get(badword));
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
		if(cmd.getName().equals("chatm")){
			if(args.length == 0) args = new String[]{"help"};
			else args[0] = args[0].toLowerCase();
			//=================================================================================================================
			if(args[0].contains("spam") && VaultHook.hasPermission(sender, "chatmanager.togglespam")){
				if(args.length >= 2){
					if(args[1].equalsIgnoreCase("on")) antiSpam = true;
					else antiSpam = false;
				}
				else antiSpam = !antiSpam;
				
				if(antiSpam) sender.sendMessage("SpamGuard Enabled.");
				else sender.sendMessage("SpamGuard Disabled.");
			}//-----------------------------------------------------------------------------------------------------------
			else if(args[0].contains("filth") && VaultHook.hasPermission(sender, "chatmanager.togglefilth")){
				if(args.length >= 2){
					if(args[1].equalsIgnoreCase("on")) antiChatFilth = true;
					else antiChatFilth = false;
				}
				else antiChatFilth = !antiChatFilth;
				
				if(antiChatFilth) sender.sendMessage("FilthGuard Enabled.");
				else sender.sendMessage("FilthGuard Disabled.");
			}//-----------------------------------------------------------------------------------------------------------
			//~ ~ ~ ~ ~ ~ ~ ~
			else if(args[0].contains("color") && VaultHook.hasPermission(sender, "chatmanager.togglecolor")){
				if(args.length >= 2){
					if(args[1].equalsIgnoreCase("on")) chatColor = true;
					else chatColor = false;
				}
				else chatColor = !chatColor;
				
				if(chatColor) sender.sendMessage("Chat-Colors Enabled.");
				else sender.sendMessage("Chat-Colors Disabled.");
			}//-----------------------------------------------------------------------------------------------------------
			else if(args[0].contains("format") && VaultHook.hasPermission(sender, "chatmanager.toggleformat")){
				if(args.length >= 2){
					if(args[1].equalsIgnoreCase("on")) chatFormat = true;
					else chatFormat = false;
				}
				else chatFormat = !chatFormat;
				
				if(chatFormat) sender.sendMessage("Chat-Formats Enabled.");
				else sender.sendMessage("Chat-Formats Disabled.");
			}//-----------------------------------------------------------------------------------------------------------
			else if(args[0].equals("help") || args[0].equals("list") || args[0].equals("info") || args[0].equals("?")){
				sender.sendMessage("§a----- §6< §2§lChatManager Commands §6> §a-----\n" +
							"§21§7. /evcm anti-spam [on/off/(toggle)]\n" +
							"§22§7. /evcm anti-filth [on/off/(toggle)]\n" +
							"§23§7. /evcm chatcolor [on/off/(toggle)]\n" +
							"§24§7. /evcm chatformat [on/off/(toggle)]\n" +
							"§24§7. /evcm reload   (or /evcm config)\n" +
							"§7§l------------------------------------");
			}
			else if((args[0].equals("reload") || args[0].equals("load") || args[0].equals("config"))
					&& VaultHook.hasPermission(sender, "chatmanager.reload")){
				HandlerList.unregisterAll((Plugin)this);
				onEnable();
				sender.sendMessage("§aFiles Reloaded!");
			}
			else{
				sender.sendMessage("§Unknown evcm command!");
				sender.sendMessage("§7Type §5/chatm ?§7 for a list of ChatManager commands.");
				
			}
			return true;
			//=============================================================================================================
		}
		else return false;
	}

	@EventHandler(priority = EventPriority.HIGHEST)
	public void onPlayerChat(AsyncPlayerChatEvent event){
		//if it is already cancelled or is something a plugin forced the player to say
		if(event.isCancelled() || !event.isAsynchronous()) return;
		
		String chat = ' '+event.getMessage()+' ';
		String pName = event.getPlayer().getName();
		
		//-------------------------------------------------------------------------
		if(antiChatFilth && !VaultHook.hasPermission(event.getPlayer(), "chatmanager.chatfilter.exempt")){
			chat = filterOutBadWords(chat);
			
			String dePuncChat = utils.removePunctuation(chat);
			String deLeetChat1 = utils.removePunctuation(utils.convertFrom1337(chat));
			String deLeetChat2 = utils.convertFrom1337(utils.removeNonAlphanumeric(chat));
			String deLowercaseChat = dePuncChat.toLowerCase();
			String deUppercaseChat = dePuncChat.toUpperCase();
			String newChat = dePuncChat+' '+deLowercaseChat+' '+deUppercaseChat+' '+
							 deLeetChat1+' '+deLeetChat2+' '+// leet variations, 1=i=l
							 deLeetChat1.replace('L', 'I')+' '+deLeetChat2.replace('L', 'I')+' '+
							 deLowercaseChat+' '+deUppercaseChat+' '+
							 utils.combineRepeatedChars(dePuncChat)+' '+utils.combineRepeatedChars(deLeetChat1)+' '+
							 utils.combineRepeatedChars(chat);
					
			if(hasBadWords(newChat.toLowerCase())){
				// TODO: make a substitution algorithm
				chat = (defaultSub.length() != 1) ? defaultSub : StringUtils.repeat(defaultSub, chat.length()/2 +1);
			}
			
			if(!chat.equals(' '+event.getMessage()+' ')){// message has changed
				// they've been naughty!
				// TODO: maybe add some punishment or something here
				if(filthResultCmd != null && !filthResultCmd.isEmpty()){
					getLogger().info("Running command: "+filthResultCmd);
					final String name = pName;
					new BukkitRunnable(){@Override public void run(){
						getServer().dispatchCommand(getServer().getConsoleSender(), filthResultCmd.replace("%name%", name));
					}}.run();
				}
			}
		}
		//-------------------------------------------------------------------------
		
		if(chatFormat && VaultHook.hasPermission(event.getPlayer(), "chatmanager.format")){
			chat = utils.determineFormatsByPermission(chat, event.getPlayer());
		}
		if(chatColor && VaultHook.hasPermission(event.getPlayer(), "chatmanager.color")){
			chat = utils.determineColorsByPermission(chat, event.getPlayer());
		}
		//-------------------------------------------------------------------------
		
		if(antiSpam && !VaultHook.hasPermission(event.getPlayer(), "chatmanager.spamfilter.exempt")){
			if(lastChats.containsKey(event.getPlayer().getUniqueId())){
				lastChats.get(event.getPlayer().getUniqueId()).add(0);
			}
			else{
				lastChats.put(event.getPlayer().getUniqueId(), new ArrayList<Integer>());
				lastChats.get(event.getPlayer().getUniqueId()).add(0);
			}
			updateLastChats(false);
			
			// Chat timestamps are thrown out every minute or so
			int inLastSecond=0, inLast10s=0, inLastMinute=0;
			for(int timeElapsed : lastChats.get(event.getPlayer().getUniqueId())){
				
				++inLastMinute;// only retained for 60 seconds, so all of them match this
				if(timeElapsed <= 10){
					++inLast10s;
					if(timeElapsed <= 1) ++inLastSecond;
				}
			}
			if(inLastSecond > maxChatsPerSecond || inLast10s > maxChatsPer10s || inLastMinute > maxChatsPerMinute){
				event.getPlayer().sendMessage(pluginPrefix + "Please slow down chat a little.");
				getLogger().info("in-last-s: "+inLastSecond+" in-last-10-s: "+inLast10s+" in-last-m: "+inLastMinute);
				//If they continue to spam after the warning...
				if(inLastSecond > maxChatsPerSecond+2 || inLast10s > maxChatsPer10s+3 || inLastMinute > maxChatsPerMinute+5){
//					getServer().dispatchCommand(getServer().getConsoleSender(), spamResultCmd);
					final String name = pName;
					getServer().getScheduler().scheduleSyncDelayedTask(this, new Runnable(){@Override public void run(){
						getServer().dispatchCommand(getServer().getConsoleSender(), spamResultCmd.replace("%name%", name));
					}});
				}
			}
			String colorless = ChatColor.stripColor(chat);
			int chatLength = colorless.length();
			String noPuncChat = utils.removePunctuation(chat);
			int noPuncChatLength = utils.removePunctuation(colorless).length();
			
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
			/** TODO: make this? **/
			//---------------------------------------------------------------------------------------------------
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
		if(evt.getMessage().contains(" ") && antiCmdFilth && !evt.isCancelled()
				&& !VaultHook.hasPermission(evt.getPlayer(), "chatmanager.chatfilter.exempt")){
			StringBuilder builder = new StringBuilder(' ');
			String[] args = evt.getMessage().split(" ");
			for(int i = 1; i < args.length; ++i){
				builder.append(args[i]).append(' ');
			}
			String chat;
			if(args.length > antiCmdFilthStartArg){
				chat = StringUtils.join(
						Arrays.copyOfRange(args, 0, antiCmdFilthStartArg), ' ')+ " " +
						filterOutBadWords(StringUtils.join(
								Arrays.copyOfRange(args, antiCmdFilthStartArg, args.length), ' '));
				evt.setMessage(chat.trim());
			}
			else chat = evt.getMessage();

			chat = utils.convertFrom1337(utils.removePunctuation(chat));
			if(hasBadWords(chat)) evt.setCancelled(true);
			else if(hasBadWords(utils.combineRepeatedChars(chat))) evt.setCancelled(true);
			else return;
			evt.getPlayer().sendMessage(pluginPrefix+"§7Really now?");
		}
	}
	
	@EventHandler
	public void signPlaceEvent(SignChangeEvent evt){
		if(!antiSignFilth || evt.isCancelled() || VaultHook.hasPermission(evt.getPlayer(), "chatmanager.chatfilter.exempt")) return;
		
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
		for(String badword : badWords) if(chat.contains(badword)) return true;
		return false;
	}

	public String filterOutBadWords(String chat){
		String lowerCaseChat = chat.toLowerCase();
		for(String badword : badWords){
			if(chat.contains(badword)){
				String trimmed = badword.trim();
				chat = chat.replace(badword,
					(badword.startsWith(" ") ? " " : "") +
					(subList.containsKey(trimmed) ? subList.get(trimmed) :
					(defaultSub.length() != 1 ? defaultSub : StringUtils.repeat(defaultSub, trimmed.length()-1)) +
					(badword.endsWith(" ") ? " " : "")));
			}
			else if(lowerCaseChat.contains(badword)){
				String trimmed = badword.trim();
				chat = utils.replaceIgnoreCase(chat, badword,
					(badword.startsWith(" ") ? " " : "") +
					(subList.containsKey(trimmed) ? subList.get(trimmed) :
					(defaultSub.length() != 1 ? defaultSub : StringUtils.repeat(defaultSub, trimmed.length()-1)) +
					(badword.endsWith(" ") ? " " : "")));
			}
		}
		return chat;
	}

	@EventHandler
	public void onPlayerQuit(PlayerQuitEvent evt){
		lastChats.remove(evt.getPlayer().getUniqueId());
	}

	private boolean alreadyRunning;
	private void updateLastChats(boolean disable){
		if(disable){alreadyRunning = false; return;}
		if(alreadyRunning) return;
		alreadyRunning = true;

		new BukkitRunnable(){
			@Override public void run(){
				boolean remainingChats = false;
				for(UUID uuid : lastChats.keySet()){
					List<Integer> chats = lastChats.get(uuid);

					ListIterator<Integer> i = chats.listIterator();
					while(i.hasNext()){
						Integer timeStamp = i.next();
						i.set(timeStamp+1);
					}
					chats.removeIf(new Predicate<Integer>(){
						@Override
						public boolean test(Integer timeStamp) {
							return timeStamp > 60;
						}
					});
					lastChats.put(uuid, chats);
					if(!chats.isEmpty()) remainingChats = true;
				}

				if(remainingChats){
					updateLastChats(alreadyRunning = false);
				}
			}
		}.runTaskLater(this, 20);// 1 second
	}

	private void loadConfig(){
		String[] lines = FileIO.loadFile("config-ChatManager.yml").split("\n");
		int settings = 0;
		boolean invalidConfig = false;

		for(String line : lines){
			if(!line.contains(":")) continue;
			String[] split = line.toLowerCase().split(":");
			String tag = split[0].replace(" ", "").replace("-", "");
			String value = split[1].trim();

			if(tag.equals("blockspam")){//1
				antiSpam = (value.equals("true") || value.equals("yes") || value.equals("yup"));
			}
			else if(tag.contains("spamresult")){//2
				spamResultCmd = value.replace("'", "").replace("null", "").replace("/", "").trim();
				if(!spamResultCmd.isEmpty()) spamResultCmd = '/'+spamResultCmd;
			}
			else if(tag.contains("perminute")){//3
				try{maxChatsPerMinute = Integer.parseInt(value);}
				catch(NumberFormatException ex){invalidConfig = true;}
			}
			else if(tag.contains("per10s")){//4
				try{maxChatsPer10s = Integer.parseInt(value);}
				catch(NumberFormatException ex){invalidConfig = true;}
			}
			else if(tag.contains("persecond")){//5
				try{maxChatsPerSecond = Integer.parseInt(value);}
				catch(NumberFormatException ex){invalidConfig = true;}
			}
			else if(tag.equals("sanitizechat")){//6
				antiChatFilth = (value.equals("true") || value.equals("yes") || value.equals("yup"));
			}
			else if(tag.contains("filthresult")){//7
				filthResultCmd = value.replace("'", "").replace("null", "").replace("/", "").trim();
				if(!filthResultCmd.isEmpty()) filthResultCmd = '/'+filthResultCmd;
			}
			else if(tag.equals("sanitizecommands")){//8
				antiCmdFilth = (value.equals("true") || value.equals("yes") || value.equals("yup"));
			}
			else if(tag.equals("santizecommandsstartarg")){//19
				try{antiCmdFilthStartArg = Integer.parseInt(value);}
				catch(NumberFormatException ex){invalidConfig = true;}
			}
			else if(tag.equals("sanitizesigntext")){//9
				antiSignFilth = (value.equals("true") || value.equals("yes") || value.equals("yup"));
			}
			else if(tag.contains("backwards")){//10
				checkWordsBackwards = (value.equals("true") || value.equals("yes") || value.equals("yup"));
			}
			else if(tag.contains("replacement")){//11
				defaultSub = value;
			}
			else if(tag.equals("chatcolors")){//12
				chatColor = (value.equals("true") || value.equals("yes") || value.equals("yup"));
			}
			else if(tag.equals("chatformats")){//13
				chatFormat = (value.equals("true") || value.equals("yes") || value.equals("yup"));
			}
			else if(tag.equals("signcolors")){//14
				signColor = (value.equals("true") || value.equals("yes") || value.equals("yup"));
			}
			else if(tag.equals("signformats")){//15
				signFormat = (value.equals("true") || value.equals("yes") || value.equals("yup"));
			}
			else if(tag.contains("update")){//16
				autoUpdate = (value.equals("true") || value.equals("yes") || value.equals("yup"));
			}
			else if(tag.contains("prefix")){//17
				pluginPrefix = line.split(":")[1].trim()+' ';
			}
			else if(tag.contains("filterself")){//18
				filterSelf = (value.equals("true") || value.equals("yes") || value.equals("yup"));
			}
			else continue;
			++settings;
		}
		if(settings < AVAILABLE_SETTINGS || invalidConfig){
			getLogger().info("Only found "+settings+" out of expected "+AVAILABLE_SETTINGS+'.');
			getLogger().info("Adding missing settings to the config");
			writeConfig();
		}
	}

	private void writeConfig(){
		StringBuilder config = new StringBuilder();
		config.append("Block Spam: "); config.append(antiSpam);//1
		config.append("\n Spam Result Console Command: "); config.append(spamResultCmd);//2
		config.append("\n MaxChatsPerPlayerPerMinute: "); config.append(maxChatsPerMinute );//3
		config.append("\n MaxChatsPerPlayerPer10s: "); config.append(maxChatsPer10s);//4
		config.append("\n MaxChatsPerPlayerPerSecond: "); config.append(maxChatsPerSecond );//5

		config.append("\n\nSanitize Chat: "); config.append(antiChatFilth);//6
		config.append("\n Filth Result Console Command: ").append(filthResultCmd);//17
		config.append("\n\nFilter self chat: ").append(filterSelf);//6
		config.append("\nSanitize Commands: "); config.append(antiCmdFilth);//7
		config.append("\nSanitize Commands Start Argument: ").append(antiCmdFilthStartArg);//19
		config.append("\nSanitize Sign Text: "); config.append(antiSignFilth);//8
		config.append("\n\n#Warning: the following configuration option can cause harmless ");
		config.append("\n#words to be blocked if their reversed spelling matches a bad word.");
		config.append("\nCheck words backwards in antiFilth: "); config.append(checkWordsBackwards);//9
		config.append("\nDefault Badword Replacement: "); config.append(defaultSub);//10

		config.append("\n\nIt is generally good to disable these if you have another plugin");
		config.append("\nthat handles sign/chat color (such as EssentialsChat)");
		config.append("\nChat Colors: "); config.append(chatColor);//11
		config.append("\nChat Formats: "); config.append(chatFormat);//12
		config.append("\nSign Colors: "); config.append(signColor);//13
		config.append("\nSign Formats: "); config.append(signFormat);//14

		config.append("\n\nAutomatic update check: "); config.append(autoUpdate); //15
		config.append("\nPlugin Prefix (before plugin->player messages): "); config.append(pluginPrefix);//16

		FileIO.saveFile("config-ChatManager.yml", config.toString());
	}
}