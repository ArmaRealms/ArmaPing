package it.frafol.cleanping.bukkit.commands.utils;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import it.frafol.cleanping.bukkit.hooks.VanishBridge;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class TabComplete implements TabCompleter {

    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, String[] args) {
        if (args.length < 1 || args.length > 2) {
            return Collections.emptyList();
        }

        String prefix = args[args.length - 1].toLowerCase(Locale.ROOT);
        return sender.getServer().getOnlinePlayers().stream()
                .filter(VanishBridge::isVisible)
                .filter(player -> player.getName().toLowerCase(Locale.ROOT).startsWith(prefix))
                .map(Player::getName)
                .collect(Collectors.toList());
    }
}
