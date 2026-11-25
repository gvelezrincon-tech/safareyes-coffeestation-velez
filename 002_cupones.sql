-- migración de la tabla cupones para una bd que ya existía (endpoint 13).
-- se lanza en la base postgres (esquema coffestation). una bd nueva no la necesita: schema.sql ya lo trae.

-- 1. los tipos pasan a mayúsculas para que cuadren con el enum TipoCupon de java
--    ('porcentaje' -> 'PORCENTAJE', 'importe' -> 'IMPORTE_FIJO').
--    primero se quita el check viejo, si no el update no deja poner los valores nuevos
alter table coffestation.cupones drop constraint cupones_tipo_check;
update coffestation.cupones
    set tipo = case tipo when 'porcentaje' then 'PORCENTAJE' else 'IMPORTE_FIJO' end;
alter table coffestation.cupones add check (tipo in ('PORCENTAJE', 'IMPORTE_FIJO'));

-- 2. RN-06: un cupón puede desactivarse. los que ya había quedan activos
alter table coffestation.cupones add column activo boolean not null default true;

-- 3. el cliente pasa a ser opcional: sin cliente = cupón para cualquiera, con cliente = personal
alter table coffestation.cupones alter column cliente_id drop not null;

-- 4. checks que faltaban
alter table coffestation.cupones add check (valor > 0);
alter table coffestation.cupones add check (max_usos >= 1);
alter table coffestation.cupones add check (fecha_fin > fecha_inicio);
