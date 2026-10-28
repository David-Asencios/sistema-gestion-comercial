package com.gestioncomercial.backend.service;

import com.gestioncomercial.backend.entity.Producto;
import com.gestioncomercial.backend.repository.ProductoRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProductoService {

    private final ProductoRepository productoRepository;

    public ProductoService(ProductoRepository productoRepository) {
        this.productoRepository = productoRepository;
    }

    public List<Producto> listarProductos() {
        return productoRepository.findAll();
    }

    public Producto buscarProductoPorId(Long id) {
        return productoRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Producto no encontrado con ID: " + id
                        )
                );
    }

    public Producto guardarProducto(Producto producto) {
        return productoRepository.save(producto);
    }

    public Producto actualizarProducto(Long id, Producto producto){

        Producto productoExistente = buscarProductoPorId(id);

        productoExistente.setNombre(producto.getNombre());
        productoExistente.setDescripcion(producto.getDescripcion());
        productoExistente.setPrecioReferencial(producto.getPrecioReferencial());
        productoExistente.setActivo(producto.getActivo());

        return productoRepository.save(productoExistente);

    }

    public void eliminarProducto(Long id) {
        Producto producto = buscarProductoPorId(id);
        productoRepository.delete(producto);
    }
}
