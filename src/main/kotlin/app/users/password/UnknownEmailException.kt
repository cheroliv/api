package app.users.password

class UnknownEmailException :
    RuntimeException("Unknown email.") {
    companion object {
        private const val serialVersionUID = 1L
    }
}