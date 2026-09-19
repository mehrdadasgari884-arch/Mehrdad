# 🤖 Sety_bozorg Instagram Bot | Mehrdad

> ربات حرفه‌ای اینستاگرام برای رشد و مدیریت پیج **@Sety_bozorg**
> Professional Instagram growth & management bot for @Sety_bozorg

Built for **Mehrdad** repo - branch `arena/01a0bc08-mehrdad`

---

## ✨ قابلیت‌ها | Features

### 🎯 مخصوص @Sety_bozorg
- **آنالیز کامل پیج** - فالوور، نرخ تعامل (ER)، میانگین لایک و کامنت
- **تعامل با جامعه** - لایک و فالو فالوورهای @Sety_bozorg برای افزایش تعامل
- **بوست تعامل** - کامنت‌گذاری خودکار روی پست‌های خود پیج
- **تاریخچه رشد** - ذخیره روزانه آمار فالوور

### 🚀 رشد ارگانیک
- **هشتگ تارگتینگ** - لایک پست‌ها بر اساس هشتگ‌های مرتبط (iran, persian, tehran...)
- **فیلتر هوشمند** - عدم تعامل با اکانت‌های خصوصی/تایید شده/فالوور کم یا زیاد
- **تاخیر انسانی** - تاخیر رندوم 25-90 ثانیه بین عملیات
- **محدودیت ساعتی** - جلوگیری از بلاک شدن

### 📊 داشبورد تحت وب
داشبورد زیبا با Tailwind + فارسی:
- نمایش آمار لحظه‌ای
- دکمه‌های عملیات سریع
- تاریخچه رشد
- تنظیمات

### 🔧 ابزارهای جانبی
- **زمان‌بندی پست** - آپلود خودکار عکس/ویدیو در زمان مشخص
- **استوری** - آپلود استوری با منشن
- **دیتابیس SQLite** - ذخیره تمام تعاملات
- **سشن** - لاگین یکباره، ذخیره سشن برای دفعات بعد

---

## 📦 نصب | Installation

```bash
git clone https://github.com/mehrdadasgari884-arch/Mehrdad.git
cd Mehrdad
git checkout arena/01a0bc08-mehrdad

# Python 3.10+ needed
pip install -r requirements.txt

# Setup env
cp .env.example .env
# Edit .env with your IG credentials
nano .env

# Config
cp config.example.json config.json
# Edit hashtags, comments etc
nano config.json
```

### .env Example
```
IG_USERNAME=your_bot_account
IG_PASSWORD=your_password
TARGET_USERNAME=Sety_bozorg
LIKE_ENABLED=true
FOLLOW_ENABLED=true
COMMENT_ENABLED=false
```

> ⚠️ **مهم**: از اکانت اصلی @Sety_bozorg برای ربات استفاده نکن! یک اکانت کمکی بساز و با اون ربات رو اجرا کن تا اکانت اصلی بلاک نشه.

---

## 🚀 استفاده | Usage

### CLI

```bash
# تست لاگین
python main.py login

# آنالیز پیج @Sety_bozorg
python main.py analyze
# خروجی: followers, engagement rate, avg likes...

# تعامل با 50 فالوور @Sety_bozorg (لایک پست‌هاشون)
python main.py interact --amount 50

# لایک بر اساس هشتگ
python main.py hashtags --tags iran,tehran,persian --amount 20

# آمار ربات
python main.py stats

# داشبورد وب
python main.py dashboard --port 8000
# باز کن: http://localhost:8000
```

### داشبورد
```bash
python main.py dashboard
```
بعد برو به:
- Local: http://localhost:8000
- Preview (Arena): https://8000-....e2b.app

ویژگی‌ها:
- 📊 نمایش آمار
- 📈 نمودار رشد @Sety_bozorg
- ⚡ دکمه آنالیز و تعامل سریع
- ⚙️ نمایش تنظیمات

---

## 🛡️ امنیت | Safety

این ربات برای جلوگیری از بلاک شدن این موارد را رعایت می‌کند:

1. **تاخیر انسانی**: 25-90 ثانیه بین هر عملیات
2. **محدودیت ساعتی**: 
   - Like: 30/hour
   - Follow: 15/hour
   - Comment: 10/hour
3. **فیلتر**: 
   - حداقل 10 فالوور، حداکثر 15k
   - اسکیپ اکانت‌های خصوصی (قابل تنظیم)
4. **استراحت**: بعد از هر 20 عملیات، 5-10 دقیقه استراحت
5. **سشن**: ذخیره سشن برای جلوگیری از لاگین مکرر

> اینستاگرام اتوماسیون را دوست ندارد. همیشه با ریسک خودت استفاده کن. پیشنهاد: روزی 50-100 تعامل بیشتر نه.

---

## 📁 ساختار پروژه

```
Mehrdad/
├── bot/
│   ├── __init__.py
│   ├── instagram_bot.py  # Login & client
│   ├── actions.py        # Like, follow, comment, analyze
│   ├── scheduler.py      # Post scheduling
│   ├── database.py       # SQLite tracking
│   └── utils.py          # Human delays, safety
├── web/
│   ├── app.py            # FastAPI dashboard
│   └── templates/
│       └── dashboard.html
├── sessions/             # Saved IG sessions
├── content/              # Posts to schedule
├── main.py               # CLI entry
├── config.example.json
├── .env.example
└── requirements.txt
```

---

## 🎨 برای @Sety_bozorg چی کار می‌کنه؟

1. **Community Building**: فالوورهای @Sety_bozorg رو پیدا می‌کنه، پست‌هاشون رو لایک می‌کنه تا اونا هم برگردن و تعامل کنن
2. **Engagement Pod**: روی پست‌های خود @Sety_bozorg کامنت‌های حمایتی می‌ذاره تا الگوریتم اینستا پست رو بیشتر نشون بده
3. **Hashtag Growth**: کسایی که به محتوای ایرانی علاقه دارن رو پیدا می‌کنه و جذب پیج می‌کنه
4. **Analytics**: هر روز تعداد فالوور و نرخ تعامل رو ذخیره می‌کنه تا رشد رو ببینی

---

## 🔮 توسعه‌های آینده

- [ ] AI کامنت با GPT (کامنت‌های هوشمند فارسی)
- [ ] آنفالو خودکار کسایی که فالوبک نکردن
- [ ] دایرکت خوش‌آمدگویی (با احتیاط)
- [ ] ریپورت هفتگی رشد به تلگرام
- [ ] پشتیبانی از چند اکانت

---

## ⚖️ دیسکلایمر

This tool is for educational purposes. Using bots may violate Instagram's Terms of Service. Use at your own risk. The author is not responsible for any account bans.

این ابزار فقط برای آموزش است. استفاده از ربات ممکن است خلاف قوانین اینستاگرام باشد. مسئولیت بلاک شدن اکانت با خود شماست.

---

## 👨‍💻 سازنده

**Mehrdad Asgari** - برای @Sety_bozorg
Branch: `arena/01a0bc08-mehrdad`
License: GPL-3.0

> اگر دوست داشتی می‌تونم قابلیت‌های بیشتری اضافه کنم - مثلاً ربات تلگرام که آمار اینستا رو روزانه بفرسته، یا اتصال به هوش مصنوعی برای تولید کپشن فارسی.

---

### Quick Start (Copy-Paste)

```bash
pip install -r requirements.txt
cp .env.example .env
# edit .env
python main.py login
python main.py analyze
python main.py dashboard
```
