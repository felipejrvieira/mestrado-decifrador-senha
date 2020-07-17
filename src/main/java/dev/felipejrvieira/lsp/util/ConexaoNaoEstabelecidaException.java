/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package dev.felipejrvieira.lsp.util;

/**
 *
 * @author FELIPE
 */
public class ConexaoNaoEstabelecidaException extends RuntimeException {

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    public ConexaoNaoEstabelecidaException() {
        super("Conexão não estabelecida com o servidor!");
    }

    public ConexaoNaoEstabelecidaException(String message) {
        super(message);
    }
}

