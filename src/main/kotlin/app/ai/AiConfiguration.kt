package app.ai

import app.ai.AiConfiguration.PromptManager.SYSTEM_MSG_FR
import dev.langchain4j.service.SystemMessage
import dev.langchain4j.service.spring.AiService
import org.springframework.context.annotation.Configuration

@Configuration
class AiConfiguration {
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

    @AiService
    interface Assistant {
        @SystemMessage(SYSTEM_MSG_FR)
        fun chat(userMessage: String?): String?
    }

}