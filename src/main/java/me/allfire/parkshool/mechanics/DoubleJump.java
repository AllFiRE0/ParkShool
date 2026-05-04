package ml.allfire.parkshool.mechanics;

import me.allfire.parkshool.ParkShool;
import me.allfire.parkshool.conditions.ConditionParser;
import me.allfire.parkshool.formula.FormulaParser;
import me.allfire.parkshool.messages.MessageSender;
import me.allfire.parkshool.particles.ParticleManager;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.*;

public class DoubleJump implements Mechanic {

    private final ParkShool plugin;
    private boolean enabled;
    private FileConfiguration config;
    private ConditionParser conditionParser;

    // Параметры
    private String heightFormula;
    private double heightFallback;
    private Double heightCap;
    private String countCapFormula;
    private int countCapFallback;
    private Integer countCapCap;
    private String timeoutTicksFormula;
    private int timeoutTicksFallback;
    private Integer timeoutTicksCap;
    private String maxSafeFallFormula;
    private double maxSafeFallFallback;
    private Double maxSafeFallCap;
    private boolean preserveFallDistance;
    private String fallDamageMultFormula;
    private double fallDamageMultFallback;
    private Double fallDamageMultCap;

    // Состояние игроков
    private final Map<UUID, Integer> jumpCounts = new HashMap<>();
    private final Map<UUID, Long> lastJumpTimes = new HashMap<>();
    private final Map<UUID, Boolean> wasOnGround = new HashMap<>();
    private final Map<UUID, Float> storedFallDistances = new HashMap<>();
    private ParticleManager particleManager;

    public DoubleJump(ParkShool plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getId() {
        return "doublejump";
    }

    @Override
    public void loadConfig() {
        this.config = plugin.getConfigManager().getMechanicConfig("doublejump");
        this.enabled = config.getBoolean("enabled", true);
        this.conditionParser = new ConditionParser(config);

        this.heightFormula = config.getString("height_formula", "1.5");
        this.heightFallback = config.getDouble("height_fallback", 2.0);
        this.heightCap = config.contains("height_cap") ? config.getDouble("height_cap") : null;

        this.countCapFormula = config.getString("count_cap_formula", "5");
        this.countCapFallback = config.getInt("count_cap_fallback", 3);
        this.countCapCap = config.contains("count_cap_cap") ? config.getInt("count_cap_cap") : null;

        this.timeoutTicksFormula = config.getString("timeout_ticks_formula", "20");
        this.timeoutTicksFallback = config.getInt("timeout_ticks_fallback", 20);
        this.timeoutTicksCap = config.contains("timeout_ticks_cap") ? config.getInt("timeout_ticks_cap") : null;

        this.maxSafeFallFormula = config.getString("max_safe_fall_distance_formula", "6.0");
        this.maxSafeFallFallback = config.getDouble("max_safe_fall_distance_fallback", 6.0);
        this.maxSafeFallCap = config.contains("max_safe_fall_distance_cap") ? config.getDouble("max_safe_fall_distance_cap") : null;

        this.preserveFallDistance = config.getBoolean("preserve_fall_distance", true);
        this.fallDamageMultFormula = config.getString("fall_damage_multiplier_formula", "1.0");
        this.fallDamageMultFallback = config.getDouble("fall_damage_multiplier_fallback", 1.0);
        this.fallDamageMultCap = config.contains("fall_damage_multiplier_cap") ? config.getDouble("fall_damage_multiplier_cap") : null;

        // Загрузка частиц
        ConfigurationSection particleSection = config.getConfigurationSection("particles");
        if (particleSection != null) {
            this.particleManager = new ParticleManager(particleSection);
        }
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
    public void handleMove(Player player, Location from, Location to) {
        if (!enabled) return;

        UUID uuid = player.getUniqueId();
        boolean onGround = player.isOnGround();
        boolean wasGround = wasOnGround.getOrDefault(uuid, true);

        if (onGround && !wasGround) {
            onLand(player);
        }

        if (!onGround && wasGround) {
            jumpCounts.put(uuid, 0);
            player.setAllowFlight(true);
        }

        wasOnGround.put(uuid, onGround);
    }

    @Override
    public void onAirJump(Player player) {
        if (!enabled) return;

        UUID uuid = player.getUniqueId();

        if (!checkConditions(player)) {
            return;
        }

        MessageSender msg = plugin.getMessageSender();

        int maxCount = FormulaParser.parseInt(countCapFormula, player, countCapFallback, countCapCap);
        int timeoutTicks = FormulaParser.parseInt(timeoutTicksFormula, player, timeoutTicksFallback, timeoutTicksCap);
        double height = FormulaParser.parseDouble(heightFormula, player, heightFallback, heightCap);
        double maxSafeFall = FormulaParser.parseDouble(maxSafeFallFormula, player, maxSafeFallFallback, maxSafeFallCap);

        int usedJumps = jumpCounts.getOrDefault(uuid, 0);

        if (usedJumps >= maxCount) {
            return;
        }

        Long lastJump = lastJumpTimes.get(uuid);
        long currentTick = player.getWorld().getFullTime();
        if (lastJump != null && (currentTick - lastJump) < timeoutTicks) {
            long remaining = timeoutTicks - (currentTick - lastJump);
            msg.send(player, "doublejump", "on_cooldown", Map.of("time", String.valueOf(remaining / 20)));
            return;
        }

        float fallDistance = player.getFallDistance();
        if (fallDistance > maxSafeFall) {
            if (!preserveFallDistance) {
                msg.send(player, "doublejump", "fall_too_far", Map.of());
                return;
            }
            storedFallDistances.put(uuid, fallDistance);
        }

        Vector velocity = player.getVelocity();
        velocity.setY(height);
        player.setVelocity(velocity);

        // Частицы
        if (particleManager != null) {
            particleManager.spawnAtPlayer(player);
        }

        jumpCounts.put(uuid, usedJumps + 1);
        lastJumpTimes.put(uuid, currentTick);

        plugin.getDebugger().debug(player, "DoubleJump", "Прыжок #" + (usedJumps + 1) + " из " + maxCount + ", высота=" + height);
    }

    @Override
    public void onLand(Player player) {
        UUID uuid = player.getUniqueId();
        jumpCounts.put(uuid, 0);
        lastJumpTimes.remove(uuid);
        storedFallDistances.remove(uuid);
        player.setAllowFlight(false);

        plugin.getDebugger().debug(player, "DoubleJump", "Приземление, счётчик сброшен");
    }

    @Override
    public void onDamage(Player player) {
    }

    @Override
    public void onSneakStart(Player player) {
    }

    @Override
    public void onSneakEnd(Player player) {
    }

    @Override
    public void cleanup(Player player) {
        UUID uuid = player.getUniqueId();
        jumpCounts.remove(uuid);
        lastJumpTimes.remove(uuid);
        wasOnGround.remove(uuid);
        storedFallDistances.remove(uuid);
    }

    @Override
    public void shutdown() {
        jumpCounts.clear();
        lastJumpTimes.clear();
        wasOnGround.clear();
        storedFallDistances.clear();
    }

    public boolean hasStoredFallDistance(Player player) {
        return storedFallDistances.containsKey(player.getUniqueId());
    }

    public double getFallDamageMultiplier(Player player) {
        return FormulaParser.parseDouble(fallDamageMultFormula, player, fallDamageMultFallback, fallDamageMultCap);
    }

    public void clearStoredFallDistance(Player player) {
        storedFallDistances.remove(player.getUniqueId());
    }
}
