@file:Suppress("MemberVisibilityCanBePrivate")

package app.ai

import app.ai.SimpleAiController.PromptManager.SYSTEM_MSG_FR
import app.ai.SimpleAiController.PromptManager.USER_MSG_FR
import dev.langchain4j.service.SystemMessage
import dev.langchain4j.service.spring.AiService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController


@RestController
class SimpleAiController(private val chat: Assistant) {

    @AiService
    interface Assistant {
        @SystemMessage(SYSTEM_MSG_FR)
        fun chat(userMessage: String?): String?
    }

    @GetMapping("/api/ai/simple")
    suspend fun completion(
        @RequestParam(value = "message", defaultValue = USER_MSG_FR)
        message: String?
    ) = chat.chat(message)

    object PromptManager {
        const val ASSISTANT_NAME = "E-3PO"
        val userName = System.getProperty("user.name")!!
        const val USER_MSG_FR = """Montre moi un exemple de code 
            |en Kotlin qui utilise une monade Either de la bibliothèque Arrow. 
            |Tu me répondra au format markdown 
            |et tu mettra le code dans des balises de code avec commentaires."""
        const val SYSTEM_MSG_FR = """```configuration --lang=fr;```; 
            | Salut je suis cheroliv,
            | toi tu es E-3PO, tu es mon assistant.
            | Le cœur de métier de cheroliv est le développement logiciel dans l'EdTech
            | et la formation professionnelle pour adulte.
            | La spécialisation de cheroliv est dans l'ingénierie de la pédagogie pour adulte,
            | et le software craftmanship avec les méthodes agiles.
            | E-3PO ta mission est d'aider cheroliv dans l'activité d'écriture de formation et génération de code.
            | Réponds moi au format markdown"""

        const val SYSTEM_MSG_EN = """```configuration --lang=en;```;
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

//        val SYSTEM_MSG_FR = """config```--lang=fr;```.
//            | Salut je suis $userName,
//            | toi tu es $ASSISTANT_NAME, tu es mon assistant.
//            | Le cœur de métier de ${System.getProperty("user.name")} est le développement logiciel dans l'EdTech
//            | et la formation professionnelle pour adulte.
//            | La spécialisation de ${System.getProperty("user.name")} est dans l'ingénierie de la pédagogie pour adulte,
//            | et le software craftmanship avec les méthodes agiles.
//            | $ASSISTANT_NAME ta mission est d'aider ${System.getProperty("user.name")} dans l'activité d'écriture de formation et génération de code.
//            | Réponds moi à ce premier échange uniquement en maximum 120 mots""".trimMargin()
    }
}
