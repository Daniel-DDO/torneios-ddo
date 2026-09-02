package com.ddo.torneios.config;

import com.ddo.torneios.exception.RegraNegocioException;
import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@ControllerAdvice
public class RestExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(RegraNegocioException.class)
    public ResponseEntity<Object> handleRegraNegocio(RegraNegocioException ex) {
        log.info("Regra de negócio violada: {}", ex.getMessage()); // INFO/WARN, não ERROR
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", HttpStatus.BAD_REQUEST.value());
        body.put("error", "Erro de Validação");
        body.put("message", ex.getMessage());
        return new ResponseEntity<>(body, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<Object> handleEntidadeNaoEncontrada(EntityNotFoundException ex) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", HttpStatus.NOT_FOUND.value());
        body.put("error", "Recurso não encontrado");
        body.put("message", ex.getMessage());
        return new ResponseEntity<>(body, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler({IllegalStateException.class, IllegalArgumentException.class, RuntimeException.class})
    public ResponseEntity<Object> handleValidacaoEstado(RuntimeException ex) {
        log.warn("Erro de negócio não mapeado: {}", ex.getMessage());
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", HttpStatus.BAD_REQUEST.value());
        body.put("error", "Erro de Validação");
        body.put("message", ex.getMessage() != null ? ex.getMessage() : "Erro ao processar a solicitação.");
        return new ResponseEntity<>(body, HttpStatus.BAD_REQUEST);
    }
}