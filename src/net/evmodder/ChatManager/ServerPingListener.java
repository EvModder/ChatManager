package net.evmodder.ChatManager;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.ServerListPingEvent;


class ServerPingListener implements Listener{
	final ChatManager pl;

	ServerPingListener(ChatManager plugin){
		this.pl = plugin;
	}

	@EventHandler
	public void onServerListPing(ServerListPingEvent evt){//ServerDataPacket channel listener
		//TODO: enforceSecureProfile, preventsChatReports
		//https://github.com/dmulloy2/ProtocolLib/blob/master/TinyProtocol/src/main/java/com/comphenix/tinyprotocol/TinyProtocol.java
		//https://modrinth.com/plugin/freedomchat
		//https://github.com/KaspianDev/AntiPopup
	}
}