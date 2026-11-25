package es.safareyes.coffestation.exception;

// se lanza cuando se intenta crear algo que ya existe (por ejemplo un cupón con un código repetido).
// GlobalExceptionHandler la pilla y la convierte en un 409.
public class RecursoDuplicadoException extends RuntimeException {

    public RecursoDuplicadoException(String mensaje) {
        super(mensaje);
    }
}
