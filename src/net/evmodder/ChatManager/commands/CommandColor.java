package net.evmodder.ChatManager.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import java.util.Arrays;
import java.util.List;
import org.bukkit.ChatColor;
import net.evmodder.ChatManager.ChatManager;
import net.evmodder.EvLib.EvCommand;
import net.evmodder.EvLib.extras.ReflectionUtils;
import net.evmodder.EvLib.extras.TextUtils;

public class CommandColor extends EvCommand{
	private final ChatManager pl;
	public CommandColor(ChatManager pl){super(pl); this.pl=pl;}

	private void runCommand(final String cmd){
		pl.getServer().dispatchCommand(pl.getServer().getConsoleSender(), cmd);
	}

	@Override public List<String> onTabComplete(CommandSender s, Command c, String a, String[] args){
		if(args.length > 0){
			if(!args[0].isEmpty() && s.hasPermission("chatmanager.command.color.custom")
					&& s.getName().toLowerCase().startsWith(
						ChatColor.stripColor(ChatColor.translateAlternateColorCodes('&', args[0]))
						.toLowerCase().replace("&", ""))) return null;
			if(ReflectionUtils.getServerVersionString().compareTo("v1_16") >= 0 && args[0].matches("^([a-f0-9]{2,6})$")){
				List<String> tabCompletions = Arrays.asList(args[0]);
				for(int i=args[0].length(); i!=3 && i<6; ++i) tabCompletions = tabCompletions.stream()
						.mapMulti((str, downstream) -> {for(char ch : TextUtils.COLOR_CHARS) downstream.accept(str+ch);})
						.map(o -> (String)o)
						.toList();
				return tabCompletions;
			}
			return new String(TextUtils.COLOR_CHARS).chars().mapToObj(ch -> String.valueOf((char)ch)).toList();
		}
		return null;
	}

	boolean isValidHex(String rrggbb){
		return rrggbb.matches("^([A-Fa-f0-9]{6}|[A-Fa-f0-9]{3})$");
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String args[]){
		if(sender instanceof Player == false){
			sender.sendMessage(ChatColor.RED+"This command can only be run by in-game players!");
			return true;
		}
		if(args.length > 1){
			sender.sendMessage(ChatColor.RED+"Too many arguments");
			return false;
		}
		String displayName = ((Player)sender).getDisplayName();
		int nameIdx = displayName.indexOf(sender.getName());
		if(nameIdx == -1){
			displayName = ChatColor.stripColor(displayName);
			nameIdx = displayName.indexOf(sender.getName());
			if(nameIdx == -1){
				sender.sendMessage(ChatColor.RED+"You cannot use this command if you have a custom nickname set");
				return true;
			}
		}
		if(args.length == 0){
			sender.sendMessage(TextUtils.translateAlternateColorCodes('&',
					"&00 &11 &22 &33 &44 &55 &66 &77 &88 &99 &aa &bb &cc &dd &ee &ff"
					+ "  &#777hex:&#e5b015R&#e9b814R&#edc013G&#f0c711G&#f4cf10B&#f8d70fB"));
			sender.sendMessage(ChatColor.GRAY+"/color #");
			return true;
		}
		String colorId = args[0].replace("&", "").replace("#", "").replace("x", "").toLowerCase().replace("hex:", "");
		final boolean isHex = isValidHex(colorId);
		if(colorId.length() == 1){
			if(ChatColor.getByChar(colorId.charAt(0)) == null){
				sender.sendMessage(ChatColor.GRAY+"Unknown color '"+colorId+"'");
				return true;
			}
			if(TextUtils.isFormat(colorId.charAt(0)) && !sender.hasPermission("chatmanager.command.color.formats")){
				sender.sendMessage(ChatColor.GRAY+"Please pick a color code");
				return true;
			}
		}
		else if(ReflectionUtils.getServerVersionString().compareTo("v1_16") < 0 || !isHex || !sender.hasPermission("chatmanager.command.color.hex")){
			String colorNick = TextUtils.translateAlternateColorCodes('&', args[0]);
			String rawNick = ChatColor.stripColor(colorNick);
			if(!sender.hasPermission("chatmanager.command.color.custom") || !rawNick.equalsIgnoreCase(sender.getName())){
				sender.sendMessage(ChatColor.GRAY+"Please provide just a single character or color-code");
				pl.getLogger().info("color: "+colorId);
			}
			else if(!rawNick.equals(sender.getName())){
				sender.sendMessage(ChatColor.GRAY+"Please use your exact name (case sensitive)");
			}
			else{
				if(!sender.hasPermission("chatmanager.command.color.formats")) args[0] = TextUtils.stripFormatsOnly(args[0], '&');
				((Player)sender).setDisplayName(TextUtils.translateAlternateColorCodes('&', args[0]));
				runCommand("nick "+sender.getName()+" "+args[0]);
				sender.sendMessage(ChatColor.GREEN+"Color set!");
				((Player)sender).addScoreboardTag("color_nick");
			}
			return true;
		}

		// Got to here, update their nickname!
		int cutBeforeIdx = nameIdx;
		while(cutBeforeIdx > 1 && displayName.charAt(cutBeforeIdx-2) == ChatColor.COLOR_CHAR) cutBeforeIdx -= 2;
		String textBeforeName = displayName.substring(0, cutBeforeIdx).replace(ChatColor.COLOR_CHAR, '&');
		String textAfterName = displayName.substring(nameIdx+sender.getName().length()).replace(ChatColor.COLOR_CHAR, '&');

		if(colorId.equals("f") || colorId.equals("r")){
			if(textBeforeName.isEmpty() && textAfterName.isEmpty()) runCommand("nick "+sender.getName()+" off");
			else runCommand("nick "+sender.getName()+" "+textBeforeName+sender.getName()+textAfterName);
			((Player)sender).setDisplayName(sender.getName());
			((Player)sender).removeScoreboardTag("color_nick");
		}
		else{
			if(colorId.length() == 3) colorId =""+colorId.charAt(0)+colorId.charAt(0)+colorId.charAt(1)+colorId.charAt(1)+colorId.charAt(2)+colorId.charAt(2);
			colorId = (colorId.length() == 1 ? "&" : "&#") + colorId;
			final String newDisplayName = textBeforeName+colorId+sender.getName()+textAfterName;
			((Player)sender).setDisplayName(newDisplayName);
			runCommand("nick "+sender.getName()+" "+newDisplayName);
			sender.sendMessage(TextUtils.translateAlternateColorCodes('&', colorId)+"Color set!");
			((Player)sender).addScoreboardTag("color_nick");
		}
		return true;
	}
}