package net.evmodder.ChatManager;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.SignChangeEvent;
import net.evmodder.EvLib.extras.TextUtils;

class SignChangeListener implements Listener{
	final ProfanityFilter chatFilter;
	final ChatManager pl;
	final boolean SANITIZE_SIGNS;
	final boolean HANDLE_COLORS, HANDLE_FORMATS;
	final String PLUGIN_PREFIX;

	SignChangeListener(ChatManager plugin){
		this.pl = plugin;
		HANDLE_COLORS = pl.getConfig().getBoolean("sign-colors", true);
		HANDLE_FORMATS = pl.getConfig().getBoolean("sign-formats", true);
		SANITIZE_SIGNS = pl.getConfig().getBoolean("sanitize-signs", true);
		chatFilter = SANITIZE_SIGNS ? pl.profanityFilter : null;
		PLUGIN_PREFIX = TextUtils.translateAlternateColorCodes('&', pl.getConfig().getString("plugin-message-prefix", "&3<&aC&3>&f "));
	}

	@EventHandler
	public void signPlaceEvent(SignChangeEvent evt){
		if(evt.isCancelled() || VaultHook.hasPermission(evt.getPlayer(), "chatmanager.chatfilter.exempt")) return;
		
		StringBuilder builder = new StringBuilder(' ');
		builder.append(evt.getLine(0)); builder.append(" \n ");
		builder.append(evt.getLine(1)); builder.append(" \n ");
		builder.append(evt.getLine(2)); builder.append(" \n ");
		builder.append(evt.getLine(3)); builder.append(' ');

		String signText = builder.toString();
		String filteredText = SANITIZE_SIGNS ? chatFilter.filterOutBadWords(signText) : signText;
		
		if(signText.equals(filteredText) == false){
			String[] signLines = filteredText.replace(" \n", "\n").replace("\n ", "\n").split("\n");
			for(int i = 0; i < 4; ++i){
				if(HANDLE_COLORS && VaultHook.hasPermission(evt.getPlayer(), "chatmanager.signcolor")){
					signLines[i] = ChatUtils.determineColorsByPermission(signLines[i], evt.getPlayer());
				}
				if(HANDLE_FORMATS && VaultHook.hasPermission(evt.getPlayer(), "chatmanager.signformat")){
					signLines[i] = ChatUtils.determineFormatsByPermission(signLines[i], evt.getPlayer());
				}
				evt.setLine(i, signLines[i]);
			}
		}

		if(SANITIZE_SIGNS){
			if(chatFilter.hasBadWords(filteredText)) evt.setCancelled(true);
			else{
				filteredText = ChatUtils.convertFrom1337(ChatUtils.removeNonAlphanumeric(filteredText));
				if(chatFilter.hasBadWords(filteredText)) evt.setCancelled(true);
				else if(chatFilter.hasBadWords(ChatUtils.combineRepeatedChars(filteredText))) evt.setCancelled(true);
				else return;
			}
			evt.getPlayer().sendMessage(PLUGIN_PREFIX+"§7Really now?");
		}
	}
}