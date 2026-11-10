package com.gestioncomercial.backend.controller;

import com.gestioncomercial.backend.entity.DetalleCotizacion;
import com.gestioncomercial.backend.dto.CotizacionMapper;
import com.gestioncomercial.backend.dto.DetalleCotizacionRequestDTO;
import com.gestioncomercial.backend.dto.DetalleCotizacionResponseDTO;
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
    public ResponseEntity<List<DetalleCotizacionResponseDTO>> listarDetalles() {

        List<DetalleCotizacion> detalles = detalleCotizacionService.listarDetalles();

        return ResponseEntity.ok(detalles.stream().map(CotizacionMapper::toResponse).toList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<DetalleCotizacionResponseDTO> buscarDetallePorId( @PathVariable Long id) {

        DetalleCotizacion detalle = detalleCotizacionService.buscarDetallePorId(id);

        return ResponseEntity.ok(CotizacionMapper.toResponse(detalle));
    }

    @PostMapping
    public ResponseEntity<DetalleCotizacionResponseDTO> guardarDetalle( @Valid @RequestBody DetalleCotizacionRequestDTO detalle) {

        DetalleCotizacion detalleGuardado = detalleCotizacionService.guardarDetalle(CotizacionMapper.toEntity(detalle));

        return ResponseEntity.status(HttpStatus.CREATED).body(CotizacionMapper.toResponse(detalleGuardado));
    }

    @PutMapping("/{id}")
    public ResponseEntity<DetalleCotizacionResponseDTO> actualizarDetalle(
            @PathVariable Long id, @Valid @RequestBody DetalleCotizacionRequestDTO detalle) {

        DetalleCotizacion detalleActualizado = detalleCotizacionService.actualizarDetalleCotizacion(id, CotizacionMapper.toEntity(detalle));

        return ResponseEntity.ok(CotizacionMapper.toResponse(detalleActualizado));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarDetalle( @PathVariable Long id) {

        detalleCotizacionService.eliminarDetalle(id);

        return ResponseEntity.noContent().build();
    }

    @GetMapping("/cotizacion/{codigo}")
    public ResponseEntity<List<DetalleCotizacionResponseDTO>> listarPorCodigoCotizacion(@PathVariable String codigo) {

        return ResponseEntity.ok(detalleCotizacionService.listarPorCodigoCotizacion(codigo)
                .stream().map(CotizacionMapper::toResponse).toList());
    }
}
