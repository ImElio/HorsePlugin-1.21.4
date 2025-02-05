package com.horseinfo.plugin;

import org.bukkit.plugin.java.JavaPlugin;

public class HorseInfoPlugin extends JavaPlugin {

    private static HorseInfoPlugin instance;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        getServer().getPluginManager().registerEvents(new HorseListener(), this);
    }

    @Override
    public void onDisable() {
    }

    public static HorseInfoPlugin getInstance() {
        return instance;
    }
}
