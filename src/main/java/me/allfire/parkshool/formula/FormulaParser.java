package ml.allfire.parkshool.formula;

import me.allfire.parkshool.ParkShool;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.entity.Player;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FormulaParser {

    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("%([^%]+)%");
    private static final Pattern MATH_PATTERN = Pattern.compile("[+\\-*/()]");
    private static final ParkShool plugin = ParkShool.getInstance();

    /**
     * Парсит строку как double.
     *
     * @param input    Строка из конфига (число, плейсхолдер или формула)
     * @param player   Игрок, для которого вычисляется
     * @param fallback Значение по умолчанию, если PlaceholderAPI нет
     * @param cap      Максимальное значение (null = без ограничения)
     * @return Итоговое double значение
     */
    public static double parseDouble(String input, Player player, double fallback, Double cap) {
        String processed = processPlaceholders(input, player, fallback);
        double result = evaluateMath(processed);

        if (cap != null && result > cap) {
            return cap;
        }
        return result;
    }

    /**
     * Парсит строку как int с floor.
     */
    public static int parseInt(String input, Player player, int fallback, Integer cap) {
        double value = parseDouble(input, player, (double) fallback, cap != null ? (double) cap : null);
        return (int) Math.floor(value);
    }

    /**
     * Парсит строку как long с floor.
     */
    public static long parseLong(String input, Player player, long fallback, Long cap) {
        double value = parseDouble(input, player, (double) fallback, cap != null ? (double) cap : null);
        return (long) Math.floor(value);
    }

    /**
     * Проверяет, является ли строка формулой.
     */
    public static boolean isFormula(String input) {
        if (input == null) return false;
        return PLACEHOLDER_PATTERN.matcher(input).find() || MATH_PATTERN.matcher(input).find();
    }

    /**
     * Заменяет плейсхолдеры на их значения.
     */
    private static String processPlaceholders(String input, Player player, double fallback) {
        if (input == null) return String.valueOf(fallback);

        Matcher matcher = PLACEHOLDER_PATTERN.matcher(input);
        StringBuffer result = new StringBuffer();

        while (matcher.find()) {
            String placeholder = matcher.group(1);

            if (plugin.isPlaceholderApiAvailable() && player != null) {
                String value = PlaceholderAPI.setPlaceholders(player, "%" + placeholder + "%");
                // Если плейсхолдер не распознан, используем fallback
                if (value.equals("%" + placeholder + "%")) {
                    matcher.appendReplacement(result, String.valueOf(fallback));
                } else {
                    matcher.appendReplacement(result, value);
                }
            } else {
                matcher.appendReplacement(result, String.valueOf(fallback));
            }
        }
        matcher.appendTail(result);
        return result.toString();
    }

    /**
     * Вычисляет математическое выражение в строке.
     */
    private static double evaluateMath(String expression) {
        try {
            // Сначала проверяем, простое ли число
            return new ExpressionParser(expression).parse();
        } catch (Exception e) {
            plugin.getLogger().warning("Ошибка вычисления выражения: " + expression + " — " + e.getMessage());
            return 0;
        }
    }
}
