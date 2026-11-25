package es.safareyes.coffestation.Modelo;

// estados de un pedido (RN-08). en la bd se guarda el nombre tal cual ('PENDIENTE', 'LISTO'...)
// con  @Enumerated(EnumType.STRING) en Pedido.
//
// camino normal: PENDIENTE -> EN_PREPARACION -> LISTO -> ENTREGADO
// y desde PENDIENTE también se puede ir a CANCELADO
public enum EstadoPedido {
    PENDIENTE,
    EN_PREPARACION,
    LISTO,
    ENTREGADO,
    CANCELADO;

    // RN-08: ¿se puede pasar de este estado al nuevo?
    // cada estado solo tiene un siguiente posible (PENDIENTE tiene dos).
    // ENTREGADO y CANCELADO son finales: de ahí ya no se mueve el pedido
    public boolean puedeCambiarA(EstadoPedido nuevo) {
        return switch (this) {
            case PENDIENTE -> nuevo == EN_PREPARACION || nuevo == CANCELADO;
            case EN_PREPARACION -> nuevo == LISTO;
            case LISTO -> nuevo == ENTREGADO;
            case ENTREGADO, CANCELADO -> false;
        };
    }
}
