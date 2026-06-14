# BardMind — Telegram Bot (Jacek Kaczmarski)

A Telegram bot that simulates the personality and works of Jacek Kaczmarski (1957–2004), the Polish bard and poet. Powered by GPT-4 with a RAG (Retrieval-Augmented Generation) pipeline built on OpenAI embeddings.

## Architecture

```
Telegram Update
    └─▶ KaczmarskiBot (TelegramLongPollingBot)
            └─▶ MessageHandler
                    └─▶ CommandStorage
                            ├─▶ CommandExecutor      — predefined commands from commands.properties
                            ├─▶ ChatSessionManager   — tracks active chat sessions
                            └─▶ ChatHandler
                                    └─▶ KaczmarskiGPTHandler
                                            ├─▶ RagService       — context retrieval
                                            │       └─▶ VectorStore / EmbeddingService
                                            └─▶ OpenAiService    — GPT-4 / fallback GPT-3.5
```

## Requirements

- Java 21+
- Maven 3.8+
- OpenAI API key
- Telegram bot token (from @BotFather)

## Configuration

Copy the example config and fill in your credentials:

```bash
cp src/main/resources/config.properties.example src/main/resources/config.properties
```

```properties
openai.api.key=sk-...
telegram.bot.token=...
bot.username=YourBotName
```

The file is excluded from git (`.gitignore`).

## Running

```bash
# 1. Generate embeddings (once, or after adding new .txt files to src/main/resources/data/)
mvn exec:java@generate-embeddings

# 2. Start the bot
mvn exec:java@run-bot
```

By default the bot looks for `embeddings.json` in the working directory. Override with:

```bash
mvn exec:java@run-bot -Dbardmind.embeddings=/path/to/embeddings.json
```

## Bot Commands

| Command | Description |
|---------|-------------|
| `/start` | Start a conversation with Kaczmarski (activates GPT mode) |
| `/end` | End the active conversation |
| `/help` | List available commands |
| `/info` | About the bot |
| `/nielubie` | Easter egg |

Once a session is started with `/start`, all messages are routed to GPT-4. Type `daj tekst [title]` to get the exact lyrics of a song.

## How RAG Works

1. `.txt` files from `src/main/resources/data/` are embedded using `text-embedding-3-small`.
2. Embeddings are saved to `embeddings.json` (external file, outside the JAR).
3. On each query: the question is embedded → cosine similarity search → top-K chunks → injected into the user message as context for GPT.

## LLM Parameters (`AppConfig.java`)

| Parameter | Value |
|-----------|-------|
| Primary model | `gpt-4` |
| Fallback model | `gpt-3.5-turbo` |
| Temperature | `0.3` |
| Max tokens | `300` |
| Presence penalty | `0.7` |
| Frequency penalty | `0.7` |
| RAG top-K | `2` |

## Tests

```bash
mvn test
```

Tests require a valid OpenAI key in `config.properties`. Integration tests hit the real API and are slow (~10s).

## Resource File Structure

```
src/main/resources/
├── config.properties          # NOT in repo — API keys
├── config.properties.example  # Template for new contributors
├── commands.properties        # Telegram commands and their actions
├── messages.properties        # UI messages (i18n-ready)
├── lyrics.properties          # Song lyrics (format: key=title|lyrics)
├── prompts/
│   └── system_prompt.txt      # System prompt for GPT
├── data/
│   ├── Mury.txt
│   ├── Rokosz.txt
│   └── Źródło.txt
└── embeddings.json            # Generated embeddings (not in repo)
```
