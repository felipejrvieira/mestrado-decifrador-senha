/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package dev.felipejrvieira.decifradorsenha;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.logging.Level;
import java.util.logging.Logger;

import dev.felipejrvieira.decifradorsenha.util.CrackPass;
import dev.felipejrvieira.decifradorsenha.util.MessageCode;
import dev.felipejrvieira.decifradorsenha.util.Util;
import dev.felipejrvieira.lsp.LspClient;
import dev.felipejrvieira.lsp.util.ConexaoJaFechadaException;
import dev.felipejrvieira.lsp.util.LspParams;

/**
 * Um cliente LSP que aceita continuamente solicitações de decifração de senha
 * Vindas do servidor, verifica exaustivamente uma senha em um intervalo de
 * strings especificado e por fim responde ao servidor com o resultado da
 * pesquisa.
 *
 * @author FELIPE
 */
public class Worker {

    private LspClient client;
    private boolean ativo = false;
    private boolean debug = true;

    public Worker(String host, int port, LspParams params) {
        client = new LspClient(host, port, params);
        if (debug) {
            System.out.println(new String(MessageCode.JOIN));
        }
        client.write(MessageCode.JOIN);
        manipuladorPacotesEntrada();
    }

    private void manipuladorPacotesEntrada() {
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                do {
                    byte[] payload = client.read();
                    if (payload != null) {
                        String msg[] = new String(payload).trim().split(" ");
                        switch (msg[0]) {
                            case "C": {
                                if (!ativo) {
                                    CrackPass cp = new CrackPass(msg[1], msg[2], msg[3]);
                                    decifradorSenha(cp);
                                }
                                break;
                            }
                            case "Q": {
                                ativo = false;
                                if (debug) {
                                    System.out.println("Wor - " + client.getConnId() + " - Quit");
                                }
                                break;
                            }
                        }
                    } else {
                        try {
                            client.close();
                        } catch (ConexaoJaFechadaException ex) {
                            break;
                        }
                    }
                } while (true);
            }
        });
        t.start();
    }

    private void decifradorSenha(final CrackPass cp) {
        ativo = true;
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                int pass = -1;
                for (int i = cp.getLower(); i <= cp.getUpper(); i++) {
                    try {
                        if (!ativo) {
                            break;
                        }
                        if (hashSHA1(Util.formatPass(i, cp.getLength())).equals(cp.getHash())) {
                            pass = i;
                            break;
                        }
                    } catch (NoSuchAlgorithmException ex) {
                        Logger.getLogger(Worker.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
                byte[] payload;
                if (pass != -1) {
                    payload = MessageCode.PASSFOUND(Util.formatPass(pass, cp.getLength()));
                } else {
                    payload = MessageCode.PASSNOTFOUND;
                }
                try {
                    if (debug) {
                        System.out.println(new String(payload));
                    }
                    client.write(payload);
                } catch (ConexaoJaFechadaException ex) {
                    Logger.getLogger(Worker.class.getName()).log(Level.SEVERE, null, ex);
                }
                ativo = false;
            }
        });
        t.start();
    }

    /**
     * Dada uma chave String como parâmetro , devolve a assinatura hash SHA -1
     * correspondente
     *
     * @param key A chave dada para que o método devolva 5 * a assinatura
     * correspondente
     * @return A assinatura hash SHA -1 da chave dada .
     */
    public static String hashSHA1(String key) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("SHA1");
        md.update(key.getBytes());
        return new BigInteger(1, md.digest()).toString(16);
    }

    public static void main(String[] args) {
        Worker w = new Worker("localhost", 4455, null);
    }
}
