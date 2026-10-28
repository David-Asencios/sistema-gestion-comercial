package com.gestioncomercial.backend.controller;

import com.gestioncomercial.backend.entity.Cliente;
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
    public ResponseEntity<List<Cliente>> listarClientes() {
        List<Cliente> clientes = clienteService.listarClientes();

        return ResponseEntity.ok(clientes);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Cliente> buscarClientePorId( @PathVariable Long id) {

        Cliente cliente = clienteService.buscarClientePorId(id);

        return ResponseEntity.ok(cliente);
    }

    @PostMapping
    public ResponseEntity<Cliente> guardarCliente( @Valid @RequestBody Cliente cliente) {

        Cliente clienteGuardado = clienteService.guardarCliente(cliente);

        return ResponseEntity.status(HttpStatus.CREATED).body(clienteGuardado);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Cliente> actualizarCliente( @PathVariable Long id, @Valid @RequestBody Cliente cliente) {

        Cliente actual = clienteService.actualizarCliente(id,cliente);

        return ResponseEntity.ok(actual);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarCliente( @PathVariable Long id) {

        clienteService.eliminarCliente(id);

        return ResponseEntity.noContent().build();
    }
}
