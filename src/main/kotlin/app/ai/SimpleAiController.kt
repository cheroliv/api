@file:Suppress("MemberVisibilityCanBePrivate")

package app.ai

import app.ai.AiConfiguration.Assistant
import app.ai.AiConfiguration.PromptManager.FRENCH
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
        @RequestParam(value = "message", defaultValue = FRENCH.USER_MSG)
        message: String?
    ): ResponseEntity<ProblemDetail> = try {
        forStatusAndDetail(OK, chat.chat(message)).run(ok()::body)
    } catch (e: Exception) {
        forStatusAndDetail(INTERNAL_SERVER_ERROR, e.message).run(internalServerError()::body)
    }

    @GetMapping("/api/ai/trivial")
    suspend fun trivial(
        @RequestParam(value = "message", defaultValue = FRENCH.USER_MSG)
        message: String?
    ): ResponseEntity<ProblemDetail> = try {
        forStatusAndDetail(OK, chat.chat(message)).run(ok()::body)
    } catch (e: Exception) {
        forStatusAndDetail(INTERNAL_SERVER_ERROR, e.message).run(internalServerError()::body)
    }
}