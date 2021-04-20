package net.evmodder.ChatManager;

import org.bukkit.inventory.ItemStack;
import net.evmodder.EvLib.extras.ReflectionUtils;
import net.evmodder.EvLib.extras.ReflectionUtils.RefClass;
import net.evmodder.EvLib.extras.ReflectionUtils.RefMethod;

public class JunkUtils{
	// ItemStack methods to get a net.minecraft.server.ItemStack object for serialization
	final static RefClass craftItemStackClazz = ReflectionUtils.getRefClass("{cb}.inventory.CraftItemStack");
	final static RefMethod asNMSCopyMethod = craftItemStackClazz.getMethod("asNMSCopy", ItemStack.class);

	// NMS Method to serialize a net.minecraft.server.vX_X.ItemStack to a valid JSON string
	final static RefClass nmsItemStackClazz = ReflectionUtils.getRefClass("{nms}.ItemStack");
	final static RefClass nbtTagCompoundClazz = ReflectionUtils.getRefClass("{nms}.NBTTagCompound");
	final static RefMethod saveNmsItemStackMethod = nmsItemStackClazz.getMethod("save", nbtTagCompoundClazz);

	// https://www.spigotmc.org/threads/tut-item-tooltips-with-the-chatcomponent-api.65964/
	/**
	 * Converts an {@link org.bukkit.inventory.ItemStack} to a JSON string
	 * for sending with TextAction.ITEM
	 *
	 * @param itemStack the item to convert
	 * @return the JSON string representation of the item
	 */
	public final static String convertItemStackToJson(ItemStack item, int JSON_LIMIT){
		Object nmsNbtTagCompoundObj = nbtTagCompoundClazz.getConstructor().create();
		Object nmsItemStackObj = asNMSCopyMethod.of(null).call(item);
		Object itemAsJsonObject = saveNmsItemStackMethod.of(nmsItemStackObj).call(nmsNbtTagCompoundObj);
		String jsonString = itemAsJsonObject.toString();
		if(jsonString.length() > JSON_LIMIT){
			item = new ItemStack(item.getType(), item.getAmount());//TODO: Reduce item json data in a less destructive way-
			//reduceItemData() -> clear book pages, clear hidden NBT, call recursively for containers
			nmsNbtTagCompoundObj = nbtTagCompoundClazz.getConstructor().create();
			nmsItemStackObj = asNMSCopyMethod.of(null).call(item);
			itemAsJsonObject = saveNmsItemStackMethod.of(nmsItemStackObj).call(nmsNbtTagCompoundObj);
			jsonString = itemAsJsonObject.toString();
		}
		return itemAsJsonObject.toString();
	}
}
