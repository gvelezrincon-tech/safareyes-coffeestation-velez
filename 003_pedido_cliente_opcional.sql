-- migración de la tabla pedido para una bd que ya existía (endpoint 9).
-- se lanza en la base postgres (esquema coffestation). una bd nueva no la necesita: schema.sql ya lo trae.

-- el cliente del pedido pasa a ser opcional: un pedido de barra lo puede tomar el barista
-- sin que el cliente esté registrado (el enunciado dice que pedido tiene cliente opcional)
alter table coffestation.pedido alter column cliente_id drop not null;
