package es.safareyes.coffestation.mapper;

import es.safareyes.coffestation.Modelo.Cupones;
import es.safareyes.coffestation.dto.request.CuponRequest;
import es.safareyes.coffestation.dto.response.CuponResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

// pasa de CuponRequest a la entidad Cupones y de la entidad a CuponResponse.
// el tipo viaja como texto ("PORCENTAJE") en los dto y como enum TipoCupon en la entidad:
// MapStruct hace la conversión solo en los dos sentidos
@Mapper
public interface CuponMapper {

    // id lo pone la bd; cliente y activo los pone el servicio; usos se llena con los pedidos
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "cliente", ignore = true)
    @Mapping(target = "activo", ignore = true)
    @Mapping(target = "usos", ignore = true)
    Cupones toEntity(CuponRequest request);

    // del cliente solo sacamos el email (campo anidado). si el cupón no es personal, sale null
    @Mapping(target = "emailCliente", source = "cliente.email")
    CuponResponse toResponse(Cupones cupon);
}
