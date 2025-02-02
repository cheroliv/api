package app.ai

import app.ai.AIAssistantWorker.AiConfiguration.HuggingfaceAssistant
import app.ai.AIAssistantWorker.AiConfiguration.OllamaAssistant
import app.ai.AIAssistantWorker.AiConfiguration.PromptManager.FRENCH
import app.ai.AIAssistantWorker.SimpleAiController.AssistantResponse.Error
import app.ai.AIAssistantWorker.SimpleAiController.AssistantResponse.Success
import app.ai.AIAssistantWorker.SimpleAiController.LocalLLMModel.localModels
import app.ai.translator.AiTranslatorController.AssistantManager.createChatTask
import app.ai.translator.AiTranslatorController.AssistantManager.createStreamingChatTask
import app.users.api.web.Web.Companion.configuration
import arrow.core.Either
import arrow.core.Either.Left
import arrow.core.Either.Right
import arrow.core.getOrElse
import dev.langchain4j.data.message.AiMessage
import dev.langchain4j.model.StreamingResponseHandler
import dev.langchain4j.model.chat.ChatLanguageModel
import dev.langchain4j.model.chat.StreamingChatLanguageModel
import dev.langchain4j.model.huggingface.HuggingFaceChatModel
import dev.langchain4j.model.huggingface.HuggingFaceModelName.TII_UAE_FALCON_7B_INSTRUCT
import dev.langchain4j.model.ollama.OllamaChatModel
import dev.langchain4j.model.ollama.OllamaStreamingChatModel
import dev.langchain4j.model.output.Response
import dev.langchain4j.service.SystemMessage
import dev.langchain4j.service.spring.AiService
import dev.langchain4j.service.spring.AiServiceWiringMode.EXPLICIT
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.reactor.mono
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.suspendCancellableCoroutine
import org.springframework.beans.factory.getBean
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR
import org.springframework.http.HttpStatus.OK
import org.springframework.http.MediaType
import org.springframework.http.ProblemDetail
import org.springframework.http.ProblemDetail.forStatusAndDetail
import org.springframework.http.ResponseEntity
import org.springframework.http.ResponseEntity.internalServerError
import org.springframework.http.ResponseEntity.ok
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono
import java.time.Duration.ofSeconds
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class AIAssistantWorker {
    @Configuration
    class AiConfiguration(private val context: ApplicationContext) {

        @AiService(chatModel = "ollamaChatModel", wiringMode = EXPLICIT)
        interface OllamaAssistant {
            @SystemMessage(FRENCH.SYSTEM_MSG)
            fun chat(userMessage: String?): String?
        }

        @AiService(chatModel = "HugginfaceChatModel", wiringMode = EXPLICIT)
        interface HuggingfaceAssistant {
            @SystemMessage(FRENCH.SYSTEM_MSG)
            fun chat(userMessage: String?): String?
        }

        @Bean(name = ["HugginfaceChatModel"])
        fun chatLanguageModel(): ChatLanguageModel = "huggingface.1.api-key.1.key"
            .run(context.configuration::getProperty)
            .run(HuggingFaceChatModel.builder()::accessToken)
            .modelId(TII_UAE_FALCON_7B_INSTRUCT)
            .timeout(ofSeconds(15))
            .temperature(0.7)
            .maxNewTokens(20)
            .waitForModel(true)
            .build()


        object PromptManager {
            const val ASSISTANT_NAME = "E-3PO"
            val userName = System.getProperty("user.name")!!

            object FRENCH {
                const val USER_MSG = """Montre moi un exemple de code 
            |en Kotlin qui utilise une monade Either de la bibliothèque Arrow. 
            |Tu me répondra au format markdown 
            |et tu mettra le code dans des balises de code avec commentaires."""

                const val SYSTEM_MSG = """
        | Tu es E-3PO, un assistant IA spécialisé en EdTech et formation professionnelle. 
        | Ton utilisateur principal est cheroliv, un artisan du logiciel et un expert en éducation des adultes. 
        | qui se concentre sur le développement EdTech et les méthodologies agiles.
        | Tes responsabilités de base :
        | 1. Aider à la création de contenu éducatif pour les apprenants adultes
        | 2. Aide aux tâches de génération de code et de développement de logiciels
        | 3. Soutenir l'application des principes agiles et de l'artisanat logiciel
        | 4. Fournir des conseils sur la conception pédagogique pour l’éducation des adultes
        | Communique de manière claire et concise, en te concentrant sur des solutions pratiques.
        | Répond-moi au format markdown."""
            }

            object ENGLISH {
                const val SYSTEM_MSG = """
        | You are E-3PO, an AI assistant specialized in EdTech and professional training. 
        | Your primary user is cheroliv, a software craftsman and adult education expert 
        | who focuses on EdTech development and agile methodologies.
        | Your base responsibilities:
        | 1. Assist with creating educational content for adult learners
        | 2. Help with code generation and software development tasks
        | 3. Support application of agile and software craftsmanship principles
        | 4. Provide guidance on instructional design for adult education
        | Please communicate clearly and concisely, focusing on practical solutions.
        | Answer me in markdown format."""
            }
        }
    }

    @RestController
    class SimpleAiController(private val context: ApplicationContext) {

        sealed class AssistantResponse {
            data class Success(val answer: String?) : AssistantResponse() {
                init {
                    when {
                        answer.isNullOrBlank() -> throw IllegalStateException("Empty response from the AI!")
                    }
                }
            }

            data class Error(
                val exception: Exception,
                val status: HttpStatus = INTERNAL_SERVER_ERROR
            ) : AssistantResponse() {
                val problemDetail: ProblemDetail
                    get() = forStatusAndDetail(status, exception.message)
            }
        }

        val AssistantResponse.toResponse: ResponseEntity<AssistantResponse>
            get() = when (this) {
                is Success -> ResponseEntity(this, OK)
                is Error -> ResponseEntity(this, status)
            }

        @GetMapping("/api/ai/simple")
        suspend fun simple(
            @RequestParam(
                value = "message",
                defaultValue = FRENCH.USER_MSG
            ) message: String?
        ): ResponseEntity<AssistantResponse> = try {
            message
                .run(context.getBean<OllamaAssistant>()::chat)
                .run(::Success)
                .toResponse
        } catch (e: Exception) {
            Error(e).toResponse
        }

        @GetMapping("/api/ai/huggingface")
        suspend fun huggingface(
            @RequestParam(
                value = "message",
                defaultValue = FRENCH.USER_MSG
            ) message: String?
        ): ResponseEntity<AssistantResponse> = try {
            message
                .run(context.getBean<HuggingfaceAssistant>()::chat)
                .run(::Success)
                .toResponse
        } catch (e: Exception) {
            Error(e).toResponse
        }


        @GetMapping("/api/ai/trivial")
        suspend fun trivial(
            @RequestParam(
                value = "message",
                defaultValue = FRENCH.USER_MSG
            ) message: String?
        ): ResponseEntity<ProblemDetail> = try {
            forStatusAndDetail(OK, context.getBean<OllamaAssistant>().chat(message)).run(ok()::body)
        } catch (e: Exception) {
            forStatusAndDetail(INTERNAL_SERVER_ERROR, e.message).run(internalServerError()::body)
        }

        object LocalLLMModel {
            val ollamaList = listOf(
                "CognitiveComputations/dolphin-2.9.3-qwen2-0.5b:f16",
                "llama3.2:3b-instruct-q8_0",
                "dolphin3:8b-llama3.1-q8_0",
                "dolphin3:8b",
                "aya:8b",
                "llama3.2:3b",
                "deepseek-r1:1.5b",
                "smollm:135m"
            )

            val localModels
                get() = setOf(
                    "llama3.2:3b" to "LlamaTiny",
                    "dolphin-llama3:8b" to "DolphinSmall",
                    "aya:8b" to "AyaSmall",
                    "smollm:135m" to "Smoll",
                    "deepseek-r1:1.5b" to "DSR1Tiny"
                )
        }
        // Creating tasks for each model

        fun ApplicationContext.createChatTasks() = localModels.forEach {
            createChatTask(it.first, "helloOllama${it.second}")
            createStreamingChatTask(it.first, "helloOllamaStream${it.second}")
        }

        fun ApplicationContext.createOllamaChatModel(model: String = "smollm:135m"): OllamaChatModel =
            OllamaChatModel.builder().apply {
                baseUrl(
                    environment.getProperty("ollama.baseUrl") as? String ?: "http://localhost:11434"
                )
//                modelName(findProperty("ollama.modelName") as? String ?: model)
//                temperature(findProperty("ollama.temperature") as? Double ?: 0.8)
//                timeout(ofSeconds(findProperty("ollama.timeout") as? Long ?: 6_000))
                logRequests(true)
                logResponses(true)
            }.build()

        fun ApplicationContext.createOllamaStreamingChatModel(model: String = "smollm:135m"): OllamaStreamingChatModel =
            OllamaStreamingChatModel.builder().apply {
//                baseUrl(findProperty("ollama.baseUrl") as? String ?: "http://localhost:11434")
//                modelName(findProperty("ollama.modelName") as? String ?: model)
//                temperature(findProperty("ollama.temperature") as? Double ?: 0.8)
//                timeout(ofSeconds(findProperty("ollama.timeout") as? Long ?: 6_000))
                logRequests(true)
                logResponses(true)
            }.build()


        suspend fun generateStreamingResponse(
            model: StreamingChatLanguageModel, promptMessage: String
        ): Either<Throwable, Response<AiMessage>> = Either.catch {
            suspendCancellableCoroutine { continuation ->
                model.generate(promptMessage, object : StreamingResponseHandler<AiMessage> {
                    override fun onNext(token: String) = print(token)

                    override fun onComplete(response: Response<AiMessage>) =
                        continuation.resume(response)

                    override fun onError(error: Throwable) =
                        continuation.resume(Left(error).getOrElse { throw it })
                })
            }
        }

        fun ApplicationContext.runChat(model: String) {
            createOllamaChatModel(model = model)
                .run { FRENCH.SYSTEM_MSG.run(::generate).let(::println) }
        }

        fun ApplicationContext.runStreamChat(model: String) {
            runBlocking {
                createOllamaStreamingChatModel(model).run {
                    when (val answer = generateStreamingResponse(this, FRENCH.SYSTEM_MSG)) {
                        is Right -> "Complete response received:\n${
                            answer.value.content().text()
                        }".run(::println)

                        is Left -> "Error during response generation:\n${answer.value}".run(::println)
                    }
                }
            }
        }
    }

    @RestController
    class StreamingChatController(private val context: ApplicationContext) {

        // Configuration du modèle Ollama pour le streaming
        private fun createStreamingModel(modelName: String = "smollm:135m"): OllamaStreamingChatModel {
            return OllamaStreamingChatModel.builder().apply {
                baseUrl("http://localhost:11434")
                modelName(modelName)
                temperature(0.7)
                logRequests(true)
                logResponses(true)
            }.build()
        }

        // Fonction pour gérer le streaming de la réponse
        @OptIn(InternalCoroutinesApi::class)
        private suspend fun streamResponse(
            message: String,
            modelName: String
        ): Mono<String> = mono {
            val model = createStreamingModel(modelName)

            suspendCancellableCoroutine { continuation ->
                model.generate(message, object : StreamingResponseHandler<AiMessage> {
                    override fun onNext(token: String) {
                        continuation.resume(token)
                    }

                    override fun onComplete(response: Response<AiMessage>) {
                        continuation.completeResume(response)
                    }

                    override fun onError(error: Throwable) {
                        continuation.resumeWithException(error)
                        throw error
                    }
                })
            }
        }

        @GetMapping("/api/ai/stream", produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
        suspend fun streamChat(
            @RequestParam(value = "message", defaultValue = FRENCH.USER_MSG) message: String,
            @RequestParam(value = "model", defaultValue = "smollm:135m") model: String
        ): Mono<String> = streamResponse(message, model)

        // Endpoint pour lister les modèles disponibles
        @GetMapping("/api/ai/models")
        fun availableModels() = mapOf(
            "models" to listOf(
                "llama3.2:3b",
                "dolphin-llama3:8b",
                "aya:8b",
                "smollm:135m",
                "deepseek-r1:1.5b"
            )
        )
    }

}