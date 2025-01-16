@file:Suppress("MemberVisibilityCanBePrivate")

package app.ai

import app.ai.AiConfiguration.Assistant
import app.ai.AiConfiguration.PromptManager.USER_MSG_FR
import org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR
import org.springframework.http.HttpStatus.OK
import org.springframework.http.ProblemDetail
import org.springframework.http.ProblemDetail.forStatusAndDetail
import org.springframework.http.ResponseEntity
import org.springframework.http.ResponseEntity.internalServerError
import org.springframework.http.ResponseEntity.ok
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController


@RestController
class SimpleAiController(private val chat: Assistant) {
    @GetMapping("/api/ai/simple")
    suspend fun simple(
        @RequestParam(value = "message", defaultValue = USER_MSG_FR)
        message: String?
    ): ResponseEntity<ProblemDetail> = try {
        ok().body(forStatusAndDetail(OK, chat.chat(message)))
    } catch (e: Exception) {
        internalServerError().body(forStatusAndDetail(INTERNAL_SERVER_ERROR, e.message))
    }

    @GetMapping("/api/ai/trivial")
    suspend fun trivial(
        @RequestParam(value = "message", defaultValue = USER_MSG_FR)
        message: String?
    ): ResponseEntity<ProblemDetail> = try {
        ok()
            .body(forStatusAndDetail(OK, chat.chat(message)))
    } catch (e: Exception) {
        internalServerError().body(forStatusAndDetail(INTERNAL_SERVER_ERROR, e.message))
    }
}
