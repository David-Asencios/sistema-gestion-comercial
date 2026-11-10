package com.gestioncomercial.backend.controller;

import com.gestioncomercial.backend.dto.OportunidadComercialMapper;
import com.gestioncomercial.backend.dto.OportunidadComercialRequestDTO;
import com.gestioncomercial.backend.dto.OportunidadComercialResponseDTO;
import com.gestioncomercial.backend.dto.PaginaResponseDTO;
import com.gestioncomercial.backend.entity.CanalOrigen;
import com.gestioncomercial.backend.entity.EtapaOportunidad;
import com.gestioncomercial.backend.service.OportunidadComercialService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/oportunidades")
public class OportunidadComercialController {

    private final OportunidadComercialService oportunidadService;

    public OportunidadComercialController( OportunidadComercialService oportunidadService) {
        this.oportunidadService = oportunidadService;
    }

    @GetMapping
    public ResponseEntity<List<OportunidadComercialResponseDTO>> listarOportunidades() {

        List<OportunidadComercialResponseDTO> oportunidades = oportunidadService.listarOportunidades()
                .stream().map(OportunidadComercialMapper::toResponse).toList();

        return ResponseEntity.ok(oportunidades);
    }

    @GetMapping("/buscar")
    public ResponseEntity<PaginaResponseDTO<OportunidadComercialResponseDTO>> buscarOportunidades(
            @RequestParam(required = false) String texto,
            @RequestParam(required = false) Long clienteId,
            @RequestParam(required = false) EtapaOportunidad etapa,
            @RequestParam(required = false) CanalOrigen canalOrigen,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "20") int tamano,
            @RequestParam(defaultValue = "id") String orden,
            @RequestParam(defaultValue = "asc") String direccion) {
        var resultado = oportunidadService.buscarOportunidades(
                texto, clienteId, etapa, canalOrigen, pagina, tamano, orden, direccion)
                .map(OportunidadComercialMapper::toResponse);
        return ResponseEntity.ok(PaginaResponseDTO.desde(resultado));
    }

    @GetMapping("/{id}")
    public ResponseEntity<OportunidadComercialResponseDTO> buscarOportunidadPorId( @PathVariable Long id) {

        OportunidadComercialResponseDTO oportunidad = OportunidadComercialMapper.toResponse(
                oportunidadService.buscarOportunidadPorId(id));

        return ResponseEntity.ok(oportunidad);
    }

    @PostMapping
    public ResponseEntity<OportunidadComercialResponseDTO> guardarOportunidad(
            @Valid @RequestBody OportunidadComercialRequestDTO oportunidad) {

        OportunidadComercialResponseDTO oportunidadGuardada = OportunidadComercialMapper.toResponse(
                oportunidadService.guardarOportunidad(OportunidadComercialMapper.toEntity(oportunidad)));

        return ResponseEntity.status(HttpStatus.CREATED).body(oportunidadGuardada);
    }

    @PutMapping("/{id}")
    public ResponseEntity<OportunidadComercialResponseDTO> actualizarOportunidad(
            @PathVariable Long id, @Valid @RequestBody OportunidadComercialRequestDTO oportunidad) {

        OportunidadComercialResponseDTO oportunidadActualizada = OportunidadComercialMapper.toResponse(
                oportunidadService.actualizarOportunidadComercial(id, OportunidadComercialMapper.toEntity(oportunidad)));

        return ResponseEntity.ok(oportunidadActualizada);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarOportunidad( @PathVariable Long id) {

        oportunidadService.eliminarOportunidad(id);

        return ResponseEntity.noContent().build();
    }
}
