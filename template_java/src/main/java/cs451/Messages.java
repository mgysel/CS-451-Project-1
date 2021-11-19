package cs451;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class Messages {
    static Host me;
    static List<Config> configs;
    static Hosts hosts;

    static HashMap<Message, ArrayList<HostAck>> sent;
    static HashMap<Message, ArrayList<HostAck>> messages;
    static HashMap<Message, ArrayList<HostAck>> ack;
    static HashMap<Message, ArrayList<HostAck>> delivered;

    private final ReentrantReadWriteLock messagesLock = new ReentrantReadWriteLock();
    
    public Messages(Host me, List<Config> configs, Hosts hosts) {
        Messages.configs = configs;
        Messages.delivered = new HashMap<Message, ArrayList<HostAck>>();
        Messages.sent = new HashMap<Message, ArrayList<HostAck>>();
        Messages.messages = new HashMap<Message, ArrayList<HostAck>>();
        Messages.ack = new HashMap<Message, ArrayList<HostAck>>();

        // Initialize messages with messages to send
        int i = 1;
        int maxMessages = getMaxMessages(configs);
        System.out.println("Max Messages");
        System.out.println(maxMessages);
        while (i <= maxMessages) {
            Message message = new Message(MessageType.BROADCAST, me, Integer.toString(i));
            for (Config config: configs) {
                if (i <= config.getM()) {
                    Host receiver = hosts.getHostById(config.getId());
                    putMessageInMap(messages, receiver, message);
                    // System.out.printf("Message: %s\n", message.toString());
                    // System.out.printf("Is Message Delivered? %s\n", isMessageDelivered(message, receiver));
                    // updateAck(receiver, message);
                    // System.out.printf("Is Message Delivered? %s\n", isMessageDelivered(message, receiver));
                }
            }
            i++;
        }
    }

    /**
     * Checks if message in delivered
     * If not in delivered, adds to delivered
     * @param from
     * @param message
     * @return boolean
     */
    public boolean putMessageInMap(HashMap<Message, ArrayList<HostAck>> map, Host from, Message message) {        
        // ArrayList<HostAck> hostList = getHostAcks(message);
        HostAck hostAck = new HostAck(from, false);
        Message m = getOGMessage(message);

        messagesLock.writeLock().lock();
        ArrayList<HostAck> hostList = messages.get(m);
        messagesLock.writeLock().unlock();

        if(hostList == null) {
            // If no messages, create list
            hostList = new ArrayList<HostAck>();
            hostList.add(hostAck);
            map.put(m, hostList);
            return true;
        } else {
            // If messages, make sure not duplicate
            for (HostAck ha: hostList) {
                if (ha.getHost().equals(from)) {
                    return false;
                }
            }
            messagesLock.writeLock().lock();
            map.get(m).add(hostAck);
            messagesLock.writeLock().unlock();
            return true;
        }
    }

    /**
     * Determines if a message is delivered
     * @param m
     * @return if message is delivered
     */
    public boolean isAllMessagesDelivered(Message m) {
        Message og = getOGMessage(m);
        if (og != null) {
            return og.getDelivered();
        }
        return false;
    }

    public boolean isMessageDelivered(Message m, Host h) {
        Message og = getOGMessage(m);
        if (og != null) {
            return og.getDelivered();
        }

        return false;
    }

    public boolean deliverMessage(Message m) {
        Message og = getOGMessage(m);
        if (og != null) {
            m.setDelivered(true);
            return true;
        }
        return false;
    }

    // public boolean isMessageDelivered(Message m, Host h) {
    //     Message og = getOGMessage(m);
    //     ArrayList<HostAck> hostList = messages.get(og);

    //     if(hostList != null) {
    //         // If messages, check if ack
    //         for (HostAck ha: hostList) {
    //             if (ha.getHost().equals(h)) {
    //                 return ha.getReceivedAck();
    //             }
    //         }
    //     } 

    //     return false;
    // }

    public ArrayList<HostAck> getHostAcks(Message m) {
        Message og = getOGMessage(m);
        if (og != null) {
            messagesLock.writeLock().lock();
            ArrayList<HostAck> hostAck = messages.get(og);
            messagesLock.writeLock().unlock();
            return hostAck;
        }

        return null;
    }

    // public boolean isMessageInMap(HashMap<Host, ArrayList<Message>> map, Host from, Message message) {
    //     ArrayList<Message> msgList = map.get(from);

    //     if(msgList == null) {
    //         // If no messages, not in list
    //         return false;
    //     } else {
    //         // If messages in delivered, make sure not a duplicate
    //         if(msgList.contains(message)) {
    //             System.out.println("Message is in map");
    //             return true;
    //         } 
    //     }

    //     return false;
    // }

    // public boolean removeMessage(HashMap<Host, ArrayList<Message>> map, Host from, Message message) {
    //     ArrayList<Message> msgList = map.get(from);
    //     Message remove = null;

    //     if (msgList == null) {
    //         return false;
    //     } else {
    //         for (Message m: msgList) {
    //             if (m.equals(message)) {
    //                 remove = m;
    //                 break;
    //             }
    //         }
    //     }

    //     if (remove != null) {
    //         msgList.remove(remove);
    //         return true;
    //     } 
    //     return false;
    // }

    // public boolean doesAckEqualMessages() {
    //     // Loop through configs, get receiver address
    //     for (Config config: configs) {
    //         Host receiver = hosts.getHostById(config.getId());

    //         ArrayList<Message> ackList = ack.get(receiver);
    //         ArrayList<Message> messageList = messages.get(receiver);

    //         if(ackList == null) {
    //             // If no messages, not in list
    //             System.out.println("Ack does not equal messages");
    //             return false;
    //         } else {
    //             for (Message message: messageList) {
    //                 if (!ackList.contains(message)) {
    //                     System.out.println("Ack does not equal messages");
    //                     return false;
    //                 }
    //             }
    //         }
    //     }

    //     System.out.println("Ack equals messages");
    //     return true;  
    // }

    // public boolean updateAck(Host from, Message message) {
    //     ArrayList<Message> msgList = messages.get(from);

    //     if (msgList == null) {
    //         return false;
    //     } else {
    //         for (Message m: msgList) {
    //             if (m.equals(message)) {
    //                 m.setReceivedAck(true);
    //                 break;
    //             }
    //         }
    //     }

    //     return true;
    // }

    public Message getOGMessage(Message m) {
        Message og = null;
        messagesLock.writeLock().lock();
        for (Message key: messages.keySet()) {
            // System.out.printf("M: ", m);
            // System.out.printf("Key: ", key);
            if (key.equals(m)) {
                og = m;
            }
        }
        messagesLock.writeLock().unlock();

        if (og == null) {
            return m;
        } else {
            return og;
        }
    }
    
    public boolean updateAck(Host from, Message message) {
        Message m = getOGMessage(message);

        messagesLock.writeLock().lock();
        ArrayList<HostAck> hostList = messages.get(m);

        if (hostList != null) {
            for (HostAck h: hostList) {
                if (h.getHost().equals(from)) {
                    h.setReceivedAck(true);
                    return true;
                }
            }
        }
        messagesLock.writeLock().unlock();

        return false;
    }

    private int getMaxMessages(List<Config> configs) {
        int maxMessages = 0;
        for (Config config: configs) {
            int configMessages = config.getM();
            if (config.getM() > maxMessages) {
                maxMessages = configMessages;
            }
        }
        return maxMessages;
    }

    public HashMap<Message, ArrayList<HostAck>> getSent() {
        return Messages.sent;
    }

    public HashMap<Message, ArrayList<HostAck>> getDelivered() {
        return Messages.delivered;
    }

    public HashMap<Message, ArrayList<HostAck>> getAck() {
        return Messages.ack;
    }

    public HashMap<Message, ArrayList<HostAck>> getMessages() {
        return Messages.messages;
    }

    public HashMap<Message, ArrayList<HostAck>> getMessagesClone() {
        HashMap<Message, ArrayList<HostAck>> messagesClone = new HashMap<Message, ArrayList<HostAck>>();

        messagesLock.readLock().lock();
        for (HashMap.Entry<Message, ArrayList<HostAck>> entry : messages.entrySet()) {
            Message key = entry.getKey();
            Message newKey = Message.getClone(key);

            ArrayList<HostAck> newValue = new ArrayList<HostAck>();
            for (HostAck oldHA: entry.getValue()) {
                HostAck newHA = HostAck.getClone(oldHA);
                newValue.add(newHA);
            }

            messagesClone.put(newKey, newValue);
        }
        messagesLock.readLock().unlock();

        return messagesClone;
    }
}
