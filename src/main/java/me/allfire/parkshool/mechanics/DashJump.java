package ml.allfire.parkshool.mechanics;

import me.allfire.parkshool.ParkShool;
import me.allfire.parkshool.conditions.ConditionParser;
import me.allfire.parkshool.formula.FormulaParser;
import me.allfire.parkshool.messages.MessageSender;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.*;

public class DashJump implements Mechanic {

    private final ParkShool plugin;
    private boolean enabled;
    private FileConfiguration config;
    private ConditionParser conditionParser;

    // Параметры
    private String activation;
    private boolean allowInAir;
    private boolean allowOnGround;
    private String distanceFormula;
    private double distanceFallback;
    private Double distanceCap;
    private String speedFormula;
    private double speedFallback;
    private Double speedCap;
    private String cooldownFormula;
    private int cooldownFallback;
    private Integer cooldownCap;

    // Состояние
    private final Map<UUID, Long> lastDashTimes = new HashMap<>();
    private final Map<UUID, Long> lastForwardPress = new HashMap<>(); // для double_forward
    private static final long DOUBLE_FORWARD_WINDOW = 300; // мс
    private ParticleManager particleManager;

    public DashJump(ParkShool plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getId() {
        return "dashjump";
    }

    @Override
    public void loadConfig() {
        this.config = plugin.getConfigManager().getMechanicConfig("dashjump");
        this.enabled = config.getBoolean("enabled", true);
        this.conditionParser = new ConditionParser(config);

        this.activation = config.getString("activation", "shift_jump");
        this.allowInAir = config.getBoolean("allow_in_air", true);
        this.allowOnGround = config.getBoolean("allow_on_ground", true);

        this.distanceFormula = config.getString("distance_formula", "4.0");
        this.distanceFallback = config.getDouble("distance_fallback", 4.0);
        this.distanceCap = config.contains("distance_cap") ? config.getDouble("distance_cap") : null;

        this.speedFormula = config.getString("speed_formula", "0.8");
        this.speedFallback = config.getDouble("speed_fallback", 0.8);
        this.speedCap = config.contains("speed_cap") ? config.getDouble("speed_cap") : null;

        this.cooldownFormula = config.getString("cooldown_ticks_formula", "40");
        this.cooldownFallback = config.getInt("cooldown_ticks_fallback", 40);
        this.cooldownCap = config.contains("cooldown_ticks_cap") ? config.getInt("cooldown_ticks_cap") : null;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public boolean checkConditions(Player player) {
        return conditionParser.check(player);
    }

    @Override
    public void onAirJump(Player player) {
        if (!enabled) return;
        if (!"shift_jump".equals(activation)) return;
        if (!checkConditions(player)) return;
        if (!allowInAir && !player.isOnGround()) return;
        if (!allowOnGround && player.isOnGround()) return;

        performDash(player);
    }

    /**
     * Выполнить рывок.
     */
    public void performDash(Player player) {
        UUID uuid = player.getUniqueId();

        // Проверка кулдауна
        int cooldown = FormulaParser.parseInt(cooldownFormula, player, cooldownFallback, cooldownCap);
        Long lastDash = lastDashTimes.get(uuid);
        long now = System.currentTimeMillis();
        if (lastDash != null && (now - lastDash) < cooldown * 50) {
            long remainingMs = (cooldown * 50) - (now - lastDash);
            MessageSender msg = plugin.getMessageSender();
            msg.send(player, "dashjump", "on_cooldown", Map.of("time", String.format("%.1f", remainingMs / 1000.0)));
            return;
        }

        double distance = FormulaParser.parseDouble(distanceFormula, player, distanceFallback, distanceCap);
        double speed = FormulaParser.parseDouble(speedFormula, player, speedFallback, speedCap);

        Vector direction = player.getEyeLocation().getDirection().normalize();
        Vector dash = direction.multiply(distance * speed);

        // Сохраняем часть вертикальной скорости чтобы не обнулять падение
        dash.setY(player.getVelocity().getY() + dash.getY());

        player.setVelocity(dash);
        lastDashTimes.put(uuid, now);

        MessageSender msg = plugin.getMessageSender();
        msg.send(player, "dashjump", "activated", Map.of());

        plugin.getDebugger().debug(player, "DashJump",
            "Рывок! Дистанция=" + distance + ", скорость=" + speed);
    }

    /**
     * Обработка двойного нажатия вперёд.
     */
    public void handleForwardPress(Player player) {
        if (!enabled) return;
        if (!"double_forward".equals(activation)) return;
        if (!checkConditions(player)) return;
        if (!allowInAir && !player.isOnGround()) return;
        if (!allowOnGround && player.isOnGround()) return;

        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();
        Long lastPress = lastForwardPress.get(uuid);

        if (lastPress != null && (now - lastPress) < DOUBLE_FORWARD_WINDOW) {
            performDash(player);
            lastForwardPress.remove(uuid);
        } else {
            lastForwardPress.put(uuid, now);
        }
    }

    @Override
    public void handleMove(Player player, Location from, Location to) {
        // Не используется
    }

    @Override
    public void onSneakStart(Player player) {
        // Не используется
    }

    @Override
    public void onSneakEnd(Player player) {
        // Не используется
    }

    @Override
    public void onLand(Player player) {
        // Можно добавить сброс или бонус
    }

    @Override
    public void onDamage(Player player) {
        // Не прерываем
    }

    @Override
    public void cleanup(Player player) {
        UUID uuid = player.getUniqueId();
        lastDashTimes.remove(uuid);
        lastForwardPress.remove(uuid);
    }

    @Override
    public void shutdown() {
        lastDashTimes.clear();
        lastForwardPress.clear();
    }
}
