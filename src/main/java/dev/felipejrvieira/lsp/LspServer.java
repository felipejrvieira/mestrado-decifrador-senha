/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package dev.felipejrvieira.lsp;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

import dev.felipejrvieira.lsp.util.ConexaoJaFechadaException;
import dev.felipejrvieira.lsp.util.ILspServer;
import dev.felipejrvieira.lsp.util.LspParams;
import dev.felipejrvieira.lsp.util.Message;
import dev.felipejrvieira.lsp.util.MessageType;
import dev.felipejrvieira.lsp.util.Pack;

/**
 *
 * @author felipejrvieira
 */
public class LspServer implements ILspServer {

    private int port;
    private LspParams params;
    private Map<Short, LspConnection> tabelaDeConexoes;
    private BlockingQueue<Short> lostClients;
    private DatagramSocket socket;
    private BlockingQueue<byte[]> receber;
    private Thread manipuladorPacotesEntrada;
    private short contConn = 0;
    private boolean conectado = true;
    private boolean debug = false;

    public LspServer(int port, LspParams params) {
        this.receber = new LinkedBlockingQueue<>();
        this.tabelaDeConexoes = new HashMap<>();
        this.port = port;
        this.params = params != null ? params : new LspParams();
        this.lostClients = new LinkedBlockingQueue<>();
        try {
            this.socket = new DatagramSocket(port);
        } catch (SocketException ex) {
            Logger.getLogger(LspServer.class.getName()).log(Level.SEVERE, null, ex);
        }
        manipuladorPacotesEntrada();
    }

    @Override
    public Pack read() {
        byte[] payload;
        do {
            payload = receber.poll();
            try {
                Thread.sleep(50);
            } catch (InterruptedException ex) {
            }
        } while (payload == null && conectado);
        if (conectado) {
            Message msg = new Message(payload);
            return new Pack(msg.getConnectionID(), msg.getPayload());
        } else {
            return null;
        }
    }

    @Override
    public void write(Pack pack) {
        LspConnection conn = tabelaDeConexoes.get(pack.getConnId());
        if (conn != null) {
            conn.adicionar(pack.getPayload());
        } else {
            throw new ConexaoJaFechadaException();
        }
    }

    @Override
    public void closeConn(int connId) {
        LspConnection conn = tabelaDeConexoes.get((short) connId);
        if (conn != null) {
            conn.finalizar();
            tabelaDeConexoes.put((short) connId, null);
        } else {
            throw new ConexaoJaFechadaException();
        }
    }

    @Override
    public void closeAll() {
        if (conectado) {
            for (int i = 1; i <= Short.MAX_VALUE; i++) {
                if (tabelaDeConexoes.get((short) i) != null) {
                    closeConn(i);
                }
            }
            conectado = false;
            if (!socket.isClosed()) {
                socket.close();
            }
        } else {
            throw new ConexaoJaFechadaException();
        }
    }

    private void msgConnect(DatagramPacket packet) {
        if (!isConectado(packet)) {
            do {
                incConn();
            } while (tabelaDeConexoes.get(contConn) != null);
            short connId = contConn;
            LspConnection conn = new LspConnection(packet.getAddress(), packet.getPort(), socket, connId, this);
            tabelaDeConexoes.put(connId, conn);
        }
    }

    private boolean isConectado(DatagramPacket packet) {
        LspConnection conn;
        for (int i = 1; i <= Short.MAX_VALUE; i++) {
            conn = tabelaDeConexoes.get((short) i);
            if (conn != null && conn.equals(packet.getAddress(), packet.getPort())) {
                return true;
            }
        }
        return false;
    }

    private void msgData(Message msg) {
        LspConnection conn = tabelaDeConexoes.get(msg.getConnectionID());
        if (conn != null) {
            conn.receberMensagem(msg);
        }
    }

    private void msgAck(Message msg) {
        LspConnection conn = tabelaDeConexoes.get(msg.getConnectionID());
        if (conn != null) {
            conn.setConfirmacaoEnvio(msg);
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
                        if (tabelaDeConexoes.get(msg.getConnectionID()) != null || msg.getMessageType() == MessageType.CONNECT) {
                            System.out.println("LspServer: Recebeu Mensagem - ".concat(msg.toString()));
                        }
                    }
                    switch (msg.getMessageType()) {
                        case MessageType.CONNECT: {
                            msgConnect(packet);
                            break;
                        }
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

    private void incConn() {
        contConn = (short) (++contConn % Short.MAX_VALUE);
    }

    /**
     * @return the params
     */
    protected LspParams getParams() {
        return params;
    }

    protected void putMensagemRecebida(byte[] payload) {
        try {
            receber.put(payload);
        } catch (InterruptedException ex) {
        }
    }

    /**
     * Adiciona em uma fila quais foram as conexões que foram perdidas.
     *
     * @param connId
     */
    protected void addLostClients(Short connId) {
        try {
            this.lostClients.put(connId);
            if (debug) {
                System.out.println("Conexão perdida: " + connId);
            }
        } catch (InterruptedException ex) {
            Logger.getLogger(LspServer.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Recupera o Id de uma conexão perdida, caso não haja conexões perdidas
     * retorna null
     *
     * @return
     */
    public Short getConnIdLostClient() {
        return this.lostClients.poll();
    }
}
