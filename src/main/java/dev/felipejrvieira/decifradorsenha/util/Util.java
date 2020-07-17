/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package dev.felipejrvieira.decifradorsenha.util;

/**
 *
 * @author FELIPE
 */
public class Util {

    public static String lower(int comprimento) {
        String lower = "";
        for (int cont = 1; cont <= comprimento; cont++) {
            lower = lower.concat("0");
        }
        return lower;
    }

    public static String upper(int comprimento) {
        String upper = "";
        for (int cont = 1; cont <= comprimento; cont++) {
            upper = upper.concat("9");
        }
        return upper;
    }
    
    public static String formatPass(int pass, int comprimento){
        String retorno = Integer.toString(pass);
        for(int i = retorno.length();i < comprimento; i++){
            retorno = "0".concat(retorno);
        }
        return retorno;
    }
}
