package com.weiboss.megumi.megumiflangun.flan;

import com.weiboss.megumi.megumiflangun.Main;
import com.weiboss.megumi.megumiflangun.tadokoro.Attribute;
import com.weiboss.megumi.megumiflangun.constructor.Template;
import com.weiboss.megumi.megumiflangun.tadokoro.Wear;
import com.weiboss.megumi.megumiflangun.util.WeiUtil;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class MyPluginManager {
    private Main plugin;
    private HashMap<String, Wear> wearMap;
    private HashMap<String, Template> templateMap;
    private HashMap<Player, List<ItemStack>> soulboundItem;

    public MyPluginManager(Main plugin) {
        this.plugin = plugin;
        this.wearMap = new HashMap<>();
        this.templateMap = new HashMap<>();
        this.soulboundItem = new HashMap<>();
    }

    public void init() {
        YamlConfiguration config = plugin.getFileManager().getConfig();

        for (String s : plugin.getFileManager().getWears()) {
            String name = WeiUtil.onReplace(config.getString("Wear." + s + ".Name"));
            Double min = config.getDouble("Wear." + s + ".Min");
            Double max = config.getDouble("Wear." + s + ".Max");
            wearMap.put(s, new Wear(name, min, max));
        }
    }

    public HashMap<String, Template> getTemplateMap() {
        return templateMap;
    }

    public HashMap<String, Wear> getWearMap() {
        return wearMap;
    }

    public HashMap<Player, List<ItemStack>> getSoulboundItem() {
        return soulboundItem;
    }
}
