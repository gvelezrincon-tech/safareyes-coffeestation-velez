package es.safareyes.coffestation.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

// la comprobación de verdad de @CodigoCupon
public class CodigoCuponValidator implements ConstraintValidator<CodigoCupon, String> {

    // letras y números, y como mucho un guion entre dos trozos (CINE-4F7K2Q sí, CINE--4F no)
    private static final String FORMATO = "[A-Za-z0-9]+(-[A-Za-z0-9]+)*";

    @Override
    public boolean isValid(String codigo, ConstraintValidatorContext context) {
        // si viene vacío no decimos nada: de eso ya se encarga @NotBlank (así no salen dos errores)
        if (codigo == null || codigo.isBlank()) {
            return true;
        }
        return codigo.length() >= 4 && codigo.length() <= 20 && codigo.matches(FORMATO);
    }
}
