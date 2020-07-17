/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package dev.felipejrvieira.decifradorsenha;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

import dev.felipejrvieira.decifradorsenha.util.CrackPass;
import dev.felipejrvieira.decifradorsenha.util.Util;

/**
 *
 * @author FELIPE
 */
public class Cracker {

    private short requester;
    private BlockingQueue<CrackPass> slices = new LinkedBlockingQueue<>();
    private int lenghtSlice = 500;
    private boolean found = false;
    private boolean disconnected = false;
    private int lenght;

    public Cracker(short requester, CrackPass crackPass) {
        this.requester = requester;
        this.slices = preencherIntervalo(crackPass);
        this.lenght = this.slices.size();
    }

    private BlockingQueue<CrackPass> preencherIntervalo(CrackPass crackPass) {
        List<CrackPass> aux = new ArrayList<>();
        for (int i = crackPass.getLower(); i <= crackPass.getUpper(); i += lenghtSlice) {
            aux.add(new CrackPass(crackPass.getHash(),
                    Util.formatPass(i, crackPass.getLength()),
                    (i + lenghtSlice < crackPass.getUpper())
                    ? Integer.toString(i + lenghtSlice)
                    : Util.formatPass(crackPass.getUpper(), crackPass.getLength()),
                    this));
        }
        Collections.shuffle(aux);
        BlockingQueue<CrackPass> retorno = new LinkedBlockingQueue<>();
        retorno.addAll(aux);
        return retorno;
    }

    public short getRequester() {
        return requester;
    }

    public CrackPass getSlice() {
        return slices.poll();
    }

    public void processedSlice() {
        this.lenght--;
    }

    public void found() {
        this.found = true;
    }

    public void disconnected() {
        this.disconnected = true;
    }

    public boolean isFound() {
        return found;
    }

    public boolean isDisconnected() {
        return disconnected;
    }

    public int getLenght() {
        return lenght;
    }

    public void addSlice(CrackPass cp) {
        try {
            this.slices.put(cp);
        } catch (InterruptedException ex) {
            Logger.getLogger(Cracker.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
