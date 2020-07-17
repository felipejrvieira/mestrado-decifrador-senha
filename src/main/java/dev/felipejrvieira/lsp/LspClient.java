/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package dev.felipejrvieira.lsp;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

import dev.felipejrvieira.lsp.util.ConexaoJaFechadaException;
import dev.felipejrvieira.lsp.util.ConexaoNaoEstabelecidaException;
import dev.felipejrvieira.lsp.util.ConexaoPerdidaException;
import dev.felipejrvieira.lsp.util.ILspClient;
import dev.felipejrvieira.lsp.util.LspParams;
import dev.felipejrvieira.lsp.util.Message;
import dev.felipejrvieira.lsp.util.MessageType;

/**
 *
 * @author felipejrvieira
 */
public class LspClient implements ILspClient {

    private String host;
    private int port;
    private LspParams params;
    private short connId;
    private short seqEnvio = 0;
    private short seqEspera = 1;
    private DatagramSocket socket;
    private BlockingQueue<byte[]> enviar;
    private BlockingQueue<byte[]> receber;
    private Message pacoteEnviar;
    private Message confirmacaoEnvio;
    private Message pacoteConfirmacao;
    private Thread manipuladorPacotesEntrada;
    private Thread manipuladorEnvioPacotes;
    private Thread disparadorEpocas;
    private boolean debug = false;
    private boolean conectado = true;
    private boolean perdeuConexao = false;
    private int epoch;

    public LspClient(String host, int port, LspParams params) {
        this.enviar = new LinkedBlockingQueue<>();
        this.receber = new LinkedBlockingQueue<>();
        this.host = host;
        this.port = port;
        this.params = params != null ? params : new LspParams();
        try {
            this.socket = new DatagramSocket();
        } catch (SocketException ex) {
            Logger.getLogger(LspClient.class.getName()).log(Level.SEVERE, null, ex);
        }
        manipuladorPacotesEntrada();
        disparadorEpocas();
        solicitarConexao();
        manipuladorEnvioPacotes();
    }

    @Override
    public int getConnId() {
        if (conectado) {
            return connId;
        } else {
            throw new ConexaoPerdidaException();
        }
    }

    @Override
    public byte[] read() {
        byte[] payload;
        do {
            payload = receber.poll();
            try {
                Thread.sleep(50);
            } catch (InterruptedException ex) {
            }
        } while (payload == null && conectado);
        return conectado ? new Message(payload).getPayload() : null;
    }

    @Override
    public void write(byte[] payload) {
        if (conectado) {
            try {
                enviar.put(payload);
            } catch (InterruptedException ex) {
            }
        } else {
            throw new ConexaoJaFechadaException();
        }
    }

    @Override
    public void close() {
        if (conectado) {
            while ((!enviar.isEmpty() || pacoteEnviar != null) && !perdeuConexao) {
                try {
                    Thread.sleep(125);
                } catch (InterruptedException ex) {
                }
            }
            conectado = false;
            if (!socket.isClosed()) {
                socket.close();
            }
            if (debug) {
                if (!perdeuConexao) {
                    System.out.println("LspClient: Encerrou - ConnectionId: " + connId);
                }
            }
        } else {
            throw new ConexaoJaFechadaException();
        }
    }

    private void solicitarConexao() {
        pacoteEnviar = Message.connect();
        enviarPacotes(pacoteEnviar.montarPayload());
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
            }
        }
        if (!conectado) {
            throw new ConexaoNaoEstabelecidaException();
        }
    }

    private boolean verificarConfirmacao() {
        return confirmacaoEnvio != null && confirmacaoEnvio.getSequenceNumber() == seqEnvio;
    }

    private void msgData(Message msg) {
        if (msg.getSequenceNumber() == seqEspera) {
            incSeqEspera();
            try {
                receber.put(msg.montarPayload());
            } catch (InterruptedException ex) {
            }
            pacoteConfirmacao = Message.ack(connId, msg.getSequenceNumber());
            enviarPacotes(pacoteConfirmacao.montarPayload());
        }
    }

    private void msgAck(Message msg) {
        if (seqEnvio == 0 && connId == 0) {
            connId = msg.getConnectionID();
        }
        if (seqEnvio == msg.getSequenceNumber()) {
            confirmacaoEnvio = msg;
        }
    }

    private void enviarPacotes(byte[] msg) {
        try {
            DatagramPacket packet = new DatagramPacket(msg, msg.length, InetAddress.getByName(host), port);
            socket.send(packet);
            if (debug) {
                System.out.println("LspClient: Enviou Mensagem - ".concat(new Message(msg).toString()));
            }
        } catch (IOException ex) {
        }
    }

    private void manipuladorPacotesEntrada() {
        manipuladorPacotesEntrada = new Thread(new Runnable() {
            @Override
            public void run() {
                do {
                    byte[] buf = new byte[Message.LENGTH];
                    DatagramPacket packet = new DatagramPacket(buf, buf.length);
                    try {
                        socket.receive(packet);
                    } catch (IOException ex) {
                        return;
                    }
                    Message msg = new Message(packet.getData());
                    if (debug) {
                        System.out.println("LspClient: Recebeu Mensagem - ".concat(msg.toString()));
                    }
                    iniciarEpoch();
                    switch (msg.getMessageType()) {
                        case MessageType.DATA: {
                            msgData(msg);
                            break;
                        }
                        case MessageType.ACK: {
                            msgAck(msg);
                            break;
                        }
                    }
                } while (true);
            }
        });
        manipuladorPacotesEntrada.start();
    }

    private void disparadorEpocas() {
        disparadorEpocas = new Thread(new Runnable() {
            @Override
            public void run() {
                iniciarEpoch();
                do {
                    try {
                        Thread.sleep(params.getEpoch());
                    } catch (InterruptedException ex) {
                        return;
                    }
                    //Reenvia mensagem que não foi reconhecida
                    if (pacoteEnviar != null) {
                        enviarPacotes(pacoteEnviar.montarPayload());
                    }
                    //Mensagem de dados recebida, reenvia mensagem de reconhecimento para mais recente.
                    if (pacoteConfirmacao != null) {
                        enviarPacotes(pacoteConfirmacao.montarPayload());
                    }
                    //Pedido de conexão foi enviado e reconhecido, mas não foram recebidas mensagens de dados
                    if (seqEspera == 1 && connId != 0) {
                        enviarPacotes(Message.ack(connId, (short) 0).montarPayload());
                    }
                    incEpoch();
                } while (epoch < params.getEpochLimit() && conectado);
                perdeuConexao = conectado;
                conectado = false;
                if (!socket.isClosed()) {
                    socket.close();
                }
                if (debug) {
                    if (epoch == params.getEpochLimit()) {
                        System.out.println("LspClient: Perdeu Conexão - ConnectionId: " + connId);
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

    private void incSeqEnvio() {
        seqEnvio = (short) (++seqEnvio % Short.MAX_VALUE);
    }

    private void iniciarEpoch() {
        this.epoch = 0;
    }

    private void incEpoch() {
        epoch++;
    }

    private void incSeqEspera() {
        seqEspera = (short) (++seqEspera % Short.MAX_VALUE);
    }
}
