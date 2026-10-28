package com.gestioncomercial.backend.controller;

import com.gestioncomercial.backend.entity.OportunidadComercial;
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
    public ResponseEntity<List<OportunidadComercial>> listarOportunidades() {

        List<OportunidadComercial> oportunidades = oportunidadService.listarOportunidades();

        return ResponseEntity.ok(oportunidades);
    }

    @GetMapping("/{id}")
    public ResponseEntity<OportunidadComercial> buscarOportunidadPorId( @PathVariable Long id) {

        OportunidadComercial oportunidad = oportunidadService.buscarOportunidadPorId(id);

        return ResponseEntity.ok(oportunidad);
    }

    @PostMapping
    public ResponseEntity<OportunidadComercial> guardarOportunidad(
            @Valid @RequestBody OportunidadComercial oportunidad) {

        OportunidadComercial oportunidadGuardada = oportunidadService.guardarOportunidad(oportunidad);

        return ResponseEntity.status(HttpStatus.CREATED).body(oportunidadGuardada);
    }

    @PutMapping("/{id}")
    public ResponseEntity<OportunidadComercial> actualizarOportunidad(
            @PathVariable Long id, @Valid @RequestBody OportunidadComercial oportunidad) {

        OportunidadComercial oportunidadActualizada = oportunidadService.actualizarOportunidadComercial(id,oportunidad);

        return ResponseEntity.ok(oportunidadActualizada);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarOportunidad( @PathVariable Long id) {

        oportunidadService.eliminarOportunidad(id);

        return ResponseEntity.noContent().build();
    }
}
