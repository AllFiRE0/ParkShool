package ml.allfire.parkshool.conditions;

import me.allfire.parkshool.ParkShool;
import me.allfire.parkshool.conditions.types.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class ConditionParser {

    private final List<Condition> conditions = new ArrayList<>();

    public ConditionParser(FileConfiguration config) {
        List<java.util.Map<?, ?>> conditionsList = config.getMapList("conditions");
        for (java.util.Map<?, ?> map : conditionsList) {
            String type = (String) map.get("type");
            if (type == null) continue;

            switch (type.toLowerCase()) {
                case "permission":
                    conditions.add(new PermissionCondition((String) map.get("permission")));
                    break;
                case "world":
                    @SuppressWarnings("unchecked")
                    List<String> allowedWorlds = (List<String>) map.get("allowed");
                    @SuppressWarnings("unchecked")
                    List<String> deniedWorlds = (List<String>) map.get("denied");
                    conditions.add(new WorldCondition(allowedWorlds, deniedWorlds));
                    break;
                case "gamemode":
                    @SuppressWarnings("unchecked")
                    List<String> allowedGamemodes = (List<String>) map.get("allowed");
                    conditions.add(new GamemodeCondition(allowedGamemodes));
                    break;
                case "placeholder":
                    conditions.add(new PlaceholderCondition(
                        (String) map.get("placeholder"),
                        (String) map.get("operator"),
                        String.valueOf(map.get("value"))
                    ));
                    break;
                case "name_contains":
                    conditions.add(new NameCondition((String) map.get("value"), false));
                    break;
                case "name_equals":
                    conditions.add(new NameCondition((String) map.get("value"), true));
                    break;
                case "sneaking":
                    conditions.add(new SneakingCondition((Boolean) map.getOrDefault("value", true)));
                    break;
                case "sprinting":
                    conditions.add(new SprintingCondition((Boolean) map.getOrDefault("value", true)));
                    break;
                case "on_ground":
                    conditions.add(new OnGroundCondition((Boolean) map.getOrDefault("value", true)));
                    break;
            }
        }
    }

    public boolean check(Player player) {
        for (Condition condition : conditions) {
            boolean result = condition.check(player);
            ParkShool.getInstance().getDebugger().debug(player, "Conditions",
                condition.getType() + " = " + result);
            if (!result) return false;
        }
        return true;
    }
}
