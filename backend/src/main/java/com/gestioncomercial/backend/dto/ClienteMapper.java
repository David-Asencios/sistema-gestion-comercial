package com.gestioncomercial.backend.dto;

import com.gestioncomercial.backend.entity.Cliente;

public final class ClienteMapper {
    private ClienteMapper() {}

    public static Cliente toEntity(ClienteRequestDTO request) {
        Cliente cliente = new Cliente();
        cliente.setRuc(request.getRuc());
        cliente.setRazonSocial(request.getRazonSocial());
        cliente.setNombreContacto(request.getNombreContacto());
        cliente.setTelefono(request.getTelefono());
        cliente.setCorreo(request.getCorreo());
        cliente.setDireccion(request.getDireccion());
        cliente.setActivo(request.getActivo());
        return cliente;
    }

    public static ClienteResponseDTO toResponse(Cliente cliente) {
        return new ClienteResponseDTO(
                cliente.getId(), cliente.getRuc(), cliente.getRazonSocial(),
                cliente.getNombreContacto(), cliente.getTelefono(), cliente.getCorreo(),
                cliente.getDireccion(), cliente.getActivo());
    }
}
