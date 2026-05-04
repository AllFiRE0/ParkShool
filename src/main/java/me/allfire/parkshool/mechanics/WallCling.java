package ml.allfire.parkshool.mechanics;

import me.allfire.parkshool.ParkShool;
import me.allfire.parkshool.conditions.ConditionParser;
import me.allfire.parkshool.formula.FormulaParser;
import me.allfire.parkshool.messages.MessageSender;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.*;

public class WallCling implements Mechanic {

    private final ParkShool plugin;
    private boolean enabled;
    private FileConfiguration config;
    private ConditionParser conditionParser;

    // Параметры
    private String durationFormula;
    private int durationFallback;
    private Integer durationCap;
    private String leapDistanceFormula;
    private double leapDistanceFallback;
    private Double leapDistanceCap;
    private String slideSpeedFormula;
    private double slideSpeedFallback;
    private Double slideSpeedCap;
    private String clingAngleFormula;
    private double clingAngleFallback;
    private Double clingAngleCap;
    private String maxChainFormula;
    private int maxChainFallback;
    private Integer maxChainCap;
    private String minDistanceFormula;
    private double minDistanceFallback;
    private Double minDistanceCap;
    private Set<Material> excludedBlocks;

    // Состояние игроков
    private final Map<UUID, ClingState> clingStates = new HashMap<>();
    private final Map<UUID, Integer> chainCounts = new HashMap<>();
    private final Map<UUID, BukkitTask> clingTasks = new HashMap<>();

    private enum ClingState {
        CLINGING,       // висит на стене
        SLIDING,        // сползает
        NONE
    }

    public WallCling(ParkShool plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getId() {
        return "wallcling";
    }

    @Override
    public void loadConfig() {
        this.config = plugin.getConfigManager().getMechanicConfig("wallcling");
        this.enabled = config.getBoolean("enabled", true);
        this.conditionParser = new ConditionParser(config);

        this.durationFormula = config.getString("duration_ticks_formula", "40");
        this.durationFallback = config.getInt("duration_ticks_fallback", 40);
        this.durationCap = config.contains("duration_ticks_cap") ? config.getInt("duration_ticks_cap") : null;

        this.leapDistanceFormula = config.getString("leap_distance_formula", "3.0");
        this.leapDistanceFallback = config.getDouble("leap_distance_fallback", 3.0);
        this.leapDistanceCap = config.contains("leap_distance_cap") ? config.getDouble("leap_distance_cap") : null;

        this.slideSpeedFormula = config.getString("slide_speed_formula", "0.1");
        this.slideSpeedFallback = config.getDouble("slide_speed_fallback", 0.1);
        this.slideSpeedCap = config.contains("slide_speed_cap") ? config.getDouble("slide_speed_cap") : null;

        this.clingAngleFormula = config.getString("cling_angle_formula", "45");
        this.clingAngleFallback = config.getDouble("cling_angle_fallback", 45);
        this.clingAngleCap = config.contains("cling_angle_cap") ? config.getDouble("cling_angle_cap") : null;

        this.maxChainFormula = config.getString("max_chain_formula", "3");
        this.maxChainFallback = config.getInt("max_chain_fallback", 3);
        this.maxChainCap = config.contains("max_chain_cap") ? config.getInt("max_chain_cap") : null;

        this.minDistanceFormula = config.getString("min_distance_formula", "0.5");
        this.minDistanceFallback = config.getDouble("min_distance_fallback", 0.5);
        this.minDistanceCap = config.contains("min_distance_cap") ? config.getDouble("min_distance_cap") : null;

        // Загружаем исключённые блоки
        this.excludedBlocks = new HashSet<>();
        List<String> excludedList = config.getStringList("excluded_blocks");
        for (String matName : excludedList) {
            try {
                Material mat = Material.valueOf(matName.toUpperCase());
                excludedBlocks.add(mat);
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Неизвестный материал в excluded_blocks: " + matName);
            }
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
        // Не используется, основная логика в onSneakStart и в таймере
    }

    @Override
    public void onSneakStart(Player player) {
        if (!enabled) return;
        if (!checkConditions(player)) return;
        if (player.isOnGround()) return;

        UUID uuid = player.getUniqueId();

        // Проверяем, не исчерпана ли цепочка
        int maxChain = FormulaParser.parseInt(maxChainFormula, player, maxChainFallback, maxChainCap);
        int currentChain = chainCounts.getOrDefault(uuid, 0);
        if (currentChain >= maxChain) return;

        // Проверяем, смотрит ли игрок на стену
        Block targetBlock = getTargetWall(player);
        if (targetBlock == null) return;
        if (excludedBlocks.contains(targetBlock.getType())) return;

        // Проверяем расстояние и угол
        double minDist = FormulaParser.parseDouble(minDistanceFormula, player, minDistanceFallback, minDistanceCap);
        double clingAngle = FormulaParser.parseDouble(clingAngleFormula, player, clingAngleFallback, clingAngleCap);

        Location eye = player.getEyeLocation();
        Vector direction = eye.getDirection();
        Vector wallNormal = getWallNormal(targetBlock, eye);

        if (wallNormal == null) return;

        // Угол между взглядом и нормалью стены
        double angle = Math.toDegrees(Math.acos(direction.dot(wallNormal) * -1));
        if (angle > clingAngle) return;

        // Расстояние до стены
        double distance = targetBlock.getLocation().distance(eye);
        if (distance > minDist * 2) return; // примерная проверка

        // Зацеп!
        startCling(player, targetBlock);
    }

    @Override
    public void onSneakEnd(Player player) {
        UUID uuid = player.getUniqueId();
        ClingState state = clingStates.getOrDefault(uuid, ClingState.NONE);
        if (state == ClingState.CLINGING || state == ClingState.SLIDING) {
            stopCling(player);
        }
    }

    @Override
    public void onAirJump(Player player) {
        UUID uuid = player.getUniqueId();
        ClingState state = clingStates.getOrDefault(uuid, ClingState.NONE);

        // Если игрок висит и нажал прыжок — отскок
        if (state == ClingState.CLINGING) {
            performLeap(player);
        }
    }

    @Override
    public void onLand(Player player) {
        UUID uuid = player.getUniqueId();
        chainCounts.put(uuid, 0);
        stopCling(player);

        plugin.getDebugger().debug(player, "WallCling", "Приземление, цепочка сброшена");
    }

    @Override
    public void onDamage(Player player) {
        // Прерываем зацеп при уроне
        stopCling(player);
    }

    @Override
    public void cleanup(Player player) {
        UUID uuid = player.getUniqueId();
        stopCling(player);
        clingStates.remove(uuid);
        chainCounts.remove(uuid);
    }

    @Override
    public void shutdown() {
        clingTasks.values().forEach(BukkitTask::cancel);
        clingTasks.clear();
        clingStates.clear();
        chainCounts.clear();
    }

    /**
     * Находит стену, на которую смотрит игрок.
     */
    private Block getTargetWall(Player player) {
        Location eye = player.getEyeLocation();
        Vector direction = eye.getDirection();

        for (double d = 0.1; d <= 5.0; d += 0.1) {
            Location check = eye.clone().add(direction.clone().multiply(d));
            Block block = check.getBlock();
            if (block.getType().isSolid() && !block.isPassable()) {
                return block;
            }
        }
        return null;
    }

    /**
     * Вычисляет нормаль к стене от блока в сторону игрока.
     */
    private Vector getWallNormal(Block block, Location playerEye) {
        Location blockLoc = block.getLocation().add(0.5, 0.5, 0.5);
        Vector toPlayer = playerEye.toVector().subtract(blockLoc.toVector());

        double absX = Math.abs(toPlayer.getX());
        double absY = Math.abs(toPlayer.getY());
        double absZ = Math.abs(toPlayer.getZ());

        if (absX >= absY && absX >= absZ) {
            return new Vector(Math.signum(toPlayer.getX()), 0, 0);
        } else if (absY >= absX && absY >= absZ) {
            return new Vector(0, Math.signum(toPlayer.getY()), 0);
        } else {
            return new Vector(0, 0, Math.signum(toPlayer.getZ()));
        }
    }

    /**
     * Начинает зацеп за стену.
     */
    private void startCling(Player player, Block wallBlock) {
        UUID uuid = player.getUniqueId();

        // Останавливаем движение
        player.setVelocity(new Vector(0, 0, 0));

        clingStates.put(uuid, ClingState.CLINGING);

        MessageSender msg = plugin.getMessageSender();
        msg.send(player, "wallcling", "cling_start", Map.of());

        int duration = FormulaParser.parseInt(durationFormula, player, durationFallback, durationCap);

        plugin.getDebugger().debug(player, "WallCling", "Зацеп! Длительность=" + duration + " тиков");

        // Запускаем таймер
        BukkitTask task = new BukkitRunnable() {
            int ticksLeft = duration;

            @Override
            public void run() {
                if (!player.isOnline()) {
                    stopCling(player);
                    cancel();
                    return;
                }

                ClingState state = clingStates.getOrDefault(uuid, ClingState.NONE);
                if (state != ClingState.CLINGING) {
                    cancel();
                    return;
                }

                ticksLeft--;

                if (ticksLeft <= 0) {
                    // Переход к сползанию
                    startSlide(player);
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 1L, 1L);

        clingTasks.put(uuid, task);
    }

    /**
     * Начинает сползание по стене.
     */
    private void startSlide(Player player) {
        UUID uuid = player.getUniqueId();
        clingStates.put(uuid, ClingState.SLIDING);

        MessageSender msg = plugin.getMessageSender();
        msg.send(player, "wallcling", "cling_end", Map.of());

        double slideSpeed = FormulaParser.parseDouble(slideSpeedFormula, player, slideSpeedFallback, slideSpeedCap);

        plugin.getDebugger().debug(player, "WallCling", "Сползание, скорость=" + slideSpeed);

        BukkitTask task = new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline() || player.isOnGround()) {
                    stopCling(player);
                    cancel();
                    return;
                }

                ClingState state = clingStates.getOrDefault(uuid, ClingState.NONE);
                if (state != ClingState.SLIDING) {
                    cancel();
                    return;
                }

                if (!player.isSneaking()) {
                    stopCling(player);
                    cancel();
                    return;
                }

                // Медленно опускаем игрока
                Location loc = player.getLocation();
                loc.setY(loc.getY() - slideSpeed);
                player.teleport(loc);
            }
        }.runTaskTimer(plugin, 1L, 1L);

        clingTasks.put(uuid, task);
    }

    /**
     * Отскок от стены.
     */
    private void performLeap(Player player) {
        UUID uuid = player.getUniqueId();

        double leapDistance = FormulaParser.parseDouble(leapDistanceFormula, player, leapDistanceFallback, leapDistanceCap);

        // Направление — куда смотрит игрок
        Vector direction = player.getEyeLocation().getDirection().normalize();

        // Применяем импульс
        Vector leap = direction.multiply(leapDistance);
        leap.setY(leap.getY() + 0.5); // небольшой подброс для удобства
        player.setVelocity(leap);

        // Увеличиваем счётчик цепочки
        int currentChain = chainCounts.getOrDefault(uuid, 0);
        chainCounts.put(uuid, currentChain + 1);

        // Останавливаем зацеп
        stopCling(player);

        MessageSender msg = plugin.getMessageSender();
        msg.send(player, "wallcling", "leap", Map.of());

        plugin.getDebugger().debug(player, "WallCling", "Отскок #" + (currentChain + 1) + ", дистанция=" + leapDistance);
    }

    /**
     * Останавливает зацеп/сползание.
     */
    private void stopCling(Player player) {
        UUID uuid = player.getUniqueId();
        clingStates.put(uuid, ClingState.NONE);

        BukkitTask task = clingTasks.remove(uuid);
        if (task != null) {
            task.cancel();
        }
    }
}
