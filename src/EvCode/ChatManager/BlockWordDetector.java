package EvCode.ChatManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;

public class BlockWordDetector implements Listener{
	@SuppressWarnings("unused")
	private ChatManager chatManager;
	
	private Map<UUID, List<Location>> recentlyPlacedBlocks;
	
	public BlockWordDetector(ChatManager plugin, Set<String> blockedBlockWords){
		chatManager = plugin;
		plugin.getServer().getPluginManager().registerEvents(this, plugin);
		
		recentlyPlacedBlocks = new HashMap<UUID, List<Location>>();
	}
	
	@EventHandler
	public void onBlockPlace(BlockPlaceEvent evt){
		if(recentlyPlacedBlocks.containsKey(evt.getPlayer().getUniqueId()) == false){
			recentlyPlacedBlocks.put(evt.getPlayer().getUniqueId(), new ArrayList<Location>());
		}
		searchForLetter(evt.getPlayer().getUniqueId(), evt.getBlock().getLocation());
	}
	
	private void searchForLetter(UUID pUUID, Location origin){
		int within10 = 1;
		for(Location l : recentlyPlacedBlocks.get(pUUID)){
			if(l.distanceSquared(origin) < 100) within10++;
		}
		recentlyPlacedBlocks.get(pUUID).add(origin);
		
		if(within10 > 15){
			//check for letters, then words
		}
	}
}
