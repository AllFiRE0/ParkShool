package ml.allfire.parkshool.mechanics;

import me.allfire.parkshool.ParkShool;
import me.allfire.parkshool.conditions.ConditionParser;
import me.allfire.parkshool.formula.FormulaParser;
import me.allfire.parkshool.messages.MessageSender;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;
import me.allfire.parkshool.particles.ParticleManager;

import java.util.*;

public class ChargedJump implements Mechanic {

    private final ParkShool plugin;
    private boolean enabled;
    private FileConfiguration config;
    private ConditionParser conditionParser;

    // Параметры
    private String chargeKey;
    private boolean allowMovement;
    private String fallMode;
    private boolean elytraAllowed;
    private Map<Integer, LevelConfig> levels = new HashMap<>();

    // Состояние игроков
    private final Map<UUID, ChargeState> chargeStates = new HashMap<>();
    private final Map<UUID, BukkitTask> chargeTasks = new HashMap<>();
    private final Set<UUID> fallDamageNegated = new HashSet<>();
    private final Map<UUID, Location> chargeStartLocations = new HashMap<>();
    private final Map<UUID, Long> elytraDisabled = new HashMap<>();
    private ParticleManager particleManager;

    private static class LevelConfig {
        String chargeTicksFormula;
        int chargeTicksFallback;
        Integer chargeTicksCap;
        String heightFormula;
        double heightFallback;
        Double heightCap;
        String speedFormula;
        double speedFallback;
        Double speedCap;
    }

    private enum ChargeState {
        CHARGING,
        CHARGED,
        NONE
    }

    public ChargedJump(ParkShool plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getId() {
        return "chargedjump";
    }

    @Override
    public void loadConfig() {
        this.config = plugin.getConfigManager().getMechanicConfig("chargedjump");
        this.enabled = config.getBoolean("enabled", true);
        this.conditionParser = new ConditionParser(config);

        this.chargeKey = config.getString("charge_key", "shift");
        this.allowMovement = config.getBoolean("allow_movement", true);
        this.fallMode = config.getString("fall_mode", "free");
        this.elytraAllowed = config.getBoolean("elytra_allowed", false);

        // Загружаем уровни
        ConfigurationSection levelsSection = config.getConfigurationSection("levels");
        if (levelsSection != null) {
            for (String key : levelsSection.getKeys(false)) {
                try {
                    int level = Integer.parseInt(key);
                    ConfigurationSection levelSection = levelsSection.getConfigurationSection(key);

                    LevelConfig lc = new LevelConfig();
                    lc.chargeTicksFormula = levelSection.getString("charge_ticks_formula", "40");
                    lc.chargeTicksFallback = levelSection.getInt("charge_ticks_fallback", 40);
                    lc.chargeTicksCap = levelSection.contains("charge_ticks_cap") ? levelSection.getInt("charge_ticks_cap") : null;
                    lc.heightFormula = levelSection.getString("height_formula", "5.0");
                    lc.heightFallback = levelSection.getDouble("height_fallback", 5.0);
                    lc.heightCap = levelSection.contains("height_cap") ? levelSection.getDouble("height_cap") : null;
                    lc.speedFormula = levelSection.getString("speed_formula", "0.8");
                    lc.speedFallback = levelSection.getDouble("speed_fallback", 0.8);
                    lc.speedCap = levelSection.contains("speed_cap") ? levelSection.getDouble("speed_cap") : null;

                    levels.put(level, lc);
                } catch (NumberFormatException e) {
                    plugin.getLogger().warning("Неверный номер уровня в chargedjump.yml: " + key);
                }
            }
        }

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
    public void onSneakStart(Player player) {
        if (!enabled) return;
        if (!chargeKey.equals("shift") && !chargeKey.equals("sneak")) return;
        if (!checkConditions(player)) return;
        if (!player.isOnGround()) return;

        UUID uuid = player.getUniqueId();

        // Определяем максимальный доступный уровень
        int maxLevel = getMaxLevel(player);
        if (maxLevel == 0) return;

        LevelConfig lc = levels.get(maxLevel);
        if (lc == null) return;

        int chargeTicks = FormulaParser.parseInt(lc.chargeTicksFormula, player, lc.chargeTicksFallback, lc.chargeTicksCap);

        chargeStates.put(uuid, ChargeState.CHARGING);
        chargeStartLocations.put(uuid, player.getLocation().clone());

        MessageSender msg = plugin.getMessageSender();

        plugin.getDebugger().debug(player, "ChargedJump", "Начало зарядки, уровень=" + maxLevel + ", тиков=" + chargeTicks);

        BukkitTask task = new BukkitRunnable() {
            int ticksCharged = 0;

            @Override
            public void run() {
                if (!player.isOnline()) {
                    cancelCharge(player);
                    cancel();
                    return;
                }

                ChargeState state = chargeStates.getOrDefault(uuid, ChargeState.NONE);
                if (state != ChargeState.CHARGING) {
                    cancel();
                    return;
                }

                // Проверяем, не прервана ли зарядка
                if (!player.isSneaking()) {
                    msg.send(player, "chargedjump", "interrupted", Map.of());
                    cancelCharge(player);
                    cancel();
                    return;
                }

                // Проверяем движение если запрещено
                if (!allowMovement) {
                    Location current = player.getLocation();
                    Location start = chargeStartLocations.get(uuid);
                    if (start != null && (current.getX() != start.getX() || current.getZ() != start.getZ())) {
                        msg.send(player, "chargedjump", "interrupted", Map.of());
                        cancelCharge(player);
                        cancel();
                        return;
                    }
                }

                ticksCharged++;

                // Сообщение о прогрессе
                double progress = Math.min(1.0, (double) ticksCharged / chargeTicks);
                int percent = (int) (progress * 100);
                msg.send(player, "chargedjump", "charging", Map.of("progress", String.valueOf(percent)));

                // Зарядка завершена
                if (ticksCharged >= chargeTicks) {
                    performChargedJump(player, maxLevel, lc);
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 1L, 1L);

        chargeTasks.put(uuid, task);
    }

    @Override
    public void onSneakEnd(Player player) {
        UUID uuid = player.getUniqueId();
        ChargeState state = chargeStates.getOrDefault(uuid, ChargeState.NONE);
        if (state == ChargeState.CHARGING) {
            cancelCharge(player);
            MessageSender msg = plugin.getMessageSender();
            msg.send(player, "chargedjump", "interrupted", Map.of());
        }
    }

    private void performChargedJump(Player player, int level, LevelConfig lc) {
        UUID uuid = player.getUniqueId();

        double height = FormulaParser.parseDouble(lc.heightFormula, player, lc.heightFallback, lc.heightCap);
        double speed = FormulaParser.parseDouble(lc.speedFormula, player, lc.speedFallback, lc.speedCap);

        Vector velocity = player.getVelocity();

        if ("vertical".equals(fallMode)) {
            velocity.setX(0);
            velocity.setZ(0);
        }

        velocity.setY(height * speed);
        player.setVelocity(velocity);
        // Частицы
        if (particleManager != null) {
            particleManager.spawnAtPlayer(player);
        }

        // Защита от урона падения
        fallDamageNegated.add(uuid);

        // Проверка элитр
        if (!elytraAllowed || !player.hasPermission("parkshool.chargedjump.elytra")) {
            disableElytra(player);
        }

        chargeStates.put(uuid, ChargeState.CHARGED);

        MessageSender msg = plugin.getMessageSender();
        msg.send(player, "chargedjump", "charged", Map.of(
            "level", String.valueOf(level),
            "height", String.format("%.1f", height * speed)
        ));

        plugin.getDebugger().debug(player, "ChargedJump",
            "Прыжок! Уровень=" + level + ", высота=" + (height * speed) + ", скорость=" + speed);
    }

    private void cancelCharge(Player player) {
        UUID uuid = player.getUniqueId();
        chargeStates.put(uuid, ChargeState.NONE);
        chargeStartLocations.remove(uuid);

        BukkitTask task = chargeTasks.remove(uuid);
        if (task != null) {
            task.cancel();
        }
    }

    private int getMaxLevel(Player player) {
        int max = 0;
        for (int level : levels.keySet()) {
            if (player.hasPermission("parkshool.chargedjump.level." + level) && level > max) {
                max = level;
            }
        }
        return max;
    }

    private void disableElytra(Player player) {
        ItemStack chestplate = player.getInventory().getChestplate();
        if (chestplate != null && chestplate.getType() == Material.ELYTRA) {
            elytraDisabled.put(player.getUniqueId(), System.currentTimeMillis());
            // TODO: можно сохранить элитры и выдать обратно после приземления
        }
    }

    private void enableElytra(Player player) {
        elytraDisabled.remove(player.getUniqueId());
        // TODO: вернуть элитры если были убраны
    }

    /**
     * Проверить, отменён ли урон падения.
     */
    public boolean isFallDamageNegated(Player player) {
        return fallDamageNegated.contains(player.getUniqueId());
    }

    /**
     * Убрать отмену урона падения.
     */
    public void removeFallDamageNegation(Player player) {
        fallDamageNegated.remove(player.getUniqueId());
    }

    @Override
    public void handleMove(Player player, Location from, Location to) {
        if (!enabled) return;
        UUID uuid = player.getUniqueId();

        // Проверяем приземление для снятия защиты
        if (player.isOnGround() && fallDamageNegated.contains(uuid)) {
            fallDamageNegated.remove(uuid);
            chargeStates.put(uuid, ChargeState.NONE);
            enableElytra(player);

            plugin.getDebugger().debug(player, "ChargedJump", "Приземление, защита снята");
        }
    }

    @Override
    public void onAirJump(Player player) {
        // Не используется
    }

    @Override
    public void onLand(Player player) {
        UUID uuid = player.getUniqueId();
        chargeStates.put(uuid, ChargeState.NONE);
        fallDamageNegated.remove(uuid);
        chargeStartLocations.remove(uuid);
        enableElytra(player);
    }

    @Override
    public void onDamage(Player player) {
        UUID uuid = player.getUniqueId();
        ChargeState state = chargeStates.getOrDefault(uuid, ChargeState.NONE);
        if (state == ChargeState.CHARGING) {
            cancelCharge(player);
            MessageSender msg = plugin.getMessageSender();
            msg.send(player, "chargedjump", "interrupted", Map.of());
        }
    }

    @Override
    public void cleanup(Player player) {
        UUID uuid = player.getUniqueId();
        cancelCharge(player);
        chargeStates.remove(uuid);
        fallDamageNegated.remove(uuid);
        chargeStartLocations.remove(uuid);
        enableElytra(player);
    }

    @Override
    public void shutdown() {
        chargeTasks.values().forEach(BukkitTask::cancel);
        chargeTasks.clear();
        chargeStates.clear();
        fallDamageNegated.clear();
        chargeStartLocations.clear();
        elytraDisabled.clear();
    }
}
