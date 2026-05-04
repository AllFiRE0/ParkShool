package ml.allfire.parkshool.config;

import me.allfire.parkshool.ParkShool;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class ConfigManager {

    private final ParkShool plugin;
    private final Map<String, FileConfiguration> configs = new HashMap<>();

    public ConfigManager(ParkShool plugin) {
        this.plugin = plugin;
    }

    public void loadAll() {
        // Создаём папку mechanics если нет
        File mechanicsDir = new File(plugin.getDataFolder(), "mechanics");
        if (!mechanicsDir.exists()) {
            mechanicsDir.mkdirs();
        }

        // Загружаем корневые конфиги
        loadConfig("config.yml");
        loadConfig("messages.yml");
        loadConfig("debug.yml");

        // Загружаем конфиги механик
        loadConfig("mechanics/doublejump.yml");
        loadConfig("mechanics/wallcling.yml");
        loadConfig("mechanics/chargedjump.yml");
        loadConfig("mechanics/dashjump.yml");
    }

    public void loadConfig(String path) {
        File file = new File(plugin.getDataFolder(), path);
        if (!file.exists()) {
            plugin.saveResource(path, false);
        }
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        configs.put(path, config);
    }

    public FileConfiguration getConfig(String path) {
        return configs.get(path);
    }

    public FileConfiguration getMainConfig() {
        return configs.get("config.yml");
    }

    public FileConfiguration getMessagesConfig() {
        return configs.get("messages.yml");
    }

    public FileConfiguration getDebugConfig() {
        return configs.get("debug.yml");
    }

    public FileConfiguration getMechanicConfig(String mechanic) {
        return configs.get("mechanics/" + mechanic + ".yml");
    }

    public void saveConfig(String path) {
        File file = new File(plugin.getDataFolder(), path);
        FileConfiguration config = configs.get(path);
        if (config != null) {
            try {
                config.save(file);
            } catch (IOException e) {
                plugin.getLogger().severe("Не удалось сохранить конфиг: " + path);
                e.printStackTrace();
            }
        }
    }

    public void reloadAll() {
        configs.clear();
        loadAll();
    }
}
