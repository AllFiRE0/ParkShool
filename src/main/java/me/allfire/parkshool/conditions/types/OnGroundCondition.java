package ml.allfire.parkshool.conditions.types;

import me.allfire.parkshool.conditions.Condition;
import org.bukkit.entity.Player;

public class OnGroundCondition implements Condition {
    private final boolean value;

    public OnGroundCondition(boolean value) {
        this.value = value;
    }

    @Override
    public boolean check(Player player) {
        return player.isOnGround() == value;
    }

    @Override
    public String getType() {
        return "on_ground:" + value;
    }
}
