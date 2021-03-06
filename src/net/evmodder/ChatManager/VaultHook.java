package net.evmodder.ChatManager;

import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;

import net.milkbowl.vault.chat.Chat;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.permission.Permission;

import org.bukkit.plugin.RegisteredServiceProvider;

public class VaultHook {
	private static boolean vaultEnabled;
	public static Economy econ = null;
	public static Permission perms = null;
	public static Chat chat = null;
	
	public VaultHook(Plugin plugin){
		if(!setupEconomy(plugin)){
			if(plugin.getServer().getPluginManager().getPlugin("Essentials") == null){
				plugin.getLogger().warning("Unable to connect to Vault or EssentialsEco economies");
			}
			else{// Removed to reduce spam
//				plugin.getLogger().info("Vault not found, using EssentialsEco as economy base");
			}
		}
		else{
			vaultEnabled = true;
			setupPermissions(plugin);
			setupChat(plugin);
		}
	}
	
	private boolean setupEconomy(Plugin plugin) {
		if (plugin.getServer().getPluginManager().getPlugin("Vault") == null) {
			return false;
		}
		RegisteredServiceProvider<Economy> rsp = plugin.getServer().getServicesManager().getRegistration(Economy.class);
		if (rsp == null) {
			return false;
		}
		econ = rsp.getProvider();
		return econ != null;
	}

	private boolean setupChat(Plugin plugin) {
		RegisteredServiceProvider<Chat> rsp = plugin.getServer().getServicesManager().getRegistration(Chat.class);
		chat = rsp.getProvider();
		return chat != null;
	}

	private boolean setupPermissions(Plugin plugin) {
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
