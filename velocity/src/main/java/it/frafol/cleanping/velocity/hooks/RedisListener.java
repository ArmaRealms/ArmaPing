package it.frafol.cleanping.velocity.hooks;

import com.imaginarycode.minecraft.redisbungee.RedisBungeeAPI;
import com.imaginarycode.minecraft.redisbungee.events.PubSubMessageEvent;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.proxy.Player;
import it.frafol.cleanping.velocity.CleanPing;
import it.frafol.cleanping.velocity.commands.PingCommand;
import it.frafol.cleanping.velocity.enums.VelocityConfig;
import it.frafol.cleanping.velocity.enums.VelocityMessages;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.UUID;

public class RedisListener {

    // Older proxies must not answer a request that requires a visibility check.
    public static final String REQUEST_CHANNEL = "ArmaPing-VisibleRequest";
    public static final String RESPONSE_CHANNEL = "ArmaPing-VisibleResponse";
    private static final String OFFLINE = "offline";

    public final CleanPing PLUGIN;

    public RedisListener(CleanPing plugin) {
        this.PLUGIN = plugin;
    }

    @Subscribe(priority = Short.MAX_VALUE)
    public void onRedisBungeeMessage(@NotNull PubSubMessageEvent event) {

        final RedisBungeeAPI redisBungeeAPI = RedisBungeeAPI.getRedisBungeeApi();

        if (event.getChannel().equals(REQUEST_CHANNEL)) {
            final String[] request = event.getMessage().split(";", -1);
            if (request.length != 4 || !redisBungeeAPI.getProxyId().equals(request[2])) {
                return;
            }

            final Optional<Player> target = PLUGIN.getServer().getPlayer(UUID.fromString(request[1]))
                    .filter(PLUGIN.getVanishState()::isVisible);
            final String result = target.map(player -> Long.toString(player.getPing())).orElse(OFFLINE);
            redisBungeeAPI.sendChannelMessage(RESPONSE_CHANNEL, request[0] + ";" + request[3] + ";" + result);
        }

        if (event.getChannel().equals(RESPONSE_CHANNEL)) {
            final String[] response = event.getMessage().split(";", -1);
            if (response.length != 3) {
                return;
            }
            final String playerName = response[0];
            final Optional<Player> requester = PLUGIN.getServer().getPlayer(UUID.fromString(response[1]));
            if (!requester.isPresent()) {
                return;
            }
            if (OFFLINE.equals(response[2])) {
                PingCommand.sendNotOnline(requester.get(), playerName);
                return;
            }
            final long ping = Long.parseLong(response[2]);

            if (!(VelocityConfig.DYNAMIC_PING.get(Boolean.class))) {

                requester.get().sendMessage(LegacyComponentSerializer.legacy('§').deserialize(VelocityMessages.OTHERS_PING.color()
                        .replace("%prefix%", VelocityMessages.PREFIX.color())
                        .replace("%user%", playerName)
                        .replace("%ping%", String.valueOf(ping))));

                return;

            }
            requester.get().sendMessage(LegacyComponentSerializer.legacy('§').deserialize(VelocityMessages.OTHERS_PING.color()
                    .replace("%prefix%", VelocityMessages.PREFIX.color())
                    .replace("%user%", playerName)
                    .replace("%ping%", colorBasedOnPing(ping) + ping)));
        }
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
}
