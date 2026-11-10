package com.gestioncomercial.backend;

import com.gestioncomercial.backend.entity.*;
import com.gestioncomercial.backend.repository.*;
import com.gestioncomercial.backend.service.CotizacionService;
import com.gestioncomercial.backend.service.DetalleCotizacionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class ConcurrenciaCotizacionIntegrationTests {
    @DynamicPropertySource
    static void baseAislada(DynamicPropertyRegistry properties) {
        String mysqlUrl = System.getenv("MYSQL_CONCURRENCY_URL");
        if (mysqlUrl != null) {
            if (!mysqlUrl.equals("jdbc:mysql://localhost:3306/gestion_comercial_concurrency_test")) {
                throw new IllegalArgumentException("Estas pruebas solo admiten el esquema MySQL local aislado de pruebas");
            }
            properties.add("spring.datasource.url", () -> mysqlUrl);
            properties.add("spring.datasource.driver-class-name", () -> "com.mysql.cj.jdbc.Driver");
            properties.add("spring.datasource.username", () -> System.getenv("MYSQL_TEST_USERNAME"));
            properties.add("spring.datasource.password", () -> System.getenv("MYSQL_TEST_PASSWORD"));
        } else {
            properties.add("spring.datasource.url", () -> "jdbc:h2:mem:concurrencia_test;MODE=MySQL;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=10000");
            properties.add("spring.datasource.driver-class-name", () -> "org.h2.Driver");
            properties.add("spring.datasource.username", () -> "sa");
            properties.add("spring.datasource.password", () -> "");
        }
        properties.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
    }

    @Autowired ClienteRepository clientes;
    @Autowired OportunidadComercialRepository oportunidades;
    @Autowired CotizacionRepository cotizaciones;
    @Autowired DetalleCotizacionRepository detalles;
    @Autowired ServicioRepository servicios;
    @Autowired ProductoRepository productos;
    @Autowired CotizacionService cotizacionService;
    @Autowired DetalleCotizacionService detalleService;
    @Autowired JdbcTemplate jdbc;
    @Autowired PlatformTransactionManager transactionManager;

    private Long oportunidadId;
    private Long servicioId;
    private Long cotizacionId;

    @BeforeEach
    void prepararBaseAislada() {
        detalles.deleteAll();
        cotizaciones.deleteAll();
        oportunidades.deleteAll();
        clientes.deleteAll();
        servicios.deleteAll();
        productos.deleteAll();
        jdbc.update("UPDATE secuencia_cotizacion SET ultimo_numero=0 WHERE id=1");
        Cliente cliente = new Cliente();
        cliente.setRazonSocial("Prueba concurrente");
        cliente.setNombreContacto("Contacto");
        cliente = clientes.save(cliente);
        OportunidadComercial oportunidad = new OportunidadComercial();
        oportunidad.setTitulo("Prueba concurrente");
        oportunidad.setCanalOrigen(CanalOrigen.WHATSAPP);
        oportunidad.setCliente(cliente);
        oportunidadId = oportunidades.save(oportunidad).getId();
        Servicio servicio = new Servicio();
        servicio.setNombre("Servicio de prueba");
        servicio.setPrecioReferencial(new BigDecimal("100.00"));
        servicioId = servicios.save(servicio).getId();
        cotizacionId = nuevaCotizacion().getId();
    }

    private Cotizacion nuevaCotizacion() {
        OportunidadComercial oportunidad = new OportunidadComercial();
        oportunidad.setId(oportunidadId);
        Cotizacion cotizacion = new Cotizacion();
        cotizacion.setOportunidad(oportunidad);
        return cotizacionService.guardarCotizacion(cotizacion);
    }

    private DetalleCotizacion detalle(Long cotizacionId, String precio) {
        Cotizacion cotizacion = new Cotizacion();
        cotizacion.setId(cotizacionId);
        Servicio servicio = new Servicio();
        servicio.setId(servicioId);
        DetalleCotizacion detalle = new DetalleCotizacion();
        detalle.setCotizacion(cotizacion);
        detalle.setServicio(servicio);
        detalle.setDescripcion("Concepto concurrente");
        detalle.setCantidad(BigDecimal.ONE);
        detalle.setPrecioUnitario(new BigDecimal(precio));
        return detalle;
    }

    private <T> List<T> ejecutarJuntos(List<Callable<T>> operaciones) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(operaciones.size());
        CountDownLatch preparados = new CountDownLatch(operaciones.size());
        CountDownLatch inicio = new CountDownLatch(1);
        List<Future<T>> resultados = new ArrayList<>();
        try {
            for (Callable<T> operacion : operaciones) {
                resultados.add(executor.submit(() -> {
                    preparados.countDown();
                    if (!inicio.await(10, TimeUnit.SECONDS)) {
                        throw new IllegalStateException("No se liberó el inicio de las operaciones");
                    }
                    return operacion.call();
                }));
            }
            assertThat(preparados.await(10, TimeUnit.SECONDS)).isTrue();
            inicio.countDown();
            List<T> valores = new ArrayList<>();
            for (Future<T> resultado : resultados) {
                valores.add(resultado.get(15, TimeUnit.SECONDS));
            }
            return valores;
        } finally {
            inicio.countDown();
            executor.shutdownNow();
            executor.awaitTermination(10, TimeUnit.SECONDS);
        }
    }

    private void comprobarTotales(Long id, String subtotal) {
        Cotizacion actual = cotizacionService.buscarCotizacionPorId(id);
        assertThat(actual.getSubtotal()).isEqualByComparingTo(subtotal);
        assertThat(detalles.sumarSubtotalPorCotizacion(id)).isEqualByComparingTo(subtotal);
        assertThat(actual.getIgv()).isEqualByComparingTo(new BigDecimal(subtotal).multiply(new BigDecimal("0.18")));
        assertThat(actual.getTotal()).isEqualByComparingTo(new BigDecimal(subtotal).multiply(new BigDecimal("1.18")));
    }

    @Test
    void asignaCodigosUnicosAlCrearOchoCotizacionesSimultaneamente() throws Exception {
        List<Callable<String>> tareas = new ArrayList<>();
        for (int i = 0; i < 8; i++) tareas.add(() -> nuevaCotizacion().getCodigo());
        List<String> codigos = ejecutarJuntos(tareas);
        assertThat(codigos).doesNotHaveDuplicates().allMatch(c -> c.matches("COT-[0-9]{5,}"));
        assertThat(codigos.stream().map(c -> Long.parseLong(c.substring(4))).sorted().toList())
                .containsExactly(2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L);
    }

    @Test
    void noReutilizaElUltimoCodigoEliminado() {
        Cotizacion eliminada = nuevaCotizacion();
        cotizacionService.eliminarCotizacion(eliminada.getId());
        Cotizacion nueva = nuevaCotizacion();
        assertThat(nueva.getCodigo()).isEqualTo("COT-00003");
        assertThat(cotizacionService.buscarCotizacionPorId(cotizacionId).getCodigo()).isEqualTo("COT-00001");
    }

    @Test
    void superaCincoDigitosSinVolverAlMaximoAlfabetico() {
        jdbc.update("UPDATE secuencia_cotizacion SET ultimo_numero=99998 WHERE id=1");
        assertThat(nuevaCotizacion().getCodigo()).isEqualTo("COT-99999");
        assertThat(nuevaCotizacion().getCodigo()).isEqualTo("COT-100000");
        assertThat(nuevaCotizacion().getCodigo()).isEqualTo("COT-100001");
    }

    @Test
    void rollbackDeCreacionTambienRevierteElContador() {
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            assertThat(nuevaCotizacion().getCodigo()).isEqualTo("COT-00002");
            status.setRollbackOnly();
        });
        assertThat(nuevaCotizacion().getCodigo()).isEqualTo("COT-00002");
    }

    @Test
    void sumarDetallesSimultaneosNoPierdeImportes() throws Exception {
        List<Callable<Long>> tareas = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            tareas.add(() -> {
                DetalleCotizacion entrada = detalle(cotizacionId, "100");
                entrada.setTipoDescuento(DetalleCotizacion.TipoDescuento.MONTO);
                entrada.setValorDescuento(new BigDecimal("10"));
                return detalleService.guardarDetalle(entrada).getId();
            });
        }
        assertThat(ejecutarJuntos(tareas)).hasSize(8).doesNotHaveDuplicates();
        comprobarTotales(cotizacionId, "720");
    }

    @Test
    void actualizarDetallesSimultaneosConservaAmbosImportes() throws Exception {
        Long primero = detalleService.guardarDetalle(detalle(cotizacionId, "100")).getId();
        Long segundo = detalleService.guardarDetalle(detalle(cotizacionId, "100")).getId();
        ejecutarJuntos(List.of(
                () -> detalleService.actualizarDetalleCotizacion(primero, detalle(cotizacionId, "200")).getId(),
                () -> detalleService.actualizarDetalleCotizacion(segundo, detalle(cotizacionId, "300")).getId()));
        comprobarTotales(cotizacionId, "500");
    }

    @Test
    void eliminarDetallesSimultaneosDejaTotalesEnCero() throws Exception {
        Long primero = detalleService.guardarDetalle(detalle(cotizacionId, "100")).getId();
        Long segundo = detalleService.guardarDetalle(detalle(cotizacionId, "200")).getId();
        ejecutarJuntos(List.of(
                () -> { detalleService.eliminarDetalle(primero); return true; },
                () -> { detalleService.eliminarDetalle(segundo); return true; }));
        comprobarTotales(cotizacionId, "0");
    }

    @Test
    void trasladosEnSentidosOpuestosNoProducenBloqueosCruzados() throws Exception {
        Long otra = nuevaCotizacion().getId();
        Long primero = detalleService.guardarDetalle(detalle(cotizacionId, "100")).getId();
        Long segundo = detalleService.guardarDetalle(detalle(otra, "200")).getId();
        ejecutarJuntos(List.of(
                () -> detalleService.actualizarDetalleCotizacion(primero, detalle(otra, "100")).getId(),
                () -> detalleService.actualizarDetalleCotizacion(segundo, detalle(cotizacionId, "200")).getId()));
        comprobarTotales(cotizacionId, "200");
        comprobarTotales(otra, "100");
    }

    @Test
    void envioConcurrenteConEdicionRespetaElEstadoYLosTotales() throws Exception {
        Long id = detalleService.guardarDetalle(detalle(cotizacionId, "100")).getId();
        List<Boolean> resultados = ejecutarJuntos(List.of(
                () -> { cotizacionService.actualizarEstadoCotizacion(cotizacionId, EstadoCotizacion.ENVIADA); return true; },
                () -> {
                    try {
                        detalleService.actualizarDetalleCotizacion(id, detalle(cotizacionId, "200"));
                        return true;
                    } catch (IllegalArgumentException ex) { return false; }
                }));
        assertThat(cotizacionService.buscarCotizacionPorId(cotizacionId).getEstado()).isEqualTo(EstadoCotizacion.ENVIADA);
        comprobarTotales(cotizacionId, resultados.get(1) ? "200" : "100");
    }

    @Test
    void envioConcurrenteConEliminarUltimoDetalleNoEnviaCotizacionVacia() throws Exception {
        Long id = detalleService.guardarDetalle(detalle(cotizacionId, "100")).getId();
        List<Boolean> resultados = ejecutarJuntos(List.of(
                () -> {
                    try {
                        cotizacionService.actualizarEstadoCotizacion(cotizacionId, EstadoCotizacion.ENVIADA);
                        return true;
                    } catch (IllegalArgumentException ex) { return false; }
                },
                () -> {
                    try { detalleService.eliminarDetalle(id); return true; }
                    catch (IllegalArgumentException ex) { return false; }
                }));
        assertThat(resultados).containsExactlyInAnyOrder(true, false);
        boolean enviada = resultados.getFirst();
        assertThat(cotizacionService.buscarCotizacionPorId(cotizacionId).getEstado())
                .isEqualTo(enviada ? EstadoCotizacion.ENVIADA : EstadoCotizacion.BORRADOR);
        assertThat(detalles.existsById(id)).isEqualTo(enviada);
        comprobarTotales(cotizacionId, enviada ? "100" : "0");
    }
}
