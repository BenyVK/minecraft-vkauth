# vkauth

A lightweight **register & login** plugin for **Paper 1.21+** Minecraft servers.
Players must register / log in before they can move, chat, build, fight or use any other command.

> Persian version: [README.FA.md](README.FA.md)

---

## ✨ Features

- `/register` and `/login` with password confirmation, length limits and a wrong-password attempt limit
- Passwords are hashed with **PBKDF2-HMAC-SHA256** (random salt per password, 120,000 iterations) - never stored in plain text
- Hashing and database work run **asynchronously**, so the server never lags
- **YAML**, **MySQL**, or **both at once** (read from the first, write to all)
- Unauthenticated players are **frozen** and cannot chat, use commands, break/place blocks, interact, drop/pick up items, open inventories, take or deal damage, and more
- **Login waiting room** (`auth-map`) - players wait in a safe place and are sent back after login
- **Fixed spawn after login** (`start_auth_all`)
- **Last login date** shown after `/login`
- **3D floating text** in front of the player while logging in (size is configurable)
- **TAB names** while logging in: `LOGIN_USER_1`, `REGISTER_USER_1`, ... (daily counter)
- **Hide players** from people who are not logged in yet
- `/logout` command to leave the server
- Login timeout and reminder messages
- Every message is editable in `configvk.yml`

---

## 📦 Installation

1. Build the plugin (or download the jar) and put it in your server's `plugins/` folder.
2. Start the server once. `plugins/vkauth/configvk.yml` is created.
3. Edit `configvk.yml` as you like and **restart** the server (there is no reload command).

**Requirements:** Paper 1.21 or newer, Java 21.
The MySQL driver is only needed if you use MySQL (it is included with Paper).

---

## 💬 Commands

| Command | Aliases | Description |
|---|---|---|
| `/register <password> <confirm>` | `/reg` | Create your account |
| `/login <password>` | `/log` | Log in to your account |
| `/logout` | - | Leave the server |

Only these commands work before login: `/reg`, `/register`, `/login`, `/log`, `/logout`.

---

## ⚙️ Configuration (`configvk.yml`)

Colors use `&` codes (`&a`, `&c`, ...). Placeholders use `{braces}`.

### Storage

```yaml
storage:
  type: "yaml"          # yaml | mysql | mysql,yaml
  mysql:
    host: "127.0.0.1"
    port: 3306
    database: "vkauth"
    username: "root"
    password: ""
    table: "vkauth_accounts"
```

- `yaml` - accounts are saved in `plugins/vkauth/accounts.yml`
- `mysql` - accounts are saved in the MySQL table (table and columns are created / updated automatically)
- `mysql,yaml` - saved in both; if MySQL is down the YAML copy is used

### Waiting room & spawn

```yaml
auth-map: false
pos-auth-world: "world"
pos-auth-start: "0.5,100,0.5,0"     # X,Y,Z,A  (A = yaw)

auth_hide_players: false

start_auth_all: false
start_auth_world: "world"
start_auth_pos: "0.5,100,0.5,0"
```

| Key | Description |
|---|---|
| `auth-map` | `true` = unauthenticated players are teleported to `pos-auth-start` and sent back to their old position after login |
| `auth_hide_players` | `true` = players who are not logged in cannot see other players |
| `start_auth_all` | `true` = after login/register the player always spawns at `start_auth_pos` |

### 3D text

```yaml
text3d: true
text3d_note: "&b&lWelcome &f{player}\n&7Please log in or register"
text3d_scale: 1.5
text3d_distance: 2.5
text3d_height: 0.3
```

| Key | Description |
|---|---|
| `text3d` | Turn the floating text on / off |
| `text3d_note` | The text. Supports `&` colors, `\n` for a new line and `{player}` |
| `text3d_scale` | Size of the text (`0.5` small, `1.0` normal, `3.0` big) |
| `text3d_distance` | Distance in front of the player (blocks) |
| `text3d_height` | Height offset from the player's eyes (blocks) |

The text is visible **only to the player who is logging in** and disappears after login.

### TAB names

```yaml
tab_names: true
tab_login_format: "LOGIN_USER_{number}"
tab_register_format: "REGISTER_USER_{number}"
```

While a player is logging in, his name in the TAB list is replaced:

- registered player -> `LOGIN_USER_1`, `LOGIN_USER_2`, ...
- new player -> `REGISTER_USER_1`, `REGISTER_USER_2`, ...

`{number}` is a **daily counter** (for example `123` means 123 logins started today). It resets every day and is saved in `counters.yml`. After login the real name comes back.

### Last login

```yaml
last-login-format: "yyyy-MM-dd HH:mm:ss"
time-zone: ""
```

- `last-login-format` - any Java [DateTimeFormatter](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/time/format/DateTimeFormatter.html) pattern
- `time-zone` - for example `Europe/Berlin` or `Asia/Tehran` (empty = server time zone)

### Security

```yaml
security:
  min-password-length: 6
  max-password-length: 32
  max-login-attempts: 5            # 0 = unlimited
  login-timeout-seconds: 60        # 0 = no limit
  reminder-interval-seconds: 10    # 0 = off
```

### Messages

All messages live under `messages:` in `configvk.yml`. Available placeholders:

| Placeholder | Where |
|---|---|
| `{prefix}` | any message |
| `{player}` | `must-register`, `must-login`, `text3d_note` |
| `{min}` / `{max}` | password length messages |
| `{left}` / `{attempts}` / `{max}` | `wrong-password` |
| `{seconds}` | `timeout-kick` |
| `{date}` | `last-login` |

---

## 🗂️ Data files

| File | Content |
|---|---|
| `configvk.yml` | Settings and messages |
| `accounts.yml` | Accounts when using YAML storage (name, hash, registered, last-login) |
| `counters.yml` | Today's login / register counters |

---

## 👤 Credits

| | |
|---|---|
| **Plugin** | Login And Register Minecraft |
| **Author** | Benyamin Gharri |
| **YouTube** | [BenyVKx](https://www.youtube.com/@BenyVKx) |
| **Tutorial Video** | Coming soon |
| **Persian README.md** | [README.FA.md](https://github.com/BenyVK/minecraft-vkauth/blob/main/README.FA.md) |
