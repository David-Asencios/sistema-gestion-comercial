package com.gestioncomercial.backend.exception;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, String>> manejarParametroInvalido(MethodArgumentTypeMismatchException ex) {
        return ResponseEntity.badRequest().body(Map.of(
                "mensaje", "El parámetro " + ex.getName() + " contiene un valor no válido"));
    }

    @ExceptionHandler({ConflictoConcurrenteException.class, ConcurrencyFailureException.class})
    public ResponseEntity<Map<String, String>> manejarConflictoConcurrente(RuntimeException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
                "mensaje", "Otro proceso modificó o está utilizando la cotización. Actualice los datos y vuelva a intentarlo."));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, String>> manejarJsonInvalido(HttpMessageNotReadableException ex) {
        return ResponseEntity.badRequest().body(Map.of(
                "mensaje", "El JSON contiene un formato o un valor no válido"));
    }

    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ResponseEntity<Map<String, String>> manejarRecursoNoEncontrado( RecursoNoEncontradoException excepcion) {

        Map<String, String> respuesta = Map.of("mensaje", excepcion.getMessage() );

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(respuesta);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String , String>> manejarValidaciones(MethodArgumentNotValidException ex){

        Map<String , String > errores = new LinkedHashMap<>();

        for(FieldError error: ex.getBindingResult().getFieldErrors())
            errores.put(error.getField(), error.getDefaultMessage());

        return new ResponseEntity<>(errores,HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(RecursoDuplicadoException.class)
    public ResponseEntity<Map<String, String>> manejarRecursoDuplicado(
            RecursoDuplicadoException excepcion) {

        Map<String, String> respuesta = Map.of("mensaje", excepcion.getMessage());

        return ResponseEntity.status(HttpStatus.CONFLICT).body(respuesta);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> manejarErrorInesperado(Exception excepcion) {

        excepcion.printStackTrace();

        Map<String, String> respuesta = Map.of(
                "mensaje", "Ocurrió un error interno en el servidor"
        );

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(respuesta);
    }


    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> manejarArgumentoInvalido(
            IllegalArgumentException excepcion) {

        Map<String, String> respuesta = new LinkedHashMap<>();
        respuesta.put("mensaje", excepcion.getMessage());

        return ResponseEntity.badRequest().body(respuesta);
    }


    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, String>> manejarIntegridadDatos(
            DataIntegrityViolationException ex) {

        Map<String, String> respuesta = new HashMap<>();
        respuesta.put(
                "mensaje",
                "No se pudo completar la operación porque existe un dato duplicado o una relación inválida."
        );

        return ResponseEntity.status(HttpStatus.CONFLICT).body(respuesta);
    }


}
