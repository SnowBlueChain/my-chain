package org.erachain.network;

import org.erachain.controller.Controller;
import org.erachain.core.BlockChain;
import org.erachain.network.message.Message;
import org.erachain.network.message.MessageFactory;
import org.erachain.ntp.NTP;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import org.erachain.settings.Settings;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

/** верт (процесс)
 * вертает общение с внешним пиром - чтение и запись
 *
 */
public class Peer extends Thread {

    private final static boolean need_wait = false;
    static Logger LOGGER = LoggerFactory.getLogger(Peer.class.getName());
    // Слишком бльшой буфер позволяет много посылок накидать не ожидая их приема. Но запросы с возратом остаются в очереди на долго
    // поэтому нужно ожидание дольще делать
    private static int SOCKET_BUFFER_SIZE = BlockChain.HARD_WORK ? 1024 << 11 : 1024 << 9;
    private static int MAX_BEFORE_PING = SOCKET_BUFFER_SIZE << 1;
    public ConnectionCallback callback;
    private InetAddress address;
    private Socket socket;
    private OutputStream out;
    private DataInputStream in;
    private Pinger pinger;
    private boolean white;
    private long pingCounter;
    private long connectionTime;
    private boolean runed;
    private boolean stoped;
    private int errors;
    private int requestKey = 0;
    private long sendedBeforePing = 0l;
    private long maxBeforePing;
    private Map<Integer, BlockingQueue<Message>> messages;

    public Peer(InetAddress address) {
        this.address = address;
        this.messages = Collections.synchronizedMap(new HashMap<Integer, BlockingQueue<Message>>());
        //LOGGER.debug("@@@ new Peer(InetAddress address) : " + address.getHostAddress());
        this.setName("Peer: " + this.getAddress().getHostAddress() + " as address");

    }

    /**
     *  при коннекте во вне связь может порваться поэтому надо
     *  сделать проверку песле выхода по isUsed
     * @param callback
     * @param socket
     * @param description
     */

    public Peer(ConnectionCallback callback, Socket socket, String description) {

        //LOGGER.debug("@@@ new Peer(ConnectionCallback callback, Socket socket) : " + socket.getInetAddress().getHostAddress());

        try {
            this.callback = callback;
            this.socket = socket;
            this.address = socket.getInetAddress();
            this.messages = Collections.synchronizedMap(new HashMap<Integer, BlockingQueue<Message>>());
            this.white = false;
            this.pingCounter = 0;
            this.connectionTime = NTP.getTime();
            this.errors = 0;
            this.sendedBeforePing = 0l;
            this.maxBeforePing = MAX_BEFORE_PING;

            //ENABLE KEEPALIVE
            this.socket.setKeepAlive(true);

            //TIMEOUT
            this.socket.setSoTimeout(0); //Settings.getInstance().getConnectionTimeout());

            this.socket.setReceiveBufferSize(SOCKET_BUFFER_SIZE);
            this.socket.setSendBufferSize(SOCKET_BUFFER_SIZE);

            //CREATE STRINGWRITER
            this.out = socket.getOutputStream();
            this.in = new DataInputStream(socket.getInputStream());

            //START PINGER
            if (this.pinger == null)
                this.pinger = new Pinger(this);
            else {
                this.pinger.setPing(Integer.MAX_VALUE);
                this.pinger.setName("Pinger - " + this.pinger.getId() + " for: " + this.getAddress().getHostAddress());
            }

            this.setName("Peer: " + this.address.getHostAddress() + " as socket"
                    + (this.isWhite()?" is White" : ""));

            // IT is STARTED
            this.runed = true;

            //START COMMUNICATON THREAD
            this.start();

            LOGGER.info(description + address.getHostAddress());

            // при коннекте во вне связь может порваться поэтому тут по runed
            callback.onConnect(this);

        } catch (Exception e) {
            //FAILED TO CONNECT NO NEED TO BLACKLIST

            this.runed = false;

            LOGGER.info("Failed to connect to : " + address.getHostAddress());
            LOGGER.error(e.getMessage(), e);

        }

    }

    // connect and run
    public boolean connect(ConnectionCallback callback, String description) {
        if (Controller.getInstance().isOnStopping()) {
            return false;
        }

        this.callback = callback;
        this.messages = Collections.synchronizedMap(new HashMap<Integer, BlockingQueue<Message>>());
        this.white = true;
        this.pingCounter = 0;
        this.connectionTime = NTP.getTime();
        this.errors = 0;
        this.sendedBeforePing = 0l;
        this.maxBeforePing = MAX_BEFORE_PING;

        int step = 0;
        try {
            //OPEN SOCKET
            step++;
            if (this.socket != null) {
                this.socket.close();
                this.out.close();
                this.in.close();
            }

            this.socket = new Socket(address, Controller.getInstance().getNetworkPort());

            //ENABLE KEEPALIVE
            step++;
            this.socket.setKeepAlive(true);

            this.socket.setReceiveBufferSize(SOCKET_BUFFER_SIZE);
            this.socket.setSendBufferSize(SOCKET_BUFFER_SIZE);

            //CREATE STRINGWRITER
            step++;
            this.out = socket.getOutputStream();
            this.in = new DataInputStream(socket.getInputStream());

        } catch (Exception e) {
            //FAILED TO CONNECT NO NEED TO BLACKLIST
            if (step != 1) {
                LOGGER.error(e.getMessage(), e);
                LOGGER.debug("Failed to connect to : " + address.getHostAddress() + " on step: " + step);
            }

            return false;

        }

        if (this.pinger == null) {
            //START PINGER
            this.pinger = new Pinger(this);

            // IT is STARTED
            this.runed = true;

            this.start();

        } else {
            this.pinger.setPing(Integer.MAX_VALUE);
            this.pinger.setName("Pinger - " + this.pinger.getId() + " for: " + this.getAddress().getHostAddress());

            // IT is STARTED
            this.runed = true;

        }

        LOGGER.info(description + address.getHostAddress());
        callback.onConnect(this);

        // при коннекте во вне связь может порваться поэтому тут по runed
        return this.runed;
    }

    // connect to old reused peer
    public boolean reconnect(Socket socket, String description) {

        //LOGGER.debug("@@@ reconnect(socket) : " + socket.getInetAddress().getHostAddress());

        try {

            if (this.socket != null) {
                this.close();
            }

            this.socket = socket;
            this.address = socket.getInetAddress();
            this.messages = Collections.synchronizedMap(new HashMap<Integer, BlockingQueue<Message>>());
            this.white = false;
            this.pingCounter = 0;
            this.connectionTime = NTP.getTime();
            this.errors = 0;
            this.sendedBeforePing = 0l;
            this.maxBeforePing = MAX_BEFORE_PING;

            //ENABLE KEEPALIVE
            this.socket.setKeepAlive(true);

            this.socket.setReceiveBufferSize(SOCKET_BUFFER_SIZE);
            this.socket.setSendBufferSize(SOCKET_BUFFER_SIZE);

            //CREATE STRINGWRITER
            this.out = socket.getOutputStream();
            this.in = new DataInputStream(socket.getInputStream());

            this.pinger.setPing(Integer.MAX_VALUE);
            this.pinger.setName("Pinger - " + this.pinger.getId() + " for: " + this.address.getHostAddress());

            // IT is STARTED
            this.runed = true;

            //ON SOCKET CONNECT
            this.setName("Peer: " + this.getAddress().getHostAddress() + " reconnected"
                + (this.isWhite()?" is White" : ""));

            LOGGER.info(description + address.getHostAddress());
            callback.onConnect(this);

        } catch (Exception e) {
            //FAILED TO CONNECT NO NEED TO BLACKLIST
            //LOGGER.info("Failed to connect to : " + address);
            //LOGGER.error(e.getMessage(), e);

            return false;
        }

        return this.runed;

    }


    public InetAddress getAddress() {
        return address;
    }

    public long getPingCounter() {
        return this.pingCounter;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Peer) {
            return Arrays.equals(((Peer)obj).address.getAddress(),
                    this.address.getAddress());
        }
        return false;
    }

    public int getErrors() {
        return this.errors;
    }

    public long resetErrors() {
        return this.errors = 0;
    }

    public void setNeedPing() {
        this.pinger.setNeedPing();
    }

    public void setMessageQueue(Message message) {
        this.pinger.setMessageQueue(message);
    }

    public void setMessageWinBlock(Message message) {
        this.pinger.setMessageWinBlock(message);
    }

    public void setMessageQueuePing(Message message) {
        this.pinger.setMessageQueuePing(message);
    }

    public void addPingCounter() {
        this.pingCounter++;
    }

    public void addError() {
        this.errors++;
    }

    public long getPing() {
        if (this.pinger == null)
            return 1999999;

        return this.pinger.getPing();
    }

    public void setPing(int ping) {
        this.pinger.setPing(ping);
    }

    public boolean tryPing(long timer) {
        return this.pinger.tryPing(timer);
    }
    public boolean tryPing() {
        return this.pinger.tryPing();
    }
    public boolean tryQuickPing() {
        return this.pinger.tryQuickPing();
    }

    public boolean isPinger() {
        return this.pinger != null;
    }

    public boolean isUsed() {
        return this.socket != null && this.socket.isConnected() && this.runed;
    }

    public void run() {
        byte[] messageMagic = null;

        while (!stoped) {

            if (false)
                LOGGER.info(this.getAddress().getHostAddress()
                    + (this.isUsed()?" is Used" : "")
                    + (this.isBanned()?" is Banned" : "")
                    + (this.isBad()?" is Bad" : "")
                    + (this.isWhite()?" is White" : ""));


            // CHECK connection
            if (socket == null || !socket.isConnected() || socket.isClosed()
                    || !runed
            ) {

                //this.setName("Peer: " + this.getAddress().getHostAddress() + " broken");

                try {
                    Thread.sleep(200);
                } catch (Exception e) {
                }

                continue;

            }

            //READ FIRST 4 BYTES
            messageMagic = new byte[Message.MAGIC_LENGTH];

            // MORE EFFECTIVE
            // в этом случае просто ожидаем прилета байтов в течении заданного времени ожидания
            // java.net.Socket.setSoTimeout
            // после чего ловим событие SocketTimeoutException т поаторяем ожидание
            // это работает без задержек и более эффективно и не ест время процессора
            try {
                in.readFully(messageMagic);
            } catch (java.net.SocketException e) {
                callback.tryDisconnect(this, 0, e.getMessage());
                continue;
            } catch (java.io.IOException e) {
                callback.tryDisconnect(this, 0, e.getMessage());
                continue;
            } catch (Exception e) {
                callback.tryDisconnect(this, 0, e.getMessage());
                continue;
            }

            if (!Arrays.equals(messageMagic, Controller.getInstance().getMessageMagic())) {
                //ERROR and BAN
                callback.tryDisconnect(this, 3600, "parse - received message with wrong magic");
                continue;
            }

            //PROCESS NEW MESSAGE
            Message message;
            try {
                message = MessageFactory.getInstance().parse(this, in);
            } catch (java.io.EOFException e) {
                // DISCONNECT and BAN
                //this.pinger.tryPing();
                callback.tryDisconnect(this, 0, "parse EOFException - " + e.getMessage());
                continue;

            } catch (Exception e) {
                //LOGGER.error(e.getMessage(), e);
                //if (this.socket.isClosed())

                //DISCONNECT and BAN
                callback.tryDisconnect(this, 6, "parse message wrong - " + e.getMessage());
                continue;
            }

            if (message == null) {
                // unknowm message
                LOGGER.debug(this + " : NULL message!!!");
                continue;
            }

            if (true && message.getType() == Message.GET_HWEIGHT_TYPE) {
                LOGGER.debug(this + " : " + message + " RECEIVED");
            }

            //CHECK IF WE ARE WAITING FOR A RESPONSE WITH THAT ID
            if (false // OLD VERSION
                    && !message.isRequest()
                    && message.hasId()
                    && this.messages.containsKey(message.getId())) {
                //ADD TO OUR OWN LIST
                if (false && (message.getType() == Message.GET_HWEIGHT_TYPE || message.getType() == Message.HWEIGHT_TYPE))
                    LOGGER.debug(this + " : " + message + " receive response for me & add to messages Queue");

                try {
                    this.messages.get(message.getId()).add(message);
                    ////LOGGER.debug(this + " : " + message + " receive response added!!!");
                } catch (java.lang.IllegalStateException e) {
                    LOGGER.debug("received message " + message.viewType() + " from " + this.address.toString());
                    LOGGER.debug("isRequest " + message.isRequest() + " hasId " + message.hasId());
                    LOGGER.debug(" Id " + message.getId() + " containsKey: " + this.messages.containsKey(message.getId()));
                    LOGGER.error(e.getMessage(), e);
                }
            } else if (!message.isRequest() && message.hasId()) {
                // это ответ на наш запрос с ID

                if (true && message.getType() == Message.HWEIGHT_TYPE)
                    LOGGER.debug(this + " >> " + message + " receive as RESPONSE for me & add to messages Queue");

                if (this.messages.containsKey(message.getId())) {
                    //ADD TO OUR OWN LIST

                    try {

                        this.messages.get(message.getId()).add(message);

                        ////LOGGER.debug(this + " : " + message + "  == my RESPONSE added!!!");

                    } catch (java.lang.IllegalStateException e) {
                        LOGGER.debug("received message " + message.viewType() + " from " + this.address.toString());
                        LOGGER.debug("isRequest " + message.isRequest() + " hasId " + message.hasId());
                        LOGGER.debug(" Id " + message.getId() + " containsKey: " + this.messages.containsKey(message.getId()));
                        LOGGER.error(e.getMessage(), e);
                    }
                } else {
                    // ответ прилетел поздно и он уже просроченный и не нужно его обрабатывать вообще
                    LOGGER.debug(this + " >> " + message + "  == my ENDED RESPONSE arrived...");
                }

            } else {
                //CALLBACK
                // see in network.Network.onMessage(Message)
                // and then see controller.Controller.onMessage(Message)

                if (true && (message.getType() == Message.GET_HWEIGHT_TYPE || message.getType() == Message.HWEIGHT_TYPE))
                    LOGGER.debug(this + " >> " + message + " received as SEND");

                long timeStart = System.currentTimeMillis();
                ///LOGGER.debug(this + " : " + message + " receive, go solve");

                this.callback.onMessage(message);

                timeStart = System.currentTimeMillis() - timeStart;
                if (timeStart > 100
                        || true && (message.getType() == Message.GET_HWEIGHT_TYPE || message.getType() == Message.HWEIGHT_TYPE)) {
                    LOGGER.debug(this + " >> " + message + " solved by period: " + timeStart);
                }

            }
        }
    }

    public boolean sendMessage(Message message) {
        //CHECK IF SOCKET IS STILL ALIVE
        if (!this.runed || this.socket == null) {
            ////callback.tryDisconnect(this, 0, "SEND - not runned");
            return false;
        }

        if (!this.socket.isConnected()) {
            //ERROR
            callback.tryDisconnect(this, 0, "SEND - socket not still alive");

            return false;
        }

        byte[] bytes = message.toBytes();

        if (true && message.getType() == Message.HWEIGHT_TYPE) {
            LOGGER.debug(message + " try SEND to " + this);
        }

        //SEND MESSAGE
        synchronized (this.out) {

            try {
                this.out.write(bytes);
                this.out.flush();
            } catch (java.net.SocketException eSock) {
                callback.tryDisconnect(this, 0, "try out.write 1 - " + eSock.getMessage());
                return false;

            } catch (IOException e) {
                if (this.socket.isOutputShutdown()) {
                    //ERROR
                    LOGGER.debug("try sendMessage to " + this.address + " " + Message.viewType(message.getType())
                            + " (**isOutputShutdown**) ERROR: " + e.getMessage());

                    callback.tryDisconnect(this, 0, "try write 2 - " + e.getMessage());
                    return false;
                } else {
                    // INFO
                    LOGGER.debug("TRACK: " + e.getMessage(), e);

                    try {
                        int newSendBufferSize = this.socket.getSendBufferSize() << 1;
                        if (newSendBufferSize > BlockChain.MAX_BLOCK_SIZE_BYTE)
                            return false;

                        LOGGER.debug("try setSendBufferSize to " + this.address + " "
                                + newSendBufferSize);
                        this.socket.setSendBufferSize(newSendBufferSize);
                        return false;
                    } catch (IOException eSize) {
                        LOGGER.debug("try setSendBufferSize to " + this.address + " on " + Message.viewType(message.getType())
                                + " ERROR: " + eSize.getMessage());
                        return false;
                    }
                }
            } catch (Exception e) {
                //ERROR
                //LOGGER.debug("try sendMessage to " + this.address + " " + Message.viewType(message.getType()) + " ERROR: " + e.getMessage());
                //callback.tryDisconnect(this, 5, "SEND - " + e.getMessage());
                callback.tryDisconnect(this, 0, "try write 3 - " + e.getMessage());

                //RETURN
                return false;
            }

        }

        // странно - если идет передача блоков в догоняющую ноду в ее буфер
        // и тут пинговать то она зависает в ожидании надолго и синхронизация удаленной ноды встает на 30-50 секунд
        // ели урать тут пинги то блоки передаются без останова быстро
        // ХОТЯ! при передаче неподтвержденных заявок пингт нормально работают - видимо тут влоенный вызов запрещен по synchronized
        int messageSize = bytes.length;
        int type = message.getType();
        if (type == Message.GET_PING_TYPE
                || type == Message.GET_HWEIGHT_TYPE) {
            this.sendedBeforePing = 0l;
        } else {
            this.sendedBeforePing += bytes.length;
        }

        if (false && type != Message.GET_PING_TYPE
                && type != Message.GET_HWEIGHT_TYPE
                && type != Message.HWEIGHT_TYPE
                && type != Message.GET_BLOCK_TYPE
                && this.sendedBeforePing > this.maxBeforePing) {

            if (messageSize < this.maxBeforePing) {

                LOGGER.debug("PING >> send to " + this.address.getHostAddress() + " " + Message.viewType(message.getType())
                        + " bytes:" + this.sendedBeforePing
                        + " maxBeforePing: " + this.maxBeforePing);

                this.pinger.tryQuickPing();
                Controller.getInstance().notifyObserveUpdatePeer(this);

                long ping = this.getPing();

                if (ping < 0) {
                    if (this.maxBeforePing > MAX_BEFORE_PING >> 2) {
                        this.maxBeforePing >>= 2;
                    }
                    LOGGER.debug("PING << send to " + this.address.getHostAddress() + " " + Message.viewType(message.getType())
                            + " ms: " + ping
                            + " maxBeforePing >>=2: " + this.maxBeforePing);
                } else if (ping > 5000) {
                    if (this.maxBeforePing > MAX_BEFORE_PING >> 2) {
                        this.maxBeforePing >>= 1;
                    }
                    LOGGER.debug("PING << send to " + this.address.getHostAddress() + " " + Message.viewType(message.getType())
                            + " ms: " + ping
                            + " maxBeforePing >>=1: " + this.maxBeforePing);
                } else if (ping < 50) {
                    if (this.maxBeforePing < MAX_BEFORE_PING << 3) {
                        this.maxBeforePing <<= 2;
                    }
                    LOGGER.debug("PING << send to <<=2" + this.address.getHostAddress() + " " + Message.viewType(message.getType())
                            + " ms: " + ping
                            + " maxBeforePing: " + this.maxBeforePing);
                } else if (ping < 100) {
                    if (this.maxBeforePing < MAX_BEFORE_PING << 3) {
                        this.maxBeforePing <<= 1;
                    }
                    LOGGER.debug("PING << send to " + this.address.getHostAddress() + " " + Message.viewType(message.getType())
                            + " ms: " + ping
                            + " maxBeforePing: <<=1" + this.maxBeforePing);
                }
            }

        }

        //RETURN
        return true;
    }

    public synchronized int getResponseKey()
    //public int getResponseKey()
    {

        if (this.requestKey == Integer.MAX_VALUE) {
            this.requestKey = 0;
        }

        //GENERATE ID
        this.requestKey += 1;

        // OLD
        if (false) {
            long counter = 0;
            while (this.messages.containsKey(this.requestKey)) {
                this.requestKey += 1;
                counter++;
            }

            if (counter > 100000) {
                LOGGER.error("getResponseKey find counter: " + counter);
            }
        } else {

            // RECIRCLE keyq and MAP
            if (requestKey > 100000 && this.messages.size() == 0) {
                this.messages = Collections.synchronizedMap(new HashMap<Integer, BlockingQueue<Message>>());
                requestKey = 1;
            }
        }

        return this.requestKey;
    }

    public Message getResponse(Message message, long timeSOT) {

        int thisRequestKey = this.getResponseKey();

        BlockingQueue<Message> blockingQueue = new ArrayBlockingQueue<Message>(1);

        message.setId(thisRequestKey);

        LOGGER.debug(this + " : " + message + ".put");

        //PUT QUEUE INTO MAP SO WE KNOW WE ARE WAITING FOR A RESPONSE
        this.messages.put(thisRequestKey, blockingQueue);

        long startPing = System.currentTimeMillis();

        if (!this.sendMessage(message)) {
            //WHEN FAILED TO SEND MESSAGE
            this.messages.remove(thisRequestKey);
            LOGGER.debug(this + " : " + message + " ERROR send");
            return null;
        }

        Message response = null;
        try {
            response = blockingQueue.poll(timeSOT, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
        }

        if (response != null && this.getPing() < 0) {
            // SET PING by request period
            this.setPing((int)(System.currentTimeMillis() - startPing));
        }

        this.messages.remove(thisRequestKey);

        return response;
    }

    public Message getResponse(Message message) {
        return getResponse(message, Settings.getInstance().getConnectionTimeout());
    }

    // TRUE = You;  FALSE = Remote
    public boolean isWhite() {
        return this.white;
    }

    public long getConnectionTime() {
        return this.connectionTime;
    }

    public boolean isBad() {
        return Controller.getInstance().getDBSet().getPeerMap().isBad(this.getAddress());
    }

    public boolean isBanned() {
        return Controller.getInstance().getDBSet().getPeerMap().isBanned(address.getAddress());
    }


    public void ban(int banForMinutes, String mess) {
        this.setName("Peer: " + this.getAddress().getHostAddress()
                + " banned for " + banForMinutes + " " + mess);

        this.callback.tryDisconnect(this, banForMinutes, mess);
    }

    public boolean isStoped() {
        return stoped;
    }

    public void close() {

        if (!runed) {
            return;
        }

        runed = false;

        LOGGER.info("Try close peer : " + this);

        try {

            //CHECK IS SOCKET EXISTS
            if (socket != null) {
                //CHECK IF SOCKET IS CONNECTED
                if (socket.isConnected()) {
                    //CLOSE SOCKET
                    this.socket.close();
                    this.out.close();
                    this.in.close();

                }
                this.socket = null;
                this.out = null;
                this.in = null;
            }
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
        }
    }

    public void halt() {

        this.stoped = true;
        this.close();
        this.setName("Peer: " + this.getAddress().getHostAddress() + " halted");

    }

    @Override
    public String toString() {
        return this.address.getHostAddress()
                + (getPing() >= 0? " ping: " + this.getPing() + "ms" : " try" + getPing())
                + (isWhite()? " [White]" : "");
    }
}
