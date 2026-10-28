package com.gestioncomercial.backend.service;

import com.gestioncomercial.backend.entity.Cliente;
import com.gestioncomercial.backend.repository.ClienteRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ClienteService {

    private final ClienteRepository clienteRepository;

    public ClienteService(ClienteRepository clienteRepository) {
        this.clienteRepository = clienteRepository;
    }

    public Cliente guardarCliente(Cliente cliente){
        return clienteRepository.save(cliente);
    }

    public List<Cliente> listarClientes(){
        return clienteRepository.findAll();
    }

    public Cliente buscarClientePorId(Long id){
        return clienteRepository.findById(id).orElseThrow(()
                -> new RuntimeException("Cliente no encontrado con ID: " + id));
    }

    public void eliminarCliente(Long id){

        Cliente cliente = buscarClientePorId(id);

        clienteRepository.delete(cliente);

    }

    public Cliente actualizarCliente(Long id, Cliente cliente){

        Cliente actual = buscarClientePorId(id);

        actual.setRuc(cliente.getRuc());
        actual.setRazonSocial(cliente.getRazonSocial());
        actual.setNombreContacto(cliente.getNombreContacto());
        actual.setTelefono(cliente.getTelefono());
        actual.setCorreo(cliente.getCorreo());
        actual.setDireccion(cliente.getDireccion());
        actual.setActivo(cliente.getActivo());

        return clienteRepository.save(actual);

    }


}
