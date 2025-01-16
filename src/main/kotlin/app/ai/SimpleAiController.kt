@file:Suppress("MemberVisibilityCanBePrivate")

package app.ai

import app.ai.AiConfiguration.Assistant
import app.ai.SimpleAiController.PromptManager.USER_MSG_FR
import org.springframework.http.HttpStatus.OK
import org.springframework.http.ProblemDetail
import org.springframework.http.ProblemDetail.forStatusAndDetail
import org.springframework.http.ResponseEntity
import org.springframework.http.ResponseEntity.ok
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController


@RestController
class SimpleAiController(private val chat: Assistant) {

    @GetMapping("/api/ai/simple")
    suspend fun completion(
        @RequestParam(value = "message", defaultValue = USER_MSG_FR)
        message: String?
    ): ResponseEntity<ProblemDetail> = ok()
        .body(forStatusAndDetail(OK, chat.chat(message)))

    object PromptManager {
        const val ASSISTANT_NAME = "E-3PO"
        val userName = System.getProperty("user.name")!!
        const val USER_MSG_FR = """Montre moi un exemple de code 
            |en Kotlin qui utilise une monade Either de la bibliothèque Arrow. 
            |Tu me répondra au format markdown 
            |et tu mettra le code dans des balises de code avec commentaires."""

        const val SYSTEM_MSG_FR = """```configuration --lang=fr;```;
        | Tu es E-3PO, un assistant IA spécialisé en EdTech et formation professionnelle. 
        | Ton utilisateur principal est cheroliv, un artisan du logiciel et un expert en éducation des adultes. 
        | qui se concentre sur le développement EdTech et les méthodologies agiles.
        | Tes responsabilités de base :
        | 1. Aider à la création de contenu éducatif pour les apprenants adultes
        | 2. Aide aux tâches de génération de code et de développement de logiciels
        | 3. Soutenir l'application des principes agiles et de l'artisanat logiciel
        | 4. Fournir des conseils sur la conception pédagogique pour l’éducation des adultes
        | Veuillez communiquer de manière claire et concise, en vous concentrant sur des solutions pratiques.
        | Répondez-moi au format markdown."""

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
    }
}
