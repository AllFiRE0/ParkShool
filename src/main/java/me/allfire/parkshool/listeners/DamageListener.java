package ml.allfire.parkshool.listeners;

import me.allfire.parkshool.ParkShool;
import me.allfire.parkshool.mechanics.ChargedJump;
import me.allfire.parkshool.mechanics.DoubleJump;
import me.allfire.parkshool.mechanics.MechanicManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

public class DamageListener implements Listener {

    private final ParkShool plugin;
    private final MechanicManager mechanicManager;

    public DamageListener(ParkShool plugin) {
        this.plugin = plugin;
        this.mechanicManager = plugin.getMechanicManager();
    }

    @EventHandler
    public void onFallDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        if (event.getCause() != DamageCause.FALL) return;

        Player player = (Player) event.getEntity();

        // Проверка ChargedJump — полностью отменяем урон
        ChargedJump chargedJump = (ChargedJump) mechanicManager.getMechanic("chargedjump");
        if (chargedJump != null && chargedJump.isFallDamageNegated(player)) {
            event.setCancelled(true);
            chargedJump.removeFallDamageNegation(player);
            return;
        }

        // Проверка DoubleJump — применяем множитель
        DoubleJump doubleJump = (DoubleJump) mechanicManager.getMechanic("doublejump");
        if (doubleJump != null && doubleJump.hasStoredFallDistance(player)) {
            double multiplier = doubleJump.getFallDamageMultiplier(player);
            if (multiplier != 1.0) {
                event.setDamage(event.getDamage() * multiplier);
            }
            doubleJump.clearStoredFallDistance(player);
        }
    }

    @EventHandler
    public void onPlayerDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        Player player = (Player) event.getEntity();

        // Прерываем зарядку ChargedJump при любом уроне
        mechanicManager.handlePlayerDamage(player);
    }
}
