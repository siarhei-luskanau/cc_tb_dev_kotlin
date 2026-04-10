package bot.telegram

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory

class TelegramPoller(
    private val telegramClient: TelegramClient,
    private val handler: suspend (chatId: Long, text: String) -> Unit,
) {
    private val logger = LoggerFactory.getLogger(TelegramPoller::class.java)

    suspend fun start(): Unit =
        coroutineScope {
            var offset = 0L
            while (true) {
                try {
                    val updates = telegramClient.getUpdates(offset = offset)
                    updates.forEach { update ->
                        offset = maxOf(offset, update.updateId + 1)
                        launch {
                            try {
                                handler(update.chatId, update.text)
                            } catch (e: CancellationException) {
                                throw e
                            } catch (e: Exception) {
                                logger.error("Error handling message for chatId=${update.chatId}", e)
                            }
                        }
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    logger.error("Error polling Telegram updates, retrying in 5s", e)
                    delay(5_000)
                }
            }
        }
}
