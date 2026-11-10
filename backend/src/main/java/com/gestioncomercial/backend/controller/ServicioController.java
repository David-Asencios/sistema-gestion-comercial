package com.gestioncomercial.backend.controller;

import com.gestioncomercial.backend.dto.ServicioMapper;
import com.gestioncomercial.backend.dto.ServicioRequestDTO;
import com.gestioncomercial.backend.dto.ServicioResponseDTO;
import com.gestioncomercial.backend.dto.PaginaResponseDTO;
import com.gestioncomercial.backend.service.ServicioService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/servicios")
public class ServicioController {

    private final ServicioService servicioService;

    public ServicioController(ServicioService servicioService) {
        this.servicioService = servicioService;
    }

    @GetMapping
    public ResponseEntity<List<ServicioResponseDTO>> listarServicios() {
        List<ServicioResponseDTO> servicios = servicioService.listarServicios()
                .stream().map(ServicioMapper::toResponse).toList();

        return ResponseEntity.ok(servicios);
    }

    @GetMapping("/buscar")
    public ResponseEntity<PaginaResponseDTO<ServicioResponseDTO>> buscarServicios(
            @RequestParam(required = false) String texto,
            @RequestParam(required = false) Boolean activo,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "20") int tamano,
            @RequestParam(defaultValue = "id") String orden,
            @RequestParam(defaultValue = "asc") String direccion) {
        var resultado = servicioService.buscarServicios(texto, activo, pagina, tamano, orden, direccion)
                .map(ServicioMapper::toResponse);
        return ResponseEntity.ok(PaginaResponseDTO.desde(resultado));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ServicioResponseDTO> buscarServicioPorId( @PathVariable Long id) {

        ServicioResponseDTO servicio = ServicioMapper.toResponse(servicioService.buscarServicioPorId(id));

        return ResponseEntity.ok(servicio);
    }

    @PostMapping
    public ResponseEntity<ServicioResponseDTO> guardarServicio( @Valid @RequestBody ServicioRequestDTO servicio) {

        ServicioResponseDTO servicioGuardado = ServicioMapper.toResponse(
                servicioService.guardarServicio(ServicioMapper.toEntity(servicio)));

        return ResponseEntity.status(HttpStatus.CREATED).body(servicioGuardado);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ServicioResponseDTO> actualizarServicio( @PathVariable Long id, @Valid @RequestBody ServicioRequestDTO servicio) {

        ServicioResponseDTO servicioActualizado = ServicioMapper.toResponse(
                servicioService.actualizarServicio(id, ServicioMapper.toEntity(servicio)));

        return ResponseEntity.ok(servicioActualizado);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarServicio( @PathVariable Long id) {

        servicioService.eliminarServicio(id);

        return ResponseEntity.noContent().build();
    }
}
