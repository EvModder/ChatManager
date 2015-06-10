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

import org.apache.commons.lang.StringUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

public final class ChatManager extends JavaPlugin implements Listener{
	/** Config options **/
	boolean antiSpam = true, antiFilth = true, antiCmdFilth, color = true, format = true, removeCaps = true, fixGrammer = false;
//	final String nickPrefix = "�";
	private String pluginPrefix = "�3<�aC�3>�f ";
	
	
	/** Anti-Spam configuration **/
	private String spamResultCmd = "/kick %name% Received TMC/TMS from client--\\n (Either lag or spam... :P)";
	private int maxChatsPerMinute = 30, maxChatsPer10s = 10, maxChatsPerSecond = 2;
	private Map<UUID, List<Integer>> lastChats;
	
	
	/** Anti-Filth configuration **/
	private Set<String> badWords;
	private Map<String, String> subList;
	private String defaultSub = "[-]";
	Utils utils = new Utils();
	
	/** If fewer/more then this number are available, such as after a plugin update,
	 ** then the server's copy of the config will be updated. */
	final int AVAILABLE_SETTINGS = 9;
	
	final int projectID = 63180;//<-- Can be found at: https://api.curseforge.com/servermods/projects?search=ev-cleanchat
	@Override public void onEnable(){
		new Updater(this, projectID, this.getFile(), Updater.UpdateType.DEFAULT, true);
		
		/** Load config **/
		loadConfig();
		
		/** Initialize variables **/
		lastChats = new HashMap<UUID, List<Integer>>();
		badWords = new HashSet<String>();
		subList = new HashMap<String, String>();
		
		/** Load word lists **/
		FileIO.loadDefaultBlockedList(this, badWords, subList);
		FileIO.loadCustomBlockedList(this, badWords, subList);
//		badWords = (String[]) wordList.toArray();
		
		getServer().getPluginManager().registerEvents(this, this);
	}
	
	public boolean onCommand(CommandSender sender, Command cmd, String label, String args[]){
		if(cmd.getName().equalsIgnoreCase("evcm") && args.length > 0){
			args[0] = args[0].toLowerCase();
			//=================================================================================================================
			if(args[0].contains("spam") && sender.hasPermission("evp.evcm.togglespam")){
				if(args.length >= 2){
					if(args[1].equalsIgnoreCase("on")) antiSpam = true;
					else antiSpam = false;
				}
				else antiSpam = !antiSpam;
				
				// Tell result
				if(antiSpam) sender.sendMessage("SpamGuard Enabled.");
				else sender.sendMessage("SpamGuard Disabled.");
			}//-----------------------------------------------------------------------------------------------------------
			else if(args[0].contains("filth") && sender.hasPermission("evp.evcm.togglefilth")){
				if(args.length >= 2){
					if(args[1].equalsIgnoreCase("on")) antiFilth = true;
					else antiFilth = false;
				}
				else{
					antiFilth = !antiFilth;
					if(antiFilth) sender.sendMessage("FilthGuard Enabled.");
					else sender.sendMessage("FilthGuard Disabled.");
				}
			}//-----------------------------------------------------------------------------------------------------------
			//~ ~ ~ ~ ~ ~ ~ ~
			else if(args[0].contains("color") && sender.hasPermission("evp.evcm.togglecolor")){
				if(args.length >= 2){
					if(args[1].equalsIgnoreCase("on")) color = true;
					else color = false;
				}
				else{
					color = !color;
					if(color) sender.sendMessage("Colors Enabled.");
					else sender.sendMessage("Colors Disabled.");
				}
			}//-----------------------------------------------------------------------------------------------------------
			else if(args[0].contains("format") && sender.hasPermission("evp.evcm.toggleformat")){
				if(args.length >= 2){
					if(args[1].equalsIgnoreCase("on")) format = true;
					else format = false;
				}
				else{
					format = !format;
					if(format) sender.sendMessage("Formats Enabled.");
					else sender.sendMessage("Formats Disabled.");
				}
			}//-----------------------------------------------------------------------------------------------------------
			else if(args[0].equals("help") || args[0].equals("list") || args[0].equals("info")){
				sender.sendMessage("�a--- �6< �2�lEvCM Commands �6> �a---\n" +
							"�21�7. /evcm anti-spam [on/off/(toggle)]\n" +
							"�22�7. /evcm anti-filth [on/off/(toggle)]\n" +
							"�23�7. /evcm anti-color [on/off/(toggle)]\n" +
							"�24�7. /evcm anti-format [on/off/(toggle)]\n" +
							"�24�7. /evcm reload   (or /evcm config)\n" +
							"�7�l----------------------------------");
			}
			else if((args[0].equals("reload") || args[0].equals("load") || args[0].equals("config"))
					&& sender.hasPermission("evp.evcm.reload")){
				onEnable();
				sender.sendMessage("�aFiles Reloaded!");
			}
			else{
				sender.sendMessage("�Unknown evcm command!");
				sender.sendMessage("�7Type �5/evcm help�7 for a list of ChatManager commands.");
				
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
		if(antiFilth){
			for(String badword : badWords){
				if(chat.contains(badword))chat = chat
						.replace(badword.trim(),
								(subList.containsKey(badword)) ? subList.get(badword) :
								((defaultSub.length() != 1) ? defaultSub : StringUtils.repeat(defaultSub, badword.trim().length()-1)));
			}
			
			String newChat = utils.removePunctuation(chat);
			newChat = utils.convertFrom1337(newChat);
			if(hasBadWords(newChat)){
				chat = defaultSub;
				getLogger().info("De-Punc. Chat: "+newChat);
			}
			else{
				newChat = utils.combineRepeatedChars(newChat);
				if(hasBadWords(newChat)){
					chat = defaultSub;
					getLogger().info("Abv. Chat: "+newChat);
				}
			}
			
			if(chat.equalsIgnoreCase("<-> "+event.getMessage()+" <->") == false){
				// they've been naughty!
				// TODO: maybe add some punishment or something here
			}
		}
		//-------------------------------------------------------------------------|
		
		if(antiSpam){
			lastChats.get(event.getPlayer().getUniqueId()).add(0);
			
			// Chat timestamps are thrown out after 1 minute
			int inLastSecond=0, inLast10s=0, inLastMinute=0;
			for(int timeElapsed : lastChats.get(event.getPlayer().getUniqueId())){
				inLastMinute++;
				
				if(timeElapsed <= 10){
					inLast10s++;
					if(timeElapsed <= 1) inLastSecond++;
				}
			}
			if(inLastSecond > maxChatsPerSecond || inLast10s > maxChatsPer10s || inLastMinute > maxChatsPerMinute){
				event.getPlayer().sendMessage(pluginPrefix + "Please slow down chat a little.");
				
				//If they continue to spam after the warning..
				if(inLastSecond+2 > maxChatsPerSecond || inLast10s+3 > maxChatsPer10s || inLastMinute+5 > maxChatsPerMinute){
					getServer().dispatchCommand(getServer().getConsoleSender(), spamResultCmd);
//					final String name = pName;
//					getServer().getScheduler().scheduleSyncDelayedTask(this, new Runnable(){@Override public void run(){
//						getServer().dispatchCommand(getServer().getConsoleSender(), spamResultCmd.replace("%name%", name));
//					}});
				}
			}
			
			
			//Keep players from repeating messages --------------------------------------------------------------
			int chatLength = chat.length();
			String noPuncChat = utils.removePunctuation(chat);
			if(noPuncChat.length() > 4 || noPuncChat.equals(noPuncChat.toUpperCase())){
				
			}
			//---------------------------------------------------------------------------------------------------
			//if more then 55% of the chat is non-alphanumerical, remove the excess punctuation
			if(chatLength > 6 && chatLength*.55 > chatLength-noPuncChat.length()){
				chat = noPuncChat;
				event.getPlayer().sendMessage(pluginPrefix+"�cSpam Detect. Perhaps try with less punctuation.");
			}
			
			//If more then 55% of the chat is uppercase, make the chat lowercase
			if(removeCaps && noPuncChat.length() > 13 && chatLength*.55 < noPuncChat.replaceAll("[^A-Z]", "").length()){
				chat = chat.toLowerCase();
				event.getPlayer().sendMessage(pluginPrefix+"�cPlease don't shout. My ears are hurting xP");
			}
		}
		if(fixGrammer){
			chat = chat.replace(" i ", " I ").replace(" u ", "you").replace("youre", "you're").replace("dont", "don't");
			//add spellcheck and grammer check and all the rest of the mess, available online somewhere
		}
		
		if(color && event.getPlayer().hasPermission("evp.evcm.chatcolor")){
			chat = chat
				.replace("&1", "�1").replace("&2", "�2").replace("&3", "�3")
				.replace("&4", "�4").replace("&5", "�5").replace("&6", "�6")
				.replace("&7", "�7").replace("&8", "�8").replace("&9", "�9")
				.replace("&a", "�a").replace("&b", "�b").replace("&c", "�c")
				.replace("&d", "�d").replace("&e", "�e").replace("&f", "�f");
		}
		
		if(format && event.getPlayer().hasPermission("evp.evcm.chatformat")){
			chat = chat.replace("&l", "�l").replace("&m", "�m").replace("&n", "�n").replace("&o", "�o").replace("&r", "�r");
		}
		
		chat = chat.trim();
		// If the new chat does not match the original message, log the original to the console
		if(event.getMessage().equalsIgnoreCase(chat) == false){
			event.setMessage(chat);
			getLogger().info("Original Chat: "+pName+": "+event.getMessage());
		}
	}
	
	@EventHandler
	public void preCommand(PlayerCommandPreprocessEvent evt){
		if(evt.getMessage().contains(" ") && antiCmdFilth){
			String[] args = evt.getMessage().split(" ");
			String chat = " ";
			for(int i = 1; i < args.length; i++) chat+=args[i] + " ";
			
			if(hasBadWords(chat)) evt.setCancelled(true);
			else if(hasBadWords(utils.convertFrom1337(utils.removePunctuation(chat)))) evt.setCancelled(true);
			else return;
			evt.getPlayer().sendMessage("�7Really now?");
		}
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
	
	@EventHandler
	public void onPlayerJoin(PlayerJoinEvent evt){
		lastChats.put(evt.getPlayer().getUniqueId(), new ArrayList<Integer>());
	}
	
	@EventHandler
	public void onPlayerQuit(PlayerQuitEvent evt){
		lastChats.remove(evt.getPlayer().getUniqueId());
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
					line = line.toLowerCase();
					String tag = line.split(":")[0].replace(" ", ""); String value = line.split(":")[1].trim();
					
					if(tag.equals("blockspam"))
						antiSpam = (value.equals("true") || value.equals("yes") || value.equals("yup"));
					else if(tag.equals("sanitizechat"))
						antiFilth = (value.equals("true") || value.equals("yes") || value.equals("yup"));
					else if(tag.equals("sanitizecommands"))
						antiCmdFilth = (value.equals("true") || value.equals("yes") || value.equals("yup"));
					else if(tag.equals("chatcolors"))
						color = (value.equals("true") || value.equals("yes") || value.equals("yup"));
					else if(tag.equals("chatformats"))
						format = (value.equals("true") || value.equals("yes") || value.equals("yup"));
					else if(tag.equals("fixgrammer"))
						fixGrammer = (value.equals("true") || value.equals("yes") || value.equals("yup"));
					else if(tag.equals("spamresult")){
						spamResultCmd = value;
						if(!spamResultCmd.startsWith("/")) spamResultCmd = '/'+spamResultCmd;
					}
					else if(tag.contains("replace")){
						defaultSub = value;
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
		//Create the file
		File conf = new File("./plugins/EvFolder/chatmanager config.txt");
		try{
			conf.createNewFile();
			BufferedWriter writer = new BufferedWriter(new FileWriter(conf));
			writer.write("Block Spam: " + String.valueOf(antiSpam) +
					 "\n  Default Replacement: " + defaultSub +
					 "\n\nSanitize Chat: " + String.valueOf(antiFilth) +
					 "\nSanitize Commands: " + String.valueOf(antiCmdFilth) +
					 "\n\nChat Colors: " + String.valueOf(color) +
					 "\nChat Formats: " + String.valueOf(format) +
					 "\nFix Grammer: " + String.valueOf(fixGrammer));
			writer.close();
		}
		catch(IOException e1){getLogger().info(e1.getStackTrace().toString());}
	}
}