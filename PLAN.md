# План реализации: Telegram-бот с локальной LLM

## Итоговые решения

| Параметр | Значение |
|----------|----------|
| Язык | Kotlin 2.1+ |
| JVM | 21 |
| Сборка | Gradle, KTS, libs.versions.toml |
| LLM-интеграция | ai.koog (JetBrains) — встроенный Ollama-провайдер |
| Telegram API | Ktor HTTP-клиент, polling, без сторонних Telegram-библиотек |
| Конфигурация | Переменные окружения: `TELEGRAM_BOT_TOKEN`, `OLLAMA_BASE_URL`, `OLLAMA_MODEL` |
| Параллелизм | Kotlin Coroutines — каждое сообщение в отдельной корутине |
| История диалога | Отсутствует — каждый запрос независим |
| БД | Нет |

---

## Архитектура

```
Telegram (пользователь)
    ↓ текстовое сообщение
TelegramPoller  [long polling, getUpdates]
    ↓
MessageHandler  [coroutine per message]
    ↓
LlmService      [ai.koog → Ollama]
    ↓
TelegramClient  [sendMessage]
    ↓
Telegram (пользователь)
```

### Структура модулей

```
src/main/kotlin/
└── bot/
    ├── Main.kt              — точка входа, запуск polling-цикла
    ├── Config.kt            — чтение переменных окружения
    ├── telegram/
    │   ├── TelegramClient.kt  — HTTP-запросы: sendMessage, sendChatAction
    │   └── TelegramPoller.kt  — long polling: getUpdates, диспетчеризация
    ├── llm/
    │   └── LlmService.kt    — обёртка ai.koog Ollama-провайдера
    └── handler/
        └── MessageHandler.kt — роутинг команд (/start, /model) и текстовых сообщений
```

---

## Пошаговый план

### Шаг 1 — Инициализация проекта

- Создать `settings.gradle.kts` (имя проекта, репозитории)
- Создать `gradle/libs.versions.toml` с версиями всех зависимостей
- Создать `build.gradle.kts` (плагины kotlin-jvm, kotlinx-serialization, application; зависимости из каталога)

**Зависимости в каталоге:**
```
ai.koog                            — LLM-фреймворк с Ollama-провайдером
io.ktor:ktor-client-core           — HTTP-клиент
io.ktor:ktor-client-cio            — CIO-движок для Ktor
io.ktor:ktor-client-content-negotiation
io.ktor:ktor-serialization-kotlinx-json
org.jetbrains.kotlinx:kotlinx-coroutines-core
org.jetbrains.kotlinx:kotlinx-serialization-json
org.slf4j:slf4j-simple             — логирование
```

---

### Шаг 2 — Config.kt

Объект, читающий переменные окружения при старте:

```
TELEGRAM_BOT_TOKEN  — токен бота (обязателен)
OLLAMA_BASE_URL     — URL Ollama (default: http://localhost:11434)
OLLAMA_MODEL        — название модели (default: qwen3:0.6b)
OLLAMA_SYSTEM_PROMPT — системный промпт (опционально, бонус)
```

Падать с понятным сообщением, если обязательная переменная не задана.

---

### Шаг 3 — TelegramClient.kt

Ktor-клиент для Telegram Bot API:

- `sendMessage(chatId, text)` — отправить текстовый ответ
- `sendChatAction(chatId, action="typing")` — индикатор набора пока LLM думает
- Базовый URL: `https://api.telegram.org/bot{TOKEN}/`
- Сериализация через `kotlinx.serialization`

---

### Шаг 4 — TelegramPoller.kt

Long polling через `getUpdates`:

- Параметр `timeout=30` (секунды ожидания на сервере)
- Отслеживание `offset` для получения только новых апдейтов
- Фильтрация: обрабатывать только `message.text`
- Для каждого входящего сообщения — запуск `MessageHandler` в отдельной корутине (`launch`)
- Устойчивость: при сетевой ошибке — логировать и повторить через 5 секунд

---

### Шаг 5 — LlmService.kt

Обёртка над ai.koog Ollama-провайдером:

- Инициализация клиента с `OLLAMA_BASE_URL` и `OLLAMA_MODEL`
- Метод `chat(userMessage: String): String`
- Каждый вызов — новый, без истории
- Если задан `OLLAMA_SYSTEM_PROMPT` — передавать как системный промпт
- При недоступности Ollama — бросать исключение с понятным сообщением

---

### Шаг 6 — MessageHandler.kt

Роутинг и обработка сообщений:

- `/start` — приветственное сообщение с описанием бота (бонус)
- `/model <name>` — временная смена модели для пользователя (бонус, in-memory per-session)
- Любой другой текст → `LlmService.chat()` → `TelegramClient.sendMessage()`
- Перед вызовом LLM — отправить `sendChatAction("typing")`
- Обработка ошибок:
  - Ollama недоступна → сообщение пользователю: "LLM временно недоступна"
  - Любая другая ошибка → логировать + сообщение "Произошла ошибка"

---

### Шаг 7 — Main.kt

Точка входа:

```kotlin
fun main() {
    val config = Config.load()
    val telegramClient = TelegramClient(config)
    val llmService = LlmService(config)
    val handler = MessageHandler(telegramClient, llmService, config)
    val poller = TelegramPoller(telegramClient, handler, config)
    runBlocking { poller.start() }
}
```

---

### Шаг 8 — Обработка ошибок и стабильность

- Polling-цикл не останавливается при единичных ошибках обработки
- LLM-ошибка не роняет бота — пользователь получает сообщение об ошибке
- Graceful shutdown по Ctrl+C (корутинный scope с отменой)
- Логирование каждого входящего запроса и ответа LLM (бонус)

---

### Шаг 9 — README.md

Содержимое:
1. Требования (JVM 21, Ollama)
2. Установка Ollama и загрузка модели (`ollama pull qwen3:0.6b`)
3. Создание Telegram-бота через @BotFather
4. Переменные окружения и их описание
5. Сборка: `./gradlew build`
6. Запуск: `./gradlew run` или `java -jar`
7. Опциональные переменные (системный промпт, смена модели)

---

## Порядок реализации (с учётом зависимостей)

```
Шаг 1 (проект)
    ↓
Шаг 2 (Config)
    ↓         ↓
Шаг 3        Шаг 5
(TelegramClient) (LlmService)
    ↓         ↓
    Шаг 6 (MessageHandler)
        ↓
    Шаг 4 (TelegramPoller)
        ↓
    Шаг 7 (Main)
        ↓
    Шаг 8 (ошибки/стабильность)
        ↓
    Шаг 9 (README)
```

Шаги 3 и 5 можно реализовывать параллельно.

---

## Бонусные фичи (опционально, после MVP)

| Фича | Где реализовать |
|------|----------------|
| `/start` с приветствием | MessageHandler |
| Системный промпт через env | LlmService + Config |
| `/model <name>` смена модели | MessageHandler |
| Логирование запросов/ответов | MessageHandler + LlmService |
| `sendChatAction("typing")` | MessageHandler |
