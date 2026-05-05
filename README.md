# SopChat

`SopChat` is a backend chat plugin for Paper/Spigot servers with:
- configurable public chat types;
- player-created channels;
- private messages with history;
- ignore list and social spy;
- anti-flood, anti-repeat and anti-spam;
- mentions with sound;
- GUI entrypoint with `/chat`.

## Features

- Local/radius, world and global chat types from config
- Player-created channels with invites, transfer, kick, leave and delete
- Active channel mode via `/channel use`
- Private messages via `/msg`, `/m`, `/tell`, `/reply`, `/r`
- Offline PM delivery and unread tracking
- Channel history and unread tracking
- Ignore list with persistence
- Social spy with persistence
- Join/quit message module
- SQLite or MySQL storage through `SopLib`
- Optional PlaceholderAPI support

## Default Chat Types

Configured in [chat-types.yml](src/main/resources/chat-types.yml):

- `normal` - radius `100`
- `@` - quiet chat, radius `20`
- `@@` - super quiet chat, radius `2`
- `!` - world chat
- `!!` - global chat

All triggers, formats, permissions and radii can be changed in config.

## Commands

### Main

- `/chat` - open the main GUI
- `/chat reload` - reload plugin config
- `/chat clear` - clear public chat
- `/chat slowmode <seconds>` - set chat slowmode
- `/chat mutechat` - toggle global public chat mute
- `/chat globalmute` - alias of `mutechat`

### Channels

- `/channel create <name>` - create a channel
- `/channel invite <channel> <player>` - invite player to channel
- `/channel accept <channelId>` - accept invite
- `/channel deny <channelId>` - deny invite
- `/channel use <channel>` - set active channel
- `/channel off` - disable active channel mode
- `/channel chat <channel> <message>` - send message to channel
- `/channel say <channel> <message>` - alias of `chat`
- `/channel transfer <channel> <player>` - transfer ownership
- `/channel kick <channel> <player>` - kick member
- `/channel leave <channel>` - leave channel
- `/channel delete <channel>` - delete owned channel

### Private Messages

- `/msg <player> <message>` - send private message
- `/m <player> <message>` - alias of `msg`
- `/tell <player> <message>` - alias of `msg`
- `/reply <message>` - reply to last conversation
- `/r <message>` - alias of `reply`

### Ignore / Spy

- `/ignore <player>` - ignore player
- `/unignore <player>` - remove ignore
- `/socialspy` - toggle private message spy

## Permissions

### Base

- `sopchat.use` - base access to commands and chat features
- `sopchat.reload` - use `/chat reload`
- `sopchat.spy` - use `/socialspy`

### Formatting

- `sopchat.format.color`
- `sopchat.format.style`
- `sopchat.format.magic`
- `sopchat.format.hex`
- `sopchat.format.minimessage`
- `sopchat.format.placeholders`

### Moderation

- `sopchat.moderation.clear`
- `sopchat.moderation.slowmode`
- `sopchat.moderation.globalmute`
- `sopchat.moderation.bypass`

### Bypass

- `sopchat.antiflood.bypass`
- `sopchat.antispam.bypass`

## Storage

Configured in [config.yml](src/main/resources/config.yml):

- `sqlite` for single-server use
- `mysql` for shared storage

## Dependencies

Required:
- `SopLib`

Optional:
- `PlaceholderAPI`

