package app.users.password

class InvalidPasswordException :
    RuntimeException("Incorrect password") {
    companion object {
        private const val serialVersionUID = 1L
    }
}