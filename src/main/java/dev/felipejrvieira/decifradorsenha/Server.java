/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package dev.felipejrvieira.decifradorsenha;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

import dev.felipejrvieira.decifradorsenha.util.CrackPass;
import dev.felipejrvieira.decifradorsenha.util.MessageCode;
import dev.felipejrvieira.decifradorsenha.util.Util;
import dev.felipejrvieira.lsp.LspServer;
import dev.felipejrvieira.lsp.util.ConexaoJaFechadaException;
import dev.felipejrvieira.lsp.util.LspParams;
import dev.felipejrvieira.lsp.util.Pack;

/**
 * Um servidor LSP que gerencia toda a logística de decifração de senhas. A
 * qualquer momento, pode haver um número qualquer de workers disponíveis,
 * podendo receber qualquer quantidade de pedidos. Para cada solicitação, ele
 * divide o pedido em tarefas menores e os envia para os workers. Ele monitora
 * os resultados de retorno e finalmente responde para o requester que fez o
 * pedido.
 *
 * @author FELIPE
 */
public class Server {

    private BlockingQueue<Short> workers = new LinkedBlockingQueue<>();
    private BlockingQueue<Cracker> crackers = new LinkedBlockingQueue<>();
    private Map<Short, CrackPass> busyWorker = new HashMap<>();
    private LspServer server;
    private boolean debug = true;
    private boolean blockedScheduleFound = false;
    private boolean blockedScheduleDisconnect = false;

    public Server(int port, LspParams params) {
        server = new LspServer(port, params);
        manipuladorPacotesEntrada();
        checkConnection();
        scheduler();
    }

    private void addWorker(Short connId) {
        try {
            workers.put(connId);
        } catch (InterruptedException ex) {
            Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void startCrack(String[] msg, Pack pack) {
        CrackPass cp = new CrackPass(msg[1], msg[2], msg[3]);
        Cracker crac = new Cracker(pack.getConnId(), cp);
        try {
            crackers.put(crac);
        } catch (InterruptedException ex) {
            Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void stopWorkers(Cracker c) {
        Set<Short> keys = busyWorker.keySet();
        for (Short worker : keys) {
            Cracker cw = (busyWorker.get(worker) != null ? busyWorker.get(worker).getCracker() : null);
            if (debug) {
                System.out.println("Wor - " + worker + " - Cra - " + (cw != null ? cw.getRequester() : "nulo"));
            }
            if (cw == c) {
                server.write(new Pack(worker, MessageCode.QUIT));
            }
        }
    }

    private void foundPass(String[] msg, Pack pack) {
        blockedScheduleFound = true;
        CrackPass cp = busyWorker.get(pack.getConnId());
        cp.getCracker().found();
        crackers.remove(cp.getCracker());
        busyWorker.put(pack.getConnId(), null);
        stopWorkers(cp.getCracker());
        addWorker(pack.getConnId());
        if (debug) {
            System.out.println("Req - " + cp.getCracker().getRequester() + " Msg - " + new String(MessageCode.PASSFOUND(msg[1])));
        }
        try {
            server.write(new Pack(cp.getCracker().getRequester(), MessageCode.PASSFOUND(msg[1])));
        } catch (ConexaoJaFechadaException ex) {
        } finally {
            blockedScheduleFound = false;
        }
    }

    private void workerNotFoundPass(Pack pack) {
        CrackPass cp = busyWorker.get(pack.getConnId());
        cp.getCracker().processedSlice();
        if (cp.getCracker().getLenght() == 0) {
            crackers.remove(cp.getCracker());
            try {
                server.write(new Pack(cp.getCracker().getRequester(), MessageCode.PASSNOTFOUND));
            } catch (ConexaoJaFechadaException ex) {
            }
        }
        busyWorker.put(pack.getConnId(), null);
        addWorker(pack.getConnId());
    }

    private void manipuladorPacotesEntrada() {
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                do {
                    Pack pack = server.read();
                    String[] msg = new String(pack.getPayload()).trim().split(" ");
                    switch (msg[0]) {
                        case "J": {
                            addWorker(pack.getConnId());
                            break;
                        }
                        case "C": {
                            startCrack(msg, pack);
                            break;
                        }
                        case "F": {
                            foundPass(msg, pack);
                            break;
                        }
                        case "X": {
                            workerNotFoundPass(pack);
                            break;
                        }

                    }
                } while (true);
            }
        });
        t.start();
    }

    private void checkConnection() {
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    Short connID = null;
                    boolean requester = false;
                    //Bloqueando enquanto uma conexão não foi finalizada.
                    do {
                        connID = server.getConnIdLostClient();
                        try {
                            Thread.sleep(50);
                        } catch (InterruptedException ex) {
                        }
                    } while (connID == null);

                    blockedScheduleDisconnect = true;
                    //Verifica se a conexão que terminou foi de um Requester
                    Cracker cracker = null;
                    for (Cracker c : crackers) {
                        if (c.getRequester() == connID.shortValue()) {
                            requester = true;
                            cracker = c;
                            break;
                        }
                    }
                    //Tratamento
                    if (!requester) {
                        CrackPass cp = busyWorker.get(connID);
                        if (cp != null) {
                            cp.getCracker().addSlice(cp);
                        }
                        busyWorker.remove(connID);
                        workers.remove(connID);
                    } else {
                        cracker.disconnected();
                        crackers.remove(cracker);
                        stopWorkers(cracker);
                    }
                    blockedScheduleDisconnect = false;
                }
            }
        });
        t.start();
    }

    private void scheduler() {
        while (true) {
            //Aguardando uma requisição
            Cracker c = null;
            do {
                try {
                    Thread.sleep(50);
                } catch (InterruptedException ex) {
                }
                c = crackers.poll();
            } while (c == null);
            if (!c.isFound() && !c.isDisconnected()) {
                crackers.add(c);
            }

            //Pega um intervalo (fatia) para processar
            CrackPass cp = c.getSlice();

            //Aguardando  um worker para processar uma fatia do Requester
            Short w = null;
            do {
                try {
                    Thread.sleep(50);
                } catch (InterruptedException ex) {
                }
                w = workers.poll();
            } while (w == null);

            //Bloqueia o funcionamento do Schedule para avisar os workers que podem parar.
            while (blockedScheduleFound || blockedScheduleDisconnect) {
                try {
                    Thread.sleep(50);
                } catch (InterruptedException ex) {
                }
            }

            //Testa se o cracker que vai ser atribuído já foi encontrado ou desconetado
            if (!c.isFound() && !c.isDisconnected() && cp != null) {
                busyWorker.put(w, cp);
                byte[] payload = MessageCode.CRACK(cp.getHash(), Util.formatPass(cp.getLower(), cp.getLength()), Util.formatPass(cp.getUpper(), cp.getLength()));
                if (debug) {
                    System.out.println("Req - " + c.getRequester() + " Wor - " + w + " Msg - " + new String(payload));
                }
                server.write(new Pack(w, payload));
            } else {
                addWorker(w);
            }
        }
    }

    public static void main(String[] args) {
        Server s = new Server(4455, null);
    }
}
