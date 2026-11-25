package es.safareyes.coffestation.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

// validador propio para el código de un cupón (lo pide el enunciado: al menos un validador propio).
// se usa igual que @NotBlank: se pone encima del campo del dto y Spring lo comprueba con @Valid.
//
// un código válido: de 4 a 20 caracteres, solo letras, números y guiones sueltos entre medias.
//   bien: CAFE1EURO, CINE-4F7K2Q, otono15 (las minúsculas se pasan luego a mayúsculas)
//   mal:  AB (corto), CAFÉ 1 (tilde y espacio), -CAFE, CAFE--1
//
// @Constraint dice qué clase hace la comprobación de verdad: CodigoCuponValidator
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = CodigoCuponValidator.class)
public @interface CodigoCupon {

    // mensaje que sale en el 400 si el código no vale
    String message() default "El código tiene que tener de 4 a 20 caracteres: letras, números y guiones (p. ej. CAFE-10)";

    // estos dos son obligatorios en cualquier validador aunque no los usemos
    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
