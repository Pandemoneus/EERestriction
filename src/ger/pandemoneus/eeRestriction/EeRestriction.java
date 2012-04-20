package ger.pandemoneus.eeRestriction;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import com.sk89q.worldguard.bukkit.WorldGuardPlugin;

/**
 * This plugin blocks right-clicking with configurable items.
 * 
 * @author Pandemoneus
 */
public class EeRestriction extends JavaPlugin implements Listener {
	private WorldGuardPlugin worldGuard;
	private Logger logger;
	
	private static final String ITEM = "[1-9][0-9]*(;[0-9]+)?"; // this format: 344 or 344;5
	
	private final Map<Integer, HashSet<Integer>> restrictedItems = new HashMap<Integer, HashSet<Integer>>();
	
	//configuration values
	private boolean sendWarningToPlayer;
	private String warningMessage;
	
	public void onEnable() {
		worldGuard = getWorldGuard();
		logger = getLogger();
		
		getServer().getPluginManager().registerEvents(this, this);
		
		setupConfig();
	}
	
	public void onDisable() {
		
	}
	
	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void onEeToolUse(PlayerInteractEvent event) {
		if (worldGuard == null)
			return;
		
		if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
			final Location blockLocation = event.getClickedBlock().getLocation();
			
			// determine whether the block is in a WG region
			if (worldGuard.getRegionManager(blockLocation.getWorld()).getApplicableRegions(blockLocation).size() != 0) {
				final ItemStack itemBeingUsed = event.getItem();

				if (itemBeingUsed != null) {
					final int itemID = itemBeingUsed.getTypeId();
					final int dataValue = itemBeingUsed.getData() != null ? itemBeingUsed.getData().getData() : 0;

					if (restrictedItems.containsKey(itemID) && restrictedItems.get(itemID).contains(dataValue)) {
						if (sendWarningToPlayer)
							event.getPlayer().sendMessage(ChatColor.RED + warningMessage);
						
						event.setCancelled(true);
					}
				}
			}
		}
	}
	
	private void setupConfig() {
		final FileConfiguration config = getConfig();

		config.options().copyDefaults(true);
		config.options().header("Put the restricted items into the list at the bottom using this format: 334;3");
		
		saveConfig();
		
		sendWarningToPlayer = config.getBoolean("Message.send");
		warningMessage = config.getString("Message.text");
		setupRestrictedItems(config.getStringList("restrictedItems"));
	}
	
	private void setupRestrictedItems(List<String> restrictedItemList) {		
		if (restrictedItemList == null || restrictedItemList.isEmpty())
			return;
	
		// parse all Strings in the list
		for (String s : restrictedItemList) {
			if (s.matches(ITEM)) {
				int itemID;
				int dataValue = 0;
				
				if (s.contains(";")) {
					final String[] splitted = s.split(";");
					
					itemID = Integer.parseInt(splitted[0]);
					dataValue = Integer.parseInt(splitted[1]);
				} else {
					itemID = Integer.parseInt(s);
				}
				
				HashSet<Integer> dataValues = restrictedItems.get(itemID);
				
				if (dataValues == null)
					dataValues = new HashSet<Integer>();
				
				dataValues.add(dataValue);
				
				restrictedItems.put(itemID, dataValues);
			} else {
				logger.warning("Found a String in the config.yml that does not match the item pattern: " + s);
			}
		}
	}
	
	private WorldGuardPlugin getWorldGuard() {
	    Plugin plugin = getServer().getPluginManager().getPlugin("WorldGuard");
	 
	    if (plugin == null || !(plugin instanceof WorldGuardPlugin)) {
	        logger.severe("Could not load WorldGuard although we depend on it. Errors will occur!");
	    }
	 
	    return (WorldGuardPlugin) plugin;
	}
}
