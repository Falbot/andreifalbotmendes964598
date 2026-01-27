package br.com.falbot.seplag.backend.api.erro;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;

import org.hibernate.exception.ConstraintViolationException;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestControllerAdvice
public class ApiErrorHandler {
    private static final Logger log = LoggerFactory.getLogger(ApiErrorHandler.class);
    
    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ExceptionHandler(EntityNotFoundException.class)
    public Map<String, Object> notFound(EntityNotFoundException ex) {
        return Map.of("mensagem", ex.getMessage());
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(IllegalArgumentException.class)
    public Map<String, Object> badRequest(IllegalArgumentException ex) {
        return Map.of("mensagem", ex.getMessage());
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Map<String, Object> validation(MethodArgumentNotValidException ex) {
        Map<String, String> campos = new HashMap<>();
        for (FieldError fe : ex.getBindingResult().getFieldErrors()) {
            campos.put(fe.getField(), fe.getDefaultMessage());
        }
        return Map.of("mensagem", "validacao_invalida", "campos", campos);
    }

    //Fallback pra não vazar stacktrace cru
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ExceptionHandler(Exception.class)
    public Map<String, Object> internal(Exception ex) {
        log.error("erro_interno (capturado pelo ApiErrorHandler)", ex);
        return Map.of("mensagem", "erro_interno");
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public Map<String, Object> jsonInvalido(HttpMessageNotReadableException ex) {
        Throwable cause = ex.getCause();

        if (cause instanceof InvalidFormatException ife) {
            Class<?> target = ife.getTargetType();

            //Tenta descobrir o campo que falhou (ex: "tipo")
            String campo = (ife.getPath() != null && !ife.getPath().isEmpty())
                    ? ife.getPath().get(0).getFieldName()
                    : null;

            //Se for enum, devolve os valores aceitos
            if (target != null && target.isEnum()) {
                String[] esperados = Arrays.stream(target.getEnumConstants())
                        .map(Object::toString)
                        .toArray(String[]::new);

                if (campo != null) {
                    return Map.of(
                            "mensagem", "valor_invalido",
                            "campo", campo,
                            "valoresEsperados", esperados
                    );
                }

                return Map.of(
                        "mensagem", "valor_invalido",
                        "valoresEsperados", esperados
                );
            }
        }

        //Fallback JSON malformado, tipo errado etc.
        return Map.of("mensagem", "json_invalido");
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<?> handleDataIntegrity(DataIntegrityViolationException ex) {

        Throwable root = rootCause(ex);

        //Defaults genéricos
        String mensagem = "violacao_integridade";
        String detalhe  = "Operação não permitida por restrição de integridade (verifique vínculos/relacionamentos).";

        //Tenta identificar pelo nome da constraint
        String constraintName = extractConstraintName(root);

        //Caso específico: apagar álbum com capa (FK da capa apontando para album)
        if ((constraintName != null && constraintName.contains("capa_album_album_id_fkey"))
                || (root.getMessage() != null && root.getMessage().contains("capa_album_album_id_fkey"))) {

            mensagem = "album_possui_capas";
            detalhe  = "Não é possível excluir o álbum porque existem capas vinculadas. Remova as capas primeiro.";
        }

        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
                "mensagem", mensagem,
                "detalhe", detalhe
        ));
    }

    private static Throwable rootCause(Throwable t) {
        Throwable cur = t;
        while (cur.getCause() != null && cur.getCause() != cur) {
            cur = cur.getCause();
        }
        return cur;
    }

    private static String extractConstraintName(Throwable root) {
        //Quando vem do Hibernate
        if (root instanceof ConstraintViolationException cve) {
            return cve.getConstraintName();
        }

        //Fallback: tenta extrair da mensagem do Postgres (quando não vem o ConstraintViolationException)
        String msg = root.getMessage();
        if (msg == null) return null;

        int idx = msg.indexOf("constraint \"");
        if (idx >= 0) {
            int start = idx + "constraint \"".length();
            int end = msg.indexOf("\"", start);
            if (end > start) {
                return msg.substring(start, end);
            }
        }
        return null;
    }
}
