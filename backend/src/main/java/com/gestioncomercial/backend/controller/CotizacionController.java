package com.gestioncomercial.backend.controller;

import com.gestioncomercial.backend.entity.Cotizacion;
import com.gestioncomercial.backend.dto.CotizacionMapper;
import com.gestioncomercial.backend.dto.CotizacionRequestDTO;
import com.gestioncomercial.backend.dto.CotizacionResponseDTO;
import com.gestioncomercial.backend.dto.PaginaResponseDTO;
import com.gestioncomercial.backend.entity.EstadoCotizacion;
import com.gestioncomercial.backend.service.CotizacionPdfService;
import com.gestioncomercial.backend.service.CotizacionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.format.annotation.DateTimeFormat;

import java.util.List;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/cotizaciones")
public class CotizacionController {

    private final CotizacionService cotizacionService;
    private final CotizacionPdfService cotizacionPdfService;

    public CotizacionController(CotizacionService cotizacionService, CotizacionPdfService cotizacionPdfService) {

        this.cotizacionService = cotizacionService;
        this.cotizacionPdfService = cotizacionPdfService;
    }

    @GetMapping
    public ResponseEntity<List<CotizacionResponseDTO>> listarCotizaciones() {

        List<Cotizacion> cotizaciones = cotizacionService.listarCotizaciones();

        return ResponseEntity.ok(cotizaciones.stream().map(CotizacionMapper::toResponse).toList());
    }

    @GetMapping("/buscar")
    public ResponseEntity<PaginaResponseDTO<CotizacionResponseDTO>> buscarCotizaciones(
            @RequestParam(required = false) String codigo,
            @RequestParam(required = false) Long clienteId,
            @RequestParam(required = false) Long oportunidadId,
            @RequestParam(required = false) EstadoCotizacion estado,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaDesde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaHasta,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "20") int tamano,
            @RequestParam(defaultValue = "id") String orden,
            @RequestParam(defaultValue = "asc") String direccion) {
        var resultado = cotizacionService.buscarCotizaciones(codigo, clienteId, oportunidadId,
                estado, fechaDesde, fechaHasta, pagina, tamano, orden, direccion)
                .map(CotizacionMapper::toResponse);
        return ResponseEntity.ok(PaginaResponseDTO.desde(resultado));
    }

    @GetMapping("/codigo/{codigo}")
    public ResponseEntity<CotizacionResponseDTO> buscarPorCodigo(@PathVariable String codigo) {

        return ResponseEntity.ok(CotizacionMapper.toResponse(cotizacionService.buscarPorCodigo(codigo)));

    }

    @GetMapping("/{id}")
    public ResponseEntity<CotizacionResponseDTO> buscarCotizacionPorId( @PathVariable Long id) {

        Cotizacion cotizacion = cotizacionService.buscarCotizacionPorId(id);

        return ResponseEntity.ok(CotizacionMapper.toResponse(cotizacion));
    }

    @PostMapping
    public ResponseEntity<CotizacionResponseDTO> guardarCotizacion( @Valid @RequestBody CotizacionRequestDTO cotizacion) {

        Cotizacion cotizacionGuardada = cotizacionService.guardarCotizacion(CotizacionMapper.toEntity(cotizacion));

        return ResponseEntity.status(HttpStatus.CREATED).body(CotizacionMapper.toResponse(cotizacionGuardada));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CotizacionResponseDTO> actualizarCotizacion(
            @PathVariable Long id, @Valid @RequestBody CotizacionRequestDTO cotizacion) {

        Cotizacion cotizacionActualizada = cotizacionService.actualizarCotizacion(id,CotizacionMapper.toEntity(cotizacion));

        return ResponseEntity.ok(CotizacionMapper.toResponse(cotizacionActualizada));
    }

    @PatchMapping("/{id}/estado")
    public ResponseEntity<CotizacionResponseDTO> actualizarEstadoCotizacion(
            @PathVariable Long id,
            @RequestBody EstadoCotizacion nuevoEstado) {

        Cotizacion cotizacionActualizada =
                cotizacionService.actualizarEstadoCotizacion(id, nuevoEstado);

        return ResponseEntity.ok(CotizacionMapper.toResponse(cotizacionActualizada));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarCotizacion( @PathVariable Long id) {

        cotizacionService.eliminarCotizacion(id);

        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{codigo}/pdf")
    public ResponseEntity<byte[]> descargarPdf(@PathVariable String codigo) {

        byte[] pdf = cotizacionPdfService.generarPdf(codigo);

        HttpHeaders headers = new HttpHeaders();

        headers.setContentType(MediaType.APPLICATION_PDF);

        headers.setContentDisposition(org.springframework.http.ContentDisposition
                .attachment().filename(codigo + ".pdf").build());

        headers.setContentLength(pdf.length);

        return new ResponseEntity<>(
                pdf,
                headers,
                HttpStatus.OK
        );
    }

}
