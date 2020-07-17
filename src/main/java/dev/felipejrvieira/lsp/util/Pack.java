/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package dev.felipejrvieira.lsp.util;

/**
 *
 * @author felipejrvieira
 */
public class Pack {

    protected short connId;
    protected byte[] payload;

    public Pack(short connId, byte[] payload) {
        this.connId = connId;
        this.payload = payload;
    }

    public short getConnId() {
        return connId;
    }

    public void setConnId(short connId) {
        this.connId = connId;
    }

    public byte[] getPayload() {
        return payload;
    }
}
