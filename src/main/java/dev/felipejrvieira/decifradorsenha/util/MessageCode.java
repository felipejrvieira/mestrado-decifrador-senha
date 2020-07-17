/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package dev.felipejrvieira.decifradorsenha.util;

/**
 *
 * @author FELIPE
 */
public class MessageCode {

    public static final byte[] JOIN = "J".getBytes();
    public static final byte[] PASSNOTFOUND = "X".getBytes();
    public static final byte[] QUIT = "Q".getBytes();

    public static byte[] CRACK(String hash, String lower, String upper) {
        return ("C " + hash + " " + lower + " " + upper).getBytes();
    }

    public static byte[] PASSFOUND(String pass) {
        return ("F " + pass).getBytes();
    }
}
