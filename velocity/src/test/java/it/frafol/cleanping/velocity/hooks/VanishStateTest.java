package it.frafol.cleanping.velocity.hooks;

import com.velocitypowered.api.event.connection.PluginMessageEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.messages.ChannelMessageSource;
import org.junit.Before;
import org.junit.Test;

import java.nio.ByteBuffer;
import java.util.Optional;
import java.util.UUID;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class VanishStateTest {
    private final VanishState state = new VanishState();
    private final Player player = mock(Player.class);
    private final ServerConnection backend = mock(ServerConnection.class);

    @Before
    public void connectPlayer() {
        when(player.getUniqueId()).thenReturn(UUID.randomUUID());
        when(player.isActive()).thenReturn(true);
        when(player.getCurrentServer()).thenReturn(Optional.of(backend));
        when(backend.getPlayer()).thenReturn(player);
    }

    @Test
    public void requiresAValidStatusFromTheCurrentBackend() {
        assertFalse(state.isVisible(player));
        assertFalse(send(player, 1, (byte) 1).getResult().isAllowed());
        send(backend, 1, (byte) 2);
        assertFalse(state.isVisible(player));

        send(backend, 2, (byte) 1);
        assertTrue(state.isVisible(player));

        ServerConnection nextBackend = mock(ServerConnection.class);
        when(player.getCurrentServer()).thenReturn(Optional.of(nextBackend));
        when(nextBackend.getPlayer()).thenReturn(player);
        send(backend, 3, (byte) 1);
        assertFalse(state.isVisible(player));

        send(nextBackend, 1, (byte) 1);
        assertTrue(state.isVisible(player));
    }

    @Test
    public void delayedVisibleStatusCannotUndoVanish() {
        send(backend, 1, (byte) 1);
        assertTrue(state.isVisible(player));

        send(backend, 3, (byte) 0);
        send(backend, 2, (byte) 1);
        assertFalse(state.isVisible(player));

        send(backend, 4, (byte) 1);
        assertTrue(state.isVisible(player));
    }

    private PluginMessageEvent send(ChannelMessageSource source, long sequence, byte visible) {
        byte[] data = ByteBuffer.allocate(Long.BYTES + 1).putLong(sequence).put(visible).array();
        PluginMessageEvent event = new PluginMessageEvent(source, player, VanishState.CHANNEL, data);
        state.onStatus(event);
        return event;
    }
}
