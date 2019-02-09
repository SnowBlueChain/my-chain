package org.erachain.network;

import org.erachain.controller.Controller;
import org.erachain.core.BlockChain;
import org.erachain.core.account.Account;
import org.erachain.core.account.PublicKeyAccount;
import org.erachain.core.crypto.Base58;
import org.erachain.core.transaction.R_Send;
import org.erachain.core.transaction.Transaction;
import org.erachain.datachain.DCSet;
import org.erachain.network.message.Message;
import org.erachain.network.message.TelegramMessage;
import org.erachain.ntp.NTP;
import org.erachain.settings.Settings;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

public class TelegramManager extends Thread {
    /**
     * count telegrams
     */
    private static final int MAX_HANDLED_TELEGRAMS_SIZE = BlockChain.HARD_WORK ? 1 << 20 : 8000;

    private static final int MAX_HANDLED_MESSAGES_SIZE = BlockChain.HARD_WORK ? 1024 << 8 : 1024<<5;

    /**
     * time to live telegram
     */
    private static final int KEEP_TIME = 60000 * 60 * (BlockChain.HARD_WORK ? 2 : 8);
    static Logger LOGGER = LoggerFactory.getLogger(TelegramManager.class.getName());
    private Network network;
    private boolean run;
    // pool of messages
    private Map<String, TelegramMessage> handledTelegrams;
    // timestamp lists for clear
    private TreeMap<Long, List<TelegramMessage>> telegramsForTime;
    // lists for RECIPIENTS only address
    private Map<String, List<TelegramMessage>> telegramsForAddress;
    // counts for CREATORs COUNT
    private Map<String, Integer> telegramsCounts;

    private static final int QUEUE_LENGTH = BlockChain.DEVELOP_USE? 2000 : 300;
    BlockingQueue<Message> blockingQueue = new ArrayBlockingQueue<Message>(QUEUE_LENGTH);
    private SortedSet<String> handledMessages;

    private Controller controller;
    private BlockChain blockChain;
    private DCSet dcSet;
    public long messageTimingAverage;

    public TelegramManager(Controller controller, BlockChain blockChain, DCSet dcSet, Network network) {

        this.controller = controller;
        this.blockChain = blockChain;
        this.dcSet = dcSet;

        this.setName("WinBlockSelector[" + this.getId() + "]");

        this.network = network;
        this.handledTelegrams = new HashMap<String, TelegramMessage>();
        this.telegramsForTime = new TreeMap<Long, List<TelegramMessage>>();
        this.telegramsForAddress = new HashMap<String, List<TelegramMessage>>();
        this.telegramsCounts = new HashMap<String, Integer>();

        this.setName("TelegramManager - " + this.getId());

        this.handledMessages = Collections.synchronizedSortedSet(new TreeSet<String>());

        this.start();

    }

    private void addHandledMessage(String hash) {
        try {
            synchronized (this.handledMessages) {
                //CHECK IF LIST IS FULL
                if (this.handledMessages.size() > MAX_HANDLED_MESSAGES_SIZE) {
                    this.handledMessages.remove(this.handledMessages.first());
                    ///LOGGER.error("handledMessages size OVERHEAT! ");
                }

                this.handledMessages.add(hash);
            }
        } catch (Exception e) {
            //LOGGER.error(e.getMessage(),e);
        }
    }

    /**
     * @param message
     */
    public void offerMessage(Message message) {

        synchronized (this.handledMessages) {
            //CHECK IF NOT HANDLED ALREADY
            String key = new String(message.getHash());
            if (this.handledMessages.contains(key)) {
                return;
            }

            //ADD TO HANDLED MESSAGES
            this.addHandledMessage(key);
        }

        blockingQueue.offer(message);
    }

    public void processMessage(Message message) {

        if (message == null)
            return;

        long onMessageProcessTiming = System.nanoTime();

        TelegramMessage telegramMessage = (TelegramMessage) message;

        if (pipeAddRemove((TelegramMessage) message, null, 0))
            return;

        onMessageProcessTiming = System.nanoTime() - onMessageProcessTiming;
        if (onMessageProcessTiming < 999999999999l) {
            // при переполнении может быть минус
            // в миеросекундах подсчет делаем
            onMessageProcessTiming /= 1000;
            messageTimingAverage = ((messageTimingAverage << 5)
                    + onMessageProcessTiming - messageTimingAverage) >> 5;
        }

        // BROADCAST
        List<Peer> excludes = new ArrayList<Peer>();
        excludes.add(message.getSender());
        this.network.broadcast(message, excludes, false);

    }

    // GET telegram
    public TelegramMessage getTelegram(String signatureKey) {
        return handledTelegrams.get(signatureKey);
    }

    public Integer telegramCount() {
        return handledTelegrams.size();
    }

    public List<TelegramMessage> toList() {
        List<TelegramMessage> result = new ArrayList();
        result.addAll(handledTelegrams.values());
        return result;
    }


    // GET telegrams for RECIPIENT from TIME
    public List<TelegramMessage> getTelegramsFromTimestamp(long timestamp, String recipient, String filter) {
        List<TelegramMessage> telegrams = new ArrayList<TelegramMessage>();
        if (!Controller.getInstance().isOnStopping()) {

            SortedMap<Long, List<TelegramMessage>> subMap = telegramsForTime.tailMap(timestamp);
            for (Entry<Long, List<TelegramMessage>> item : subMap.entrySet()) {
                List<TelegramMessage> telegramsTimestamp = item.getValue();
                if (telegramsTimestamp != null) {
                    for (TelegramMessage telegram : telegramsTimestamp) {
                        Transaction transaction = telegram.getTransaction();
                        if (timestamp > 0 && telegram.getTransaction().getTimestamp() < timestamp)
                            continue;

                        if (filter != null && transaction.getType() == Transaction.SEND_ASSET_TRANSACTION) {
                            String head = ((R_Send) transaction).getHead();
                            if (!filter.equals(head))
                                continue;
                        }

                        if (recipient != null && transaction.getType() == Transaction.SEND_ASSET_TRANSACTION) {
                            Account account = ((R_Send) transaction).getRecipient();
                            if (!account.equals(recipient))
                                continue;
                        }

                        telegrams.add(telegram);
                    }
                }
            }
        }

        // RETURN
        return telegrams;
    }

    // GET telegrams for RECIPIENT from TIME
    public List<TelegramMessage> getTelegramsForAddress(String recipient, long timestamp, String filter) {
        // TelegramMessage telegram;
        List<TelegramMessage> telegrams = new ArrayList<TelegramMessage>();
        // ASK DATABASE FOR A LIST OF PEERS
        if (!Controller.getInstance().isOnStopping()) {
            List<TelegramMessage> telegramsAddress = telegramsForAddress.get(recipient);
            if (telegramsAddress == null)
                return telegrams;
            for (TelegramMessage telegram : telegramsAddress) {
                Transaction transaction = telegram.getTransaction();
                if (timestamp > 0 && telegram.getTransaction().getTimestamp() < timestamp)
                    continue;

                if (filter != null && transaction.getType() == Transaction.SEND_ASSET_TRANSACTION) {
                    String head = ((R_Send) transaction).getHead();
                    if (!filter.equals(head))
                        continue;
                }

                telegrams.add(telegram);
            }
        }

        // RETURN
        return telegrams;
    }

    /**
     * DELETE one telegram
     *
     * @param signatureStr
     * @param withTimestamp if TRUE delete from timestamp MAP too
     * @param withAddress   if TRUE delete from senders MAP too
     */
    public void delete(String signatureStr, boolean withTimestamp, boolean withAddress) {

        HashSet<Account> recipients;
        String address;
        TelegramMessage telegram;
        Transaction transaction;
        long timestamp;

        telegram = this.handledTelegrams.remove(signatureStr);
        if (telegram == null)
            return;

        transaction = telegram.getTransaction();
        byte[] signature = transaction.getSignature();

        if (withAddress) {
            recipients = transaction.getRecipientAccounts();
            if (recipients != null && !recipients.isEmpty()) {
                int i;
                for (Account recipient : recipients) {
                    address = recipient.getAddress();
                    List<TelegramMessage> addressTelegrams = this.telegramsForAddress.get(address);
                    if (addressTelegrams != null) {
                        i = 0;
                        for (TelegramMessage addressTelegram : addressTelegrams) {
                            if (Arrays.equals(addressTelegram.getTransaction().getSignature(), signature)) {
                                addressTelegrams.remove(i);
                                break;
                            }
                            i++;
                        }
                    }
                    // IF list is empty
                    if (addressTelegrams.isEmpty()) {
                        this.telegramsForAddress.remove(address);
                    } else {
                        this.telegramsForAddress.put(address, addressTelegrams);
                    }
                }
            }
        }

        // CREATOR counts
        address = transaction.getCreator().getAddress();
        Integer count = this.telegramsCounts.get(address);
        if (count != null) {
            if (count < 2) {
                this.telegramsCounts.remove(address);
            } else {
                this.telegramsCounts.put(address, count - 1);
            }
        }

        if (withTimestamp) {
            timestamp = transaction.getTimestamp();
            List<TelegramMessage> telegrams = this.telegramsForTime.get(timestamp);
            int i = 0;
            for (TelegramMessage telegram_item : telegrams) {
                if (Arrays.equals(telegram_item.getTransaction().getSignature(), signature)) {
                    telegrams.remove(i);
                    break;
                }
                i++;
            }

            if (telegrams.isEmpty())
                this.telegramsForTime.remove(timestamp);
            else
                this.telegramsForTime.put(timestamp, telegrams);
        }
    }

    //

    /**
     * delete all to this Timestamp and filter by Recipient address and Title
     *
     * @param toTimestamp timestamp (Long)
     * @param recipient   String
     * @param filter      String
     * @return - counter (long)
     */
    public long deleteToTimestamp(long toTimestamp, String recipient, String filter) {
        long counter = 0;

        SortedMap<Long, List<TelegramMessage>> subMap = telegramsForTime.headMap(toTimestamp);
        for (Long time_item : subMap.keySet()) {
            List<TelegramMessage> telegramsTimestamp = telegramsForTime.remove(time_item);
            if (telegramsTimestamp == null || telegramsTimestamp.isEmpty())
                continue;

            for (TelegramMessage telegram : telegramsTimestamp) {
                Transaction transaction = telegram.getTransaction();
                if (telegram.getTransaction().getTimestamp() > toTimestamp)
                    continue;

                if (filter != null && transaction.getType() == Transaction.SEND_ASSET_TRANSACTION) {
                    String head = ((R_Send) transaction).getHead();
                    if (!filter.equals(head))
                        continue;
                }

                if (recipient != null && transaction.getType() == Transaction.SEND_ASSET_TRANSACTION) {
                    Account account = ((R_Send) transaction).getRecipient();
                    if (!account.equals(recipient))
                        continue;
                }

                counter++;
                delete(Base58.encode(transaction.getSignature()), false, true);
            }
        }

        return counter;
    }

    /**
     * delete all to this Recipient address and filter by Timestamp and Title
     *
     * @param recipient
     * @param toTimestamp if > 0 use filter to this value (delete all that less)
     * @param filter      if not null use it for filter
     * @return
     */
    public long deleteForRecipient(String recipient, long toTimestamp, String filter) {
        long counter = 0;

        List<TelegramMessage> telegrams = telegramsForAddress.remove(recipient);
        if (telegrams == null || telegrams.isEmpty())
            return 0;

        for (TelegramMessage telegram : telegrams) {
            Transaction transaction = telegram.getTransaction();
            if (toTimestamp > 0 && telegram.getTransaction().getTimestamp() > toTimestamp)
                continue;

            if (filter != null && transaction.getType() == Transaction.SEND_ASSET_TRANSACTION) {
                String head = ((R_Send) transaction).getHead();
                if (!filter.equals(head))
                    continue;
            }

            counter++;
            delete(Base58.encode(transaction.getSignature()), true, false);
        }

        return counter;
    }

    /**
     * DELETE telegrams by signature list
     *
     * @param signatures
     * @return
     */
    public List<String> deleteList(List<String> signatures) {

        List<String> left = new ArrayList<>();
        HashSet<Account> recipients;
        String address;
        int i;
        TelegramMessage telegram;
        Transaction transaction;
        byte[] signature;
        long timestamp;
        for (String signatureStr : signatures) {
            telegram = this.handledTelegrams.remove(signatureStr);
            if (telegram == null) {
                left.add(signatureStr);
                continue;
            }

            transaction = telegram.getTransaction();
            signature = transaction.getSignature();
            recipients = transaction.getRecipientAccounts();
            if (recipients != null && !recipients.isEmpty()) {
                for (Account recipient : recipients) {
                    address = recipient.getAddress();
                    List<TelegramMessage> addressTelegrams = this.telegramsForAddress.get(address);
                    if (addressTelegrams != null) {
                        i = 0;
                        for (TelegramMessage addressTelegram : addressTelegrams) {
                            if (Arrays.equals(addressTelegram.getTransaction().getSignature(), signature)) {
                                addressTelegrams.remove(i);
                                break;
                            }
                            i++;
                        }
                    }
                    // IF list is empty
                    if (addressTelegrams.isEmpty()) {
                        this.telegramsForAddress.remove(address);
                    } else {
                        this.telegramsForAddress.put(address, addressTelegrams);
                    }
                }
            }

            // CREATOR counts
            address = transaction.getCreator().getAddress();
            Integer count = this.telegramsCounts.get(address);
            if (count != null) {
                if (count < 2) {
                    this.telegramsCounts.remove(address);
                } else {
                    this.telegramsCounts.put(address, count - 1);
                }
            }

            timestamp = transaction.getTimestamp();
            List<TelegramMessage> telegrams = this.telegramsForTime.get(timestamp);
            i = 0;
            for (TelegramMessage telegram_item : telegrams) {
                if (Arrays.equals(telegram_item.getTransaction().getSignature(), signature)) {
                    telegrams.remove(i);
                    break;
                }
                i++;
            }
            if (telegrams.isEmpty())
                this.telegramsForTime.remove(timestamp);
            else
                this.telegramsForTime.put(timestamp, telegrams);
        }

        return left;
    }

    /**
     * Do internal commands sent in transactions message as JSON.
     * ... "__DELETE": "list": ["BaseSignature",..], "toTime":TIMESTAMP, ...
     *
     * @param transactionCommand transaction from telegram
     * @return true if has LOAD and need to save local
     */
    protected boolean try_command(Transaction transactionCommand) {

        boolean hasLoad = true;

        if (transactionCommand.getType() == Transaction.SEND_ASSET_TRANSACTION) {
            R_Send tx = (R_Send) transactionCommand;
            if (tx.isEncrypted() || !tx.isText() || tx.getData() == null)
                return hasLoad;

            String message;
            try {
                message = new String(tx.getData(), "UTF-8");
            } catch (UnsupportedEncodingException e) {
                return hasLoad;
            }

            if (message == null)
                return hasLoad;

            JSONObject jsonObject;
            try {
                jsonObject = (JSONObject) JSONValue.parse(message);
            } catch (Exception e) {
                return hasLoad;
            }

            if (jsonObject == null)
                return hasLoad;

            if (jsonObject.containsKey("__DELETE")) {

                // if THERE only ONE command DELETE - not broadcast if not delete
                JSONObject delete = (JSONObject) jsonObject.remove("__DELETE");

                PublicKeyAccount commander = transactionCommand.getCreator();
                if (delete.containsKey("list")) {

                    // TRY MAKE DELETION LIST
                    List<String> deleteList = new ArrayList<>();

                    for (Object sign58 : (JSONArray) delete.get("list")) {
                        TelegramMessage telegramToDelete = this.handledTelegrams.get((String) sign58);
                        if (telegramToDelete != null
                                && telegramToDelete.getTransaction().getCreator().equals(commander)) {
                            // IT FOUND and same CREATOR
                            deleteList.add((String) sign58);
                        }
                    }

                    // DELETE FOUNDED LIST
                    if (!deleteList.isEmpty()) {
                        deleteList(deleteList);
                    }

                }
                if (delete.containsKey("toTime")) {
                    long timestamp = (Long) delete.get("toTime");

                    // TRY MAKE DELETION LIST
                    List<String> deleteList = new ArrayList<>();

                    SortedMap<Long, List<TelegramMessage>> subMap = telegramsForTime.headMap(timestamp);
                    for (Entry<Long, List<TelegramMessage>> item : subMap.entrySet()) {
                        List<TelegramMessage> telegramsTimestamp = item.getValue();
                        if (telegramsTimestamp != null) {
                            for (TelegramMessage telegram : telegramsTimestamp) {
                                Transaction transaction = telegram.getTransaction();

                                if (commander.equals(transaction.getCreator())) {
                                    deleteList.add(transactionCommand.viewSignature());
                                }
                            }

                        }
                    }

                    // DELETE FOUNDED LIST
                    if (!deleteList.isEmpty()) {
                        deleteList(deleteList);
                    }
                }
            }

            hasLoad = !jsonObject.isEmpty();

        }

        return hasLoad;

    }

    /**
     * Заносит телеграмму в память Ноды или удаляет по списку
     *
     * @param telegram  телеграмма котрую надо добавить
     * @param firstItem если залан то будет удаление
     * @param timeKey   ключ времени с которой удалять
     * @return TRUE if not added
     */
    public synchronized boolean pipeAddRemove(TelegramMessage telegram, List<TelegramMessage> firstItem,
                                              long timeKey) {

        Transaction transaction;

        if (firstItem == null) {

            transaction = telegram.getTransaction();

            // CHECK IF SIGNATURE IS VALID OR GENESIS TRANSACTION
            Account creator = transaction.getCreator();
            if (creator == null || !transaction.isSignatureValid(DCSet.getInstance())) {
                // DISHONEST PEER
                ///this.tryDisconnect(telegram.getSender(), Synchronizer.BAN_BLOCK_TIMES,
                ///		"ban PeerOnError - invalid telegram signature");
                return true;
            }

            long timestamp = transaction.getTimestamp();
            if (timestamp > NTP.getTime() + 10000) {
                // DISHONEST PEER
                ///this.tryDisconnect(telegram.getSender(), Synchronizer.BAN_BLOCK_TIMES,
                ///		"ban PeerOnError - invalid telegram timestamp >>");
                return true;
            } else if (30000 + timestamp < NTP.getTime()) {
                // DISHONEST PEER
                ///this.tryDisconnect(telegram.getSender(), Synchronizer.BAN_BLOCK_TIMES,
                ///		"ban PeerOnError - invalid telegram timestamp <<");
                return true;
            }

            // TRY DO COMMANDS
            if (!try_command(transaction)) {
                // go broadcast
                return false;
            }

            String signatureKey;

            // CHECK IF LIST IS FULL
            if (this.handledTelegrams.size() > MAX_HANDLED_TELEGRAMS_SIZE) {
                List<TelegramMessage> telegrams = this.telegramsForTime.remove(this.telegramsForTime.firstKey());
                pipeAddRemove(null, telegrams, 0);
            }

            // signatureKey =
            // java.util.Base64.getEncoder().encodeToString(transaction.getSignature());
            signatureKey = Base58.encode(transaction.getSignature());

            if (false) {
                Message old_value = this.handledTelegrams.put(signatureKey, telegram.copy());
                if (old_value != null)
                    return true;
            } else {
                if (this.handledTelegrams.containsKey(signatureKey))
                    return true;


                this.handledTelegrams.put(signatureKey, telegram);

                if (Settings.getInstance().getTelegramStoreUse() && Settings.getInstance().getTelegramStorePeriod() > 0) {
                    // IF MY STORE is USED
                    if (Controller.getInstance().wallet.isWalletDatabaseExisting()) {

                        // save telegram to wallet DB
                        Transaction trans = telegram.getTransaction();
                        if (Controller.getInstance().wallet.accountExists(trans.getCreator().getAddress())) {
                            // add as my OUTCOME
                            Controller.getInstance().wallet.database.getTelegramsMap().add(signatureKey, telegram.getTransaction());
                        } else {
                            // TRY ADD as my INCOME
                            HashSet<Account> recipients = trans.getRecipientAccounts();
                            for (Account recipient : recipients) {
                                if (Controller.getInstance().wallet.accountExists(recipient.getAddress())) {
                                    Controller.getInstance().wallet.database.getTelegramsMap().add(signatureKey, telegram.getTransaction());
                                    break;
                                }
                            }
                        }
                    }
                }
            }

            // MAP timestamps
            List<TelegramMessage> timestampTelegrams = this.telegramsForTime.get(timestamp);
            if (timestampTelegrams == null) {
                timestampTelegrams = new ArrayList<TelegramMessage>();
            }

            timestampTelegrams.add(telegram);
            this.telegramsForTime.put((Long) timestamp, timestampTelegrams);

            HashSet<Account> recipients;
            String address;

            // MAP addresses
            recipients = transaction.getRecipientAccounts();
            if (recipients != null) {
                for (Account recipient : recipients) {
                    address = recipient.getAddress();
                    List<TelegramMessage> addressTelegrams = this.telegramsForAddress.get(address);
                    if (addressTelegrams == null) {
                        addressTelegrams = new ArrayList<TelegramMessage>();
                    }
                    addressTelegrams.add(telegram);
                    this.telegramsForAddress.put(address, addressTelegrams);
                }
            }

            address = transaction.getCreator().getAddress();
            if (this.telegramsCounts.containsKey(address)) {
                this.telegramsCounts.put(address, this.telegramsCounts.get(address) + 1);
            } else {
                this.telegramsCounts.put(address, 1);
            }

        } else {

            HashSet<Account> recipients;
            String address;
            int i;

            List<TelegramMessage> telegrams = firstItem;
            // for all signatures on this TIME
            for (TelegramMessage telegram_item : telegrams) {
                telegram = this.handledTelegrams.remove(Base58.encode(telegram_item.getTransaction().getSignature()));
                if (telegram != null) {
                    transaction = telegram.getTransaction();
                    byte[] signature = transaction.getSignature();
                    recipients = transaction.getRecipientAccounts();
                    if (recipients != null) {
                        for (Account recipient : recipients) {
                            address = recipient.getAddress();
                            List<TelegramMessage> addressTelegrams = this.telegramsForAddress.get(address);
                            if (addressTelegrams != null) {
                                i = 0;
                                for (TelegramMessage addressTelegram : addressTelegrams) {
                                    if (addressTelegram.getTransaction().getSignature().equals(signature)) {
                                        addressTelegrams.remove(i);
                                        break;
                                    }
                                    i++;
                                }
                            }
                            // IF list is empty
                            if (addressTelegrams.isEmpty()) {
                                this.telegramsForAddress.remove(address);
                            } else {
                                this.telegramsForAddress.put(address, addressTelegrams);
                            }
                        }
                    }

                    // CREATOR counts
                    address = transaction.getCreator().getAddress();
                    Integer count = this.telegramsCounts.get(address);
                    if (count != null) {
                        if (count < 2) {
                            this.telegramsCounts.remove(address);
                        } else {
                            this.telegramsCounts.put(address, count - 1);
                        }
                    }
                }
            }

            // by TIME
            if (timeKey > 0)
                this.telegramsForTime.remove(timeKey);
        }

        return false;

    }

    /**
     * endless cycle remove telegrams by time
     */

    public void run() {
        this.run = true;

        while (this.run && !this.isInterrupted()) {
            try {
                processMessage(blockingQueue.poll(1000, TimeUnit.MILLISECONDS));
            } catch (java.lang.OutOfMemoryError e) {
                Controller.getInstance().stopAll(81);
                break;
            } catch (java.lang.IllegalMonitorStateException e) {
                break;
            } catch (java.lang.InterruptedException e) {
                break;
            }

            long timestamp = NTP.getTime();

            synchronized (this.handledTelegrams) {

                do {
                    Entry<Long, List<TelegramMessage>> firstItem = this.telegramsForTime.firstEntry();
                    if (firstItem == null)
                        break;

                    long timeKey = firstItem.getKey();

                    if (timeKey + KEEP_TIME < timestamp) {
                        pipeAddRemove(null, firstItem.getValue(), timeKey);
                    } else {
                        break;
                    }
                } while (true);
            }

        }

        LOGGER.info("Telegram Manager halted");

    }

    public void halt() {
        this.run = false;
    }

}
