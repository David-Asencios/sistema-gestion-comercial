package com.gestioncomercial.backend.controller;

import com.gestioncomercial.backend.entity.DetalleCotizacion;
import com.gestioncomercial.backend.service.DetalleCotizacionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/detalles-cotizacion")
public class DetalleCotizacionController {

    private final DetalleCotizacionService detalleCotizacionService;

    public DetalleCotizacionController(DetalleCotizacionService detalleCotizacionService) {
        this.detalleCotizacionService = detalleCotizacionService;
    }

    @GetMapping
    public ResponseEntity<List<DetalleCotizacion>> listarDetalles() {

        List<DetalleCotizacion> detalles = detalleCotizacionService.listarDetalles();

        return ResponseEntity.ok(detalles);
    }

    @GetMapping("/{id}")
    public ResponseEntity<DetalleCotizacion> buscarDetallePorId( @PathVariable Long id) {

        DetalleCotizacion detalle = detalleCotizacionService.buscarDetallePorId(id);

        return ResponseEntity.ok(detalle);
    }

    @PostMapping
    public ResponseEntity<DetalleCotizacion> guardarDetalle( @Valid @RequestBody DetalleCotizacion detalle) {

        DetalleCotizacion detalleGuardado = detalleCotizacionService.guardarDetalle(detalle);

        return ResponseEntity.status(HttpStatus.CREATED).body(detalleGuardado);
    }

    @PutMapping("/{id}")
    public ResponseEntity<DetalleCotizacion> actualizarDetalle(
            @PathVariable Long id, @Valid @RequestBody DetalleCotizacion detalle) {

        DetalleCotizacion detalleActualizado = detalleCotizacionService.actualizarDetalleCotizacion(id, detalle);

        return ResponseEntity.ok(detalleActualizado);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarDetalle( @PathVariable Long id) {

        detalleCotizacionService.eliminarDetalle(id);

        return ResponseEntity.noContent().build();
    }
}
