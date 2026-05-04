package ml.allfire.parkshool.messages;

import me.allfire.parkshool.ParkShool;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.title.Title;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.time.Duration;
import java.util.Map;

public class MessageSender {

    private final ParkShool plugin;
    private final LegacyComponentSerializer serializer = LegacyComponentSerializer.legacyAmpersand();

    public MessageSender(ParkShool plugin) {
        this.plugin = plugin;
    }

    /**
     * Отправить сообщение игроку.
     *
     * @param player    Игрок
     * @param mechanic  ID механики
     * @param messageKey Ключ сообщения
     * @param placeholders Плейсхолдеры для замены в тексте
     */
    public void send(Player player, String mechanic, String messageKey, Map<String, String> placeholders) {
        // Ищем сообщение в конфиге механики
        FileConfiguration mechConfig = plugin.getConfigManager().getMechanicConfig(mechanic);
        String channel = "actionbar";
        boolean enabled = false;
        String text = "";

        if (mechConfig != null) {
            String path = "messages." + messageKey;
            if (mechConfig.contains(path)) {
                enabled = mechConfig.getBoolean(path + ".enabled", false);
                channel = mechConfig.getString(path + ".channel", "actionbar");
                text = mechConfig.getString(path + ".text", "");
            }
        }

        // Если в механике не настроено — ищем в глобальном messages.yml
        if (!enabled || text.isEmpty()) {
            FileConfiguration msgConfig = plugin.getConfigManager().getMessagesConfig();
            String globalPath = "mechanics." + mechanic + "." + messageKey;
            if (msgConfig.contains(globalPath)) {
                enabled = msgConfig.getBoolean(globalPath + ".enabled", false);
                channel = msgConfig.getString(globalPath + ".channel", "actionbar");
                text = msgConfig.getString(globalPath + ".text", "");
            }
        }

        if (!enabled || text.isEmpty()) return;

        // Заменяем плейсхолдеры
        if (placeholders != null) {
            for (Map.Entry<String, String> entry : placeholders.entrySet()) {
                text = text.replace("%" + entry.getKey() + "%", entry.getValue());
            }
        }

        // Добавляем префикс
        FileConfiguration msgConfig = plugin.getConfigManager().getMessagesConfig();
        String prefix = msgConfig.getString("global.prefix", "");
        text = prefix + text;

        Component component = serializer.deserialize(text);

        // Отправляем в нужный канал
        switch (channel.toLowerCase()) {
            case "chat":
                player.sendMessage(component);
                break;
            case "actionbar":
                player.sendActionBar(component);
                break;
            case "title":
                Title title = Title.title(component, Component.empty(), Title.Times.times(
                    Duration.ofMillis(200),
                    Duration.ofMillis(1000),
                    Duration.ofMillis(200)
                ));
                player.showTitle(title);
                break;
        }
    }

    /**
     * Отправить простое сообщение в чат.
     */
    public void sendChat(Player player, String text) {
        player.sendMessage(serializer.deserialize(text));
    }
}
