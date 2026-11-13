# vkauth

**English** | [فارسی](README.FA.md)

A lightweight register / login plugin for **Paper 1.21+** servers.
Players must register once and log in on every join. Until they do, they are frozen and cannot chat, run commands, break/place blocks, or interact with anything.

> Created by **BenyVK**
> Telegram: <https://t.me/i36VK> · GitHub: <https://github.com/BenyVK>

---

## Features

- `/register`, `/login` and `/logout` with short aliases (`/reg`, `/log`)
- Passwords hashed with **PBKDF2-HMAC-SHA256** (120,000 iterations, random salt per password) — never stored in plain text
- **Case-sensitive or case-insensitive accounts** (`replay-id-account`) — decide whether `BenyVK` and `BEnyVK` are two accounts or one
- Storage in **YAML**, **MySQL**, or **both at once**, with automatic migration between them
- Unauthenticated players are completely locked: movement, chat, commands, interaction, inventory, item pick-up/drop, buckets, damage (given and taken) are all blocked
- Blocked commands are also removed from the client's tab-completion
- Optional waiting room (`auth-map`) and optional fixed spawn after login (`start_auth_all`)
- Optional floating **3D text** shown only to the player who is logging in
- Optional **TAB list names** such as `LOGIN_USER_3` / `REGISTER_USER_7` (daily counters)
- Hide other players from not-yet-logged-in players
- Login timeout, wrong-password limit and reminder messages
- "Last login" date shown after a successful login
- Every message is editable in `configvk.yml`

---

## Requirements

- **Paper** 1.21 or newer (the plugin uses Paper's chat event and Adventure API — Spigot/Bukkit alone is not supported)
- **Java 21**
- For MySQL storage: a MySQL/MariaDB server and the MySQL JDBC driver (`com.mysql.cj.jdbc.Driver`) available on the server. The plugin does not bundle a driver itself.

---

## Installation

1. Put `vkauth.jar` into your server's `plugins/` folder.
2. Start the server once. `plugins/vkauth/configvk.yml` is created.
3. Edit `configvk.yml` to your liking.
4. **Restart the server** (the config is only read at startup).

---

## Commands

| Command | Aliases | Description |
|---|---|---|
| `/register <password> <confirm>` | `/reg` | Create your account |
| `/login <password>` | `/log` | Log in to your account |
| `/logout` | — | Leave the server (kicks you with a goodbye message) |

These commands are the only ones usable before logging in. You can change that list with `auth-commands`. The `vkauth:` prefix is accepted too (for example `/vkauth:login`).

There are no permission nodes — every player can use the commands, and there are no admin commands.

---

## Configuration (`configvk.yml`)

Colors use `&` codes (`&a`, `&c`, …). Placeholders use `{braces}`.

### Accounts: `replay-id-account`

```yml
replay-id-account: true
```

| Value | Behavior |
|---|---|
| `true` | Upper/lower case of the name matters. `BenyVK` and `BEnyVK` are **two different accounts**, each with its own password. |
| `false` | Upper/lower case is ignored. `BenyVK`, `BEnyVK` and every other spelling are **one account with one password**. |

**Switching from `true` to `false`:** old accounts are found by name (ignoring case) the first time that player joins, and copied to the shared account automatically. If several spellings were registered before, the **oldest registration wins**.
**Switching from `false` to `true`:** this is not automatic — players will have to register again under their exact name.

> This option matters on offline-mode servers, where the player's UUID is derived from the exact spelling of the name. On online-mode servers Mojang does not allow names that differ only by case, so there is no visible difference.

### Storage

```yml
storage:
  type: "yaml"          # yaml | mysql | mysql,yaml
  migrate: true
  delete-old-after-migrate: true
  mysql:
    host: "127.0.0.1"
    port: 3306
    database: "vkauth"
    username: "root"
    password: ""
    table: "vkauth_accounts"
```

| Option | Description |
|---|---|
| `type: yaml` | Accounts are saved in `plugins/vkauth/accounts.yml` |
| `type: mysql` | Accounts are saved in the MySQL table |
| `type: mysql,yaml` | Saved in **both**; read from the first one, with the second as fallback |
| `migrate` | When you change the storage type, accounts from the storage that is no longer enabled are copied into the enabled one(s). With `mysql,yaml` both are kept in sync at startup. |
| `delete-old-after-migrate` | After a fully successful transfer, the old storage (the `accounts.yml` file or the MySQL table) is deleted |

The MySQL table and its columns (`registered`, `last_login`) are created and repaired automatically.

### Waiting room and spawn

```yml
auth-map: false
pos-auth-world: "world"
pos-auth-start: "0.5,100,0.5,0"

start_auth_all: false
start_auth_all_replay: true
start_auth_world: "world"
start_auth_pos: "0.5,100,0.5,0"
```

Positions use the format `X,Y,Z,A` (`A` = yaw / facing angle).

- `auth-map: true` — players who are not logged in are teleported to `pos-auth-start`, and go back to where they were after logging in.
- `start_auth_all: true` — after login/register the player is teleported to `start_auth_pos`.
- `start_auth_all_replay` (only when `start_auth_all` is `true`):
  - `true` — **every** login spawns the player at `start_auth_pos`
  - `false` — only the **first** time (right after `/register`); later logins return the player to where they were

### Hiding players

```yml
auth_hide_players: false
```

`true` = a player who is not logged in cannot see other players (works with `auth-map` or on its own).

### Allowed commands before login

```yml
auth-commands:
  - login
  - log
  - register
  - reg
  - logout
```

Every other command is blocked on the server and removed from the tab-completion suggestions sent to the player.

### 3D text

```yml
text3d: true
text3d_note: "&b&lWelcome &f{player}\n&7Please log in or register"
text3d_scale: 1.5
text3d_distance: 2.5
text3d_height: 0.3
```

A floating text in front of the player while logging in. **Only that player can see it**, and it disappears after login. `\n` = new line, `{player}` = the player's name.

### TAB list names

```yml
tab_names: true
tab_login_format: "LOGIN_USER_{number}"
tab_register_format: "REGISTER_USER_{number}"
```

While a player is logging in / registering, their TAB name is replaced so their real name is not shown. `{number}` is how many logins / registrations started **today** (resets every day, saved in `counters.yml`).

### Last login and time zone

```yml
last-login-format: "yyyy-MM-dd HH:mm:ss"
time-zone: ""
```

`last-login-format` follows Java's [`DateTimeFormatter`](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/time/format/DateTimeFormatter.html) patterns. `time-zone` takes values like `Europe/Berlin` or `Asia/Tehran`; empty means the server's time zone.

### Security

```yml
security:
  min-password-length: 6
  max-password-length: 32
  max-login-attempts: 5          # 0 = unlimited
  login-timeout-seconds: 60      # 0 = no limit
  reminder-interval-seconds: 10  # 0 = off
```

### Messages

Every message shown to players lives under `messages:` and can be changed freely. Available placeholders:

| Placeholder | Where it works |
|---|---|
| `{prefix}` | Any message (value of `messages.prefix`) |
| `{player}` | `must-register`, `must-login`, `text3d_note` |
| `{min}` / `{max}` | `password-too-short` / `password-too-long` |
| `{date}` | `last-login` |
| `{attempts}`, `{max}`, `{left}` | `wrong-password` |
| `{seconds}` | `timeout-kick` |

---

## How it works

1. A player joins and is immediately locked. They are optionally moved to the waiting room, hidden from others and given a temporary TAB name.
2. The account is loaded **asynchronously** from storage, so the server thread is never blocked by MySQL or disk access.
3. The player is asked to `/register` (new) or `/login` (existing).
4. Password hashing and verification also run asynchronously.
5. On success the lock is removed, the player is teleported (if configured) and their last-login time is saved.
6. Too many wrong passwords, or taking too long, kicks the player.

If the storage cannot be read, the player is kicked with `messages.storage-error` instead of being let in unprotected.

---

## Files created by the plugin

| File | Purpose |
|---|---|
| `plugins/vkauth/configvk.yml` | Settings and messages |
| `plugins/vkauth/accounts.yml` | Accounts (when YAML storage is enabled) |
| `plugins/vkauth/counters.yml` | Daily login/register counters for TAB names |

---

## Building

The source is a standard Paper plugin (`ir.gharri.vkauth`, main class `ir.gharri.vkauth.Vkauth`, `api-version: 1.21`). Build it with Java 21 against the Paper 1.21 API using your Maven or Gradle setup, then drop the resulting jar into `plugins/`.

---

## Support

Questions or ideas? Open an issue on GitHub or contact **BenyVK** on Telegram: <https://t.me/i36VK>
