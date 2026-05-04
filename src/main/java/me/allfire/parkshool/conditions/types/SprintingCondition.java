package ml.allfire.parkshool.conditions.types;

import me.allfire.parkshool.conditions.Condition;
import org.bukkit.entity.Player;

public class SprintingCondition implements Condition {
    private final boolean value;

    public SprintingCondition(boolean value) {
        this.value = value;
    }

    @Override
    public boolean check(Player player) {
        return player.isSprinting() == value;
    }

    @Override
    public String getType() {
        return "sprinting:" + value;
    }
}
