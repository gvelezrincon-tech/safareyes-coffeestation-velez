package es.safareyes.coffestation.Modelo;

// tipos de cupón. en la bd se guarda el nombre tal cual gracias a @Enumerated(EnumType.STRING)
//   PORCENTAJE   -> valor 10 = un 10 % de descuento
//   IMPORTE_FIJO -> valor 1  = 1 euro de descuento
public enum TipoCupon {
    PORCENTAJE,
    IMPORTE_FIJO
}
