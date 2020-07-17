/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package dev.felipejrvieira.lsp.util;

import java.nio.ByteBuffer;

/**
 *
 * @author felipejrvieira
 */
public class Message {

    public static final int LENGTH = 1000;
    private short messageType;
    private short connectionID;
    private short sequenceNumber;
    private byte[] payload;

    public Message(byte[] payload) {
        ByteBuffer bbf = ByteBuffer.wrap(payload);
        this.messageType = bbf.getShort();
        this.connectionID = bbf.getShort();
        this.sequenceNumber = bbf.getShort();
        this.payload = new byte[Message.LENGTH - 6];
        if (this.messageType == 1) {
            bbf.get(this.payload);
        }
    }

    private Message(short messageType, short connectionID, short sequenceNumber, byte[] payload) {
        this.messageType = messageType;
        this.connectionID = connectionID;
        this.sequenceNumber = sequenceNumber;
        this.payload = payload;
    }

    public short getMessageType() {
        return messageType;
    }

    public short getConnectionID() {
        return connectionID;
    }

    public short getSequenceNumber() {
        return sequenceNumber;
    }

    public byte[] getPayload() {
        return payload;
    }

    public byte[] montarPayload() {
        ByteBuffer buf = ByteBuffer.allocate(Message.LENGTH);
        buf.putShort(getMessageType());
        buf.putShort(getConnectionID());
        buf.putShort(getSequenceNumber());
        if (getPayload() != null) {
            buf.put(getPayload());
        }
        return buf.array();
    }

    public static Message connect() {
        return new Message(MessageType.CONNECT, (short) 0, (short) 0, null);
    }

    public static Message data(short connId, short sequenceNumber, byte[] payload) {
        return new Message(MessageType.DATA, connId, sequenceNumber, payload);
    }

    public static Message ack(short connId, short sequenceNumber) {
        return new Message(MessageType.ACK, connId, sequenceNumber, null);
    }

    @Override
    public String toString() {
        String retorno = "";
        switch (getMessageType()) {
            case MessageType.CONNECT: {
                retorno = "Message Type: Connect";
                break;
            }
            case MessageType.DATA: {
                retorno = "Message Type: Data";
                break;
            }
            case MessageType.ACK: {
                retorno = "Message Type: ACK";
                break;
            }
        }
        retorno = retorno.concat(" Connection ID: ".concat(Short.toString(getConnectionID())));
        retorno = retorno.concat(" Sequence Number: ".concat(Short.toString(getSequenceNumber())));
        retorno = retorno.concat(" Payload: ".concat(new String(getPayload())));
        return retorno;
    }
}
