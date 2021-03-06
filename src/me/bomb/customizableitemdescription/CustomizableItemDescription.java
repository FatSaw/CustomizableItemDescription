package me.bomb.customizableitemdescription;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.craftbukkit.v1_12_R1.entity.CraftPlayer;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionType;

import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.ChannelPromise;
import net.minecraft.server.v1_12_R1.Container;
import net.minecraft.server.v1_12_R1.ContainerAnvil;
import net.minecraft.server.v1_12_R1.PacketDataSerializer;
import net.minecraft.server.v1_12_R1.PacketPlayOutSetSlot;
import net.minecraft.server.v1_12_R1.PacketPlayOutWindowItems;
import net.minecraft.server.v1_12_R1.PacketPlayInSetCreativeSlot;

public class CustomizableItemDescription extends JavaPlugin implements Listener {

	private YamlConfiguration lang;

	public void onEnable() {
		if (!new File(getDataFolder() + File.separator + "lang.yml").exists()) {
			saveResource("lang.yml", true);
		}
		lang = YamlConfiguration.loadConfiguration(new File(getDataFolder() + File.separator + "lang.yml"));
		Bukkit.getPluginManager().registerEvents(this, this);
		for(Player player : Bukkit.getOnlinePlayers()) {
			registerHandler(player);
		}
	}

	public void onDisable() {
		for(Player player : Bukkit.getOnlinePlayers()) {
			unregisterHandler(player);
		}
	}
	
	protected void clearItemName(net.minecraft.server.v1_12_R1.ItemStack nmsitem) {
		ItemStack itemstack = nmsitem.asBukkitMirror();
		if(nmsitem.hasName()) {
			String displayname = itemstack.getItemMeta().getDisplayName();
			if(displayname.startsWith("??R??r")) {
				nmsitem.s();
			} else {
				if(lang.isString("items.".concat(itemstack.getType().name()).concat("-").concat(Byte.toString(itemstack.getData().getData())).concat(".custom"))) {
					String newdisplayname = lang.getString("items.".concat(itemstack.getType().name()).concat("-").concat(Byte.toString(itemstack.getData().getData())).concat(".custom"));
					if(newdisplayname.contains("%displayname%")) {
						try{
							int startindex = newdisplayname.lastIndexOf("%displayname%");
							int endindext = newdisplayname.lastIndexOf(newdisplayname.substring(startindex+13));
							String itemnameend = newdisplayname.substring(endindext);
							if(displayname.contains(itemnameend)) {
								nmsitem.g(displayname.substring(startindex, displayname.lastIndexOf(itemnameend)));
							}
						} catch (Exception e) {
							nmsitem.s();
						}
					} else {
						nmsitem.s();
					}
				} else {
					//nmsitem.s();
				}
			}
		}
	}
	
	protected void clear(net.minecraft.server.v1_12_R1.ItemStack nmsitem) {
		ItemStack itemstack = nmsitem.asBukkitMirror();
		clearItemName(nmsitem);
		ItemMeta meta = itemstack.getItemMeta();
		if(meta.hasLore() && meta.getLore().get(meta.getLore().size()-1).endsWith("??0??0??0??r")) {
			List<String> lore = new ArrayList<String>();
			short i = 0;
			while(i<meta.getLore().size()) {
				String line = meta.getLore().get(i);
				if(line.startsWith("??f??f??f??r")) {
					break;
				} else {
					lore.add(line);
				}
				++i;
			}
			meta.removeItemFlags(ItemFlag.HIDE_POTION_EFFECTS);
			meta.removeItemFlags(ItemFlag.HIDE_ENCHANTS);
			meta.removeItemFlags(ItemFlag.HIDE_UNBREAKABLE);
			meta.setLore(lore);
			itemstack.setItemMeta(meta);
		}
	}

	protected void deeper(net.minecraft.server.v1_12_R1.ItemStack nmsitem) {
		ItemStack itemstack = nmsitem.asBukkitMirror();
		boolean hoa = false;
		ItemMeta meta = itemstack.getItemMeta();
		if(meta.hasDisplayName()) {
			if(!meta.getDisplayName().startsWith("??R??r")) {
				String newdisplayname = lang.getString("items.".concat(itemstack.getType().name()).concat("-").concat(Byte.toString(itemstack.getData().getData())).concat(".custom"), "??c??l".concat(itemstack.getType().name()).concat("-").concat(Byte.toString(itemstack.getData().getData())).concat(" ").concat(meta.getDisplayName())).replace("%displayname%", meta.getDisplayName());
				meta.setDisplayName(newdisplayname);
			}
		} else {
			String newdisplayname = "??R??r".concat(lang.getString("items.".concat(itemstack.getType().name()).concat("-").concat(Byte.toString(itemstack.getData().getData())).concat(".default"), "??4??l".concat(itemstack.getType().name()).concat("-").concat(Byte.toString(itemstack.getData().getData()))));
			meta.setDisplayName(newdisplayname);
		}
		List<String> infolore = new ArrayList<String>();
		if(itemstack.getType().equals(Material.POTION) || itemstack.getType().equals(Material.SPLASH_POTION) || itemstack.getType().equals(Material.LINGERING_POTION) || itemstack.getType().equals(Material.TIPPED_ARROW)) {
			PotionMeta potionmeta = (PotionMeta) itemstack.getItemMeta();
			List<PotionEffect> effects = getPotionEffects(potionmeta);
			if(!effects.isEmpty()) {
				hoa = true;
				meta.addItemFlags(ItemFlag.HIDE_POTION_EFFECTS);
				for(PotionEffect effect : effects) {
					if(effect.getDuration()>0 && effect.getAmplifier()>-1) {
						short amplifier = (short) effect.getAmplifier();
						++amplifier;
						short duration = (short) effect.getDuration();
						if(itemstack.getType().equals(Material.LINGERING_POTION)) duration=(short) (duration>>2);
						if(itemstack.getType().equals(Material.TIPPED_ARROW)) duration=(short) (duration>>3);
						String effectname = effect.getType().getName();
						String ramplifier = RomanNumeral.getwithspace(amplifier);
						String rduration = getTimeLeftFromTicks(duration);
						if(effectname!=null && !effectname.isEmpty()) {
							infolore.add(lang.getString("effects.".concat(effectname), "??r".concat(effectname).concat(ramplifier).concat(" ").concat(rduration)).replaceAll("%amplifier%", ramplifier).replaceAll("%duration%", rduration));
						}
					}
				}
			}
		}
		if(itemstack.getType().equals(Material.ENCHANTED_BOOK)) {
			hoa = true;
			EnchantmentStorageMeta enchantmentmeta = (EnchantmentStorageMeta) meta;
			meta.addItemFlags(ItemFlag.HIDE_POTION_EFFECTS);
			Map<Enchantment,Integer> enchs = enchantmentmeta.getStoredEnchants();
			for(Enchantment ench : enchs.keySet()) {
				short level = 0;
				if(enchs.containsKey(ench)) {
					level = enchs.get(ench).shortValue();
					if(level>0) {
						String rlevel = RomanNumeral.get(level);
						String enchantmentname = ench.getName();
						if(enchantmentname!=null && !enchantmentname.isEmpty()) {
							infolore.add(lang.getString("enchantments.".concat(enchantmentname), "??r".concat(enchantmentname).concat(" ").concat(rlevel)).replaceAll("%value%", rlevel));
						}
					}
				}
			}
		}
		if(meta.hasEnchants()) {
			hoa = true;
			meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
			Map<Enchantment,Integer> enchs = meta.getEnchants();
			for(Enchantment ench : enchs.keySet()) {
				short level = 0;
				if(enchs.containsKey(ench)) {
					level = enchs.get(ench).shortValue();
					if(level>0) {
						String rlevel = RomanNumeral.get(level);
						String enchantmentname = ench.getName();
						if(enchantmentname!=null && !enchantmentname.isEmpty()) {
							infolore.add(lang.getString("enchantments.".concat(enchantmentname), "??r".concat(enchantmentname).concat(" ").concat(rlevel)).replaceAll("%value%", rlevel));
						}
					}
				}
			}
		}
		if(meta.isUnbreakable()) {
			hoa = true;
			meta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE);
			infolore.add(lang.getString("unbreakable","??rUNBREAKABLE"));
		}
		if(hoa&&!infolore.isEmpty()) {
			List<String> lore = new ArrayList<String>();
			if(meta.hasLore()) {
				lore = meta.getLore();
			}
			lore.add("??f??f??f??r".concat(infolore.get(0)));
			byte i = 1;
			while (i<infolore.size()&&i>0) {
				lore.add(infolore.get(i));
				++i;
			}
			lore.set(lore.size()-1, lore.get(lore.size()-1).concat("??0??0??0??r"));
			meta.setLore(lore);
		}
		itemstack.setItemMeta(meta);
	}

	private String getTimeLeftFromTicks(int duration) {
		if(duration<0||duration>32766) return "**:**";
		duration = (short) (duration/20);
		byte minutes = (byte) (duration/60);
		byte seconds = (byte) (duration-minutes*60);
		String secondss = Byte.toString(seconds);
		if(seconds<10) secondss = "0".concat(secondss);
		return Byte.toString(minutes).concat(":").concat(secondss);
    }

	private List<PotionEffect> getPotionEffects(PotionMeta potion) {
		PotionEffect effect = null;
		PotionType potiontype = potion.getBasePotionData().getType();
		boolean upgraded = potion.getBasePotionData().isUpgraded();
		boolean extended = potion.getBasePotionData().isExtended();
		if(upgraded == false && extended == false) {
			switch(potiontype) {
			case NIGHT_VISION:
				effect = new PotionEffect(PotionEffectType.NIGHT_VISION, 3600, 0);
			break;
			case INVISIBILITY:
				effect = new PotionEffect(PotionEffectType.INVISIBILITY, 3600, 0);
			break;
			case JUMP:
				effect = new PotionEffect(PotionEffectType.JUMP, 3600, 0);
			break;
			case FIRE_RESISTANCE:
				effect = new PotionEffect(PotionEffectType.FIRE_RESISTANCE, 3600, 0);
			break;
			case SPEED:
				effect = new PotionEffect(PotionEffectType.SPEED, 3600, 0);
			break;
			case SLOWNESS:
				effect = new PotionEffect(PotionEffectType.SLOW, 1800, 0);
			break;
			case WATER_BREATHING:
				effect = new PotionEffect(PotionEffectType.WATER_BREATHING, 3600, 0);
			break;
			case INSTANT_HEAL:
				effect = new PotionEffect(PotionEffectType.HEAL, 1, 0);
			break;
			case INSTANT_DAMAGE:
				effect = new PotionEffect(PotionEffectType.HARM, 1, 0);
			break;
			case POISON:
				effect = new PotionEffect(PotionEffectType.POISON, 900, 0);
			break;
			case REGEN:
				effect = new PotionEffect(PotionEffectType.REGENERATION, 900, 0);
			break;
			case STRENGTH:
				effect = new PotionEffect(PotionEffectType.INCREASE_DAMAGE, 3600, 0);
			break;
			case WEAKNESS:
				effect = new PotionEffect(PotionEffectType.WEAKNESS, 1800, 0);
			break;
			case LUCK:
				effect = new PotionEffect(PotionEffectType.LUCK, 6000, 0);
			break;
			default:
			break;
			}
		} else if(upgraded == true && extended == false) {
			switch(potiontype) {
			case JUMP:
				effect = new PotionEffect(PotionEffectType.JUMP, 1800, 1);
			break;
			case SPEED:
				effect = new PotionEffect(PotionEffectType.SPEED, 1800, 1);
			break;
			case SLOWNESS:
				effect = new PotionEffect(PotionEffectType.SLOW, 400, 3);
			break;
			case INSTANT_HEAL:
				effect = new PotionEffect(PotionEffectType.HEAL, 1, 1);
			break;
			case INSTANT_DAMAGE:
				effect = new PotionEffect(PotionEffectType.HARM, 1, 1);
			break;
			case POISON:
				effect = new PotionEffect(PotionEffectType.POISON, 420, 1);
			break;
			case REGEN:
				effect = new PotionEffect(PotionEffectType.REGENERATION, 440, 1);
			break;
			case STRENGTH:
				effect = new PotionEffect(PotionEffectType.INCREASE_DAMAGE, 1800, 1);
			break;
			default:
			break;
			}
		} else if(upgraded == false && extended == true) {
			switch(potiontype) {
			case NIGHT_VISION:
				effect = new PotionEffect(PotionEffectType.NIGHT_VISION, 9600, 0);
			break;
			case INVISIBILITY:
				effect = new PotionEffect(PotionEffectType.INVISIBILITY, 9600, 0);
			break;
			case JUMP:
				effect = new PotionEffect(PotionEffectType.JUMP, 9600, 0);
			break;
			case FIRE_RESISTANCE:
				effect = new PotionEffect(PotionEffectType.FIRE_RESISTANCE, 9600, 0);
			break;
			case SPEED:
				effect = new PotionEffect(PotionEffectType.SPEED, 9600, 0);
			break;
			case SLOWNESS:
				effect = new PotionEffect(PotionEffectType.SLOW, 4800, 0);
			break;
			case WATER_BREATHING:
				effect = new PotionEffect(PotionEffectType.WATER_BREATHING, 9600, 0);
			break;
			case POISON:
				effect = new PotionEffect(PotionEffectType.POISON, 1800, 0);
			break;
			case REGEN:
				effect = new PotionEffect(PotionEffectType.REGENERATION, 1800, 0);
			break;
			case STRENGTH:
				effect = new PotionEffect(PotionEffectType.INCREASE_DAMAGE, 9600, 0);
			break;
			case WEAKNESS:
				effect = new PotionEffect(PotionEffectType.WEAKNESS, 4800, 0);
			break;
			default:
			break;
			}
		}
		List<PotionEffect> effects = new ArrayList<PotionEffect>();
		if(potion.hasCustomEffects()) effects.addAll(potion.getCustomEffects());
		boolean foundsameefect = false;
		if(effect!=null) {
			boolean maineffectoverride = false;
			byte i = 0;
			while(!foundsameefect && i<effects.size()) {
				PotionEffect aeffect = effects.get(i);
				if(aeffect.getType().equals(effect.getType())) {
					if((aeffect.getAmplifier()==effect.getAmplifier()&&effect.getDuration()>aeffect.getDuration())||effect.getAmplifier()>aeffect.getAmplifier()) {
						effects.set(i, effect);
						maineffectoverride = true;
					}
					foundsameefect = true;
				}
				++i;
			}
			if(!maineffectoverride) effects.add(effect);
		}
		return effects;
	}

	private void registerHandler(Player player) {
		ChannelDuplexHandler channelDuplexHandler = new ChannelDuplexHandler() {
			@Override
			public void channelRead(ChannelHandlerContext context, Object packet) throws Exception {
				if (packet instanceof PacketPlayInSetCreativeSlot) {
					PacketPlayInSetCreativeSlot info = (PacketPlayInSetCreativeSlot) packet;
					PacketDataSerializer packetdataserializer = new PacketDataSerializer(Unpooled.buffer(0));
					info.b(packetdataserializer);
					short slotid = packetdataserializer.readShort();
					packetdataserializer.writeShort(slotid);
					net.minecraft.server.v1_12_R1.ItemStack item = packetdataserializer.k();
					if(!item.isEmpty()) clear(item);
			        packetdataserializer.a(item);
			        info.a(packetdataserializer);
				}
				super.channelRead(context, packet);
			}
			@Override
			public void write(ChannelHandlerContext context, Object packet, ChannelPromise channelPromise) throws Exception {
				if (packet instanceof PacketPlayOutWindowItems) {
					PacketPlayOutWindowItems info = (PacketPlayOutWindowItems) packet;
					PacketDataSerializer packetdataserializer = new PacketDataSerializer(Unpooled.buffer(0));
					info.b(packetdataserializer);
					short invid = packetdataserializer.readUnsignedByte();
					packetdataserializer.writeByte(invid);
			        short inventorysize = packetdataserializer.readShort();
			        packetdataserializer.writeShort(inventorysize);
			        Container container = ((CraftPlayer)player).getHandle().activeContainer;
					if(container instanceof ContainerAnvil && invid==container.windowId) {
						{
							net.minecraft.server.v1_12_R1.ItemStack item = packetdataserializer.k();
							if(!item.isEmpty()) clearItemName(item);
							packetdataserializer.a(item);
						}
						for (int i = 1; i < inventorysize; ++i) {
				        	net.minecraft.server.v1_12_R1.ItemStack item = packetdataserializer.k();
				        	if(!item.isEmpty()) deeper(item);
				            packetdataserializer.a(item);
				        }
					} else {
						for (int i = 0; i < inventorysize; ++i) {
				        	net.minecraft.server.v1_12_R1.ItemStack item = packetdataserializer.k();
				        	if(!item.isEmpty()) deeper(item);
				            packetdataserializer.a(item);
				        }
					}
			        info.a(packetdataserializer);
				}
				if (packet instanceof PacketPlayOutSetSlot) {
					PacketPlayOutSetSlot info = (PacketPlayOutSetSlot) packet;
					PacketDataSerializer packetdataserializer = new PacketDataSerializer(Unpooled.buffer(0));
					info.b(packetdataserializer);
					short invid = packetdataserializer.readUnsignedByte();
					short slotid = packetdataserializer.readShort();
					packetdataserializer.writeByte(invid);
					packetdataserializer.writeShort(slotid);
					net.minecraft.server.v1_12_R1.ItemStack item = packetdataserializer.k();
					if(!item.isEmpty()) {
						Container container = ((CraftPlayer)player).getHandle().activeContainer;
						if(container instanceof ContainerAnvil && invid==container.windowId && slotid == 0) {
							clearItemName(item);
						} else deeper(item);
					}
			        packetdataserializer.a(item);
					info.a(packetdataserializer);
				}
				super.write(context, packet, channelPromise);
			}
		};
		ChannelPipeline pipeline = ((CraftPlayer) player).getHandle().playerConnection.networkManager.channel.pipeline();
		pipeline.addBefore("packet_handler", "cid_" + player.getUniqueId(), channelDuplexHandler);
	}

	private void unregisterHandler(Player player) {
		Channel channel = ((CraftPlayer) player).getHandle().playerConnection.networkManager.channel;
		channel.eventLoop().submit(() -> {
			channel.pipeline().remove("cid_" + player.getUniqueId());
			return null;
		});
	}

	@EventHandler
	public void onJoin(PlayerJoinEvent e) {
		registerHandler(e.getPlayer());
	}

	@EventHandler
	public void onQuit(PlayerQuitEvent e) {
		unregisterHandler(e.getPlayer());
	}

}
