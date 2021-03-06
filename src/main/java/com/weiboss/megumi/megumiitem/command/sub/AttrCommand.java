package com.weiboss.megumi.megumiitem.command.sub;

import com.weiboss.megumi.megumiitem.Main;
import com.weiboss.megumi.megumiitem.command.WeiCommand;
import com.weiboss.megumi.megumiitem.file.Config;
import com.weiboss.megumi.megumiitem.file.Message;
import com.weiboss.megumi.megumiitem.core.tadokoro.Attribute;
import com.weiboss.megumi.megumiitem.util.MegumiUtil;
import com.weiboss.megumi.megumiitem.util.WeiUtil;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class AttrCommand extends WeiCommand {
    private Main plugin = Main.getInstance();

    @Override
    public void perform(CommandSender CommandSender, String[] Strings) {
        if (Strings.length == 1) {
            List<String> attr = new ArrayList<>();
            for (Attribute attribute : Attribute.values()) {
                attr.add(attribute.getType());
            }
            CommandSender.sendMessage(attr.toString());
            return;
        }
        if (Strings.length == 3) {
            Player p = getPlayer();
            ItemStack item = p.getItemInHand();
            if (item == null || item.getItemMeta() == null) {
                CommandSender.sendMessage(Config.Prefix + "&c你手中没有物品");
                return;
            }
            String attr = Strings[1];
            String s = Strings[2];
            if (!(MegumiUtil.hasAttribute(attr))) {
                CommandSender.sendMessage(Config.Prefix + "&c该属性不存在");
                return;
            }
            if (!(WeiUtil.isFloat(s))) {
                CommandSender.sendMessage(Config.Prefix + Message.InvalidValue.replace("%s%", s));
                return;
            }
            ItemMeta meta = item.getItemMeta();
            List<String> lore = new ArrayList<>();
            if (meta.getLore() != null) lore = meta.getLore();
            Attribute attribute = Attribute.valueOf(attr);
            float value = Float.valueOf(s);
            lore.add(MegumiUtil.getAttrLore(attribute, value, attribute.isRate()));
            meta.setLore(lore);
            item.setItemMeta(meta);
            p.sendMessage(Config.Prefix + "&a添加成功");
        }
    }

    @Override
    public boolean playerOnly() {
        return true;
    }

    @Override
    public String getPermission() {
        return "megumi.admin";
    }
}
