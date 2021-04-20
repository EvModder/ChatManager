package net.evmodder.ChatManager;

import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;

import net.milkbowl.vault.chat.Chat;
import net.milkbowl.vault.permission.Permission;

import org.bukkit.plugin.RegisteredServiceProvider;

public class VaultHook {
	private static boolean vaultEnabled;
	public static Permission perms = null;
	public static Chat chat = null;

	public VaultHook(Plugin plugin){
		if(setupPermissions(plugin)){
			vaultEnabled = true;
			setupChat(plugin);
		}
	}

	private boolean setupChat(Plugin plugin){
		RegisteredServiceProvider<Chat> rsp = plugin.getServer().getServicesManager().getRegistration(Chat.class);
		chat = rsp.getProvider();
		return chat != null;
	}

	private boolean setupPermissions(Plugin plugin){
		RegisteredServiceProvider<Permission> rsp = plugin.getServer().getServicesManager().getRegistration(Permission.class);
		perms = rsp.getProvider();
		return perms != null;
	}

	public static boolean hasPermission(CommandSender p, String perm){
		if(perms != null && vaultEnabled) return perms.has(p, perm);
		else return p.hasPermission(perm);
	}

	public static boolean vaultEnabled(){return vaultEnabled;}
}