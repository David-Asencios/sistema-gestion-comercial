package com.gestioncomercial.backend.controller;

import com.gestioncomercial.backend.entity.Cotizacion;
import com.gestioncomercial.backend.entity.EstadoCotizacion;
import com.gestioncomercial.backend.service.CotizacionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/cotizaciones")
public class CotizacionController {

    private final CotizacionService cotizacionService;

    public CotizacionController(CotizacionService cotizacionService) {
        this.cotizacionService = cotizacionService;
    }

    @GetMapping
    public ResponseEntity<List<Cotizacion>> listarCotizaciones() {

        List<Cotizacion> cotizaciones = cotizacionService.listarCotizaciones();

        return ResponseEntity.ok(cotizaciones);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Cotizacion> buscarCotizacionPorId( @PathVariable Long id) {

        Cotizacion cotizacion = cotizacionService.buscarCotizacionPorId(id);

        return ResponseEntity.ok(cotizacion);
    }

    @PostMapping
    public ResponseEntity<Cotizacion> guardarCotizacion( @Valid @RequestBody Cotizacion cotizacion) {

        Cotizacion cotizacionGuardada = cotizacionService.guardarCotizacion(cotizacion);

        return ResponseEntity.status(HttpStatus.CREATED).body(cotizacionGuardada);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Cotizacion> actualizarCotizacion(
            @PathVariable Long id, @Valid @RequestBody Cotizacion cotizacion) {

        Cotizacion cotizacionActualizada = cotizacionService.actualizarCotizacion(id,cotizacion);

        return ResponseEntity.ok(cotizacionActualizada);
    }

    @PatchMapping("/{id}/estado")
    public ResponseEntity<Cotizacion> actualizarEstadoCotizacion(
            @PathVariable Long id,
            @RequestBody EstadoCotizacion nuevoEstado) {

        Cotizacion cotizacionActualizada =
                cotizacionService.actualizarEstadoCotizacion(id, nuevoEstado);

        return ResponseEntity.ok(cotizacionActualizada);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarCotizacion( @PathVariable Long id) {

        cotizacionService.eliminarCotizacion(id);

        return ResponseEntity.noContent().build();
    }
}
