package ml.allfire.parkshool.mechanics;

import me.allfire.parkshool.ParkShool;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;

public class MechanicManager {

    private final ParkShool plugin;
    private final Map<String, Mechanic> mechanics = new HashMap<>();

    public MechanicManager(ParkShool plugin) {
        this.plugin = plugin;
        registerMechanics();
    }

    private void registerMechanics() {
        mechanics.put("doublejump", new DoubleJump(plugin));
        mechanics.put("wallcling", new WallCling(plugin));
        mechanics.put("chargedjump", new ChargedJump(plugin));
        mechanics.put("dashjump", new DashJump(plugin));

        for (Mechanic mechanic : mechanics.values()) {
            mechanic.loadConfig();
        }
    }

    public Mechanic getMechanic(String id) {
        return mechanics.get(id);
    }

    public void handleMove(Player player, Location from, Location to) {
        for (Mechanic mechanic : mechanics.values()) {
            if (mechanic.isEnabled()) {
                mechanic.handleMove(player, from, to);
            }
        }
    }

    public void handleDoubleJumpAttempt(Player player) {
        DoubleJump dj = (DoubleJump) mechanics.get("doublejump");
        if (dj != null && dj.isEnabled()) {
            dj.onAirJump(player);
        }

        DashJump dash = (DashJump) mechanics.get("dashjump");
        if (dash != null && dash.isEnabled()) {
            dash.onAirJump(player);
        }
    }

    public void handleSneakStart(Player player) {
        WallCling wc = (WallCling) mechanics.get("wallcling");
        if (wc != null && wc.isEnabled()) {
            wc.onSneakStart(player);
        }

        ChargedJump cj = (ChargedJump) mechanics.get("chargedjump");
        if (cj != null && cj.isEnabled()) {
            cj.onSneakStart(player);
        }
    }

    public void handleSneakEnd(Player player) {
        WallCling wc = (WallCling) mechanics.get("wallcling");
        if (wc != null && wc.isEnabled()) {
            wc.onSneakEnd(player);
        }

        ChargedJump cj = (ChargedJump) mechanics.get("chargedjump");
        if (cj != null && cj.isEnabled()) {
            cj.onSneakEnd(player);
        }
    }

    public void handlePlayerDamage(Player player) {
        for (Mechanic mechanic : mechanics.values()) {
            if (mechanic.isEnabled()) {
                mechanic.onDamage(player);
            }
        }
    }

    public void handlePlayerLand(Player player) {
        for (Mechanic mechanic : mechanics.values()) {
            if (mechanic.isEnabled()) {
                mechanic.onLand(player);
            }
        }
    }

    public void shutdown() {
        for (Mechanic mechanic : mechanics.values()) {
            mechanic.shutdown();
        }
        mechanics.clear();
    }
}
