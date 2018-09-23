package com.weiboss.megumi.megumiflangun.listener;

import com.weiboss.megumi.megumiflangun.Main;
import com.weiboss.megumi.megumiflangun.event.MegumiDamageEvent;
import com.weiboss.megumi.megumiflangun.event.MegumiVampireEvent;
import com.weiboss.megumi.megumiflangun.file.Config;
import com.weiboss.megumi.megumiflangun.file.Message;
import com.weiboss.megumi.megumiflangun.flan.data.AttrData;
import com.weiboss.megumi.megumiflangun.flan.data.FlanGun;
import com.weiboss.megumi.megumiflangun.task.CombatHoloTask;
import com.weiboss.megumi.megumiflangun.util.ReflectionUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;

import java.text.DecimalFormat;

public class DamageListener implements Listener {
    private Main plugin = Main.getInstance();

    @EventHandler
    public void onDamage(EntityDamageByEntityEvent e) {
        Entity damage = e.getDamager();
        Entity entity = e.getEntity();
        if (damage.getType() == null) return;
        String type = damage.getType().toString();
        if (!type.equals("FLANSMOD_BULLET")) return;
        ReflectionUtil util = new ReflectionUtil(plugin.getVersion());
        Entity owner = util.getOwner(damage);
        if (!(owner instanceof LivingEntity && entity instanceof LivingEntity)) return;

        LivingEntity attacker = (LivingEntity) owner;
        LivingEntity victim = (LivingEntity) entity;
        Player a = attacker instanceof Player ? (Player) attacker : null;
        Player b = victim instanceof Player ? (Player) victim : null;

        ItemStack handItem = attacker.getEquipment().getItemInHand();
        if (handItem == null || handItem.getItemMeta() == null) return;
        FlanGun flanGun = new FlanGun(handItem);
        if (a != null) {
            String display = handItem.getItemMeta().getDisplayName() == null ? handItem.getType().toString() : handItem.getItemMeta().getDisplayName();
            if (Config.SoulBound.Interact) {
                if (!(Config.SoulBound.OpLock && a.isOp())) {
                    if (flanGun.getBind()){
                        e.setCancelled(true);
                        a.sendMessage(Config.Prefix + Message.NeedBind.replace("%item%", display));
                        return;
                    }
                    if (flanGun.getBindUser() != null && !flanGun.getBindUser().equalsIgnoreCase(a.getName())) {
                        e.setCancelled(true);
                        ItemStack itemStack = new ItemStack(handItem);
                        a.getInventory().setItemInHand(null);
                        a.getWorld().dropItem(a.getLocation(), itemStack).setPickupDelay(40);
                        return;
                    }
                }
            }
            if (flanGun.getLevelLimit() != null) {
                if (flanGun.getLevelLimit() > a.getLevel()) {
                    e.setCancelled(true);
                    a.sendMessage(Config.Prefix + Message.LevelLimit.replace("%item%", display));
                    return;
                }
            }
        }

        FlanGun attackItem = new FlanGun(
                a,
                attacker.getEquipment().getItemInHand(),
                attacker.getEquipment().getHelmet(),
                attacker.getEquipment().getChestplate(),
                attacker.getEquipment().getLeggings(),
                attacker.getEquipment().getBoots()
        );
        FlanGun victimItem = new FlanGun(
                b,
                victim.getEquipment().getItemInHand(),
                victim.getEquipment().getHelmet(),
                victim.getEquipment().getChestplate(),
                victim.getEquipment().getLeggings(),
                victim.getEquipment().getBoots()
        );
        AttrData attackData = attackItem.getAttrData();
        AttrData victimData = victimItem.getAttrData();

        boolean dodge = false;
        boolean block = false;
        boolean crit = false;

        double dodgeChance = limit1(victimData.getDodgeChance()) - limit1(attackData.getHitChance());
        if (dodgeChance > 0) {
            double random = Math.random();
            if (dodgeChance > random) dodge = true;
        }

        double blockChance = limit1(victimData.getBlockChance()) - limit1(attackData.getWreckChance());
        if (dodgeChance > 0) {
            double random = Math.random();
            if (blockChance > random) block = true;
        }

        if (!block) {
            double critChance = limit1(attackData.getCritChance()) - limit1(victimData.getAntiKnockChance());
            if (critChance > 0) {
                double random = Math.random();
                if (critChance > random) crit = true;
            }
        }

        double damageValue = attackData.getDamage() - limit0(victimData.getIgnoreDamage());
        double defenseValue = victimData.getDefense() - limit0(attackData.getIgnoreDefense());

        double defaultDamage = e.getDamage();
        double attrDamage = limit0(damageValue - defenseValue);

        MegumiDamageEvent damageEvent = new MegumiDamageEvent(attacker, victim,  defaultDamage, attrDamage, dodge, crit, block);
        Bukkit.getPluginManager().callEvent(damageEvent);
        if (damageEvent.isCancel()) return;

        double lastDamage = reservedTwo(damageEvent.getDefaultDamage() + damageEvent.getAttrDamage());

        if (damageEvent.isDodge()) {
            e.setCancelled(true);
            sendHoloMsg(entity.getLocation(), Message.Combat.Dodge);
            return;
        }
        if (damageEvent.isCrit()) {
            lastDamage = reservedTwo(lastDamage * (1 + limit0(attackData.getCritRate())));
            sendHoloMsg(damage.getLocation(), Message.Combat.Crit.replace("%value%", String.valueOf(lastDamage)));
        }
        else if (damageEvent.isBlock()) {
            lastDamage = reservedTwo(lastDamage * (1 - limit0(victimData.getBlockRate())));
            sendHoloMsg(damage.getLocation(), Message.Combat.Block.replace("%value%", String.valueOf(lastDamage)));
        }
        if (!(damageEvent.isCrit() || damageEvent.isBlock())) sendHoloMsg(damage.getLocation(), Message.Combat.Damage.replace("%value%", String.valueOf(lastDamage)));

        double vampire = limitVampire(attackData.getVampire() * lastDamage);
        MegumiVampireEvent vampireEvent = new MegumiVampireEvent(attacker, victim, vampire);
        Bukkit.getPluginManager().callEvent(vampireEvent);
        if (!vampireEvent.isCancel()) {
            double heath = attacker.getHealth() + vampireEvent.getVampire();
            if (heath > attacker.getMaxHealth()) attacker.setHealth(attacker.getMaxHealth());
            else attacker.setHealth(heath);
        }

        e.setDamage(lastDamage);
    }

    private double limit1(double value) {
        if (value > 1d) return 1d;
        else return value;
    }

    private double limit0(double value) {
        if (value < 0) return 0d;
        else return value;
    }

    private double limitVampire(double value) {
        if (value <= Config.MaxVampire) return value;
        else return Config.MaxVampire;
    }

    private void sendHoloMsg(Location location, String message) {
        CombatHoloTask task = new CombatHoloTask(location.add(0d, 0.5d, 0d), message);
        task.runTaskTimer(plugin, 0, 2);
    }

    private static double reservedTwo(double value) {
        DecimalFormat format = new DecimalFormat("#.00");
        return Double.valueOf(format.format(value));
    }
}
