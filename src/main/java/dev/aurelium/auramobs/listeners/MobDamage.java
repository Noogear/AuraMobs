package dev.aurelium.auramobs.listeners;

import dev.aurelium.auramobs.AuraMobs;
import dev.aurelium.auramobs.util.ColorUtils;
import org.bukkit.Difficulty;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.persistence.PersistentDataType;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class MobDamage implements Listener {

    private final AuraMobs plugin;
    private final NamespacedKey force;
    private final Map<Difficulty, Double> creeperDamage;

    public MobDamage(AuraMobs plugin) {
        this.plugin = plugin;
        this.force = new NamespacedKey(plugin, "arrow-damage");
        this.creeperDamage = new HashMap<>();
        creeperDamage.put(Difficulty.EASY, 24.5);
        creeperDamage.put(Difficulty.NORMAL, 48.5);
        creeperDamage.put(Difficulty.HARD, 72.5);

    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onMobDamage(EntityDamageEvent e) {
        if (!(e.getEntity() instanceof LivingEntity entity)) {
            return;
        }

        if (!plugin.isAuraMob(entity)) {
            return;
        }

        int level = entity.getPersistentDataContainer().getOrDefault(plugin.getMobKey(), PersistentDataType.INTEGER, 1);
        double resHealth = entity.getHealth() - e.getFinalDamage();
        resHealth = Math.max(resHealth, 0.0);
        String formattedHealth = plugin.getFormatter().format(resHealth);
        try {
            entity.setCustomName(ColorUtils.colorMessage(plugin.optionString("custom_name.format")
                    .replace("{mob}", plugin.getMsg("mobs." + entity.getType().name().toLowerCase(Locale.ROOT)))
                    .replace("{lvl}", Integer.toString(level))
                    .replace("{health}", formattedHealth)
                    .replace("{maxhealth}", plugin.getFormatter().format(entity.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue()))
            ));
        } catch (NullPointerException ex) {
            entity.setCustomName(ColorUtils.colorMessage(plugin.optionString("custom_name.format")
                    .replace("{mob}", entity.getType().name())
                    .replace("{lvl}", Integer.toString(level))
                    .replace("{health}", formattedHealth)
                    .replace("{maxhealth}", plugin.getFormatter().format(entity.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue()))
            ));
        }

    }

    @EventHandler(ignoreCancelled = true)
    public void onShootBow(EntityShootBowEvent e) {
        LivingEntity entity = e.getEntity();

        if (entity instanceof Player) {
            return;
        }

        if (!plugin.isAuraMob(entity)) {
            return;
        }

        e.getProjectile().getPersistentDataContainer().set(force, PersistentDataType.FLOAT, e.getForce());

    }


    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOW)
    public void onArrowHit(EntityDamageByEntityEvent e) {
        if (!(e.getDamager() instanceof Projectile p)) {
            return;
        }

        if (!(p.getShooter() instanceof LivingEntity entity)) {
            return;
        }

        if (!plugin.isAuraMob(entity)) {
            return;
        }

        double attack = (entity.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE).getValue() - 2.0);
        if (attack <= 0) return;
        e.setDamage(p.getPersistentDataContainer().getOrDefault(force, PersistentDataType.FLOAT, 1f) * attack + e.getDamage());
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOW)
    public void onExplosionDamage(EntityDamageByEntityEvent e) {
        if (e.getCause() != EntityDamageEvent.DamageCause.ENTITY_EXPLOSION) {
            return;
        }

        if (!(e.getDamager() instanceof Creeper entity)) {
            return;
        }

        if (!plugin.isAuraMob(entity)) {
            return;
        }

        double attack = entity.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE).getValue() - 2.0;
        if (attack <= 0) return;
        Difficulty difficulty = entity.getWorld().getDifficulty();
        double damage = e.getDamage();
        e.setDamage((damage / creeperDamage.get(difficulty)) * attack + damage);
    }

}
