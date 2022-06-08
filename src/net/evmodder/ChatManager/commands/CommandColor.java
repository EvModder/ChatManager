package net.evmodder.ChatManager.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import org.bukkit.ChatColor;
import net.evmodder.ChatManager.ChatManager;
import net.evmodder.EvLib.EvCommand;
import net.evmodder.EvLib.extras.ReflectionUtils;
import net.evmodder.EvLib.extras.TellrawUtils.ClickEvent;
import net.evmodder.EvLib.extras.TellrawUtils.Format;
import net.evmodder.EvLib.extras.TellrawUtils.HoverEvent;
import net.evmodder.EvLib.extras.TellrawUtils.ListComponent;
import net.evmodder.EvLib.extras.TellrawUtils.RawTextComponent;
import net.evmodder.EvLib.extras.TellrawUtils.TextClickAction;
import net.evmodder.EvLib.extras.TellrawUtils.TextHoverAction;
import net.evmodder.EvLib.extras.TextUtils;

public class CommandColor extends EvCommand{
	private final ChatManager pl;
	private final boolean SET_NICKNAME, SET_DISPLAYNAME = true, SET_TAG = true;
	private final String COLOR_TAG = "color_nick", COLOR_TAG_ = COLOR_TAG+"_";
	private final boolean HEX_AVAILABLE;
	private final int NUM_RDM_COLORS_SHOWN = 15;
	final char[] HEX_DIGITS = "0123456789abcdef".toCharArray();
	final Random rand;

	public CommandColor(ChatManager pl){
		super(pl);
		this.pl=pl;
		rand = new Random();
		SET_NICKNAME = pl.getConfig().getBoolean("set-nickname-when-setting-color", false);

		if(SET_TAG && SET_DISPLAYNAME){
			pl.getServer().getPluginManager().registerEvents(new Listener(){
				@EventHandler public void onPlayerJoin(PlayerJoinEvent evt){
					if(evt.getPlayer().getScoreboardTags().contains(COLOR_TAG)){
						for(String tag : evt.getPlayer().getScoreboardTags()){
							if(tag.startsWith(COLOR_TAG_)){
								evt.getPlayer().setDisplayName(TextUtils.translateAlternateColorCodes('.', tag.substring(COLOR_TAG_.length())));
							}
						}
					}
				}
			}, pl);
		}
		HEX_AVAILABLE = ReflectionUtils.getServerVersionString().compareTo("v1_16") >= 0;
	}

	private void runCommand(final String cmd){
		pl.getServer().dispatchCommand(pl.getServer().getConsoleSender(), cmd);
	}
	
	private String getRandomColor(){
		return new StringBuilder()
				.append(HEX_DIGITS[rand.nextInt(HEX_DIGITS.length)])
				.append(HEX_DIGITS[rand.nextInt(HEX_DIGITS.length)])
				.append(HEX_DIGITS[rand.nextInt(HEX_DIGITS.length)])
				.append(HEX_DIGITS[rand.nextInt(HEX_DIGITS.length)])
				.append(HEX_DIGITS[rand.nextInt(HEX_DIGITS.length)])
				.append(HEX_DIGITS[rand.nextInt(HEX_DIGITS.length)])
			.toString();
	}

	private void setColoredName(Player player, String coloredName){
		if(SET_DISPLAYNAME) player.setDisplayName(TextUtils.translateAlternateColorCodes('&', coloredName));
		if(SET_NICKNAME) runCommand("nick "+player.getName()+" "+coloredName);
		if(SET_TAG){
			player.getScoreboardTags().removeIf(tag -> tag.startsWith(COLOR_TAG));
			player.addScoreboardTag(COLOR_TAG);
			player.addScoreboardTag(COLOR_TAG_+coloredName.replace('&', '.'));
		}
	}

	@Override public List<String> onTabComplete(CommandSender s, Command c, String a, String[] args){
		if(args.length > 0){
			if(!args[0].isEmpty() && s.hasPermission("chatmanager.command.color.custom")
					&& s.getName().toLowerCase().startsWith(
						ChatColor.stripColor(ChatColor.translateAlternateColorCodes('&', args[0]))
						.toLowerCase().replace("&", ""))) return null;
			if(HEX_AVAILABLE && args[0].matches("^([a-f0-9]{2,6})$")){
				List<String> tabCompletions = Arrays.asList(args[0]);
				for(int i=args[0].length(); i!=3 && i<6; ++i) tabCompletions = tabCompletions.stream()
						.mapMulti((str, downstream) -> {for(char ch : TextUtils.COLOR_CHARS) downstream.accept(str+ch);})
						.map(o -> (String)o)
						.toList();
				return tabCompletions;
			}
			ArrayList<String> tabCompletions = new ArrayList<>();
			if(HEX_AVAILABLE && s.hasPermission("chatmanager.command.color.hex")){
				tabCompletions.add("random");
				if("random".startsWith(args[0]) && args[0].startsWith("ra")) return tabCompletions;
			}
			tabCompletions.add("r");
			tabCompletions.addAll(new String(TextUtils.COLOR_CHARS).chars().mapToObj(ch -> String.valueOf((char)ch)).toList());
			return tabCompletions;
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
		// asked for random color
		else if(HEX_AVAILABLE && (colorId.equals("random") || colorId.equals("rdm")) && sender.hasPermission("chatmanager.command.color.hex")){
			ListComponent comp = new ListComponent();
			comp.addComponent("Click to select:");
			for(int i=0; i<NUM_RDM_COLORS_SHOWN; ++i){
				final String rdmColor = getRandomColor();
				comp.addComponent(" ");
				comp.addComponent(new RawTextComponent(
					/*text=*/"@", /*insert=*/null,
					new TextClickAction(ClickEvent.RUN_COMMAND, "/color "+rdmColor),
					new TextHoverAction(HoverEvent.SHOW_TEXT, new RawTextComponent("/color "+rdmColor, null, null, null, null, null)),
					/*color=*/"#"+rdmColor, /*formats=*/Collections.singletonMap(Format.BOLD, true)
				));
			}
			runCommand("minecraft:tellraw "+sender.getName()+" "+comp.toString());
			return true;
		}
		// Single-char or multi-color
		else if(!HEX_AVAILABLE || !isHex || !sender.hasPermission("chatmanager.command.color.hex")){
			String colorNick = TextUtils.translateAlternateColorCodes('&', args[0]);
			String rawNick = ChatColor.stripColor(colorNick);
			if(!sender.hasPermission("chatmanager.command.color.custom") || !rawNick.equalsIgnoreCase(sender.getName())){
				sender.sendMessage(ChatColor.GRAY+"Please provide just a single character or color-code");
			}
			else if(!rawNick.equals(sender.getName())){
				sender.sendMessage(ChatColor.GRAY+"Please use your exact name (case sensitive)");
			}
			else{
				if(!sender.hasPermission("chatmanager.command.color.formats")) args[0] = TextUtils.stripFormatsOnly(args[0], '&');
				setColoredName((Player)sender, args[0]);
				sender.sendMessage(TextUtils.getCurrentColorAndFormats(args[0])+"Color set!");
			}
			return true;
		}
		//else: Hex color code
		int cutBeforeIdx = nameIdx;
		while(cutBeforeIdx > 1 && displayName.charAt(cutBeforeIdx-2) == ChatColor.COLOR_CHAR) cutBeforeIdx -= 2;
		final String textBeforeName = displayName.substring(0, cutBeforeIdx).replace(ChatColor.COLOR_CHAR, '&');
		final String textAfterName = displayName.substring(nameIdx+sender.getName().length()).replace(ChatColor.COLOR_CHAR, '&');

		if(colorId.equals("f") || colorId.equals("r")){
			if(SET_DISPLAYNAME) ((Player)sender).setDisplayName(sender.getName());
			if(SET_NICKNAME){
				if(textBeforeName.isEmpty() && textAfterName.isEmpty()) runCommand("nick "+sender.getName()+" off");
				else runCommand("nick "+sender.getName()+" "+textBeforeName+sender.getName()+textAfterName);
			}
			if(SET_TAG){
				((Player)sender).getScoreboardTags().removeIf(tag -> tag.startsWith(COLOR_TAG));
			}
			sender.sendMessage("Color removed");
		}
		else{
			if(colorId.length() == 3) colorId =""+colorId.charAt(0)+colorId.charAt(0)+colorId.charAt(1)+colorId.charAt(1)+colorId.charAt(2)+colorId.charAt(2);
			colorId = (colorId.length() == 1 ? "&" : "&#") + colorId;
			final String newDisplayName = textBeforeName+colorId+sender.getName()+textAfterName;
			setColoredName((Player)sender, newDisplayName);
			sender.sendMessage(TextUtils.translateAlternateColorCodes('&', colorId)+"Color set!");
		}
		return true;
	}
}