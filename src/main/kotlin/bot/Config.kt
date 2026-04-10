package bot

data class Config(
    val telegramBotToken: String,
    val ollamaBaseUrl: String,
    val ollamaModel: String,
    val ollamaSystemPrompt: String?,
) {
    companion object {
        fun load(): Config {
            val token =
                System.getenv("TELEGRAM_BOT_TOKEN")
                    ?: error("Required environment variable TELEGRAM_BOT_TOKEN is not set")
            val baseUrl = System.getenv("OLLAMA_BASE_URL") ?: "http://localhost:11434"
            val model = System.getenv("OLLAMA_MODEL") ?: "qwen3:0.6b"
            val systemPrompt = System.getenv("OLLAMA_SYSTEM_PROMPT")
            return Config(
                telegramBotToken = token,
                ollamaBaseUrl = baseUrl,
                ollamaModel = model,
                ollamaSystemPrompt = systemPrompt,
            )
        }
    }
}
