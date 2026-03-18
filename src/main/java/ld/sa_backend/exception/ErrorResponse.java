/**
 * Représente une entité d'erreur renvoyée au client en cas d'exception.
 * <p>
 * Cette classe est un DTO simple et immuable (record) contenant :
 * <ul>
 *   <li>un code d'erreur optionnel ({@code code}) ;</li>
 *   <li>un message descriptif de l'erreur ({@code message}).</li>
 * </ul>
 *
 * Exemple de réponse JSON :
 * <pre>
 * {
 *   "code": "ENTITY_NOT_FOUND",
 *   "message": "La ressource demandée est introuvable."
 * }
 * </pre>
 */

package ld.sa_backend.exception;

public record ErrorResponse(
        String code,
        String message
) {
}
