package ml.allfire.parkshool.conditions.types;

import me.allfire.parkshool.conditions.Condition;
import org.bukkit.entity.Player;

import java.util.List;

public class WorldCondition implements Condition {
    private final List<String> allowed;
    private final List<String> denied;

    public WorldCondition(List<String> allowed, List<String> denied) {
        this.allowed = allowed;
        this.denied = denied;
    }

    @Override
    public boolean check(Player player) {
        String worldName = player.getWorld().getName();

        if (denied != null && denied.contains(worldName)) return false;
        if (allowed != null && !allowed.isEmpty() && !allowed.contains(worldName)) return false;

        return true;
    }

    @Override
    public String getType() {
        return "world";
    }
}
