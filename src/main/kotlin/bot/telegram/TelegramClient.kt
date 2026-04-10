package bot.telegram

import bot.Config
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

class TelegramClient(
    config: Config,
) {
    private val baseUrl = "https://api.telegram.org/bot${config.telegramBotToken}/"

    private val client =
        HttpClient(CIO) {
            install(ContentNegotiation) {
                json()
            }
        }

    suspend fun sendMessage(
        chatId: Long,
        text: String,
    ) {
        client.post("${baseUrl}sendMessage") {
            contentType(ContentType.Application.Json)
            setBody(SendMessageRequest(chatId = chatId, text = text))
        }
    }

    suspend fun sendChatAction(
        chatId: Long,
        action: String = "typing",
    ) {
        client.post("${baseUrl}sendChatAction") {
            contentType(ContentType.Application.Json)
            setBody(SendChatActionRequest(chatId = chatId, action = action))
        }
    }

    @Serializable
    private data class SendMessageRequest(
        @SerialName("chat_id") val chatId: Long,
        val text: String,
    )

    @Serializable
    private data class SendChatActionRequest(
        @SerialName("chat_id") val chatId: Long,
        val action: String,
    )
}
