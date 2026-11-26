package es.safareyes.coffestation.Modelo;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "pedido", schema = "coffestation")
@Getter@Setter@NoArgsConstructor@AllArgsConstructor


public class Pedido {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column (name = "numero_turno")
    private Long numeroTurno;

    @Column (name = "fecha_hora")
    private LocalDateTime fechaHora;

    @Column (name = "base")
    private BigDecimal base;

    @Column (name = "iva")
    private BigDecimal iva;

    // STRING: en la bd se guarda el texto ('ENTREGADO') y no el número de posición del enum,
    // así si algún día se reordena el enum no se lían los pedidos que ya hay
    @Enumerated(EnumType.STRING)
    @Column (name = "estado")
    private EstadoPedido estado;

    // lado con la FK (Pedido → Cliente)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id")
    private Cliente cliente;

    // cascade PERSIST: al guardar un pedido nuevo (endpoint 9) se guardan también sus líneas
    // y su cupón, sin tener que hacer un save() de cada cosa
    // lado inverso (Pedido → sus líneas en ProductoPedido)
    @OneToMany(mappedBy = "pedido", fetch = FetchType.LAZY, cascade = CascadeType.PERSIST)
    private Set<ProductoPedido> lineas = new HashSet<>();

    // lado inverso (Pedido → sus cupones en PedidoCupones)
    @OneToMany(mappedBy = "pedido", fetch = FetchType.LAZY, cascade = CascadeType.PERSIST)
    private Set<PedidoCupones> cupones = new HashSet<>();

    // lado inverso (Pedido → sus Descuentos)
    @OneToMany(mappedBy = "pedido", fetch = FetchType.LAZY)
    private Set<Descuento> descuentos = new HashSet<>();





}
