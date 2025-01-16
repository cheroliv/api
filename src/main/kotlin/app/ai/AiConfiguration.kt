package app.ai

import app.ai.SimpleAiController.PromptManager.SYSTEM_MSG_FR
import dev.langchain4j.service.SystemMessage
import dev.langchain4j.service.spring.AiService
import org.springframework.context.annotation.Configuration

@Configuration
class AiConfiguration {
    @AiService
    interface Assistant {
        @SystemMessage(SYSTEM_MSG_FR)
        fun chat(userMessage: String?): String?
    }
}