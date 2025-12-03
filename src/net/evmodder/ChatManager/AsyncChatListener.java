package net.evmodder.ChatManager;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.Nonnull;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;
import net.evmodder.EvLib.bukkit.TellrawUtils.ClickEvent;
import net.evmodder.EvLib.bukkit.TellrawUtils.Component;
import net.evmodder.EvLib.bukkit.TellrawUtils.Format;
import net.evmodder.EvLib.bukkit.TellrawUtils.HoverEvent;
import net.evmodder.EvLib.bukkit.TellrawUtils.ListComponent;
import net.evmodder.EvLib.bukkit.TellrawUtils.RawTextComponent;
import net.evmodder.EvLib.bukkit.TellrawUtils.TextClickAction;
import net.evmodder.EvLib.bukkit.TellrawUtils.TextHoverAction;
import net.evmodder.EvLib.bukkit.TellrawUtils.TranslationComponent;
import net.evmodder.EvLib.bukkit.TellrawUtils;
import net.evmodder.EvLib.TextUtils;
import net.evmodder.EvLib.bukkit.NBTTagUtils;
import net.evmodder.EvLib.util.ReflectionUtils;
import net.evmodder.EvLib.bukkit.NBTTagUtils.RefNBTTagCompound;

class AsyncChatListener implements Listener{
	final ProfanityFilter chatFilter;
	private Map<UUID, List<Integer>> lastChats;
	final ChatManager pl;
	final boolean HANDLE_COLORS, HANDLE_FORMATS, LOG_CHATS;
	final boolean SANITIZE_CHAT, ANTI_SPAM, ANTI_CAPS, SEND_FAKE_EVT_TO_PLUGINS = true;
	final HashSet<AsyncPlayerChatEvent> fakeChatEvents = new HashSet<>();
	final String DEFAULT_BADWORD_SUB;
	final String BADWORD_RESULT_COMMAND, SPAM_RESULT_COMMAND;
	final String PLUGIN_PREFIX;
	final int MAX_CHATS_PER_MINUTE, MAX_CHATS_PER_10S, MAX_CHATS_PER_SECOND;
	final boolean DISPLAY_ITEMS, USE_DISPLAY_NAMES, USE_TEAM_COLORS, USE_MAIN_SCOREBOARD;
	final int JSON_LIMIT;
	final String DEFAULT_ITEM_DISPLAY_COLOR = "#cccccc";
	final String DI_PREFIX, DI_SUFFIX, DI_MAINHAND, DI_OFFHAND;
	final String DI_INNER_PAT, DI_FULL_PAT;
	final String DI_FULL_ESCAPE_PAT, DI_FULL_ESCAPE_REPL;
	final String DI_FULL_CAPTURE_PAT, DI_FULL_CAPTURE_REPL;
	final String DI_FULL_MAINHAND, DI_FULL_OFFHAND;

	AsyncChatListener(ChatManager plugin){
		this.pl = plugin;
		PLUGIN_PREFIX = TextUtils.translateAlternateColorCodes('&', pl.getConfig().getString("plugin-message-prefix", "&3<&aC&3>&f "));
		// Cosmetic
		USE_DISPLAY_NAMES = pl.getConfig().getBoolean("use-player-displaynames", true);
		USE_TEAM_COLORS = pl.getConfig().getBoolean("use-player-teams", true);
		USE_MAIN_SCOREBOARD = pl.getConfig().getBoolean("use-main-scoreboard", true);
		HANDLE_COLORS = pl.getConfig().getBoolean("chat-colors", true);
		HANDLE_FORMATS = pl.getConfig().getBoolean("chat-formats", true);
		// Filter
		SANITIZE_CHAT = pl.getConfig().getBoolean("sanitize-chat", true);
		chatFilter = SANITIZE_CHAT ? pl.profanityFilter : null;
		DEFAULT_BADWORD_SUB = pl.getConfig().getString("default-badword-replacement", "*");
		BADWORD_RESULT_COMMAND = pl.getConfig().getString("badword-result-command", "").trim();
		// Spam
		ANTI_SPAM = pl.getConfig().getBoolean("anti-spam", false);
		ANTI_CAPS = pl.getConfig().getBoolean("anti-caps", true);
		lastChats = new HashMap<UUID, List<Integer>>();
		SPAM_RESULT_COMMAND = pl.getConfig().getString("spam-result-command", "kick %name% §cReceived TMC/TMS from client\\\\n§fEither lag or spam... :P");
		MAX_CHATS_PER_MINUTE = pl.getConfig().getInt("max-chats-per-minute", 35);
		MAX_CHATS_PER_10S = pl.getConfig().getInt("max-chats-per-10s", 15);
		MAX_CHATS_PER_SECOND = pl.getConfig().getInt("max-chats-per-second", 4);
		LOG_CHATS = pl.getConfig().getBoolean("log-chat-to-console", true);
		// DI
		DISPLAY_ITEMS = pl.getConfig().getBoolean("item-hover-display", true);
		JSON_LIMIT = pl.getConfig().getInt("message-json-limit", 15000);
		DI_PREFIX = pl.getConfig().getString("item-share-prefix", "[");
		DI_SUFFIX = pl.getConfig().getString("item-share-suffix", "]");
		DI_MAINHAND = pl.getConfig().getString("item-share-mainhand", "i");
		DI_OFFHAND = pl.getConfig().getString("item-share-offhand", "o");
		DI_INNER_PAT = Pattern.quote(DI_MAINHAND)+"|"+Pattern.quote(DI_OFFHAND)+"|[1-9]";
		DI_FULL_PAT = Pattern.quote(DI_PREFIX)+"(?:"+DI_INNER_PAT+")"+Pattern.quote(DI_SUFFIX);
		DI_FULL_ESCAPE_PAT = Pattern.quote(DI_PREFIX)+"("+DI_INNER_PAT+")(\\1+)"+Pattern.quote(DI_SUFFIX);
		DI_FULL_ESCAPE_REPL = Matcher.quoteReplacement(DI_PREFIX)+"$2"+Matcher.quoteReplacement(DI_SUFFIX);
		DI_FULL_CAPTURE_PAT = "("+DI_FULL_PAT+")";
		DI_FULL_CAPTURE_REPL = Matcher.quoteReplacement(DI_PREFIX)+"$1"+Matcher.quoteReplacement(DI_SUFFIX);
		DI_FULL_MAINHAND = DI_PREFIX + DI_MAINHAND + DI_SUFFIX;
		DI_FULL_OFFHAND = DI_PREFIX + DI_OFFHAND + DI_SUFFIX;
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

	private final static Field displayNameField = ReflectionUtils.getField(ReflectionUtils.getClass("{cb}.inventory.CraftMetaItem"), "displayName");
	private final static Method toStrMethod;
	private final static Object registryAccessObj;//class: IRegistryCustom.Dimension
	static{
		if(ReflectionUtils.isAtLeastVersion("v1_20_5")){
			final Class<?> iChatBaseComponentClass = ReflectionUtils.getClass("{nm}.network.chat.IChatBaseComponent");
			final Class<?> chatSerializerClass = ReflectionUtils.getClass("{nm}.network.chat.IChatBaseComponent$ChatSerializer");
			final Class<?> holderLookupProviderClass = ReflectionUtils.getClass("{nm}.core.HolderLookup$Provider", "{nm}.core.HolderLookup$a");
			toStrMethod = ReflectionUtils.findMethod(chatSerializerClass, /*isStatic=*/true, String.class, iChatBaseComponentClass, holderLookupProviderClass);

			Class<?> classCraftServer = ReflectionUtils.getClass("{cb}.CraftServer");
			Method method_CraftServer_getServer = ReflectionUtils.getMethod(classCraftServer, "getServer");
			final Object nmsServerObj = ReflectionUtils.call(method_CraftServer_getServer, Bukkit.getServer());
//			registryAccessObj = ReflectionUtils.getClass("{nm}.server.MinecraftServer").findMethod(/*isStatic=*/false,
//					ReflectionUtils.getClass("net.minecraft.core.IRegistryCustom$Dimension")).of(nmsServerObj).call();
			Class<?> classMinecraftServer = ReflectionUtils.getClass("{nm}.server.MinecraftServer");
			Method method_MinecraftServer_getRegistryAccess = ReflectionUtils.findMethod(
					classMinecraftServer, /*isStatic=*/false, ReflectionUtils.getClass("net.minecraft.core.IRegistryCustom$Dimension"));
			registryAccessObj = ReflectionUtils.call(method_MinecraftServer_getRegistryAccess, nmsServerObj);
		}
		else registryAccessObj = toStrMethod = null;
	}
	private static final String jsonStrFromChatComp(Object chatComp){
		if(chatComp == null) return null;
		if(ReflectionUtils.isAtLeastVersion("v1_21_6")) return (String)ReflectionUtils.callStatic(toStrMethod, chatComp);
		else return (String)ReflectionUtils.callStatic(toStrMethod, chatComp, registryAccessObj);
	}
	public static final String getDisplayName(@Nonnull ItemStack item){
		if(toStrMethod != null){
			if(!item.hasItemMeta()) return null;
			try{return jsonStrFromChatComp(ReflectionUtils.get(displayNameField, item.getItemMeta()));}
			catch(RuntimeException ex){
				//Caused by: java.lang.reflect.InvocationTargetException
				//Caused by: java.lang.NullPointerException: Cannot invoke "net.minecraft.network.chat.Component.tryCollapseToString()" because "text" is null
				return null;
			}
		}
		else{
			RefNBTTagCompound tag = NBTTagUtils.getTag(item);
			RefNBTTagCompound display = tag.hasKey("display") ? (RefNBTTagCompound)tag.get("display") : new RefNBTTagCompound();
			return display.hasKey("Name") ? display.getString("Name") : null;
		}
	}

	Component getItemComponent(ItemStack item){
		//TODO: Config option for shulker boxes, show first 6 or entire contents?
		TextHoverAction hoverAction = new TextHoverAction(HoverEvent.SHOW_ITEM, JunkUtils.convertItemStackToJson(item, JSON_LIMIT));
		String rarityColor = TextUtils.getRarityColor(item).name().toLowerCase();
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

	@EventHandler(priority = EventPriority.LOWEST)
	public void onPlayerChat(AsyncPlayerChatEvent evt){
		//if it is already cancelled or is something a plugin forced the player to say
		if(evt.isCancelled() || !evt.isAsynchronous()) return;
		synchronized(fakeChatEvents){if(fakeChatEvents.remove(evt)) return;}

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
				if(BADWORD_RESULT_COMMAND != null && !BADWORD_RESULT_COMMAND.isEmpty()){
					String command = BADWORD_RESULT_COMMAND.replace("%name%", pName);
					pl.getLogger().info("Running command: "+command);
					new BukkitRunnable(){@Override public void run(){
						pl.getServer().dispatchCommand(pl.getServer().getConsoleSender(), command);
					}}.run();
				}
			}
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
		chat = chat.trim();
		final boolean nautyChat = !evt.getMessage().equals(chat);
		if(nautyChat) pl.getLogger().info("Unfiltered Chat: "+String.format(evt.getFormat(), pName, chat));

		//-------------------------------------------------------------------------
		if(HANDLE_FORMATS && evt.getPlayer().hasPermission("chatmanager.format")){
			chat = ChatUtils.determineFormatsByPermission(chat, evt.getPlayer());
		}
		if(HANDLE_COLORS && evt.getPlayer().hasPermission("chatmanager.color")){
			chat = ChatUtils.determineColorsByPermission(chat, evt.getPlayer());
		}

		//-------------------------------------------------------------------------
		final boolean canShareItem = DISPLAY_ITEMS && evt.getPlayer().hasPermission("chatmanager.displayitems");
		boolean hasSharedItem = false;
		if(canShareItem){
			hasSharedItem = chat.matches(".*?"+DI_FULL_PAT+".*?");
			if(!hasSharedItem) chat = chat.replaceAll(DI_FULL_ESCAPE_PAT, DI_FULL_ESCAPE_REPL);//[ii] -> [i], [iii] -> [ii]
		}
		if(evt.getMessage().equals(chat) == false) evt.setMessage(chat);

		//-------------------------------------------------------------------------
		if(USE_DISPLAY_NAMES || USE_TEAM_COLORS || hasSharedItem){
			//pl.getLogger().info("has shared item: "+hasSharedItem);
			String prefix = "", suffix = "";
			if(USE_TEAM_COLORS){
				final Scoreboard scoreboard = USE_MAIN_SCOREBOARD
						? pl.getServer().getScoreboardManager().getMainScoreboard() : evt.getPlayer().getScoreboard();
				final Team team = scoreboard.getEntryTeam(evt.getPlayer().getName());
				if(team != null){
					// Note: Advanced selctors/tellraw comps are unsupported here (blame Bukkit API for Teams)
					prefix = team.getColor() + team.getPrefix() + team.getColor(); // Yep, this is how it works in vanilla *shrug*
					suffix = team.getSuffix();
				}
			}
			final String chatName = prefix+(USE_DISPLAY_NAMES ? evt.getPlayer().getDisplayName()
					+(ChatColor.getLastColors(evt.getPlayer().getDisplayName()).isEmpty() ? "" : ChatColor.RESET) : pName)+suffix;
			final String chatNamePlaceholder = "<<<"+pName+">>>";
			// The fully-formed chat message
			chat = String.format(evt.getFormat(), chatNamePlaceholder, chat);
			if(hasSharedItem) chat = chat.replaceAll(DI_FULL_CAPTURE_PAT, DI_FULL_CAPTURE_REPL); // Add an extra layer of brackets
			ListComponent comp = TellrawUtils.convertHexColorsToComponentsWithReset(chat);

			final String selectorHoverText = pName+"\nType: Player\n"+evt.getPlayer().getUniqueId();
			final String selectorClickSuggestText = "/tell "+pName+" ";
			comp.replaceRawTextWithComponent(chatNamePlaceholder, new ListComponent(
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

			if(hasSharedItem){
				if(chat.contains(DI_FULL_MAINHAND)){
					ItemStack hand = evt.getPlayer().getInventory().getItemInMainHand();
					comp.replaceRawTextWithComponent(DI_FULL_MAINHAND, getItemComponentWithAmount(hand));
				}
				if(chat.contains(DI_FULL_OFFHAND)){
					ItemStack hand = evt.getPlayer().getInventory().getItemInOffHand();
					comp.replaceRawTextWithComponent(DI_FULL_OFFHAND, getItemComponentWithAmount(hand));
				}
				for(int i=1; i<=9; ++i){
					if(chat.contains(DI_PREFIX+i+DI_SUFFIX)){
						ItemStack item = evt.getPlayer().getInventory().getItem(i-1);
						if(item == null) item = new ItemStack(Material.AIR);
						comp.replaceRawTextWithComponent(DI_PREFIX+i+DI_SUFFIX, getItemComponentWithAmount(item));
					}
				}
			}
			String compStr = comp.toString();
			String plainText = comp.toPlainText();
			if(hasSharedItem){
				compStr = compStr.replaceAll(DI_FULL_ESCAPE_PAT, DI_FULL_ESCAPE_REPL);//[ii] -> [i], [iii] -> [ii]
				plainText = plainText.replaceAll(DI_FULL_ESCAPE_PAT, DI_FULL_ESCAPE_REPL);//[ii] -> [i], [iii] -> [ii]
			}
			final String finalCompStr = compStr;
			final String finalPlainText = plainText;
			new BukkitRunnable(){@Override public void run(){
				// TODO: handle evt.getRecipients()
				if(evt.getRecipients().size() != pl.getServer().getOnlinePlayers().size()){
					pl.getServer().getLogger().warning("Sending DisplayItem chat to @a, ignoring getRecipients()");
				}
				pl.getServer().dispatchCommand(pl.getServer().getConsoleSender(), "minecraft:tellraw @a "+finalCompStr);
				if(LOG_CHATS) pl.getServer().getLogger().info(finalPlainText);
			}}.runTask(pl);

			if(SEND_FAKE_EVT_TO_PLUGINS){ // Helps with compatibility
				final String hackyMsgWithoutName = plainText.substring(plainText.indexOf(' ')+1);
				AsyncPlayerChatEvent newEvt = new AsyncPlayerChatEvent(/*async=*/true, evt.getPlayer(), hackyMsgWithoutName, evt.getRecipients());
				synchronized(fakeChatEvents){fakeChatEvents.add(newEvt);}
				pl.getServer().getPluginManager().callEvent(newEvt);
			}
		}
	}
}
