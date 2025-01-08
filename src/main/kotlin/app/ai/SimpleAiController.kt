package app.ai

import app.ai.SimpleAiController.PromptManager.SYSTEM_MSG_EN
import dev.langchain4j.model.chat.ChatLanguageModel
import dev.langchain4j.service.SystemMessage
import dev.langchain4j.service.spring.AiService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController


@RestController
class SimpleAiController(private val chat: ChatLanguageModel) {

    @AiService
    interface Assistant {
        @SystemMessage(SYSTEM_MSG_EN)
        fun chat(userMessage: String?): String?
    }

    @GetMapping("/api/ai/simple")
    suspend fun completion(
        @RequestParam(value = "message", defaultValue = "Tell me a joke")
        message: String?
    ) = "simple api : $message"

    //            : Map<String, String> = java.util.Map.of("generation", chatClient.call(message))
    object PromptManager {
        const val ASSISTANT_NAME = "E-3PO"
        val userName = System.getProperty("user.name")!!
        val SYSTEM_MSG_FR = """config```--lang=fr;```. 
            | Salut je suis $userName,
            | toi tu es $ASSISTANT_NAME, tu es mon assistant.
            | Le cœur de métier de ${System.getProperty("user.name")} est le développement logiciel dans l'EdTech
            | et la formation professionnelle pour adulte.
            | La spécialisation de ${System.getProperty("user.name")} est dans l'ingénierie de la pédagogie pour adulte,
            | et le software craftmanship avec les méthodes agiles.
            | $ASSISTANT_NAME ta mission est d'aider ${System.getProperty("user.name")} dans l'activité d'écriture de formation et génération de code.
            | Réponds moi à ce premier échange uniquement en maximum 120 mots""".trimMargin()
        const val SYSTEM_MSG_EN = """config```--lang=en;```.
        | You are E-3PO, an AI assistant specialized in EdTech and professional training. 
        | Your primary user is cheroliv, a software craftsman and adult education expert 
        | who focuses on EdTech development and agile methodologies.
        | Your base responsibilities:
        | 1. Assist with creating educational content for adult learners
        | 2. Help with code generation and software development tasks
        | 3. Support application of agile and software craftsmanship principles
        | 4. Provide guidance on instructional design for adult education
        | Please communicate clearly and concisely, focusing on practical solutions.
        | Keep initial responses under 120 words."""
    }
}
