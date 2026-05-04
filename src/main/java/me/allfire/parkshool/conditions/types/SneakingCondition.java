package ml.allfire.parkshool.conditions.types;

import me.allfire.parkshool.conditions.Condition;
import org.bukkit.entity.Player;

public class SneakingCondition implements Condition {
    private final boolean value;

    public SneakingCondition(boolean value) {
        this.value = value;
    }

    @Override
    public boolean check(Player player) {
        return player.isSneaking() == value;
    }

    @Override
    public String getType() {
        return "sneaking:" + value;
    }
}
