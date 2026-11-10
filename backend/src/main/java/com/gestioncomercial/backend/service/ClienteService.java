package com.gestioncomercial.backend.service;

import com.gestioncomercial.backend.entity.Cliente;
import com.gestioncomercial.backend.exception.RecursoDuplicadoException;
import com.gestioncomercial.backend.exception.RecursoNoEncontradoException;
import com.gestioncomercial.backend.repository.ClienteRepository;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.Objects;
import java.util.Set;

@Service
public class ClienteService {

    private final ClienteRepository clienteRepository;

    public ClienteService(ClienteRepository clienteRepository) {
        this.clienteRepository = clienteRepository;
    }


    public Cliente guardarCliente(Cliente cliente) {

        cliente.setRuc(normalizarRuc(cliente.getRuc()));
        if (cliente.getRuc() != null && clienteRepository.existsByRuc(cliente.getRuc())) {
            throw new RecursoDuplicadoException(
                    "Ya existe un cliente registrado con el RUC: "
                            + cliente.getRuc()
            );
        }

        return clienteRepository.save(cliente);
    }


    public List<Cliente> listarClientes(){
        return clienteRepository.findAll();
    }

    public Page<Cliente> buscarClientes(String texto, Boolean activo, int pagina, int tamano,
                                       String orden, String direccion) {
        var solicitud = ConsultaPaginada.pagina(pagina, tamano, orden, direccion,
                Set.of("id", "razonSocial", "ruc", "nombreContacto", "activo"));
        return clienteRepository.buscar(ConsultaPaginada.patron(texto), activo, solicitud);
    }

    public Cliente buscarClientePorId(Long id){
        return clienteRepository.findById(id).orElseThrow(()
                -> new RecursoNoEncontradoException("Cliente no encontrado con ID: " + id));
    }

    public void eliminarCliente(Long id){

        Cliente cliente = buscarClientePorId(id);

        clienteRepository.delete(cliente);

    }


    public Cliente actualizarCliente(Long id, Cliente cliente) {

        Cliente actual = buscarClientePorId(id);

        cliente.setRuc(normalizarRuc(cliente.getRuc()));
        if (cliente.getRuc() != null
                && !Objects.equals(actual.getRuc(), cliente.getRuc())
                && clienteRepository.existsByRuc(cliente.getRuc())) {

            throw new RecursoDuplicadoException(
                    "Ya existe otro cliente registrado con el RUC: " + cliente.getRuc()
            );
        }

        actual.setRuc(cliente.getRuc());
        actual.setRazonSocial(cliente.getRazonSocial());
        actual.setNombreContacto(cliente.getNombreContacto());
        actual.setTelefono(cliente.getTelefono());
        actual.setCorreo(cliente.getCorreo());
        actual.setDireccion(cliente.getDireccion());
        actual.setActivo(cliente.getActivo());

        return clienteRepository.save(actual);
    }

    private String normalizarRuc(String ruc) {
        return ruc == null || ruc.isBlank() ? null : ruc.trim();
    }


}
