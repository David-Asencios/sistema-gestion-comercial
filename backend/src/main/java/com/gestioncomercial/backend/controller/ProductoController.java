package com.gestioncomercial.backend.controller;

import com.gestioncomercial.backend.dto.ProductoMapper;
import com.gestioncomercial.backend.dto.ProductoRequestDTO;
import com.gestioncomercial.backend.dto.ProductoResponseDTO;
import com.gestioncomercial.backend.dto.PaginaResponseDTO;
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
    public ResponseEntity<List<ProductoResponseDTO>> listarProductos() {
        List<ProductoResponseDTO> productos = productoService.listarProductos()
                .stream().map(ProductoMapper::toResponse).toList();

        return ResponseEntity.ok(productos);
    }

    @GetMapping("/buscar")
    public ResponseEntity<PaginaResponseDTO<ProductoResponseDTO>> buscarProductos(
            @RequestParam(required = false) String texto,
            @RequestParam(required = false) Boolean activo,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "20") int tamano,
            @RequestParam(defaultValue = "id") String orden,
            @RequestParam(defaultValue = "asc") String direccion) {
        var resultado = productoService.buscarProductos(texto, activo, pagina, tamano, orden, direccion)
                .map(ProductoMapper::toResponse);
        return ResponseEntity.ok(PaginaResponseDTO.desde(resultado));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductoResponseDTO> buscarProductoPorId( @PathVariable Long id) {

        ProductoResponseDTO producto = ProductoMapper.toResponse(productoService.buscarProductoPorId(id));

        return ResponseEntity.ok(producto);
    }

    @PostMapping
    public ResponseEntity<ProductoResponseDTO> guardarProducto( @Valid @RequestBody ProductoRequestDTO producto) {

        ProductoResponseDTO productoGuardado = ProductoMapper.toResponse(
                productoService.guardarProducto(ProductoMapper.toEntity(producto)));

        return ResponseEntity.status(HttpStatus.CREATED).body(productoGuardado);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProductoResponseDTO> actualizarProducto(
            @PathVariable Long id, @Valid @RequestBody ProductoRequestDTO producto) {

        ProductoResponseDTO productoActualizado = ProductoMapper.toResponse(
                productoService.actualizarProducto(id, ProductoMapper.toEntity(producto)));

        return ResponseEntity.ok(productoActualizado);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarProducto( @PathVariable Long id) {

        productoService.eliminarProducto(id);

        return ResponseEntity.noContent().build();
    }
}
