/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package dev.felipejrvieira.lsp.util;

/**
 *
 * @author felipejrvieira
 */
public class LspParams {

    private int epoch = 2000;
    private int epochLimit = 5;

    public LspParams() {
    }

    public LspParams(int epoch, int epochLimit) {
        this.epoch = epoch;
        this.epochLimit = epochLimit;
    }

    public int getEpoch() {
        return epoch;
    }

    public int getEpochLimit() {
        return epochLimit;
    }
}
