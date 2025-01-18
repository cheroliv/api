package app.users.password

import com.fasterxml.jackson.annotation.JsonIgnore
import jakarta.validation.constraints.Size
import java.time.Instant
import java.util.UUID

data class UserReset(
    val id: UUID,
    val userId: UUID,
    @JsonIgnore
    @field:Size(max = 20)
    val resetKey: String,
    val resetDate: Instant,
    val changeDate: Instant? = null,
)