/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package dev.felipejrvieira.decifradorsenha;

import dev.felipejrvieira.decifradorsenha.util.MessageCode;
import dev.felipejrvieira.decifradorsenha.util.Util;
import dev.felipejrvieira.lsp.LspClient;
import dev.felipejrvieira.lsp.util.ConexaoJaFechadaException;

/**
 * Um cliente LSP que envia uma solicitação de decifração de senha, especificada
 * pelo usuário, para o servidor, recebe e imprime a resposta e, por fim,
 * encerra a sua execução.
 *
 * @author FELIPE
 */
public class Requester {

    private boolean debug = true;

    public static void main(String[] args) {
        Requester req = new Requester();
        req.request("localhost", 4455, "7c4a8d09ca3762af61e59520943dc26494f8941b", 6);
    }

    public void request(String host, int port, String hash, int comprimento) {
        String lower = Util.lower(comprimento);
        String upper = Util.upper(comprimento);
        LspClient client = new LspClient(host, port, null);
        if (debug) {
            System.out.println(new String(MessageCode.CRACK(hash, lower, upper)));
        }
        client.write(MessageCode.CRACK(hash, lower, upper));
        messageResult(client.read());
        try {
            client.close();
        } catch (ConexaoJaFechadaException ex) {
        }
    }

    private void messageResult(byte[] payload) {
        if (payload == null) {
            System.out.println("Disconnected");
            return;
        }
        String msg[] = new String(payload).trim().split(" ");
        switch (msg[0]) {
            case "F": {
                System.out.println("Found: ".concat(msg[1]));
                break;
            }
            case "X": {
                System.out.println("Not Found");
                break;
            }
        }
    }
}
