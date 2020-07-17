/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package dev.felipejrvieira.decifradorsenha.util;

import dev.felipejrvieira.decifradorsenha.Cracker;

/**
 *
 * @author felipejrvieira
 */
public class CrackPass {

    private String hash;
    private int lower;
    private int upper;
    private int length;
    private Cracker cracker;

    public CrackPass(String hash, String lower, String upper) {
        this.hash = hash;
        this.lower = Integer.parseInt(lower);
        this.upper = Integer.parseInt(upper);
        this.length = lower.length();
    }

    public CrackPass(String hash, String lower, String upper, Cracker cracker) {
        this(hash, lower, upper);
        this.cracker = cracker;
    }

    public int getLength() {
        return length;
    }

    public String getHash() {
        return hash;
    }

    public int getLower() {
        return lower;
    }

    public int getUpper() {
        return upper;
    }

    public Cracker getCracker() {
        return cracker;
    }
}
