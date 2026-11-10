package com.gestioncomercial.backend.dto;

import org.springframework.data.domain.Page;
import java.util.List;

/** Contrato estable de la API, independiente de la serialización interna de Page. */
public record PaginaResponseDTO<T>(
        List<T> contenido, int pagina, int tamano, long totalElementos,
        int totalPaginas, boolean primera, boolean ultima) {
    public static <T> PaginaResponseDTO<T> desde(Page<T> pagina) {
        return new PaginaResponseDTO<>(pagina.getContent(), pagina.getNumber(), pagina.getSize(),
                pagina.getTotalElements(), pagina.getTotalPages(), pagina.isFirst(), pagina.isLast());
    }
}
