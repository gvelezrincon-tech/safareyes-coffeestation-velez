-- migración para una bd que ya existía antes de añadir estas columnas a schema.sql.
-- se lanza en la base postgres (esquema coffestation). una bd nueva no la necesita: schema.sql ya las trae.

-- RN-08: estado del pedido. los pedidos que ya había se quedan en PENDIENTE
alter table coffestation.pedido
    add column estado varchar(20) not null default 'PENDIENTE'
    check (estado in ('PENDIENTE', 'EN_PREPARACION', 'LISTO', 'ENTREGADO', 'CANCELADO'));

-- RN-03: precio del producto en el momento del pedido.
-- primero se crea vacía, se rellena con el precio actual de cada producto y luego se pone not null
alter table coffestation.producto_pedido add column precio_unitario decimal(10,2);
update coffestation.producto_pedido pp set precio_unitario = pr.precio
    from coffestation.producto pr where pr.id = pp.producto_id;
alter table coffestation.producto_pedido alter column precio_unitario set not null;
alter table coffestation.producto_pedido add check (precio_unitario >= 0);
