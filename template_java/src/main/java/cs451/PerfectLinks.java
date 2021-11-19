package cs451;

import java.net.DatagramPacket;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class PerfectLinks extends Thread {
    private Host me;
    public List<Config> configs;
    public Hosts hosts;
    private String output;
    private MyEventListener listener;
    private Messages messages;
    private UDP udp;

    private boolean running;

    private final ReentrantReadWriteLock outputLock = new ReentrantReadWriteLock();

    public PerfectLinks(Host me, List<Config> configs, Hosts hosts) {
        this.me = me;
        this.configs = configs;
        this.hosts = hosts;
        this.output = "";
        this.running = false;
        this.messages = new Messages(me, configs, hosts);
        this.udp = new UDP(me);
    }

    public boolean send(Host dest, Message m) {
        // System.out.println("Inside send");
        InetSocketAddress address = dest.getAddress();
        String content = m.toString();

        return udp.send(address, content);
    }

    /**
     * Sends m to all hosts who have not sent an ack
     * @param m
     */
    public void broadcast(Message m) {
        ArrayList<HostAck> hostList = messages.getHostAcks(m);
        if (!messages.isAllMessagesDelivered(m)) {
            // Only send if message not delivered
            for (HostAck ha: hostList) {
                // Only send to hosts we have not received ack from
                if (!ha.getReceivedAck() && !(ha.getHost().equals(me))) {
                    send(ha.getHost(), m);
                }
            }
        }
    }

    /**
     * Send messages per configuration
     * Do not send messages to self
     */
    public void sendMessages() {
        // Send messages until we receive all acks
        boolean firstBroadcast = true;
        // Must sort messages by content
        while (true) {
            ArrayList<Message> msgList = new ArrayList<Message>(messages.getMessages().keySet());
            Collections.sort(msgList);

            for (Message m : msgList) {
                broadcast(m);
                writeBroadcast(m, firstBroadcast);
            }
            firstBroadcast = false;
        }
    }

    // NOTE: start is used to run a thread asynchronously
    public void run() {
        System.out.println("INSIDE RUN");

        running = true;

        while (running) {

            // Receive Packet
            DatagramPacket packet = udp.receive();

            if (packet != null) {
                Host from = hosts.getHostByAddress(packet.getAddress(), packet.getPort());
                String received = new String(packet.getData(), packet.getOffset(),  packet.getLength()).trim();
                Message message = new Message(received, hosts);
                // System.out.println("***** Inside Receive");
                // System.out.printf("RECEIVED MESSAGE: %s\n", received);
                // System.out.printf("FORMATTED MESSAGE: %s\n", message.toString());
                // System.out.printf("TYPE: %s\n", message.getType());
                // System.out.printf("CONTENT: %s\n", message.getContent());
                if (message.getType() == MessageType.BROADCAST) {
                    messages.putMessageInMap(messages.getMessages(), from, message);
                    deliver(from, message);
                    // Send ack back, even if already delivered
                    Message ack = new Message(MessageType.ACK, me, message.getContent());
                    send(from, ack);
                } else if (message.getType() == MessageType.ACK) {
                    // Process ACK
                    Message m = new Message(MessageType.BROADCAST, me, message.getContent());
                    messages.updateAck(from, m);
                } else {
                    System.out.println("***** Not proper messages sent");
                    System.out.printf("Message: %s\n", received);
                }
            }
        }
    }

    public String close() {
        running = false;
        udp.socket.close();
        return output;
    }

    public void setMyEventListener (MyEventListener listener) {
        this.listener = listener;
    }

    private void deliver(Host src, Message m) {
        if (!messages.isMessageDelivered(m, src)) {
            System.out.println("Message not delivered");
            System.out.println(m.toString());
            messages.deliverMessage(m);
            writeDeliver(src, m);
            // listener.PerfectLinksDeliver(src, m);
        } else {
            System.out.println("Message delivered");
        }

    }

    private void writeDeliver(Host p, Message m) {
        outputLock.writeLock().lock();
        output = String.format("%sd %s %s\n", output, p.getId(), m.getContent());
        outputLock.writeLock().unlock();
    }

    private void writeBroadcast(Message m, Boolean firstBroadcast) {
        if (firstBroadcast) {
            outputLock.writeLock().lock();
            output = String.format("%sb %s\n", output, m.getContent());
            outputLock.writeLock().unlock();
        }
    }

    public List<Config> getConfigs() {
        return configs;
    }

    public Hosts getHosts() {
        return hosts;
    }

    public Host getMe() {
        return me;
    }

    public UDP getUDP() {
        return udp;
    }

    public Messages getMessages() {
        return messages;
    }
}
