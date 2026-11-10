package com.gestioncomercial.backend.dto;

import com.gestioncomercial.backend.entity.*;

/** Conversión del contrato HTTP; los cálculos y las reglas permanecen en los servicios. */
public final class CotizacionMapper {
    private CotizacionMapper() {
    }

    public static Cotizacion toEntity(CotizacionRequestDTO request) {
        Cotizacion cotizacion = new Cotizacion();
        OportunidadComercial oportunidad = new OportunidadComercial();
        oportunidad.setId(request.oportunidad().id());
        cotizacion.setOportunidad(oportunidad);
        cotizacion.setFechaEmision(request.fechaEmision());
        cotizacion.setVigenciaDias(request.vigenciaDias());
        cotizacion.setAlcanceServicio(request.alcanceServicio());
        return cotizacion;
    }

    public static DetalleCotizacion toEntity(DetalleCotizacionRequestDTO request) {
        DetalleCotizacion detalle = new DetalleCotizacion();
        Cotizacion cotizacion = new Cotizacion();
        cotizacion.setId(request.cotizacion().id());
        detalle.setCotizacion(cotizacion);
        if (request.servicio() != null) {
            Servicio servicio = new Servicio();
            servicio.setId(request.servicio().id());
            detalle.setServicio(servicio);
        }
        if (request.producto() != null) {
            Producto producto = new Producto();
            producto.setId(request.producto().id());
            detalle.setProducto(producto);
        }
        detalle.setDescripcion(request.descripcion());
        detalle.setCantidad(request.cantidad());
        detalle.setPrecioUnitario(request.precioUnitario());
        detalle.setTipoDescuento(request.tipoDescuento());
        detalle.setValorDescuento(request.valorDescuento());
        return detalle;
    }

    public static CotizacionResponseDTO toResponse(Cotizacion cotizacion) {
        OportunidadComercial oportunidad = cotizacion.getOportunidad();
        Cliente cliente = oportunidad.getCliente();
        var clienteDTO = new CotizacionResponseDTO.ClienteDTO(
                cliente.getId(), cliente.getRuc(), cliente.getRazonSocial(),
                cliente.getNombreContacto(), cliente.getTelefono(), cliente.getCorreo(),
                cliente.getDireccion(), cliente.getActivo());
        var oportunidadDTO = new CotizacionResponseDTO.OportunidadDTO(
                oportunidad.getId(), oportunidad.getTitulo(), oportunidad.getDescripcion(),
                oportunidad.getCanalOrigen(), oportunidad.getEtapa(), oportunidad.getFechaCreacion(),
                oportunidad.getObservaciones(), clienteDTO);
        return new CotizacionResponseDTO(
                cotizacion.getId(), cotizacion.getCodigo(), cotizacion.getFechaEmision(),
                cotizacion.getFechaVencimiento(), cotizacion.getVigenciaDias(),
                cotizacion.getAlcanceServicio(), cotizacion.getEstado(), cotizacion.getSubtotal(),
                cotizacion.getIgv(), cotizacion.getTotal(), oportunidadDTO);
    }

    public static DetalleCotizacionResponseDTO toResponse(DetalleCotizacion detalle) {
        Servicio servicio = detalle.getServicio();
        Producto producto = detalle.getProducto();
        var servicioDTO = servicio == null ? null : new DetalleCotizacionResponseDTO.ConceptoDTO(
                servicio.getId(), servicio.getNombre(), servicio.getDescripcion(),
                servicio.getPrecioReferencial(), servicio.getActivo());
        var productoDTO = producto == null ? null : new DetalleCotizacionResponseDTO.ConceptoDTO(
                producto.getId(), producto.getNombre(), producto.getDescripcion(),
                producto.getPrecioReferencial(), producto.getActivo());
        return new DetalleCotizacionResponseDTO(
                detalle.getId(), detalle.getDescripcion(), detalle.getCantidad(),
                detalle.getPrecioUnitario(), detalle.getTipoDescuento(), detalle.getValorDescuento(),
                detalle.getDescuentoUnitario(), detalle.getDescuentoTotal(), detalle.getSubtotal(),
                toResponse(detalle.getCotizacion()), servicioDTO, productoDTO);
    }
}
