package bot.handler

import bot.Config
import bot.llm.LlmService
import bot.telegram.TelegramClient
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap

class MessageHandler(
    private val telegramClient: TelegramClient,
    private val llmService: LlmService,
    config: Config,
) {
    private val logger = LoggerFactory.getLogger(MessageHandler::class.java)
    private val defaultModel = config.ollamaModel
    private val userModels = ConcurrentHashMap<Long, String>()

    suspend fun handle(
        chatId: Long,
        text: String,
    ) {
        when {
            text == "/start" -> {
                telegramClient.sendMessage(
                    chatId,
                    "Привет! Я бот на базе локальной LLM (${userModels.getOrDefault(chatId, defaultModel)}). Отправь мне любое сообщение.",
                )
            }

            text.startsWith("/model ") -> {
                val modelName = text.removePrefix("/model ").trim()
                if (modelName.isNotEmpty()) {
                    userModels[chatId] = modelName
                    telegramClient.sendMessage(chatId, "Модель переключена на: $modelName")
                } else {
                    telegramClient.sendMessage(chatId, "Укажи название модели: /model <name>")
                }
            }

            else -> {
                telegramClient.sendChatAction(chatId)
                logger.info("chatId=$chatId request: $text")
                try {
                    val response = llmService.chat(text)
                    logger.info("chatId=$chatId response: $response")
                    telegramClient.sendMessage(chatId, response)
                } catch (e: IllegalStateException) {
                    logger.error("LLM unavailable for chatId=$chatId", e)
                    telegramClient.sendMessage(chatId, "LLM временно недоступна")
                } catch (e: Exception) {
                    logger.error("Unexpected error for chatId=$chatId", e)
                    telegramClient.sendMessage(chatId, "Произошла ошибка")
                }
            }
        }
    }
}
