package com.gestioncomercial.backend.controller;

import com.gestioncomercial.backend.entity.Servicio;
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
    public ResponseEntity<List<Servicio>> listarServicios() {
        List<Servicio> servicios = servicioService.listarServicios();

        return ResponseEntity.ok(servicios);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Servicio> buscarServicioPorId( @PathVariable Long id) {

        Servicio servicio = servicioService.buscarServicioPorId(id);

        return ResponseEntity.ok(servicio);
    }

    @PostMapping
    public ResponseEntity<Servicio> guardarServicio( @Valid @RequestBody Servicio servicio) {

        Servicio servicioGuardado = servicioService.guardarServicio(servicio);

        return ResponseEntity.status(HttpStatus.CREATED).body(servicioGuardado);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Servicio> actualizarServicio( @PathVariable Long id, @Valid @RequestBody Servicio servicio) {

        Servicio servicioActualizado = servicioService.actualizarServicio(id, servicio);

        return ResponseEntity.ok(servicioActualizado);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarServicio( @PathVariable Long id) {

        servicioService.eliminarServicio(id);

        return ResponseEntity.noContent().build();
    }
}
