package com.gestioncomercial.backend.controller;

import com.gestioncomercial.backend.entity.Producto;
import com.gestioncomercial.backend.service.ProductoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/productos")
public class ProductoController {

    private final ProductoService productoService;

    public ProductoController(ProductoService productoService) {
        this.productoService = productoService;
    }

    @GetMapping
    public ResponseEntity<List<Producto>> listarProductos() {
        List<Producto> productos = productoService.listarProductos();

        return ResponseEntity.ok(productos);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Producto> buscarProductoPorId( @PathVariable Long id) {

        Producto producto = productoService.buscarProductoPorId(id);

        return ResponseEntity.ok(producto);
    }

    @PostMapping
    public ResponseEntity<Producto> guardarProducto( @Valid @RequestBody Producto producto) {

        Producto productoGuardado = productoService.guardarProducto(producto);

        return ResponseEntity.status(HttpStatus.CREATED).body(productoGuardado);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Producto> actualizarProducto(
            @PathVariable Long id, @Valid @RequestBody Producto producto) {

        Producto productoActualizado = productoService.actualizarProducto(id, producto);

        return ResponseEntity.ok(productoActualizado);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarProducto( @PathVariable Long id) {

        productoService.eliminarProducto(id);

        return ResponseEntity.noContent().build();
    }
}
