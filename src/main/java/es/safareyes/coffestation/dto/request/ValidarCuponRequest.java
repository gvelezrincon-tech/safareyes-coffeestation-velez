package es.safareyes.coffestation.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

// cuerpo de POST /cupones/validar: ¿se puede usar este cupón con este importe?
// por ejemplo: { "codigo": "BIENVENIDA10", "subtotal": 12.50, "email": "ana.lopez@correo.es" }

public record ValidarCuponRequest(

        @NotBlank(message = "El código del cupón es obligatorio")
        String codigo,

        @NotNull(message = "El subtotal es obligatorio")
        @DecimalMin(value = "0.00", message = "El subtotal no puede ser negativo")
        BigDecimal subtotal,

        // opcional: email del cliente que quiere usar el cupón. solo hace falta si el cupón es
        // personal (RN-06). en la fase 4, si quien valida es un CLIENTE, saldrá de su token
        @Email(message = "El email no tiene un formato válido")
        String email

) {
}
