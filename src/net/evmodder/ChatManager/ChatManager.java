package net.evmodder.ChatManager;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.Plugin;
import net.evmodder.ChatManager.commands.CommandColor;
import net.evmodder.EvLib.EvPlugin;
import net.evmodder.EvLib.Updater;

//TODO: (ChatManager) [-] -> *, no filter for self chat, toggle filtering with tag
//TODO: Chat prefixes
//TODO: sign colors actually working
//TODO: Discord integration
//TODO: multiple-account spam detection
//TODO: /ignore <who/@a> [deaths/kills/beheads/chats/advancements/logins]
//(Seen only by t sender) <EvDoc> hi guys what's up? (-1)    // <-- hover comp on (-1) showing players that didn't receive the msg
//(Seen by everyone else) <EvDoc> hi guys what's up?
//TEST: Merge with DisplayItem
public final class ChatManager extends EvPlugin{
	ProfanityFilter profanityFilter;

	@Override public void onEvEnable(){
		if(config.getBoolean("update-plugin", false)){
			// Project id can be found at: https://api.curseforge.com/servermods/projects?search=ev-cleanchat
			new Updater(/*plugin=*/this, /*id=*/63180, getFile(), Updater.UpdateType.DEFAULT, /*announce=*/true);
		}
//		instance = this;
//		new VaultHook(this);
		profanityFilter = new ProfanityFilter(this);

		// commands
		new CommandColor(this);

		// listeners
		getServer().getPluginManager().registerEvents(new AsyncChatListener(this), this);
		getServer().getPluginManager().registerEvents(new CommandPreprocessListener(this), this);
		getServer().getPluginManager().registerEvents(new SignChangeListener(this), this);
		//1.19.1+
//		getServer().getPluginManager().registerEvents(new ServerPingListener(this), this);
	}

	//TODO: actually have working commands
	public boolean onCommand(CommandSender sender, Command cmd, String label, String args[]){
		if(cmd.getName().equals("chatm")){
			if(args.length == 0) args = new String[]{"help"};
			else args[0] = args[0].toLowerCase();
			//=================================================================================================================
			if(args[0].contains("spam") && sender.hasPermission("chatmanager.togglespam")){
//				if(args.length >= 2){
//					if(args[1].equalsIgnoreCase("on")) antiSpam = true;
//					else antiSpam = false;
//				}
//				else antiSpam = !antiSpam;
//				
//				if(antiSpam) sender.sendMessage("SpamGuard Enabled.");
//				else sender.sendMessage("SpamGuard Disabled.");
			}//-----------------------------------------------------------------------------------------------------------
			else if(args[0].contains("filth") && sender.hasPermission("chatmanager.togglefilth")){
//				if(args.length >= 2){
//					if(args[1].equalsIgnoreCase("on")) antiChatFilth = true;
//					else antiChatFilth = false;
//				}
//				else antiChatFilth = !antiChatFilth;
//				
//				if(antiChatFilth) sender.sendMessage("FilthGuard Enabled.");
//				else sender.sendMessage("FilthGuard Disabled.");
			}//-----------------------------------------------------------------------------------------------------------
			//~ ~ ~ ~ ~ ~ ~ ~
			else if(args[0].contains("color") && sender.hasPermission("chatmanager.togglecolor")){
//				if(args.length >= 2){
//					if(args[1].equalsIgnoreCase("on")) chatColor = true;
//					else chatColor = false;
//				}
//				else chatColor = !chatColor;
//				
//				if(chatColor) sender.sendMessage("Chat-Colors Enabled.");
//				else sender.sendMessage("Chat-Colors Disabled.");
			}//-----------------------------------------------------------------------------------------------------------
			else if(args[0].contains("format") && sender.hasPermission("chatmanager.toggleformat")){
//				if(args.length >= 2){
//					if(args[1].equalsIgnoreCase("on")) chatFormat = true;
//					else chatFormat = false;
//				}
//				else chatFormat = !chatFormat;
//				
//				if(chatFormat) sender.sendMessage("Chat-Formats Enabled.");
//				else sender.sendMessage("Chat-Formats Disabled.");
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
					&& sender.hasPermission("chatmanager.reload")){
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
}