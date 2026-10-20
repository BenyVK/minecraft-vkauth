<div dir="rtl">

# vkauth

یک پلاگین سبک **ثبت‌نام و ورود** برای سرورهای ماینکرفت **Paper 1.21+**.
بازیکن‌ها باید قبل از هر کاری ثبت‌نام یا لاگین کنن؛ تا اون موقع نمی‌تونن حرکت کنن، چت کنن، بسازن، بجنگن یا از کامندهای دیگه استفاده کنن.

> نسخه انگلیسی: [README.md](README.md)

---

## ✨ امکانات

- دستورهای `/register` و `/login` با تکرار رمز، محدودیت طول رمز و محدودیت تعداد رمز اشتباه
- رمزها با **PBKDF2-HMAC-SHA256** هش می‌شن (سالت تصادفی برای هر رمز، ۱۲۰٬۰۰۰ تکرار) و هیچ‌وقت به‌صورت متن ساده ذخیره نمی‌شن
- هش کردن و کار با دیتابیس **ناهمزمان (Async)** انجام می‌شه، پس سرور لگ نمی‌خوره
- ذخیره‌سازی با **YAML**، **MySQL** یا **هر دو با هم** (خوندن از اولی، نوشتن توی همه)
- بازیکنی که لاگین نکرده **فریز** می‌شه و نمی‌تونه چت کنه، کامند بزنه، بلاک بشکنه یا بذاره، تعامل کنه، آیتم بندازه یا برداره، اینونتوری باز کنه، آسیب بزنه یا ببینه و ...
- **اتاق انتظار لاگین** (`auth-map`)؛ بازیکن‌ها توی یه جای امن منتظر می‌مونن و بعد از لاگین به جای قبلیشون برمی‌گردن
- **اسپاون ثابت بعد از لاگین** (`start_auth_all`)
- نمایش **تاریخ آخرین لاگین** بعد از `/login`
- **متن سه‌بعدی** شناور جلوی بازیکن موقع لاگین (اندازه‌ش قابل تنظیمه)
- **اسم‌های TAB** موقع لاگین: `LOGIN_USER_1` و `REGISTER_USER_1` و ... (شمارنده روزانه)
- **مخفی کردن بازیکن‌ها** از کسی که هنوز لاگین نکرده
- کامند `/logout` برای خروج از بازی
- زمان‌سنج لاگین و پیام‌های یادآوری
- همه‌ی پیام‌ها توی `configvk.yml` قابل تغییرن

---

## 📦 نصب

۱. پلاگین رو بیلد کن (یا فایل jar رو دانلود کن) و بذارش توی پوشه‌ی `plugins/` سرور.
۲. سرور رو یک بار روشن کن. فایل `plugins/vkauth/configvk.yml` ساخته می‌شه.
۳. `configvk.yml` رو هر جور خواستی تغییر بده و سرور رو **ری‌استارت** کن (کامند ریلود نداره).

**نیازمندی‌ها:** Paper نسخه ۱.۲۱ یا بالاتر و Java 21.
درایور MySQL فقط وقتی لازمه که از MySQL استفاده کنی (همراه Paper هست).

---

## 💬 کامندها

| کامند | میانبر | توضیح |
|---|---|---|
| `/register <password> <confirm>` | `/reg` | ساخت اکانت |
| `/login <password>` | `/log` | ورود به اکانت |
| `/logout` | - | خروج از سرور |

قبل از لاگین فقط این کامندها کار می‌کنن: `/reg`، `/register`، `/login`، `/log`، `/logout`.

---

## ⚙️ تنظیمات (`configvk.yml`)

رنگ‌ها با کد `&` نوشته می‌شن (`&a`، `&c`، ...) و جایگزین‌ها (Placeholder) داخل `{}` هستن.

### ذخیره‌سازی

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

- `yaml` ← اکانت‌ها توی `plugins/vkauth/accounts.yml` ذخیره می‌شن
- `mysql` ← اکانت‌ها توی جدول MySQL ذخیره می‌شن (جدول و ستون‌ها خودکار ساخته یا آپدیت می‌شن)
- `mysql,yaml` ← توی هر دو ذخیره می‌شه؛ اگه MySQL قطع باشه از کپی YAML استفاده می‌شه

### اتاق انتظار و اسپاون

```yaml
auth-map: false
pos-auth-world: "world"
pos-auth-start: "0.5,100,0.5,0"     # X,Y,Z,A  (A = زاویه نگاه)

auth_hide_players: false

start_auth_all: false
start_auth_world: "world"
start_auth_pos: "0.5,100,0.5,0"
```

| کلید | توضیح |
|---|---|
| `auth-map` | اگه `true` باشه بازیکن‌های لاگین‌نکرده به `pos-auth-start` تلپورت می‌شن و بعد از لاگین به جای قبلیشون برمی‌گردن |
| `auth_hide_players` | اگه `true` باشه بازیکنی که لاگین نکرده بقیه‌ی بازیکن‌ها رو نمی‌بینه |
| `start_auth_all` | اگه `true` باشه بعد از لاگین/ثبت‌نام بازیکن همیشه توی `start_auth_pos` اسپاون می‌شه |

### متن سه‌بعدی

```yaml
text3d: true
text3d_note: "&b&lWelcome &f{player}\n&7Please log in or register"
text3d_scale: 1.5
text3d_distance: 2.5
text3d_height: 0.3
```

| کلید | توضیح |
|---|---|
| `text3d` | روشن / خاموش کردن متن شناور |
| `text3d_note` | خود متن. از رنگ `&`، `\n` برای خط جدید و `{player}` پشتیبانی می‌کنه |
| `text3d_scale` | اندازه‌ی متن (`0.5` کوچک، `1.0` معمولی، `3.0` بزرگ) |
| `text3d_distance` | فاصله از جلوی بازیکن (بلاک) |
| `text3d_height` | ارتفاع نسبت به چشم بازیکن (بلاک) |

این متن **فقط برای خود بازیکنی که داره لاگین می‌کنه** دیده می‌شه و بعد از لاگین پاک می‌شه.

### اسم‌های TAB

```yaml
tab_names: true
tab_login_format: "LOGIN_USER_{number}"
tab_register_format: "REGISTER_USER_{number}"
```

وقتی بازیکن داره لاگین می‌کنه، اسمش توی لیست TAB عوض می‌شه:

- بازیکنی که قبلاً ثبت‌نام کرده ← `LOGIN_USER_1`، `LOGIN_USER_2`، ...
- بازیکن جدید ← `REGISTER_USER_1`، `REGISTER_USER_2`، ...

عدد `{number}` یک **شمارنده‌ی روزانه** هست (مثلاً `123` یعنی امروز ۱۲۳ لاگین شروع شده). هر روز از اول شروع می‌شه و توی `counters.yml` ذخیره می‌شه. بعد از لاگین اسم واقعی برمی‌گرده.

### آخرین لاگین

```yaml
last-login-format: "yyyy-MM-dd HH:mm:ss"
time-zone: ""
```

- `last-login-format` ← هر الگوی [DateTimeFormatter](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/time/format/DateTimeFormatter.html) جاوا
- `time-zone` ← مثلاً `Europe/Berlin` یا `Asia/Tehran` (خالی = منطقه‌ی زمانی سرور)

### امنیت

```yaml
security:
  min-password-length: 6
  max-password-length: 32
  max-login-attempts: 5            # 0 = نامحدود
  login-timeout-seconds: 60        # 0 = بدون محدودیت
  reminder-interval-seconds: 10    # 0 = خاموش
```

### پیام‌ها

همه‌ی پیام‌ها زیر `messages:` توی `configvk.yml` هستن. جایگزین‌های موجود:

| جایگزین | کجا |
|---|---|
| `{prefix}` | همه‌ی پیام‌ها |
| `{player}` | `must-register`، `must-login`، `text3d_note` |
| `{min}` / `{max}` | پیام‌های طول رمز |
| `{left}` / `{attempts}` / `{max}` | `wrong-password` |
| `{seconds}` | `timeout-kick` |
| `{date}` | `last-login` |

---

## 🗂️ فایل‌های داده

| فایل | محتوا |
|---|---|
| `configvk.yml` | تنظیمات و پیام‌ها |
| `accounts.yml` | اکانت‌ها در حالت YAML (اسم، هش، تاریخ ثبت‌نام، آخرین لاگین) |
| `counters.yml` | شمارنده‌های امروز برای لاگین / ثبت‌نام |

---

## 👤 Credits

| | |
|---|---|
| **Plugin** | Ai For Minecraft |
| **Author** | Benyamin Gharri |
| **YouTube** | [BenyVKx](https://www.youtube.com/@BenyVKx) |
| **Tutorial Video** | Coming soon |
| **Persian README.md** | [README.FA.md](https://github.com/BenyVK/minecraft-vkauth/blob/main/README.FA.md) |

</div>
