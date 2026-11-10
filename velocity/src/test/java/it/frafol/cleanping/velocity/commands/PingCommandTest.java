package it.frafol.cleanping.velocity.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.velocitypowered.api.command.BrigadierCommand;
import com.velocitypowered.api.command.CommandMeta;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import it.frafol.cleanping.velocity.CleanPing;
import it.frafol.cleanping.velocity.hooks.VanishState;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.simpleyaml.configuration.file.YamlFile;

import java.util.List;
import java.util.Optional;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class PingCommandTest {
    @Test
    public void acceptsBedrockNamesAndPreservesSuggestionsVisibilityAndPermissions() throws Exception {
        CleanPing plugin = mock(CleanPing.class, RETURNS_DEEP_STUBS);
        ProxyServer proxy = mock(ProxyServer.class, RETURNS_DEEP_STUBS);
        CommandSource source = mock(CommandSource.class);
        VanishState visibility = mock(VanishState.class);
        when(plugin.getVanishState()).thenReturn(visibility);
        YamlFile config = new YamlFile();
        config.set("permissions.others_ping", "others");
        config.set("permissions.difference_ping", "compare");
        config.set("settings.others_ping_enabled", true);
        config.set("settings.difference_ping_enabled", true);
        config.set("settings.dynamic_ping", false);
        config.set("settings.minimessage", false);
        when(plugin.getConfigTextFile().getConfig()).thenReturn(config);
        YamlFile messages = new YamlFile();
        for (String key : List.of("prefix", "others_ping", "difference_ping", "not_online", "no_permission", "usage")) {
            messages.set("messages." + key, key + " %user% %arg1% %arg2% %ping%");
        }
        when(plugin.getMessagesTextFile().getConfig()).thenReturn(messages);
        when(source.hasPermission(anyString())).thenReturn(true);
        Player bedrock = mock(Player.class);
        Player java = mock(Player.class);
        when(bedrock.getUsername()).thenReturn("*Bedrock");
        when(java.getUsername()).thenReturn("JavaPlayer");
        when(bedrock.getPing()).thenReturn(42L);
        when(java.getPing()).thenReturn(10L);
        when(proxy.getPlayer("*Bedrock")).thenReturn(Optional.of(bedrock));
        when(proxy.getPlayer("JavaPlayer")).thenReturn(Optional.of(java));
        when(proxy.getAllPlayers()).thenReturn(List.of(bedrock, java));
        when(visibility.isVisible(any())).thenReturn(true);
        try (MockedStatic<CleanPing> singleton = mockStatic(CleanPing.class)) {
            singleton.when(CleanPing::getInstance).thenReturn(plugin);
            PingCommand.register(proxy, plugin);
            ArgumentCaptor<BrigadierCommand> command = ArgumentCaptor.forClass(BrigadierCommand.class);
            verify(proxy.getCommandManager()).register(any(CommandMeta.class), command.capture());
            CommandDispatcher<CommandSource> dispatcher = new CommandDispatcher<>();
            dispatcher.getRoot().addChild(command.getValue().getNode());
            assertEquals(1, dispatcher.execute("cleanping *Bedrock", source));
            assertEquals(1, dispatcher.execute("cleanping *Bedrock JavaPlayer", source));
            assertEquals(1, dispatcher.execute("cleanping JavaPlayer *Bedrock", source));
            assertEquals(1, dispatcher.execute("cleanping *Bedrock *Bedrock", source));
            assertEquals(1, dispatcher.execute("cleanping JavaPlayer", source));
            for (String input : List.of("cleanping *B", "cleanping JavaPlayer *B", "cleanping *Bedrock  *B")) {
                var suggestions = dispatcher.getCompletionSuggestions(dispatcher.parse(input, source)).get();
                assertEquals(1, suggestions.getList().size());
                assertEquals(input.substring(0, input.lastIndexOf('*')) + "*Bedrock",
                        suggestions.getList().get(0).apply(input));
            }
            assertEquals(0, dispatcher.execute("cleanping *Bedrock JavaPlayer extra", source));
            assertEquals(0, dispatcher.execute("cleanping   ", source));
            assertTrue(dispatcher.getCompletionSuggestions(dispatcher.parse("cleanping JavaPlayer *Bedrock *", source)).get().isEmpty());
            when(source.hasPermission("compare")).thenReturn(false);
            clearInvocations(bedrock, java);
            assertEquals(0, dispatcher.execute("cleanping *Bedrock JavaPlayer", source));
            verify(bedrock, never()).getPing();
            assertTrue(dispatcher.getCompletionSuggestions(dispatcher.parse("cleanping JavaPlayer *", source)).get().isEmpty());
            when(source.hasPermission("compare")).thenReturn(true);
            when(visibility.isVisible(bedrock)).thenReturn(false);
            assertEquals(0, dispatcher.execute("cleanping *Bedrock", source));
            assertEquals(0, dispatcher.execute("cleanping JavaPlayer *Bedrock", source));
            assertTrue(dispatcher.getCompletionSuggestions(dispatcher.parse("cleanping *", source)).get().isEmpty());
            verify(bedrock, never()).getPing();
        }
    }
}
