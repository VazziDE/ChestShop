package de.vazzi;

import cn.nukkit.plugin.PluginBase;
import de.vazzi.listener.SignListener;

import java.util.HashMap;

public class ChestShop extends PluginBase {

    public static ChestShop instance;

    public static ChestShop getInstance() {
        return instance;
    }

    @Override
    public void onLoad() {
        instance = this;
    }

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(new SignListener(), this);
    }

    public HashMap<String, Long> cooldown = new HashMap<>();

    public void setPlayerCooldown(String playername) {
        if (!hasCooldown(playername)) {
            cooldown.put(playername, System.currentTimeMillis());
            return;
        }
        cooldown.put(playername, System.currentTimeMillis() + 100);
    }

    public void removeCooldown(String playername) {
        if (hasCooldown(playername)) {
            cooldown.remove(playername);
        }
    }

    public boolean inCooldown(String playername) {
        if (hasCooldown(playername)) {
            return cooldown.get(playername) > System.currentTimeMillis();
        }
        return false;
    }

    public boolean hasCooldown(String playername) {
        return cooldown.containsKey(playername);
    }


}
