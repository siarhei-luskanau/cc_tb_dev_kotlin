package bot

import bot.handler.MessageHandler
import bot.llm.LlmService
import bot.telegram.TelegramClient
import bot.telegram.TelegramPoller
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

fun main() {
    val config = Config.load()
    val telegramClient = TelegramClient(config)
    val llmService = LlmService(config)
    val handler = MessageHandler(telegramClient, llmService, config)
    val poller = TelegramPoller(telegramClient, handler::handle)
    runBlocking {
        val job = launch { poller.start() }
        Runtime.getRuntime().addShutdownHook(Thread { job.cancel() })
        job.join()
    }
}
