package it.frafol.cleanping.velocity.hooks;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.connection.PluginMessageEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;

import java.nio.ByteBuffer;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public final class VanishState {
    public static final MinecraftChannelIdentifier CHANNEL = MinecraftChannelIdentifier.from("armaping:vanish");
    private static final long MAX_AGE = TimeUnit.SECONDS.toNanos(3);

    private final Map<UUID, Status> states = new ConcurrentHashMap<>();

    @Subscribe
    public void onStatus(PluginMessageEvent event) {
        if (!CHANNEL.equals(event.getIdentifier())) {
            return;
        }
        event.setResult(PluginMessageEvent.ForwardResult.handled());
        if (!(event.getSource() instanceof ServerConnection)) {
            return;
        }

        byte[] data = event.getData();
        if (data.length != Long.BYTES + 1) {
            return;
        }
        ByteBuffer payload = ByteBuffer.wrap(data);
        long sequence = payload.getLong();
        byte visible = payload.get();
        if (visible != 0 && visible != 1) {
            return;
        }

        ServerConnection backend = (ServerConnection) event.getSource();
        Player player = backend.getPlayer();
        Status update = new Status(backend, sequence, visible == 1, System.nanoTime());
        states.compute(player.getUniqueId(), (id, previous) -> {
            if (!player.isActive() || player.getCurrentServer().orElse(null) != backend) {
                return previous;
            }
            if (previous != null && previous.backend == backend && sequence - previous.sequence <= 0) {
                return previous;
            }
            return update;
        });
    }

    @Subscribe
    public void onDisconnect(DisconnectEvent event) {
        Player player = event.getPlayer();
        states.computeIfPresent(player.getUniqueId(), (id, status) -> status.backend.getPlayer() == player ? null : status);
    }

    public boolean isVisible(Player player) {
        Status status = states.get(player.getUniqueId());
        return status != null && status.visible && player.isActive()
                && status.backend == player.getCurrentServer().orElse(null)
                && System.nanoTime() - status.receivedAt < MAX_AGE;
    }

    private static final class Status {
        private final ServerConnection backend;
        private final long sequence;
        private final boolean visible;
        private final long receivedAt;

        private Status(ServerConnection backend, long sequence, boolean visible, long receivedAt) {
            this.backend = backend;
            this.sequence = sequence;
            this.visible = visible;
            this.receivedAt = receivedAt;
        }
    }
}
