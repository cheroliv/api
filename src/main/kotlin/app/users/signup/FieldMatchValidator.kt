package app.users.signup

import org.apache.commons.beanutils.BeanUtils
import javax.validation.ConstraintValidator
import javax.validation.ConstraintValidatorContext

class FieldMatchValidator : ConstraintValidator<FieldMatch, Any?> {
    private lateinit var firstFieldName: String
    private lateinit var secondFieldName: String
    private lateinit var message: String

    override fun initialize(constraintAnnotation: FieldMatch) {
        firstFieldName = constraintAnnotation.first
        secondFieldName = constraintAnnotation.second
        message = constraintAnnotation.message
    }

    override fun isValid(value: Any?, context: ConstraintValidatorContext): Boolean {
        var valid = true
        try {
            val firstObj: Any = BeanUtils.getProperty(value, firstFieldName)
            val secondObj: Any = BeanUtils.getProperty(value, secondFieldName)

            valid = firstObj == null && secondObj == null || firstObj != null && firstObj == secondObj
        } catch (ignore: Exception) {
        }

        when {
            !valid -> {
                context.buildConstraintViolationWithTemplate(message)
                    .addPropertyNode(firstFieldName)
                    .addConstraintViolation()
                    .disableDefaultConstraintViolation()
            }
        }

        return valid
    }
}