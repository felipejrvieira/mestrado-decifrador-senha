/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package dev.felipejrvieira.lsp.util;

/**
 *
 * @author felipejrvieira
 */
public class ConexaoPerdidaException extends RuntimeException {

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    public ConexaoPerdidaException() {
        super("Conexão perdida!");
    }

    public ConexaoPerdidaException(String message) {
        super(message);
    }
}
