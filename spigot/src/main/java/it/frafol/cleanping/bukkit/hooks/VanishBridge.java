package it.frafol.cleanping.bukkit.hooks;

import com.github.Anon8281.universalScheduler.UniversalScheduler;
import com.github.Anon8281.universalScheduler.scheduling.schedulers.TaskScheduler;
import com.github.Anon8281.universalScheduler.scheduling.tasks.MyScheduledTask;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRegisterChannelEvent;
import org.bukkit.event.player.PlayerUnregisterChannelEvent;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.plugin.java.JavaPlugin;

import java.nio.ByteBuffer;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class VanishBridge implements Listener {
    private static final String CHANNEL = "armaping:vanish";
    private static final int HEARTBEAT_TICKS = 20;

    private final JavaPlugin plugin;
    private final TaskScheduler scheduler;
    private final Map<Player, MyScheduledTask> tasks = new ConcurrentHashMap<>();

    public VanishBridge(JavaPlugin plugin) {
        this.plugin = plugin;
        this.scheduler = UniversalScheduler.getScheduler(plugin);
    }

    public void register() {
        plugin.getServer().getMessenger().registerOutgoingPluginChannel(plugin, CHANNEL);
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            if (player.getListeningPluginChannels().contains(CHANNEL)) {
                watch(player);
            }
        }
    }

    public static boolean isVisible(Player player) {
        if (player == null || !player.isOnline()) {
            return false;
        }
        for (MetadataValue value : player.getMetadata("vanished")) {
            if (value.asBoolean()) {
                return false;
            }
        }
        return true;
    }

    @EventHandler
    public void onRegisterChannel(PlayerRegisterChannelEvent event) {
        if (CHANNEL.equals(event.getChannel())) {
            watch(event.getPlayer());
        }
    }

    @EventHandler
    public void onUnregisterChannel(PlayerUnregisterChannelEvent event) {
        if (CHANNEL.equals(event.getChannel())) {
            stopWatching(event.getPlayer());
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        stopWatching(event.getPlayer());
    }

    private void watch(Player player) {
        tasks.computeIfAbsent(player, ignored -> scheduler.runTaskTimer(player, new Runnable() {
            private boolean lastVisible;
            private int ticksSinceUpdate = HEARTBEAT_TICKS;

            @Override
            public void run() {
                boolean visible = isVisible(player);
                if (++ticksSinceUpdate >= HEARTBEAT_TICKS || visible != lastVisible) {
                    // A monotonic timestamp lets Velocity discard out-of-order events, including after a plugin reload.
                    byte[] status = ByteBuffer.allocate(Long.BYTES + 1)
                            .putLong(System.nanoTime())
                            .put((byte) (visible ? 1 : 0))
                            .array();
                    player.sendPluginMessage(plugin, CHANNEL, status);
                    lastVisible = visible;
                    ticksSinceUpdate = 0;
                }
            }
        }, 1L, 1L));
    }

    private void stopWatching(Player player) {
        MyScheduledTask task = tasks.remove(player);
        if (task != null) {
            task.cancel();
        }
    }
}
