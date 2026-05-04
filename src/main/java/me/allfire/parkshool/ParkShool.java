package ml.allfire.parkshool;

import me.allfire.parkshool.commands.ParkShoolCommand;
import me.allfire.parkshool.config.ConfigManager;
import me.allfire.parkshool.debug.Debugger;
import me.allfire.parkshool.listeners.*;
import me.allfire.parkshool.mechanics.MechanicManager;
import me.allfire.parkshool.messages.MessageSender;
import org.bukkit.plugin.java.JavaPlugin;

public final class ParkShool extends JavaPlugin {

    private static ParkShool instance;

    private ConfigManager configManager;
    private MessageSender messageSender;
    private Debugger debugger;
    private MechanicManager mechanicManager;
    private boolean placeholderApiAvailable;

    @Override
    public void onEnable() {
        instance = this;

        // Проверка PlaceholderAPI
        placeholderApiAvailable = getServer().getPluginManager().getPlugin("PlaceholderAPI") != null;
        if (placeholderApiAvailable) {
            getLogger().info("PlaceholderAPI найден — формулы с плейсхолдерами будут работать.");
        } else {
            getLogger().warning("PlaceholderAPI не найден — будут использоваться fallback-значения.");
        }

        // Инициализация компонентов
        this.configManager = new ConfigManager(this);
        this.messageSender = new MessageSender(this);
        this.debugger = new Debugger(this);
        this.mechanicManager = new MechanicManager(this);

        // Загрузка конфигов
        configManager.loadAll();

        // Регистрация слушателей и команд
        registerListeners();
        getCommand("parkshool").setExecutor(new ParkShoolCommand(this));

        getLogger().info("ParkShool v1.0.0 by AllFire успешно запущен!");
    }

    @Override
    public void onDisable() {
        if (mechanicManager != null) {
            mechanicManager.shutdown();
        }
        getLogger().info("ParkShool выключен.");
    }

    private void registerListeners() {
        getServer().getPluginManager().registerEvents(new MoveListener(this), this);
        getServer().getPluginManager().registerEvents(new JumpListener(this), this);
        getServer().getPluginManager().registerEvents(new DamageListener(this), this);
        getServer().getPluginManager().registerEvents(new SneakListener(this), this);
    }

    public static ParkShool getInstance() {
        return instance;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public MessageSender getMessageSender() {
        return messageSender;
    }

    public Debugger getDebugger() {
        return debugger;
    }

    public MechanicManager getMechanicManager() {
        return mechanicManager;
    }

    public boolean isPlaceholderApiAvailable() {
        return placeholderApiAvailable;
    }
}
