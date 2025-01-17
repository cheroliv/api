package app.ai

import app.ai.AiConfiguration.Assistant
import app.ai.AiConfiguration.PromptManager.FRENCH
import app.ai.SimpleAiController.AssistantResponse.Error
import app.ai.SimpleAiController.AssistantResponse.Success
import org.springframework.http.HttpStatus
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
            is Error -> ResponseEntity.status(this.status).body(this)
        }

    @GetMapping("/api/ai/simple")
    suspend fun simple(
        @RequestParam(
            value = "message",
            defaultValue = FRENCH.USER_MSG
        ) message: String?
    ): ResponseEntity<AssistantResponse> = try {
        message.run(chat::chat).run(::Success).toResponse
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
        forStatusAndDetail(OK, chat.chat(message)).run(ok()::body)
    } catch (e: Exception) {
        forStatusAndDetail(INTERNAL_SERVER_ERROR, e.message).run(internalServerError()::body)
    }
}