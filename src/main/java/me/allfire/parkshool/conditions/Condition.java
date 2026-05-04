package ml.allfire.parkshool.conditions;

import org.bukkit.entity.Player;

public interface Condition {
    boolean check(Player player);
    String getType();
}
