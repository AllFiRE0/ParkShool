package ml.allfire.parkshool.listeners;

import me.allfire.parkshool.ParkShool;
import me.allfire.parkshool.mechanics.DoubleJump;
import me.allfire.parkshool.mechanics.MechanicManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerToggleFlightEvent;
import org.bukkit.GameMode;

public class JumpListener implements Listener {

    private final ParkShool plugin;
    private final MechanicManager mechanicManager;

    public JumpListener(ParkShool plugin) {
        this.plugin = plugin;
        this.mechanicManager = plugin.getMechanicManager();
    }

    @EventHandler
    public void onPlayerToggleFlight(PlayerToggleFlightEvent event) {
        Player player = event.getPlayer();

        // Не работаем в креативе/спектаторе
        if (player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR) {
            return;
        }

        // Отменяем стандартный полёт
        event.setCancelled(true);
        player.setFlying(false);
        player.setAllowFlight(false);

        // Игрок должен быть в воздухе и нажимать пробел
        if (!player.isOnGround()) {
            mechanicManager.handleDoubleJumpAttempt(player);
        }
    }
}
