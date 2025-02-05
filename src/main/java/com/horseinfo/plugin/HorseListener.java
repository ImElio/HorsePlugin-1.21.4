package com.horseinfo.plugin;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Horse;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityTameEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

public class HorseListener implements Listener {

    private final NamespacedKey tamedTimeKey;

    public HorseListener() {
        this.tamedTimeKey = new NamespacedKey(Bukkit.getPluginManager().getPlugin("HorseInfoPlugin"), "tamedTime");
    }

    @EventHandler
    public void onEntityTame(EntityTameEvent event) {
        if (event.getEntity() instanceof Horse) {
            Horse horse = (Horse) event.getEntity();
            PersistentDataContainer pdc = horse.getPersistentDataContainer();
            pdc.set(tamedTimeKey, PersistentDataType.LONG, System.currentTimeMillis());
        }
    }

    @EventHandler
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        if (!(event.getRightClicked() instanceof Horse)) {
            return;
        }
        Player player = event.getPlayer();
        if (!player.isSneaking()) {
            return;
        }
        event.setCancelled(true);
        Horse horse = (Horse) event.getRightClicked();
        FileConfiguration config = HorseInfoPlugin.getInstance().getConfig();
        String guiTitle = config.getString("gui.title", "Info del Cavallo");
        int guiSize = config.getInt("gui.size", 9);
        int centerSlot = config.getInt("gui.center_slot", 4);
        Inventory gui = Bukkit.createInventory(null, guiSize, guiTitle);
        String materialStr = config.getString("item.material", "PAPER");
        Material material = Material.matchMaterial(materialStr);
        if (material == null) {
            material = Material.PAPER;
        }
        String itemDisplayName = config.getString("item.display_name", "Informazioni del Cavallo");
        List<String> loreConfig = config.getStringList("item.lore");
        String horseType = horse.getType().name();
        String horseName = (horse.getCustomName() != null) ? horse.getCustomName() : "N/D";
        String tamedDateStr = "N/D";
        PersistentDataContainer pdc = horse.getPersistentDataContainer();
        Long tamedTime = pdc.get(tamedTimeKey, PersistentDataType.LONG);
        if (tamedTime != null) {
            DateFormat df = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
            Date date = new Date(tamedTime);
            tamedDateStr = df.format(date);
        }
        String horseSpeed = "N/D";
        try {
            Attribute movementSpeed = Attribute.valueOf("GENERIC_MOVEMENT_SPEED");
            if (horse.getAttribute(movementSpeed) != null) {
                double speed = horse.getAttribute(movementSpeed).getBaseValue();
                horseSpeed = String.format("%.2f", speed);
            }
        } catch (IllegalArgumentException e) {
            horseSpeed = "N/A";
        }
        String horseJump = String.format("%.2f", horse.getJumpStrength());
        String horseHearts = "N/D";
        if (horse.getMaxHealth() > 0) {
            double hearts = horse.getMaxHealth() / 2.0;
            horseHearts = String.format("%.0f", hearts);
        }
        List<String> finalLore = new ArrayList<>();
        for (String line : loreConfig) {
            line = line.replace("%HORSE_TYPE%", horseType)
                    .replace("%HORSE_NAME%", horseName)
                    .replace("%TAMED_DATE%", tamedDateStr)
                    .replace("%HORSE_SPEED%", horseSpeed)
                    .replace("%HORSE_JUMP%", horseJump)
                    .replace("%HORSE_HEARTS%", horseHearts);
            finalLore.add(line);
        }
        ItemStack infoItem = new ItemStack(material);
        ItemMeta meta = infoItem.getItemMeta();
        meta.setDisplayName(itemDisplayName);
        meta.setLore(finalLore);
        infoItem.setItemMeta(meta);
        gui.setItem(centerSlot, infoItem);
        player.openInventory(gui);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        if (e.getView().getTitle().equals(HorseInfoPlugin.getInstance().getConfig().getString("gui.title", "Info del Cavallo"))) {
            e.setCancelled(true);
        }
    }
}
