package com.gestioncomercial.backend.service;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import java.util.Locale;
import java.util.Set;

/** Validación común de consultas; cada módulo declara sus campos de orden permitidos. */
final class ConsultaPaginada {
    private ConsultaPaginada() {}

    static PageRequest pagina(int pagina, int tamano, String orden, String direccion, Set<String> campos) {
        if (pagina < 0 || tamano < 1 || tamano > 100
                || (long) pagina * tamano > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("La página debe ser no negativa y el tamaño estar entre 1 y 100; el desplazamiento no puede superar 2147483647");
        }
        if (!campos.contains(orden)) {
            throw new IllegalArgumentException("El campo de ordenamiento no está permitido");
        }
        if (direccion == null || !(direccion.equalsIgnoreCase("asc") || direccion.equalsIgnoreCase("desc"))) {
            throw new IllegalArgumentException("La dirección debe ser asc o desc");
        }
        Sort.Direction sentido = Sort.Direction.fromString(direccion);
        Sort sort = Sort.by(sentido, orden);
        if (!orden.equals("id")) {
            sort = sort.and(Sort.by(sentido, "id"));
        }
        return PageRequest.of(pagina, tamano, sort);
    }

    static String patron(String texto) {
        if (texto == null || texto.isBlank()) return null;
        String limpio = texto.trim();
        if (limpio.length() > 200) {
            throw new IllegalArgumentException("El texto de búsqueda admite hasta 200 caracteres");
        }
        // Los caracteres especiales de LIKE se buscan literalmente.
        return "%" + limpio.toLowerCase(Locale.ROOT).replace("!", "!!")
                .replace("%", "!%").replace("_", "!_") + "%";
    }
}
