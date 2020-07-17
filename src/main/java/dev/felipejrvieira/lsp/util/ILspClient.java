/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package dev.felipejrvieira.lsp.util;

/**
 *
 * @author felipejrvieira
 */
public interface ILspClient {

    /**
     * Devolve o Id da conexão
     */
    public int getConnId();

    /**
     * Devolve um vetor de bytes de uma mensagem enviada pelo lado servidor .
     * Devolve null se a conexão for perdida .
     */
    public byte[] read();

    /**
     * Envia uma mensagen para o lado servidor como um vetor de bytes . Devolve
     * exceção se a conexão for perdida .
     */
    public void write(byte[] payload);

    /**
     * Encerra a conexão .
     */
    public void close();
}
