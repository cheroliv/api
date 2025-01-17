package app.ai

import app.ai.AiConfiguration.PromptManager.FRENCH
import dev.langchain4j.service.SystemMessage
import dev.langchain4j.service.spring.AiService
import org.springframework.beans.factory.getBean
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Configuration
import org.springframework.core.io.ClassPathResource
import java.util.Properties

@Configuration
class AiConfiguration(private val context: ApplicationContext) {
    @AiService
    interface Assistant {
        @SystemMessage(FRENCH.SYSTEM_MSG)
        fun chat(userMessage: String?): String?
    }

    object PromptManager {
        const val ASSISTANT_NAME = "E-3PO"
        val userName = System.getProperty("user.name")!!

        object FRENCH {
            const val USER_MSG = """Montre moi un exemple de code 
            |en Kotlin qui utilise une monade Either de la bibliothèque Arrow. 
            |Tu me répondra au format markdown 
            |et tu mettra le code dans des balises de code avec commentaires."""

            const val SYSTEM_MSG = """```configuration --lang=fr;```;
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
            const val SYSTEM_MSG = """```configuration --lang=en;```;
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