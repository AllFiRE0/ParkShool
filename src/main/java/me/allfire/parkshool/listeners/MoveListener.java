package ml.allfire.parkshool.listeners;

import me.allfire.parkshool.ParkShool;
import me.allfire.parkshool.mechanics.MechanicManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

public class MoveListener implements Listener {

    private final ParkShool plugin;
    private final MechanicManager mechanicManager;

    public MoveListener(ParkShool plugin) {
        this.plugin = plugin;
        this.mechanicManager = plugin.getMechanicManager();
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        // Игнорируем повороты головы
        if (!event.hasChangedPosition()) return;

        Player player = event.getPlayer();
        mechanicManager.handleMove(player, event.getFrom(), event.getTo());
    }
}
