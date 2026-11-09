package it.frafol.cleanping.velocity.commands;

import com.imaginarycode.minecraft.redisbungee.RedisBungeeAPI;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.velocitypowered.api.command.BrigadierCommand;
import com.velocitypowered.api.command.CommandMeta;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import it.frafol.cleanping.velocity.CleanPing;
import it.frafol.cleanping.velocity.enums.VelocityConfig;
import it.frafol.cleanping.velocity.enums.VelocityMessages;
import it.frafol.cleanping.velocity.enums.VelocityRedis;
import it.frafol.cleanping.velocity.hooks.RedisListener;
import it.frafol.cleanping.velocity.hooks.VanishState;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public final class PingCommand  {

    public static void register(ProxyServer proxyServer, CleanPing plugin) {
        VanishState vanishState = plugin.getVanishState();
        LiteralCommandNode<CommandSource> node = LiteralArgumentBuilder.<CommandSource>literal("cleanping")
                .executes(ctx -> {

                    if (!ctx.getSource().hasPermission(VelocityConfig.PING_PERMISSION.get(String.class))) {
                        ctx.getSource().sendMessage(LegacyComponentSerializer.legacySection()
                                .deserialize(VelocityMessages.NO_PERMISSION.color()
                                        .replace("%prefix%", VelocityMessages.PREFIX.color())
                                ));
                        return Command.SINGLE_SUCCESS;
                    }

                    if (!(ctx.getSource() instanceof Player)) {
                        ctx.getSource().sendMessage(LegacyComponentSerializer.legacySection()
                                .deserialize(VelocityMessages.ONLY_PLAYERS.color()
                                        .replace("%prefix%", VelocityMessages.PREFIX.color())
                                ));
                        return Command.SINGLE_SUCCESS;
                    }

                    final Player player = (Player) ctx.getSource();
                    final long ping = player.getPing();

                    final String color = colorBasedOnPing(ping);

                    player.sendMessage(LegacyComponentSerializer.legacySection()
                            .deserialize(VelocityMessages.PING.color()
                                    .replace("%prefix%", VelocityMessages.PREFIX.color())
                                    .replace("%ping%", color + player.getPing())
                            ));
                    return Command.SINGLE_SUCCESS;
                })
                .then(RequiredArgumentBuilder.<CommandSource, String>argument("player1", StringArgumentType.word())
                        .requires(src -> src.hasPermission(VelocityConfig.PING_OTHERS_PERMISSION.get(String.class)))
                        .suggests((ctx, builder) -> suggestPlayers(proxyServer, vanishState, builder))
                        .executes(ctx -> {

                            final String argument = StringArgumentType.getString(ctx, "player1");

                            final Optional<Player> localTarget = proxyServer.getPlayer(argument);
                            if (localTarget.isPresent() && !vanishState.isVisible(localTarget.get())) {
                                return sendNotOnline(ctx.getSource(), argument);
                            }

                            if (!localTarget.isPresent() && VelocityRedis.REDIS.get(Boolean.class)
                                    && proxyServer.getPluginManager().isLoaded("redisbungee")
                                    && ctx.getSource() instanceof Player) {
                                final RedisBungeeAPI redis = RedisBungeeAPI.getRedisBungeeApi();
                                final UUID uuid = redis.getUuidFromName(argument);
                                if (uuid != null && redis.isPlayerOnline(uuid)) {
                                    final String targetProxy = redis.getProxy(uuid);
                                    if (targetProxy != null && !targetProxy.equals(redis.getProxyId())) {
                                        final Player player = (Player) ctx.getSource();
                                        redis.sendChannelMessage(RedisListener.REQUEST_CHANNEL,
                                                argument + ";" + uuid + ";" + targetProxy + ";" + player.getUniqueId());
                                        return Command.SINGLE_SUCCESS;
                                    }
                                }
                            }

                            if (localTarget.isPresent()) {
                                final Player target = localTarget.get();

                                if (!(VelocityConfig.OTHERS_PING_OPTION.get(Boolean.class))) {
                                    ctx.getSource().sendMessage(LegacyComponentSerializer.legacySection()
                                            .deserialize(VelocityMessages.USAGE.color()
                                                    .replace("%prefix%", VelocityMessages.PREFIX.color())
                                            ));

                                    return Command.SINGLE_SUCCESS;
                                }

                                final long ping = target.getPing();

                                if (!(VelocityConfig.DYNAMIC_PING.get(Boolean.class))) {
                                    ctx.getSource().sendMessage(LegacyComponentSerializer.legacySection()
                                            .deserialize(VelocityMessages.OTHERS_PING.color()
                                                    .replace("%prefix%", VelocityMessages.PREFIX.color())
                                                    .replace("%user%", argument)
                                                    .replace("%ping%", Long.toString(target.getPing()))
                                            ));

                                    return Command.SINGLE_SUCCESS;
                                }

                                final String color = colorBasedOnPing(ping);

                                ctx.getSource().sendMessage(LegacyComponentSerializer.legacySection()
                                        .deserialize(VelocityMessages.OTHERS_PING.color()
                                                .replace("%prefix%", VelocityMessages.PREFIX.color())
                                                .replace("%user%", argument)
                                                .replace("%ping%", color + target.getPing())
                                        ));
                            } else {
                                return sendNotOnline(ctx.getSource(), argument);
                            }

                            return Command.SINGLE_SUCCESS;
                        })
                        .then(RequiredArgumentBuilder.<CommandSource, String>argument("player2", StringArgumentType.word())
                                .requires(src -> src.hasPermission(VelocityConfig.DIFFERENCE_PING_PERMISSION.get(String.class)))
                                .suggests((ctx, builder) -> suggestPlayers(proxyServer, vanishState, builder))
                                .executes(ctx -> {

                                    final String argument = StringArgumentType.getString(ctx, "player1");
                                    final String argument2 = StringArgumentType.getString(ctx, "player2");

                                    final Optional<Player> optionalTarget = proxyServer.getPlayer(argument).filter(vanishState::isVisible);
                                    final Optional<Player> optionalTarget2 = proxyServer.getPlayer(argument2).filter(vanishState::isVisible);

                                    if (optionalTarget.isPresent() && optionalTarget2.isPresent()) {

                                        final Player target = optionalTarget.get();
                                        final Player target2 = optionalTarget2.get();

                                        if (!(VelocityConfig.DIFFERENCE_PING_OPTION.get(Boolean.class))) {
                                            ctx.getSource().sendMessage(LegacyComponentSerializer.legacySection()
                                                    .deserialize(VelocityMessages.USAGE.color()
                                                            .replace("%prefix%", VelocityMessages.PREFIX.color())
                                                    ));

                                            return Command.SINGLE_SUCCESS;
                                        }

                                        final long ping = target.getPing();
                                        final long ping2 = target2.getPing();
                                        final long difference = getDifference(ping, ping2);

                                        ctx.getSource().sendMessage(LegacyComponentSerializer.legacySection()
                                                .deserialize(VelocityMessages.PING_DIFFERENCE.color()
                                                        .replace("%prefix%", VelocityMessages.PREFIX.color())
                                                        .replace("%arg1%", argument)
                                                        .replace("%arg2%", argument2)
                                                        .replace("%ping%", Long.toString(difference))
                                                        .replace("%difference%", Long.toString(difference))
                                                ));

                                    } else {
                                        return sendNotOnline(ctx.getSource(), optionalTarget.isPresent() ? argument2 : argument);
                                    }

                                    return Command.SINGLE_SUCCESS;
                                })
                        )).build();
        final BrigadierCommand command = new BrigadierCommand(node);
        final CommandMeta meta = proxyServer.getCommandManager().metaBuilder(command)
                .aliases("ping").plugin(plugin).build();
        proxyServer.getCommandManager().register(meta, command);
	}

    private static CompletableFuture<Suggestions> suggestPlayers(
            ProxyServer proxyServer, VanishState vanishState, SuggestionsBuilder builder) {
        proxyServer.getAllPlayers().stream()
                .filter(vanishState::isVisible)
                .map(Player::getUsername)
                .filter(name -> name.toLowerCase(Locale.ROOT).startsWith(builder.getRemainingLowerCase()))
                .forEach(builder::suggest);
        return builder.buildFuture();
    }

    public static int sendNotOnline(CommandSource source, String username) {
        source.sendMessage(LegacyComponentSerializer.legacySection()
                .deserialize(VelocityMessages.NOT_ONLINE.color()
                        .replace("%prefix%", VelocityMessages.PREFIX.color())
                        .replace("%user%", username)));
        return 0;
    }

    private static String colorBasedOnPing(long ping) {
        if (ping < VelocityConfig.MEDIUM_MS.get(Integer.class)) {
            return VelocityConfig.LOW_MS_COLOR.color();
        } else if (ping > VelocityConfig.MEDIUM_MS.get(Integer.class) && ping < VelocityConfig.HIGH_MS.get(Integer.class)) {
            return VelocityConfig.MEDIUM_MS_COLOR.color();
        } else {
            return VelocityConfig.HIGH_MS_COLOR.color();
        }
    }

    private static Integer getDifference(long ping1, long ping2) {
        if (ping1 > ping2) {
            return (int) Math.abs(ping1 - ping2);
        }
        return (int) Math.abs(ping2 - ping1);
    }
}