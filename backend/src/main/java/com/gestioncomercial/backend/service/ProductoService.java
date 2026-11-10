package com.gestioncomercial.backend.service;

import com.gestioncomercial.backend.entity.Producto;
import com.gestioncomercial.backend.exception.RecursoNoEncontradoException;
import com.gestioncomercial.backend.repository.ProductoRepository;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.Set;

@Service
public class ProductoService {

    private final ProductoRepository productoRepository;

    public ProductoService(ProductoRepository productoRepository) {
        this.productoRepository = productoRepository;
    }

    public List<Producto> listarProductos() {
        return productoRepository.findAll();
    }

    public Page<Producto> buscarProductos(String texto, Boolean activo, int pagina, int tamano,
                                         String orden, String direccion) {
        var solicitud = ConsultaPaginada.pagina(pagina, tamano, orden, direccion,
                Set.of("id", "nombre", "precioReferencial"));
        return productoRepository.buscar(ConsultaPaginada.patron(texto), activo, solicitud);
    }

    public Producto buscarProductoPorId(Long id) {
        return productoRepository.findById(id)
                .orElseThrow(() ->
                        new RecursoNoEncontradoException(
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
