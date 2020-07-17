/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package dev.felipejrvieira.lsp;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import dev.felipejrvieira.lsp.util.Message;

/**
 *
 * @author felipejrvieira
 */
public class LspConnection {

    private InetAddress address;
    private int port;
    private DatagramSocket socket;
    private short connId;
    private short seqEnvio = 1;
    private short seqEspera = 1;
    private Message pacoteConfirmacao;
    private Message confirmacaoEnvio;
    private Message pacoteEnviar;
    private boolean debug = false;
    private Thread disparadorEpocas;
    private Thread manipuladorEnvioPacotes;
    ;
    private int epoch;
    private LspServer server;
    private BlockingQueue<byte[]> enviar;
    private boolean conectado = true;
    private boolean perdeuConexao = false;
    private boolean bloqueado = false;

    public LspConnection(InetAddress address, int port, DatagramSocket socket, short connId, LspServer server) {
        this.enviar = new LinkedBlockingQueue<>();
        this.address = address;
        this.port = port;
        this.socket = socket;
        this.connId = connId;
        this.server = server;
        disparadorEpocas();
        pacoteConfirmacao = Message.ack(connId, (short) 0);
        enviarPacotes(pacoteConfirmacao.montarPayload());
        manipuladorEnvioPacotes();
    }

    private void disparadorEpocas() {
        disparadorEpocas = new Thread(new Runnable() {
            @Override
            public void run() {
                iniciarEpoch();
                do {
                    try {
                        Thread.sleep(server.getParams().getEpoch());
                    } catch (InterruptedException ex) {
                        return;
                    }
                    //Reenvia mensagem de reconhecimento para mensagem mais recente recebida (Connect ou Data))
                    if (pacoteConfirmacao != null && conectado) {
                        enviarPacotes(pacoteConfirmacao.montarPayload());
                    }
                    //Reenvia mensagem que não foi reconhecida
                    if (pacoteEnviar != null && conectado) {
                        enviarPacotes(pacoteEnviar.montarPayload());
                    }
                    incEpoch();
                } while (epoch < server.getParams().getEpochLimit() && conectado);
                perdeuConexao = conectado;
                conectado = false;
                if (perdeuConexao) {
                    server.closeConn(connId);
                    server.addLostClients(connId);
                }
                if (debug) {
                    if (epoch == server.getParams().getEpochLimit()) {
                        System.out.println("LspConnection: Perdeu Conexão - ConnectionId: " + connId);
                    }
                }
            }
        });
        disparadorEpocas.start();
    }

    private void manipuladorEnvioPacotes() {
        manipuladorEnvioPacotes = new Thread(new Runnable() {
            @Override
            public void run() {
                do {
                    byte[] payload;
                    do {
                        payload = enviar.poll();
                        try {
                            Thread.sleep(50);
                        } catch (InterruptedException ex) {
                        }
                    } while (payload == null && conectado);
                    if (payload != null) {
                        pacoteEnviar = Message.data(connId, seqEnvio, payload);
                        enviarPacotes(pacoteEnviar.montarPayload());
                    }
                    while (conectado) {
                        if (verificarConfirmacao()) {
                            pacoteEnviar = null;
                            confirmacaoEnvio = null;
                            incSeqEnvio();
                            break;
                        }
                        try {
                            Thread.sleep(100);
                        } catch (InterruptedException ex) {
                            return;
                        }
                    }
                } while (conectado);
            }
        });
        manipuladorEnvioPacotes.start();
    }

    private void enviarPacotes(byte[] msg) {
        try {
            DatagramPacket packet = new DatagramPacket(msg, msg.length, address, port);
            socket.send(packet);
            if (debug) {
                System.out.println("LspConnection: Enviou Mensagem - ".concat(new Message(msg).toString()));
            }
        } catch (IOException ex) {
            return;
        }
    }

    protected void iniciarEpoch() {
        this.epoch = 0;
    }

    private void incEpoch() {
        epoch++;
    }

    public void finalizar() {
        bloqueado = true;
        while (!enviar.isEmpty() || pacoteEnviar != null && !perdeuConexao) {
            try {
                Thread.sleep(125);
            } catch (InterruptedException ex) {
            }
        }
        conectado = false;
        if (debug) {
            if (!perdeuConexao) {
                System.out.println("LspConnection: Encerrou - ConnectionId: " + connId);
            }
        }
    }

    /**
     * @param confirmacaoEnvio the confirmacaoEnvio to set
     */
    protected void setConfirmacaoEnvio(Message confirmacaoEnvio) {
        iniciarEpoch();
        if (seqEnvio == confirmacaoEnvio.getSequenceNumber()) {
            this.confirmacaoEnvio = confirmacaoEnvio;
        }
    }

    private void incSeqEnvio() {
        seqEnvio = (short) (++seqEnvio % Short.MAX_VALUE);
    }

    private void incSeqEspera() {
        seqEspera = (short) (++seqEspera % Short.MAX_VALUE);
    }

    protected void receberMensagem(Message msg) {
        if (!bloqueado) {
            iniciarEpoch();
            if (msg.getSequenceNumber() == seqEspera) {
                incSeqEspera();
                server.putMensagemRecebida(msg.montarPayload());
                pacoteConfirmacao = Message.ack(connId, msg.getSequenceNumber());
                enviarPacotes(pacoteConfirmacao.montarPayload());
            }
        }
    }

    private boolean verificarConfirmacao() {
        return confirmacaoEnvio != null && confirmacaoEnvio.getSequenceNumber() == seqEnvio;
    }

    protected void adicionar(byte[] payload) {
        try {
            enviar.put(payload);
        } catch (InterruptedException ex) {
        }
    }

    public boolean equals(InetAddress address, int port) {
        return (address.getHostAddress().equals(this.address.getHostAddress()) && port == this.port);
    }
}
