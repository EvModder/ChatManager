package Evil_Code_ChatManager;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

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
	final String prefix = "˜";
	final int AVAILABLE_SETTINGS = 9;
	boolean antiSpam = true, antiFilth = true, antiCmdFilth, color = true, format = true, removeCaps = true, fixGrammer = false;
	private Map<String, Short> lastChats = new HashMap<String, Short>();
	private Map<String, String> subs;
	private float spamTolerance = 1;
	private String spamResult = "/kick %name% Received TMC/TMS from client--\\n (Either lag or spam... :P)";
	private String defaultSub = "[-]";
	
	String[] badWords;
	
	final int projectID = 63180;// Can be found at: https://api.curseforge.com/servermods/projects?search=ev-cleanchat
	@Override public void onEnable(){
		new Updater(this, projectID, this.getFile(), Updater.UpdateType.DEFAULT, true);
		
		loadAndImplementConfig();
		
		Set<String> wordList = new HashSet<String>();
		subs = new HashMap<String, String>();
		wordList.addAll(loadDefaultBlockedList());
		wordList.addAll(loadCustomBlockedList());
		badWords = new String[wordList.size()];
		int i=0;
		for(String word : wordList){
			badWords[i] = word; i++;
		}
		
		if(antiSpam) clearChatsLoop();
		getServer().getPluginManager().registerEvents(this, this);
	}
	
	public boolean onCommand(CommandSender sender, Command cmd, String label, String args[]){
		if(cmd.getName().equalsIgnoreCase("evcm") && args.length > 0){
			args[0] = args[0].toLowerCase();
			//=================================================================================================================
			if(args[0].contains("spam") && sender.hasPermission("evp.evcm.togglespam")){
				if(args.length >= 2){
					if(args[1].equalsIgnoreCase("on")){
						sender.sendMessage("SpamGuard Enabled.");
						antiSpam = true;
						clearChatsLoop();
					}
					else{
						sender.sendMessage("SpamGuard Disabled.");
						antiSpam = false;
						lastChats.clear();
					}
				}
				else{
					if(antiSpam == false){
						sender.sendMessage("SpamGuard Enabled.");
						clearChatsLoop();
					}
					else{
						sender.sendMessage("SpamGuard Disabled.");
						lastChats.clear();
					}
					antiSpam = !antiSpam;
				}
			}//-----------------------------------------------------------------------------------------------------------
			else if(args[0].contains("filth") && sender.hasPermission("evp.evcm.togglefilth")){
				if(args.length >= 2){
					if(args[1].equalsIgnoreCase("on")){sender.sendMessage("FilthGuard Enabled."); antiFilth = true;}
					else{sender.sendMessage("FilthGuard Disabled."); antiFilth = false;}
				}
				else{
					if(antiFilth == false){sender.sendMessage("FilthGuard Enabled."); antiFilth = true;}
					else{sender.sendMessage("FilthGuard Disabled."); antiFilth = false;}
				}
			}//-----------------------------------------------------------------------------------------------------------
			//~ ~ ~ ~ ~ ~ ~ ~
			else if(args[0].contains("color") && sender.hasPermission("evp.evcm.togglecolor")){
				if(args.length >= 2){
					if(args[1].equalsIgnoreCase("on")){sender.sendMessage("Colors Enabled."); color = true;}
					else{sender.sendMessage("Colors Disabled."); color = false;}
				}
				else{
					if(color == false){sender.sendMessage("Colors Enabled."); color = true;}
					else{sender.sendMessage("Colors Disabled."); color = false;}
				}
			}//-----------------------------------------------------------------------------------------------------------
			else if(args[0].contains("format") && sender.hasPermission("evp.evcm.toggleformat")){
				if(args.length >= 2){
					if(args[1].equalsIgnoreCase("on")){sender.sendMessage("Formats Enabled."); format = true;}
					else{sender.sendMessage("Formats Disabled."); format = false;}
				}
				else{
					if(format == false){sender.sendMessage("Formats Enabled."); format = true;}
					else{sender.sendMessage("Formats Disabled."); format = false;}
				}
			}//-----------------------------------------------------------------------------------------------------------
			else if(args[0].equals("help") || args[0].equals("list") || args[0].equals("info")){
				sender.sendMessage("§a--- §6< §2§lEvCM Commands §6> §a---\n" +
							"§21§7. /evcm anti-spam [on/off/(toggle)]\n" +
							"§22§7. /evcm anti-filth [on/off/(toggle)]\n" +
							"§23§7. /evcm anti-color [on/off/(toggle)]\n" +
							"§24§7. /evcm anti-format [on/off/(toggle)]\n" +
							"§24§7. /evcm reload   (or /evcm config)\n" +
							"§7§l----------------------------------");
			}
			else if((args[0].equals("reload") || args[0].equals("load") || args[0].equals("config"))
					&& sender.hasPermission("evp.evcm.reload")){
				onEnable();
				sender.sendMessage("§aFiles Reloaded!");
			}
			else{
				sender.sendMessage("§Unknown evcm command!");
				sender.sendMessage("§7Type §5/evcm help§7 for a list of ChatManager commands.");
				
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
								(subs.containsKey(badword)) ? subs.get(badword) :
									((defaultSub.length() > 1) ? defaultSub : StringUtils.repeat(defaultSub, badword.trim().length())));
			}
			
			String newChat = combineRepeats(chat);
			if(hasBadWords(newChat)){
				//event.setCancelled(true);
				//chat = "Gobble gobble I'm a turkey!";
				chat = "[censored]";
				getLogger().info("Abv. Chat: "+newChat);
			}
			
			newChat = removePunctuation(chat);
			if(hasBadWords(newChat)) chat = "[censored]";
			
			if(chat.equalsIgnoreCase("<-> "+event.getMessage()+" <->") == false){
				if(antiSpam && lastChats.containsKey(pName)) lastChats.put(pName, (short) (lastChats.get(pName) + 1));
			}
		}
		//-------------------------------------------------------------------------|
		
		if(antiSpam){
			//Keep players from repeating messages --------------------------------------------------------------
			String noPuncChat = removePunctuation(chat);
			if(noPuncChat.length() > 4 || noPuncChat.equals(noPuncChat.toUpperCase())){
				
				short numMessages = 1;
				if(lastChats.containsKey(pName)) numMessages += lastChats.get(pName);
				lastChats.put(pName, numMessages);
				
				if(numMessages > 15){
					/*To get here, you would have to spam & have filth within the spam. */
					final String nameToBan = pName;
					getServer().getScheduler().scheduleSyncDelayedTask(this, new Runnable(){@Override public void run(){
						getServer().dispatchCommand(getServer().getConsoleSender(), "tempban "+nameToBan+" 10m");
					}});
				}
				else if(numMessages == 15){
					final String name = pName;
					getServer().getScheduler().scheduleSyncDelayedTask(this, new Runnable(){@Override public void run(){
						getServer().dispatchCommand(getServer().getConsoleSender(), spamResult.replace("%name%", name));
					}});
				}
				else if(numMessages == 11) event.getPlayer().sendMessage("Careful, you can get kicked for spamming");
				else if(numMessages == 8) event.getPlayer().sendMessage("Whoa there! Slow down chat a little..");
			}
			//---------------------------------------------------------------------------------------------------
			//if more then about 50% of the chat is non-alphanumerical, remove punctuation
			if(noPuncChat.length()*2.2 < chat.length()){
				chat = noPuncChat;
				event.getPlayer().sendMessage("§cTry with less punctuation?");
			}
			//keep players from abusing caps
			else if(removeCaps && (noPuncChat.replace(" ", "").toUpperCase()).equals(noPuncChat.replace(" ", ""))
					&& (removePunctuation(chat).replace(" ", "")).length() > 13){
				chat = chat.toLowerCase();
				event.getPlayer().sendMessage("§cPlease don't shout. My ears are hurting xP");
			}
		}
		if(fixGrammer){
			chat = chat.replace(" i ", " I ").replace(" u ", "you").replace("youre", "you're");
			
		}
		
		if(color && event.getPlayer().hasPermission("evp.evcm.chatcolor")){
			chat = chat
				.replace("&1", "§1").replace("&2", "§2").replace("&3", "§3")
				.replace("&4", "§4").replace("&5", "§5").replace("&6", "§6")
				.replace("&7", "§7").replace("&8", "§8").replace("&9", "§9")
				.replace("&a", "§a").replace("&b", "§b").replace("&c", "§c")
				.replace("&d", "§d").replace("&e", "§e").replace("&f", "§f");
		}
		
		if(format && event.getPlayer().hasPermission("evp.evcm.chatformat")){
			chat = chat.replace("&l", "§l").replace("&m", "§m").replace("&n", "§n").replace("&o", "§o").replace("&r", "§r");
		}
		
		// Dun Dun Dun
		chat = chat.trim();
		if(event.getMessage().equalsIgnoreCase(chat) == false) getLogger().info("Original Chat: "+pName+": "+event.getMessage());
		event.setMessage(chat);
	}
	
	@EventHandler
	public void preCommand(PlayerCommandPreprocessEvent evt){
		if(evt.getMessage().contains(" ") && antiCmdFilth){
			String[] args = evt.getMessage().split(" ");
			String chat = " ";
			for(int i = 1; i < args.length; i++) chat+=args[i] + " ";
			
			if(hasBadWords(chat)) evt.setCancelled(true);
			else if(hasBadWords(removePunctuation(chat))) evt.setCancelled(true);
			else return;
			evt.getPlayer().sendMessage("§7Really now?");
		}
	}
	
	public boolean hasBadWords(String chat){
		chat = chat.toLowerCase();
		for(int i = 0; i < badWords.length; i++)
			if(chat.contains(badWords[i])){
				//If this bad word has a replacement to use and its replacement is itself, then it's not considered a bad word
				if(!subs.containsKey(badWords[i]) || !subs.get(badWords[i]).equals(badWords[i])) return true;
			}
		return false;
	}
	
	public String removePunctuation(String oldChat){
		char[] chars = oldChat.toCharArray();
		
		StringBuilder chat = new StringBuilder();
		for(int i = 0; i < chars.length; i++){
			if(
				chars[i] == 'A' || chars[i] == 'a' ||
				chars[i] == 'B' || chars[i] == 'b' ||
				chars[i] == 'C' || chars[i] == 'c' ||
				chars[i] == 'D' || chars[i] == 'd' ||
				chars[i] == 'E' || chars[i] == 'e' ||
				chars[i] == 'F' || chars[i] == 'f' ||
				chars[i] == 'G' || chars[i] == 'g' ||
				chars[i] == 'H' || chars[i] == 'h' ||
				chars[i] == 'I' || chars[i] == 'i' ||
				chars[i] == 'J' || chars[i] == 'j' ||
				chars[i] == 'K' || chars[i] == 'k' ||
				chars[i] == 'L' || chars[i] == 'l' ||
				chars[i] == 'M' || chars[i] == 'm' ||
				chars[i] == 'N' || chars[i] == 'n' ||
				chars[i] == 'O' || chars[i] == 'o' ||
				chars[i] == 'P' || chars[i] == 'p' ||
				chars[i] == 'Q' || chars[i] == 'q' ||
				chars[i] == 'R' || chars[i] == 'r' ||
				chars[i] == 'S' || chars[i] == 's' ||
				chars[i] == 'T' || chars[i] == 't' ||
				chars[i] == 'U' || chars[i] == 'u' ||
				chars[i] == 'V' || chars[i] == 'v' ||
				chars[i] == 'W' || chars[i] == 'w' ||
				chars[i] == 'X' || chars[i] == 'x' ||
				chars[i] == 'Y' || chars[i] == 'y' ||
				chars[i] == 'Z' || chars[i] == 'z'
			)chat.append(chars[i]);
			else chat.append(' ');
		}
		return chat.toString();
	}

	public String combineRepeats(String oldChat){
		char[] chars = oldChat.toCharArray();
		
		StringBuilder builder = new StringBuilder();
		builder.append(chars[0]);
		for(int i = 1; i < chars.length; i++){
			if(chars[i] != chars[i-1] || chars[i] == 'o')builder.append(chars[i]);
		}
		return builder.toString();
	}
	
	@EventHandler
	public void onPlayerJoin(PlayerJoinEvent evt){
		lastChats.put(evt.getPlayer().getName(), (short) 0);
	}
	
	@EventHandler
	public void onPlayerQuit(PlayerQuitEvent evt){
		lastChats.remove(evt.getPlayer().getName());
	}
	
	private void clearChatsLoop(){
		getServer().getScheduler().scheduleSyncDelayedTask(this, new Runnable(){@Override public void run(){
			for(String playerName : lastChats.keySet()){
				lastChats.put(playerName, (short) (lastChats.get(playerName)-1));
			}
			if(antiSpam) clearChatsLoop();
		}}, (long) (20*spamTolerance));//20 ticks = 1 second
	}
		
	private Set<String> loadDefaultBlockedList(){
		Set<String> wordList = new HashSet<String>();
		
		BufferedReader reader = null;
		reader = new BufferedReader(new InputStreamReader(getClass().getResourceAsStream("/defaultblocked.txt")));
		
		if(reader != null){
			StringBuilder builder = new StringBuilder();
			String line = null;
			try{
				while((line = reader.readLine()) != null){
					if(!line.replace(" ", "").startsWith("//") && !line.replace(" ", "").startsWith("#")) builder.append(line);
				}reader.close();
			}
			catch(IOException e){getLogger().info(e.getMessage());}
			
			for(String word : builder.toString().replace("\", \"", "").replace("\",\"", "").replace("\"", "").split(",")){
				if(word.length() > 1 && !word.replace(" ", "").isEmpty()){
					wordList.add(word.split("=")[0]);
					if(word.contains("=")) subs.put(word.split("=")[0], word.split("=")[1]);
				}
			}
			
			if(wordList.isEmpty()){
				/** If unable to load from file, load a hard-coded list **/
//				for(int i = 0; i < defBadWords.length; i++) wordList.add(defBadWords[i]);
			}
		}
		return wordList;
	}
	
	private Set<String> loadCustomBlockedList(){
		Set<String> wordList = new HashSet<String>();
		
		BufferedReader reader = null;
		try{reader = new BufferedReader(new FileReader("./plugins/EvFolder/blocked words list.txt"));}
		catch(FileNotFoundException e){
			//Create the file
			File blockedList = new File("./plugins/EvFolder/blocked words list.txt");
			try{
				BufferedWriter writer = new BufferedWriter(new FileWriter(blockedList));
				writer.write("" +"thisisabadword,badword248,a@@ , word45 ,CRAPS**T,lolbutts,LMFAO=haha" +
					   "\n\n# Spaces are fine to use, but please realize that " +
						 "\n# they will be counted as a part of a word." +
					  "\n#\n# To set a custom replacement (sub) for a bad word, simply use" +
					     "\n# '='" +
					     "\n# For example, to have \"hell\" replaced with \"nether\", write it" +
					     "\n# like this:" +
					     "\n# thisisabadword,hell=nether,ass=donkey,f***=meep,a@@,etc.." +
					  "\n#\n# If you want to allow a word on the 'Default blocked list'," +
					     "\n# Then put it in like this: hell=hell,crap=crap,f***=meep" +
					     "\n# What this does is simply \"replace\" the badword with itself" +
					     "\n# (Note that you can also open/unzip the .jar file and directly" +
					     "\n# edit the default list there)");
				writer.flush();
				writer.close();
			}
			catch(IOException e1){getLogger().info(e1.getStackTrace().toString());}
			//Attempt again to load the file
			try{reader = new BufferedReader(new FileReader("./plugins/EvFolder/blocked words list.txt"));}
			catch(FileNotFoundException e2){getLogger().info(e2.getStackTrace().toString());reader = null;}
		}
		if(reader != null){
			StringBuilder builder = new StringBuilder();
			String line = null;
			try{
				while((line = reader.readLine()) != null){
					if(!line.replace(" ", "").startsWith("//") && !line.replace(" ", "").startsWith("#")) builder.append(line);
				}reader.close();
			}
			catch(IOException e){getLogger().info(e.getMessage());}
			
			for(String word : builder.toString().split(",")){
				if(word.length() > 1 && !word.replace(" ", "").isEmpty()){
					wordList.add(word.split("=")[0]);
					if(word.contains("=")) subs.put(word.split("=")[0], word.split("=")[1]);
				}
			}
		}
		return wordList;
	}
	
	private void loadAndImplementConfig(){
		BufferedReader reader = null;
		try{reader = new BufferedReader(new FileReader("./plugins/EvFolder/chatmanager config.txt"));}
		catch(FileNotFoundException e){
			
			//Create Directory
			File dir = new File("./plugins/EvFolder");
			if(!dir.exists()){dir.mkdir(); getLogger().info("Directory Created!");}
			
			//Create the file
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
					else if(tag.contains("spamtolerance") || tag.contains("chatspeed")){
						try{
							spamTolerance = Float.parseFloat(value.replaceAll("[^0-9.]", ""));
							if(spamTolerance < .1) spamTolerance = .1F;
							if(spamTolerance > 5) spamTolerance = 5F;
						}
						catch(IllegalArgumentException ex){spamTolerance=1;}
					}
					else if(tag.equals("spamresult")){
						spamResult = value;
						if(!spamResult.startsWith("/")) spamResult = '/'+spamResult;
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
					 "\n  Chat Speed (seconds): " +  spamTolerance +
					 "\n  Default Replacement: " + defaultSub +
					 "\n  Spam Result: " + spamResult +
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
