package net.evmodder.ChatManager;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import net.evmodder.EvLib.util.FileIO;
import net.evmodder.EvLib.util.ReflectionUtils;

public class JunkUtils{
	// ItemStack methods to get a net.minecraft.server.ItemStack object for serialization
	final static Class<?> craftItemStackClazz = ReflectionUtils.getClass("{cb}.inventory.CraftItemStack");
	final static Method asNMSCopyMethod = ReflectionUtils.getMethod(craftItemStackClazz, "asNMSCopy", ItemStack.class);

//	// NMS Method to serialize a net.minecraft.server.vX_X.ItemStack to a valid JSON string
//	final static RefClass nmsItemStackClazz = ReflectionUtils.getRefClass("{nms}.ItemStack", "{nm}.world.item.ItemStack");
//	final static RefClass nbtTagCompoundClazz = ReflectionUtils.getRefClass("{nms}.NBTTagCompound", "{nm}.nbt.NBTTagCompound");
//	final static RefMethod saveNmsItemStackMethod = nmsItemStackClazz.findMethod(/*isStatic=*/false, nbtTagCompoundClazz, nbtTagCompoundClazz);

	// https://www.spigotmc.org/threads/tut-item-tooltips-with-the-chatcomponent-api.65964/
	/**
	 * Converts an {@link org.bukkit.inventory.ItemStack} to a JSON string
	 * for sending with TextAction.ITEM
	 *
	 * @param itemStack the item to convert
	 * @return the JSON string representation of the item
	 */
	public final static String convertItemStackToJson(ItemStack item, int JSON_LIMIT){
		Material type = item.getType();
		String id = type.isRegistered() ? type.getKeyOrThrow().getKey() : type.name().toLowerCase();
		if(item.hasItemMeta()){
			String jsonString = "{id:\""+id+"\",count:"+item.getAmount()+",components:"+item.getItemMeta().getAsString()+"}";
			if(jsonString.length() <= JSON_LIMIT) return jsonString;
			//TODO: Reduce item json data in a less destructive way
			//reduceItemData() -> clear book pages, clear hidden NBT, call recursively for containers
		}
		return "{id:\""+id+"\",count:"+item.getAmount()+"}";
	}
//	public final static String convertItemStackToJson(ItemStack item, int JSON_LIMIT){
//		Object nmsNbtTagCompoundObj = nbtTagCompoundClazz.getConstructor().create();
//		Object nmsItemStackObj = asNMSCopyMethod.of(null).call(item);
//		Object itemAsJsonObject = saveNmsItemStackMethod.of(nmsItemStackObj).call(nmsNbtTagCompoundObj);
//		String jsonString = itemAsJsonObject.toString();
//		if(jsonString.length() > JSON_LIMIT){
//			item = new ItemStack(item.getType(), item.getAmount());//TODO: Reduce item json data in a less destructive way-
//			//reduceItemData() -> clear book pages, clear hidden NBT, call recursively for containers
//			nmsNbtTagCompoundObj = nbtTagCompoundClazz.getConstructor().create();
//			nmsItemStackObj = asNMSCopyMethod.of(null).call(item);
//			itemAsJsonObject = saveNmsItemStackMethod.of(nmsItemStackObj).call(nmsNbtTagCompoundObj);
//			jsonString = itemAsJsonObject.toString();
//		}
//		return itemAsJsonObject.toString();
//	}

	// Different from FileIO only in that it doesn't trim trailing/leading line whitespace
	public static String loadResource(Object pl, String filename/*, boolean keepComments*/){
		try{
			BufferedReader reader = new BufferedReader(
					new InputStreamReader(pl.getClass().getResourceAsStream("/"+filename)));

			StringBuilder file = new StringBuilder();
			String line;
			while((line = reader.readLine()) != null){
				line = line.replace("//", "#");
				int cut = /*keepComments ? -1 :*/ line.indexOf('#');
				if(cut == -1) file.append('\n').append(line);
				else if(cut > 0) file.append('\n').append(line.substring(0, cut));
			}
			reader.close();
			return file.substring(1);
		}
		catch(IOException ex){ex.printStackTrace();}
		return "";
	}

	// Different from FileIO only in that it doesn't trim trailing/leading line whitespace
	public static String loadFile(String filename, String defaultContent){
		BufferedReader reader = null;
		try{reader = new BufferedReader(new FileReader(FileIO.DIR+filename));}
		catch(FileNotFoundException e){
			if(defaultContent == null || defaultContent.isEmpty()) return defaultContent;

			//Create Directory
			File dir = new File(FileIO.DIR);
			if(!dir.exists())dir.mkdir();

			//Create the file
			File conf = new File(FileIO.DIR+filename);
			try{
				conf.createNewFile();
				BufferedWriter writer = new BufferedWriter(new FileWriter(conf));
				writer.write(defaultContent);
				writer.close();
			}
			catch(IOException e1){e1.printStackTrace();}
			return defaultContent;
		}
		StringBuilder file = new StringBuilder();
		if(reader != null){
			try{
				String line;
				while((line = reader.readLine()) != null){
					line = line.replace("//", "#");
					int cut = line.indexOf('#');
					if(cut == -1) file.append('\n').append(line);
					else if(cut > 0) file.append('\n').append(line.substring(0, cut));
				}
				reader.close();
			}catch(IOException e){}
		}
		return file.length() == 0 ? "" : file.substring(1);//Hmm; return "" or defaultContent
	}
}
