package ml.allfire.parkshool.listeners;

import me.allfire.parkshool.ParkShool;
import me.allfire.parkshool.mechanics.MechanicManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerToggleSneakEvent;

public class SneakListener implements Listener {

    private final ParkShool plugin;
    private final MechanicManager mechanicManager;

    public SneakListener(ParkShool plugin) {
        this.plugin = plugin;
        this.mechanicManager = plugin.getMechanicManager();
    }

    @EventHandler
    public void onSneak(PlayerToggleSneakEvent event) {
        Player player = event.getPlayer();

        if (event.isSneaking()) {
            // Игрок начал красться — уведомляем все механики
            mechanicManager.handleSneakStart(player);
        } else {
            // Игрок перестал красться
            mechanicManager.handleSneakEnd(player);
        }
    }
}
