/*
 * ApplicationControllerAdvice est une classe centralisée de gestion des exceptions 
 * pour l’ensemble des contrôleurs Spring.
 * Elle intercepte les exceptions et renvoie une réponse structurée (ErrorEntity) 
 * avec un statut HTTP adéquat. 
 * Cette approche permet d’uniformiser le format des erreurs dans toute l’application et 
 * d’améliorer la lisibilité des réponses côté client.
 */

package ld.sa_backend.controller.advice;

import java.util.Arrays;

import org.springframework.dao.DataIntegrityViolationException;
import static org.springframework.http.HttpStatus.BAD_GATEWAY;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import jakarta.persistence.EntityNotFoundException;
import ld.sa_backend.exception.ErrorResponse;
import ld.sa_backend.exception.ExternalApiException;


@ControllerAdvice
public class ApplicationControllerAdvice {

    @ResponseStatus(NOT_FOUND)
    @ExceptionHandler({EntityNotFoundException.class})
    public @ResponseBody ErrorResponse handleException(EntityNotFoundException exception) {
        return new ErrorResponse("ENTITY_NOT_FOUND", exception.getMessage());
    }

    @ResponseStatus(CONFLICT)
    @ExceptionHandler({DataIntegrityViolationException.class})
    public @ResponseBody ErrorResponse handleException(DataIntegrityViolationException exception) {
        return new ErrorResponse("CONFLICT_WITH_EXISTING_DATA", exception.getMessage());
    }

    @ResponseStatus(BAD_REQUEST)
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public @ResponseBody ErrorResponse handleException(MethodArgumentTypeMismatchException exception) {

        String fieldName = exception.getName();
        Object invalidValue = exception.getValue();
        Class<?> requiredType = exception.getRequiredType();

        if (requiredType != null && requiredType.isEnum()) {

            String allowedValues = Arrays.toString(requiredType.getEnumConstants());

            return new ErrorResponse(
                "ENUM_VALUE_INVALID",
                "Invalid value '" + invalidValue + "' for field '" + fieldName +
                "'. Allowed values : " + allowedValues
            );
        }

        return new ErrorResponse(
            "ARGUMENTS_INVALID",
            "Invalid parameter : " + fieldName
        );
    }

    @ResponseStatus(BAD_REQUEST)
    @ExceptionHandler({IllegalArgumentException.class})
    public @ResponseBody ErrorResponse handleException(IllegalArgumentException exception) {
        return new ErrorResponse("ARGUMENTS_INVALID", exception.getMessage());
    }

    @ResponseStatus(BAD_REQUEST)
    @ExceptionHandler(org.springframework.http.converter.HttpMessageNotReadableException.class)
    public @ResponseBody ErrorResponse handleException(HttpMessageNotReadableException ex) {
        return new ErrorResponse(
            "REQUEST_BODY_INVALID",
            "JSON body is invalid or missing."
        );
    }

    @ResponseStatus(BAD_GATEWAY)
    @ExceptionHandler({ExternalApiException.class})
    public @ResponseBody ErrorResponse handleException(ExternalApiException exception) {
        return new ErrorResponse(
            "EXTERNAL_API_ERROR",
            "Error during external API call : " + exception.getExternalErrorMessage()
        );
    }
}