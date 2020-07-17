/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package dev.felipejrvieira.lsp.util;

/**
 *
 * @author felipejrvieira
 */
public class ConexaoJaFechadaException extends RuntimeException {

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    public ConexaoJaFechadaException() {
        super("Conexão já foi encerrada!");
    }

    public ConexaoJaFechadaException(String message) {
        super(message);
    }
    
}
