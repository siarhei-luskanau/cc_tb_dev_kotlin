package bot.llm

import ai.koog.prompt.dsl.prompt
import ai.koog.prompt.executor.llms.MultiLLMPromptExecutor
import ai.koog.prompt.executor.llms.all.simpleOllamaAIExecutor
import ai.koog.prompt.executor.ollama.client.OllamaClient
import ai.koog.prompt.llm.LLMCapability
import ai.koog.prompt.llm.LLMProvider
import ai.koog.prompt.llm.LLModel
import bot.Config
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
class LlmService(
    config: Config,
) {
    private val chatClient = OllamaClient(config.ollamaBaseUrl)

    private val executor = MultiLLMPromptExecutor(chatClient)
    private val baseUrl = config.ollamaBaseUrl
    private val systemPrompt = config.ollamaSystemPrompt

    private val model =
        LLModel(
            provider = LLMProvider.Ollama,
            id = config.ollamaModel,
            capabilities = listOf(LLMCapability.Temperature),
            contextLength = 8192,
        )

    suspend fun chat(userMessage: String): String {
        chatClient.getModelOrNull(model.id, pullIfMissing = true)
        val p =
            prompt("chat") {
                systemPrompt?.let { system(it) }
                user(userMessage)
            }
        return try {
            executor.execute(prompt = p, model = model).first().content
        } catch (e: Exception) {
            throw IllegalStateException("Ollama is unavailable at $baseUrl: ${e.message}", e)
        }
    }
}
