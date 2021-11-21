package cs451;

import java.net.DatagramPacket;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class PerfectLinks extends Thread {
    private Host me;
    public List<Config> configs;
    public Hosts hosts;
    private String output;
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
        InetSocketAddress address = dest.getAddress();
        String content = m.toString();

        // NOTE: For testing
        // try {
        //     TimeUnit.SECONDS.sleep(2);
        // } catch (InterruptedException ex) {
        //     System.out.printf("Sleep exception: %s\n", ex);
        // }
        

        if (udp.send(address, content)) {
            return true;
        }

        return false;
    }

    /**
     * Send messages per configuration
     * Do not send messages to self
     */
    public void sendAll() {
        // System.out.println("Inside SendAll");
        
        // Send messages until we receive all acks
        boolean firstBroadcast = true;
        while (true) {
            // For Host in config that is not me
            for (Config config: configs) {
                Host peer = hosts.getHostById(config.getId());
                if (peer.getId() != me.getId()) {
                    // Send all messages
                    List<Message> msgList = messages.getMessages().get(peer);
                    if (msgList != null) {
                        for (Message m: msgList) {
                            if (m.getReceivedAck() == false) {
                                send(peer, m);
                                writeBroadcast(m, firstBroadcast);
                            }
                        }
                    }
                    firstBroadcast = false;
                }
            }
        }
    }

    /**
     * Listen to and process packets
     */
    public void run() {
        // System.out.println("INSIDE RUN");

        running = true;
        while (running) {

            // Receive Packet
            DatagramPacket packet = udp.receive();

            if (packet != null) {
                Host from = hosts.getHostByAddress(packet.getAddress(), packet.getPort());
                String received = new String(packet.getData(), packet.getOffset(), packet.getLength()).trim();
                Message message = new Message(received, hosts);
                if (message.getType() == MessageType.BROADCAST) {
                    deliver(from, message);
                    // Send ack back, even if already delivered
                    Message ack = new Message(MessageType.ACK, message.getSequenceNumber(), me, message.getContent());
                    send(from, ack);
                } else if (message.getType() == MessageType.ACK) {
                    // Process ACK
                    Message m = new Message(MessageType.BROADCAST, message.getSequenceNumber(), me, message.getContent());
                    messages.updateAck(from, m);
                } else {
                    System.out.println("***** Not proper messages sent");
                    System.out.printf("Message: %s\n", received);
                }
            }
        }
    }

    // public void setMyEventListener (MyEventListener listener) {
    //     this.listener = listener;
    // }

    private void deliver(Host src, Message m) {
        // if (messages.putMessageInMap(messages.getDelivered(), src, m)) {
        //     deliver(src, m);
        //     writeDeliver(src, m);
        //     // listener.PerfectLinksDeliver(src, m);
        // } 
        deliver(src, m);
        writeDeliver(src, m);
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

    /**
     * Close udp socket
     * @return output
     */
    public String close() {
        running = false;
        udp.socket.close();
        return output;
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
