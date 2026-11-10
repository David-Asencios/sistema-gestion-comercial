package com.gestioncomercial.backend.controller;

import com.gestioncomercial.backend.dto.ClienteMapper;
import com.gestioncomercial.backend.dto.ClienteRequestDTO;
import com.gestioncomercial.backend.dto.ClienteResponseDTO;
import com.gestioncomercial.backend.dto.PaginaResponseDTO;
import com.gestioncomercial.backend.service.ClienteService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/clientes")
public class ClienteController {

    private final ClienteService clienteService;

    public ClienteController(ClienteService clienteService) {
        this.clienteService = clienteService;
    }

    @GetMapping
    public ResponseEntity<List<ClienteResponseDTO>> listarClientes() {
        List<ClienteResponseDTO> clientes = clienteService.listarClientes().stream()
                .map(ClienteMapper::toResponse).toList();

        return ResponseEntity.ok(clientes);
    }

    @GetMapping("/buscar")
    public ResponseEntity<PaginaResponseDTO<ClienteResponseDTO>> buscarClientes(
            @RequestParam(required = false) String texto,
            @RequestParam(required = false) Boolean activo,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "20") int tamano,
            @RequestParam(defaultValue = "id") String orden,
            @RequestParam(defaultValue = "asc") String direccion) {
        var resultado = clienteService.buscarClientes(texto, activo, pagina, tamano, orden, direccion)
                .map(ClienteMapper::toResponse);
        return ResponseEntity.ok(PaginaResponseDTO.desde(resultado));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ClienteResponseDTO> buscarClientePorId( @PathVariable Long id) {

        ClienteResponseDTO cliente = ClienteMapper.toResponse(clienteService.buscarClientePorId(id));

        return ResponseEntity.ok(cliente);
    }

    @PostMapping
    public ResponseEntity<ClienteResponseDTO> guardarCliente( @Valid @RequestBody ClienteRequestDTO cliente) {

        ClienteResponseDTO clienteGuardado = ClienteMapper.toResponse(
                clienteService.guardarCliente(ClienteMapper.toEntity(cliente)));

        return ResponseEntity.status(HttpStatus.CREATED).body(clienteGuardado);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ClienteResponseDTO> actualizarCliente( @PathVariable Long id, @Valid @RequestBody ClienteRequestDTO cliente) {

        ClienteResponseDTO actual = ClienteMapper.toResponse(
                clienteService.actualizarCliente(id, ClienteMapper.toEntity(cliente)));

        return ResponseEntity.ok(actual);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarCliente( @PathVariable Long id) {

        clienteService.eliminarCliente(id);

        return ResponseEntity.noContent().build();
    }
}
