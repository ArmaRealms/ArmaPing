# Clean Ping

```
Clean Ping is a basic /ping plugin, compatible with BungeeCord, Spigot and Velocity.
```

## Features
```
» You can see your ping, or that of other players.
» You can see the difference between two pings;
» Extensive compatibility;
» Command to reload the plugin;
» The plugin and translations are configurable.
```

## Velocity, BungeeCord and Spigot compatibility is here.
```
» BungeeCord 1.7-1.21.X & forks
» Velocity 3.2.0/3.3.0/3.4.0 & forks
» Spigot 1.7.10-1.21.X & forks
```

## Commands
```
» /ping • Send your ping;
» /ping (player) • Send the ping of the player of your choice;
» /ping (player1) (player2) • Send the ping difference between two players;
» /pingreload • Reloads the plugin.
```

## Bedrock player names

Use the complete nickname, including its `*` prefix, without quotes: `/ping *Player` or `/ping JavaPlayer *BedrockPlayer`. Both arguments support name suggestions and keep the same vanish and permission checks.

## Essentials vanish

On Paper and Velocity, vanished players are excluded from both name suggestions and ping lookups, including comparisons. Named PlaceholderAPI lookups on Paper also treat vanished players as absent. Players can still check their own latency with `/ping`.

Install this build on the Velocity proxy and every modern Paper/Spigot backend it routes to. The backend reports the Essentials `vanished` metadata for each connection. Velocity treats a player as unavailable while their current backend has no fresh status. If you use RedisBungee, update every participating Velocity proxy together so remote requests use the same visibility check.

## Permissions
```
Permissions are configurable in the config.yml
```
 
## Placeholders
### You can use PlaceholderAPI to have these placeholders in your spigot server:
```
» %cleanping_ping% • Send your ping;
» %cleanping_coloured_ping% • Send your ping with the colors added in config.yml;
» %cleanping_ping_(player's name)% • Send the ping of a player.
» %cleanping_coloured_ping_(player's name)% • Send the ping of a player with the colors added in config.yml.
» %cleanping_difference_(player's name)% • Send the difference between your ping and an online player's ping.
» %cleanping_difference_(player's name)_(player2's name)% • Send the difference between an online player's ping and an another online player's ping.
```
