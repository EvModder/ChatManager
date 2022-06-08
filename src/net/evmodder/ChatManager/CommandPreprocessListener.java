package net.evmodder.ChatManager;

import java.util.Arrays;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import net.evmodder.EvLib.extras.TextUtils;

class CommandPreprocessListener implements Listener{
	final ProfanityFilter chatFilter;
	final ChatManager pl;
	final boolean SANITIZE_COMMANDS;
	final String PLUGIN_PREFIX;
	final int COMMAND_SANITIZE_START_ARG;

	CommandPreprocessListener(ChatManager plugin){
		this.pl = plugin;
		SANITIZE_COMMANDS = pl.getConfig().getBoolean("sanitize-commands", true);
		chatFilter = SANITIZE_COMMANDS ? pl.profanityFilter : null;
		PLUGIN_PREFIX = TextUtils.translateAlternateColorCodes('&', pl.getConfig().getString("plugin-message-prefix", "&3<&aC&3>&f "));
		COMMAND_SANITIZE_START_ARG = pl.getConfig().getInt("sanitize-commands-start-argument", 2);
	}

	@EventHandler
	public void preCommand(PlayerCommandPreprocessEvent evt){
		if(evt.getMessage().contains(" ") && SANITIZE_COMMANDS && !evt.isCancelled()
				&& !evt.getPlayer().hasPermission("chatmanager.chatfilter.exempt")){
			StringBuilder builder = new StringBuilder(' ');
			String[] args = evt.getMessage().split(" ");
			for(int i = 1; i < args.length; ++i){
				builder.append(args[i]).append(' ');
			}
			String chat;
			if(args.length > COMMAND_SANITIZE_START_ARG){
				chat = StringUtils.join(
						Arrays.copyOfRange(args, 0, COMMAND_SANITIZE_START_ARG), ' ')+ " " +
						chatFilter.filterOutBadWords(StringUtils.join(
								Arrays.copyOfRange(args, COMMAND_SANITIZE_START_ARG, args.length), ' '));
				evt.setMessage(chat.trim());
			}
			else chat = evt.getMessage();

			chat = ChatUtils.convertFrom1337(ChatUtils.removeNonAlphanumeric(chat));
			if(chatFilter.hasBadWords(chat)) evt.setCancelled(true);
			else if(chatFilter.hasBadWords(ChatUtils.combineRepeatedChars(chat))) evt.setCancelled(true);
			else return;
			evt.getPlayer().sendMessage(PLUGIN_PREFIX+"§7Really now?");
		}
	}
	}
