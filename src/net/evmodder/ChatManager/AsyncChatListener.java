package net.evmodder.ChatManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.UUID;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import net.evmodder.EvLib.extras.TellrawUtils.ClickEvent;
import net.evmodder.EvLib.extras.TellrawUtils.Component;
import net.evmodder.EvLib.extras.TellrawUtils.Format;
import net.evmodder.EvLib.extras.TellrawUtils.HoverEvent;
import net.evmodder.EvLib.extras.TellrawUtils.ListComponent;
import net.evmodder.EvLib.extras.TellrawUtils.RawTextComponent;
import net.evmodder.EvLib.extras.TellrawUtils.TextClickAction;
import net.evmodder.EvLib.extras.TellrawUtils.TextHoverAction;
import net.evmodder.EvLib.extras.TellrawUtils.TranslationComponent;
import net.evmodder.EvLib.extras.TellrawUtils;
import net.evmodder.EvLib.extras.TextUtils;
import net.evmodder.EvLib.extras.TypeUtils;
import net.evmodder.EvLib.extras.NBTTagUtils;
import net.evmodder.EvLib.extras.NBTTagUtils.RefNBTTagCompound;

class AsyncChatListener implements Listener{
	final ProfanityFilter chatFilter;
	private Map<UUID, List<Integer>> lastChats;
	final ChatManager pl;
	final boolean HANDLE_COLORS, HANDLE_FORMATS, LOG_CHATS;
	final boolean SANITIZE_CHAT, ANTI_SPAM, ANTI_CAPS;
	final String DEFAULT_BADWORD_SUB;
	final String FILTH_RESULT_COMMAND, SPAM_RESULT_COMMAND;
	final String PLUGIN_PREFIX;
	final int MAX_CHATS_PER_MINUTE, MAX_CHATS_PER_10S, MAX_CHATS_PER_SECOND;
	final boolean DISPLAY_ITEMS, USE_DISPLAY_NAMES;
	final String ITEM_REPLACEMENT;
	final int JSON_LIMIT = 15000;//TODO: move to config
	final String DEFAULT_ITEM_DISPLAY_COLOR = "#cccccc";

	AsyncChatListener(ChatManager plugin){
		this.pl = plugin;
		lastChats = new HashMap<UUID, List<Integer>>();
		HANDLE_COLORS = pl.getConfig().getBoolean("chat-colors", true);
		HANDLE_FORMATS = pl.getConfig().getBoolean("chat-formats", true);
		DISPLAY_ITEMS = pl.getConfig().getBoolean("item-hover-display", true);
		USE_DISPLAY_NAMES = pl.getConfig().getBoolean("use-player-displaynames", true);
		ANTI_SPAM = pl.getConfig().getBoolean("anti-spam", false);
		ANTI_CAPS = pl.getConfig().getBoolean("anti-caps", true);
		SANITIZE_CHAT = pl.getConfig().getBoolean("sanitize-chat", true);
		chatFilter = SANITIZE_CHAT ? pl.profanityFilter : null;
		DEFAULT_BADWORD_SUB = pl.getConfig().getString("default-badword-replacement", "*");
		FILTH_RESULT_COMMAND = pl.getConfig().getString("filth-result-command", "");
		SPAM_RESULT_COMMAND = pl.getConfig().getString("spam-result-command", "kick %name% §cReceived TMC/TMS from client\\\\n§fEither lag or spam... :P");
		PLUGIN_PREFIX = TextUtils.translateAlternateColorCodes('&', pl.getConfig().getString("plugin-message-prefix", "&3<&aC&3>&f "));
		MAX_CHATS_PER_MINUTE = pl.getConfig().getInt("max-chats-per-minute", 35);
		MAX_CHATS_PER_10S = pl.getConfig().getInt("max-chats-per-10s", 15);
		MAX_CHATS_PER_SECOND = pl.getConfig().getInt("max-chats-per-second", 4);
		ITEM_REPLACEMENT = pl.getConfig().getString("item-replacement", "[i]");
		LOG_CHATS = pl.getConfig().getBoolean("log-chat-to-console", true);
	}

	private boolean alreadyRunning;
	private void updateLastChats(boolean disable){
		if(disable){alreadyRunning = false; return;}
		if(alreadyRunning) return;
		alreadyRunning = true;

		new BukkitRunnable(){
			@Override public void run(){
				boolean remainingChats = false;
				for(UUID uuid : lastChats.keySet()){
					List<Integer> chats = lastChats.get(uuid);

					ListIterator<Integer> i = chats.listIterator();
					while(i.hasNext()){
						Integer timeStamp = i.next();
						i.set(timeStamp+1);
					}
					chats.removeIf(ts -> ts > 60);
					lastChats.put(uuid, chats);
					if(!chats.isEmpty()) remainingChats = true;
				}
				if(remainingChats) updateLastChats(alreadyRunning = false);
			}
		}.runTaskLater(pl, 20);// 1 second
	}

	@EventHandler
	public void onPlayerQuit(PlayerQuitEvent evt){lastChats.remove(evt.getPlayer().getUniqueId());}

	public final static String getDisplayName(ItemStack item){
		RefNBTTagCompound tag = NBTTagUtils.getTag(item);
		RefNBTTagCompound display = tag.hasKey("display") ? (RefNBTTagCompound)tag.get("display") : new RefNBTTagCompound();
		return display.hasKey("Name") ? display.getString("Name") : null;
//		return TellrawUtils.parseComponentFromString(display.getString("Name"));
	}

	Component getItemComponent(ItemStack item){
		TextHoverAction hoverAction = new TextHoverAction(HoverEvent.SHOW_ITEM, JunkUtils.convertItemStackToJson(item, JSON_LIMIT));
		String rarityColor = TypeUtils.getRarityColor(item).name().toLowerCase();
		if(rarityColor.equals("white")) rarityColor = DEFAULT_ITEM_DISPLAY_COLOR;
		if(item.hasItemMeta() && item.getItemMeta().hasDisplayName()){
			ListComponent parentPropertiesComp = new ListComponent(new RawTextComponent(
					/*text=*/"", /*insert=*/null, /*click=*/null, hoverAction, /*color=*/rarityColor,
					/*formats=*/Collections.singletonMap(Format.ITALIC, true)));
			String rawDisplayName = getDisplayName(item);
			parentPropertiesComp.addComponent(TellrawUtils.parseComponentFromString(rawDisplayName));
			return parentPropertiesComp;
		}
		else{
			Component localizedName = TellrawUtils.getLocalizedDisplayName(item);
			if(localizedName instanceof TranslationComponent) return new TranslationComponent(
				((TranslationComponent)localizedName).getJsonKey(), ((TranslationComponent)localizedName).getWith(),
				/*insert=*/null, /*click=*/null, hoverAction, /*color=*/rarityColor, /*formats=*/null);
			return new ListComponent(
				new RawTextComponent(/*text=*/"", /*insert=*/null, /*click=*/null, hoverAction, /*color=*/rarityColor, /*formats=*/null),
				TellrawUtils.getLocalizedDisplayName(item)
			);
		}
	}
	Component getItemComponentWithAmount(ItemStack item){
		if(item.getAmount() <= 1) return getItemComponent(item);
		else return new ListComponent(getItemComponent(item), new RawTextComponent(ChatColor.YELLOW+" x"+item.getAmount()));
	}

	@EventHandler(priority = EventPriority.HIGH)
	public void onPlayerChat(AsyncPlayerChatEvent evt){
		//if it is already cancelled or is something a plugin forced the player to say
		if(evt.isCancelled() || !evt.isAsynchronous()) return;

		String chat = ' '+evt.getMessage()+' ';
		final String pName = evt.getPlayer().getName();

		//-------------------------------------------------------------------------
		if(SANITIZE_CHAT && !evt.getPlayer().hasPermission("chatmanager.chatfilter.exempt")){
			chat = chatFilter.filterOutBadWords(chat);

			final String dePuncChat = ChatUtils.removeNonAlphanumeric(chat);
			final String deLeetChat1 = ChatUtils.removeNonAlphanumeric(ChatUtils.convertFrom1337(chat));
			final String deLeetChat2 = ChatUtils.convertFrom1337(ChatUtils.removeNonAlphanumeric(chat));
			final String deLowercaseChat = dePuncChat.toLowerCase();
			final String deUppercaseChat = dePuncChat.toUpperCase();
			final String newChat = dePuncChat+' '+deLowercaseChat+' '+deUppercaseChat+' '+
							deLeetChat1+' '+deLeetChat2+' '+// leet variations, 1=i=l
							deLeetChat1.replace('L', 'I')+' '+deLeetChat2.replace('L', 'I')+' '+
							deLowercaseChat+' '+deUppercaseChat+' '+
							ChatUtils.combineRepeatedChars(dePuncChat)+' '+ChatUtils.combineRepeatedChars(deLeetChat1)+' '+
							ChatUtils.combineRepeatedChars(chat);

			if(chatFilter.hasBadWords(newChat.toLowerCase())){
				// TODO: make a substitution algorithm
				chat = (DEFAULT_BADWORD_SUB.length() != 1) ? DEFAULT_BADWORD_SUB : StringUtils.repeat(DEFAULT_BADWORD_SUB, chat.length()/2 +1);
				pl.getLogger().info("badWord detected in modified chat: "+newChat.toLowerCase());
			}
			if(!chat.equals(' '+evt.getMessage()+' ')){// message has changed
				// they've been naughty!
				// TODO: maybe add some punishment or something here
				if(FILTH_RESULT_COMMAND != null && !FILTH_RESULT_COMMAND.isEmpty()){
					String command = FILTH_RESULT_COMMAND.replace("%name%", pName);
					pl.getLogger().info("Running command: "+command);
					new BukkitRunnable(){@Override public void run(){
						pl.getServer().dispatchCommand(pl.getServer().getConsoleSender(), command);
					}}.run();
				}
			}
		}
		//-------------------------------------------------------------------------
		if(HANDLE_FORMATS && evt.getPlayer().hasPermission("chatmanager.format")){
			chat = ChatUtils.determineFormatsByPermission(chat, evt.getPlayer());
		}
		if(HANDLE_COLORS && evt.getPlayer().hasPermission("chatmanager.color")){
			chat = ChatUtils.determineColorsByPermission(chat, evt.getPlayer());
		}
		//-------------------------------------------------------------------------
		if(ANTI_SPAM && !evt.getPlayer().hasPermission("chatmanager.spamfilter.exempt")){
			if(lastChats.containsKey(evt.getPlayer().getUniqueId())){
				lastChats.get(evt.getPlayer().getUniqueId()).add(0);
			}
			else{
				lastChats.put(evt.getPlayer().getUniqueId(), new ArrayList<Integer>());
				lastChats.get(evt.getPlayer().getUniqueId()).add(0);
			}
			updateLastChats(false);

			// Chat timestamps are thrown out every minute or so
			int inLastSecond=0, inLast10s=0, inLastMinute=0;
			for(int timeElapsed : lastChats.get(evt.getPlayer().getUniqueId())){
				++inLastMinute;// only retained for 60 seconds, so all of them match this
				if(timeElapsed <= 10){
					++inLast10s;
					if(timeElapsed <= 1) ++inLastSecond;
				}
			}
			if(inLastSecond > MAX_CHATS_PER_SECOND || inLast10s > MAX_CHATS_PER_10S || inLastMinute > MAX_CHATS_PER_MINUTE){
				evt.getPlayer().sendMessage(PLUGIN_PREFIX + "Please slow down chat a little.");
				pl.getLogger().info("in-last-s: "+inLastSecond+" in-last-10-s: "+inLast10s+" in-last-m: "+inLastMinute);
				//If they continue to spam after the warning...
				if(inLastSecond > MAX_CHATS_PER_SECOND+2 || inLast10s > MAX_CHATS_PER_10S+3 || inLastMinute > MAX_CHATS_PER_MINUTE+5){
//					getServer().dispatchCommand(getServer().getConsoleSender(), spamResultCmd);
					if(SPAM_RESULT_COMMAND != null && !SPAM_RESULT_COMMAND.isEmpty()){
						String command = SPAM_RESULT_COMMAND.replace("%name%", pName);
						pl.getLogger().info("Running command: "+command);
						new BukkitRunnable(){@Override public void run(){
							pl.getServer().dispatchCommand(pl.getServer().getConsoleSender(), command);
						}}.run();
					}
				}
			}
			String colorless = ChatColor.stripColor(chat);
			int chatLength = colorless.length();
			String noPuncChat = ChatUtils.removeNonAlphanumeric(chat);
			int noPuncChatLength = ChatUtils.removeNonAlphanumeric(colorless).length();

			//if more then 55% of the chat is non-alphanumerical, remove the excess punctuation
			if((chatLength > 7 && chatLength*.55 < chatLength-noPuncChatLength)){
				chat = noPuncChat;
				evt.getPlayer().sendMessage(PLUGIN_PREFIX+"§cSpam Detect. Perhaps try with less punctuation.");
			}

			//If more then 55% of the chat is uppercase, make the chat lowercase
			if(ANTI_CAPS && noPuncChatLength > 13 && chatLength*.55 < noPuncChat.replaceAll("[^A-Z]", "").length()){
				chat = chat.toLowerCase();
				evt.getPlayer().sendMessage(PLUGIN_PREFIX+"§cPlease don't shout. My ears are hurting xP");
			}

			//Keep players from repeating messages
			/** TODO: make this? **/
		}
		//-------------------------------------------------------------------------

		chat = chat.trim();
		// If the new chat does not match the original message, log the original to the console
		if(evt.getMessage().equals(chat) == false){
			pl.getLogger().info("Unfiltered Chat: "+String.format(evt.getFormat(), pName, chat));
			evt.setMessage(chat);
		}

		final boolean canShareItem = DISPLAY_ITEMS && evt.getPlayer().hasPermission("chatmanager.displayitems");
		final boolean hasSharedItem = canShareItem && chat.matches(".*?\\[[i1-9]\\].*?");
		if(canShareItem) chat = chat.replace("[I]", "[i]");
		if(USE_DISPLAY_NAMES || hasSharedItem){
			final String chatName = (USE_DISPLAY_NAMES ? evt.getPlayer().getDisplayName()+ChatColor.RESET : pName);
			final String chatNamePlaceholder = "<<<"+pName+">>>";
			// The fully-formed chat message
			chat = String.format(evt.getFormat(), chatNamePlaceholder, chat);
			ListComponent comp = TellrawUtils.convertHexColorsToComponentsWithReset(chat);

			if(hasSharedItem){
				chat = chat.replaceAll("\\[[(i1-9)]\\]", "[$0]"); // Add an extra layer of brackets
				comp = TellrawUtils.convertHexColorsToComponentsWithReset(chat);
				if(chat.contains("[i]")){
					ItemStack hand = evt.getPlayer().getInventory().getItemInMainHand();
					comp.replaceRawDisplayTextWithComponent("[i]", getItemComponentWithAmount(hand));
				}
				for(int i=1; i<=9; ++i){
					if(chat.contains("["+i+"]")){
						ItemStack item = evt.getPlayer().getInventory().getItem(i-1);
						if(item == null) item = new ItemStack(Material.AIR);
						comp.replaceRawDisplayTextWithComponent("["+i+"]", getItemComponentWithAmount(item));
					}
				}
			}
			final String selectorHoverText = pName+"\nType: Player\n"+evt.getPlayer().getUniqueId();
			final String selectorClickSuggestText = "/tell "+pName+" ";
			comp.replaceRawDisplayTextWithComponent(chatNamePlaceholder, new ListComponent(
					new RawTextComponent(
						/*text=*/"", /*insert=*/null,
						new TextClickAction(ClickEvent.SUGGEST_COMMAND, selectorClickSuggestText),
						new TextHoverAction(HoverEvent.SHOW_TEXT, selectorHoverText),
						/*color=*/null, /*formats=*/null
					),
					TellrawUtils.convertHexColorsToComponentsWithReset(chatName)
			));
			evt.setCancelled(true);
			evt.setMessage("");
			final String compStr = comp.toString();
			final String plainText = comp.toPlainText();
			new BukkitRunnable(){@Override public void run(){
				pl.getServer().dispatchCommand(pl.getServer().getConsoleSender(), "minecraft:tellraw @a "+compStr);
				if(LOG_CHATS) pl.getServer().getLogger().info(plainText);
			}}.runTask(pl);
		}
	}
}
