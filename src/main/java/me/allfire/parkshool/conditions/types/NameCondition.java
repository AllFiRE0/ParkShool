package ml.allfire.parkshool.conditions.types;

import me.allfire.parkshool.conditions.Condition;
import org.bukkit.entity.Player;

public class NameCondition implements Condition {
    private final String value;
    private final boolean exact;

    public NameCondition(String value, boolean exact) {
        this.value = value;
        this.exact = exact;
    }

    @Override
    public boolean check(Player player) {
        if (exact) {
            return player.getName().equals(value);
        } else {
            return player.getName().contains(value);
        }
    }

    @Override
    public String getType() {
        return exact ? "name_equals:" + value : "name_contains:" + value;
    }
}
