package ml.allfire.parkshool.conditions.types;

import me.allfire.parkshool.conditions.Condition;
import org.bukkit.entity.Player;

import java.util.List;

public class GamemodeCondition implements Condition {
    private final List<String> allowed;

    public GamemodeCondition(List<String> allowed) {
        this.allowed = allowed;
    }

    @Override
    public boolean check(Player player) {
        if (allowed == null || allowed.isEmpty()) return true;
        return allowed.contains(player.getGameMode().name().toLowerCase());
    }

    @Override
    public String getType() {
        return "gamemode";
    }
}
