package ml.allfire.parkshool.commands;

import me.allfire.parkshool.ParkShool;
import me.allfire.parkshool.messages.MessageSender;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class ParkShoolCommand implements CommandExecutor, TabCompleter {

    private final ParkShool plugin;

    public ParkShoolCommand(ParkShool plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "reload":
                if (!sender.hasPermission("parkshool.admin")) {
                    sendNoPermission(sender);
                    return true;
                }
                plugin.getConfigManager().reloadAll();
                plugin.getDebugger().loadConfig();
                sender.sendMessage("§aParkShool конфигурация перезагружена.");
                break;

            case "debug":
                if (!sender.hasPermission("parkshool.admin")) {
                    sendNoPermission(sender);
                    return true;
                }
                if (args.length < 2) {
                    sender.sendMessage("§cИспользование: /parkshool debug <игрок>");
                    return true;
                }
                Player target = Bukkit.getPlayer(args[1]);
                if (target == null) {
                    sender.sendMessage("§cИгрок не найден.");
                    return true;
                }
                plugin.getDebugger().toggleDebug(target);
                sender.sendMessage("§aДебаг для " + target.getName() + " переключён.");
                break;

            case "info":
                if (!sender.hasPermission("parkshool.admin")) {
                    sendNoPermission(sender);
                    return true;
                }
                if (args.length < 2) {
                    sender.sendMessage("§cИспользование: /parkshool info <игрок>");
                    return true;
                }
                Player infoTarget = Bukkit.getPlayer(args[1]);
                if (infoTarget == null) {
                    sender.sendMessage("§cИгрок не найден.");
                    return true;
                }
                sendInfo(sender, infoTarget);
                break;

            default:
                sendHelp(sender);
                break;
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            completions.add("reload");
            completions.add("debug");
            completions.add("info");
        } else if (args.length == 2 && (args[0].equals("debug") || args[0].equals("info"))) {
            for (Player p : Bukkit.getOnlinePlayers()) {
                completions.add(p.getName());
            }
        }
        return completions;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage("§8[§bParkShool§8] §fПомощь:");
        sender.sendMessage("§e/parkshool reload §7- Перезагрузить конфигурацию");
        sender.sendMessage("§e/parkshool debug <игрок> §7- Переключить дебаг");
        sender.sendMessage("§e/parkshool info <игрок> §7- Информация о механиках игрока");
    }

    private void sendNoPermission(CommandSender sender) {
        sender.sendMessage("§cУ тебя нет прав на эту команду.");
    }

    private void sendInfo(CommandSender sender, Player target) {
        sender.sendMessage("§8[§bParkShool§8] §fИнформация о §e" + target.getName() + "§f:");
        sender.sendMessage("§7DoubleJump: §f" + (target.hasPermission("parkshool.doublejump") ? "§a✓" : "§c✗"));
        sender.sendMessage("§7WallCling: §f" + (target.hasPermission("parkshool.wallcling") ? "§a✓" : "§c✗"));
        sender.sendMessage("§7ChargedJump: §f" + (target.hasPermission("parkshool.chargedjump") ? "§a✓" : "§c✗"));
        sender.sendMessage("§7DashJump: §f" + (target.hasPermission("parkshool.dashjump") ? "§a✓" : "§c✗"));
        sender.sendMessage("§7Debug: §f" + (plugin.getDebugger().isDebugEnabled(target) ? "§a✓" : "§c✗"));
    }
}
