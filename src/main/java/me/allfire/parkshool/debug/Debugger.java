package ml.allfire.parkshool.debug;

import me.allfire.parkshool.ParkShool;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class Debugger {

    private final ParkShool plugin;
    private final LegacyComponentSerializer serializer = LegacyComponentSerializer.legacyAmpersand();
    private final Set<UUID> debugPlayers = new HashSet<>();
    private String prefix;

    public Debugger(ParkShool plugin) {
        this.plugin = plugin;
        loadConfig();
    }

    public void loadConfig() {
        FileConfiguration config = plugin.getConfigManager().getDebugConfig();
        this.prefix = config.getString("prefix", "&7[&eParkShool Debug&7] ");
    }

    public void toggleDebug(Player player) {
        UUID uuid = player.getUniqueId();
        if (debugPlayers.contains(uuid)) {
            debugPlayers.remove(uuid);
            player.sendMessage(serializer.deserialize(prefix + "&cДебаг выключен."));
        } else {
            debugPlayers.add(uuid);
            player.sendMessage(serializer.deserialize(prefix + "&aДебаг включён."));
        }
    }

    public boolean isDebugEnabled(Player player) {
        return player.hasPermission("parkshool.debug") && debugPlayers.contains(player.getUniqueId());
    }

    public void debug(Player player, String mechanic, String message) {
        if (!isDebugEnabled(player)) return;

        String fullMessage = prefix + "&7[" + mechanic + "] &f" + message;
        player.sendMessage(serializer.deserialize(fullMessage));
    }
}
