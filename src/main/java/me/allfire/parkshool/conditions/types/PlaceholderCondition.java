package ml.allfire.parkshool.conditions.types;

import me.allfire.parkshool.ParkShool;
import me.allfire.parkshool.conditions.Condition;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.entity.Player;

public class PlaceholderCondition implements Condition {
    private final String placeholder;
    private final String operator;
    private final String value;

    public PlaceholderCondition(String placeholder, String operator, String value) {
        this.placeholder = placeholder;
        this.operator = operator;
        this.value = value;
    }

    @Override
    public boolean check(Player player) {
        if (!ParkShool.getInstance().isPlaceholderApiAvailable()) return true;

        String result = PlaceholderAPI.setPlaceholders(player, placeholder);
        double playerValue;
        double targetValue;

        try {
            playerValue = Double.parseDouble(result);
            targetValue = Double.parseDouble(value.replace("%", ""));
        } catch (NumberFormatException e) {
            // Если не числа — сравниваем как строки
            return compareStrings(result, value.replace("%", ""), operator);
        }

        return compare(playerValue, targetValue, operator);
    }

    private boolean compare(double a, double b, String op) {
        return switch (op) {
            case "<" -> a < b;
            case ">" -> a > b;
            case "<=" -> a <= b;
            case ">=" -> a >= b;
            case "==" -> a == b;
            case "!=" -> a != b;
            default -> false;
        };
    }

    private boolean compareStrings(String a, String b, String op) {
        return switch (op) {
            case "==" -> a.equals(b);
            case "!=" -> !a.equals(b);
            default -> false;
        };
    }

    @Override
    public String getType() {
        return "placeholder:" + placeholder + operator + value;
    }
}
