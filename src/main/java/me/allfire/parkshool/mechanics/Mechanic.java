package ml.allfire.parkshool.mechanics;

import org.bukkit.entity.Player;
import org.bukkit.Location;

public interface Mechanic {

    /**
     * Уникальный идентификатор механики.
     */
    String getId();

    /**
     * Загрузить конфигурацию механики.
     */
    void loadConfig();

    /**
     * Включена ли механика.
     */
    boolean isEnabled();

    /**
     * Проверить условия для игрока.
     */
    boolean checkConditions(Player player);

    /**
     * Обработка движения игрока.
     */
    void handleMove(Player player, Location from, Location to);

    /**
     * Игрок начал красться.
     */
    void onSneakStart(Player player);

    /**
     * Игрок перестал красться.
     */
    void onSneakEnd(Player player);

    /**
     * Игрок нажал прыжок в воздухе.
     */
    void onAirJump(Player player);

    /**
     * Игрок получил урон.
     */
    void onDamage(Player player);

    /**
     * Игрок приземлился.
     */
    void onLand(Player player);

    /**
     * Очистка данных игрока.
     */
    void cleanup(Player player);

    /**
     * Выгрузить механику.
     */
    void shutdown();
}
