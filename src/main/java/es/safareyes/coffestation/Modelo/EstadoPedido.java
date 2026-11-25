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
    CANCELADO
}
