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
import org.bukkit.configuration.ConfigurationSection;
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
import org.bukkit.ChatColor;

public class HorseListener implements Listener {

    private final NamespacedKey tamedTimeKey;

    public HorseListener() {
        this.tamedTimeKey = new NamespacedKey(
                Bukkit.getPluginManager().getPlugin("HorseInfoPlugin"),
                "tamedTime"
        );
    }

    private String formatDouble(double value) {
        return String.format("%.2f", value).replace('.', ',');
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
        if (!(event.getRightClicked() instanceof Horse)) return;

        Player player = event.getPlayer();

        if (!player.isSneaking()) return;

        event.setCancelled(true);

        Horse horse = (Horse) event.getRightClicked();

        FileConfiguration config = HorseInfoPlugin.getInstance().getConfig();

        String guiTitle = config.getString("gui.title", "&6Info del Cavallo");
        int guiSize = config.getInt("gui.size", 27);

        Inventory gui = Bukkit.createInventory(null, guiSize,
                ChatColor.translateAlternateColorCodes('&', guiTitle));

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

        double baseSpeed = 0.0;
        try {
            // Tentativo di recuperare l'attributo in runtime
            Attribute movementSpeed = Attribute.valueOf("GENERIC_MOVEMENT_SPEED");
            if (horse.getAttribute(movementSpeed) != null) {
                baseSpeed = horse.getAttribute(movementSpeed).getBaseValue();
            }
        } catch (IllegalArgumentException e) {
            // Se la costante NON esiste in questa versione,
            // ignora o gestisci diversamente
        }

        String horseSpeed = formatDouble(baseSpeed);
        String horseJump = formatDouble(horse.getJumpStrength());

        double heartsValue = horse.getMaxHealth() / 2.0;
        String horseHearts = formatDouble(heartsValue);

        double scoreValue = (baseSpeed * 100.0) + (horse.getJumpStrength() * 10.0) + heartsValue;
        String horseScore = formatDouble(scoreValue);

        ConfigurationSection itemsSection = config.getConfigurationSection("items");
        if (itemsSection == null) return;

        for (String key : itemsSection.getKeys(false)) {
            ConfigurationSection itemSection = itemsSection.getConfigurationSection(key);
            if (itemSection == null) continue;

            int slot = itemSection.getInt("slot");
            String materialStr = itemSection.getString("material", "BOOK");
            Material material = Material.matchMaterial(materialStr);
            if (material == null) material = Material.BOOK;

            String displayName = itemSection.getString("display_name", "");
            List<String> loreList = itemSection.getStringList("lore");
            List<String> finalLore = new ArrayList<>();

            for (String line : loreList) {
                line = line.replace("%HORSE_TYPE%", horseType)
                        .replace("%HORSE_NAME%", horseName)
                        .replace("%TAMED_DATE%", tamedDateStr)
                        .replace("%HORSE_SPEED%", horseSpeed)
                        .replace("%HORSE_JUMP%", horseJump)
                        .replace("%HORSE_HEARTS%", horseHearts)
                        .replace("%HORSE_SCORE%", horseScore);

                finalLore.add(ChatColor.translateAlternateColorCodes('&', line));
            }

            ItemStack itemStack = new ItemStack(material);
            ItemMeta itemMeta = itemStack.getItemMeta();
            if (itemMeta != null) {
                itemMeta.setDisplayName(ChatColor.translateAlternateColorCodes('&', displayName));
                itemMeta.setLore(finalLore);
                itemStack.setItemMeta(itemMeta);
            }

            gui.setItem(slot, itemStack);
        }

        player.openInventory(gui);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        String configuredTitle = ChatColor.translateAlternateColorCodes('&',
                HorseInfoPlugin.getInstance().getConfig().getString("gui.title", "&6Info del Cavallo"));
        if (e.getView().getTitle().equals(configuredTitle)) {
            e.setCancelled(true);
        }
    }
}
