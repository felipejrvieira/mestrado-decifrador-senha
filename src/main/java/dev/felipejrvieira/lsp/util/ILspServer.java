/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package dev.felipejrvieira.lsp.util;

/**
 *
 * @author felipejrvieira
 */
public interface ILspServer {

    /**
     * Lê dados da fila de entrada do servidor . Se não houver dados recebidos ,
     * bloqueia o chamador até que dados sejam recebidos . Os dados estão
     * encapsulados pela classe Pack .
     */
    public Pack read();

    /**
     * Envia dados para um determinado cliente . Deve Devolver exceção se a
     * conexão estiver encerrada .
     */
    public void write(Pack pack);

    /**
     * Encerra uma conexão com o identificador connId .
     */
    public void closeConn(int connId);
    
     /**
     * Encerra todas as conexões ativas.
     */
    public void closeAll();
}
