package network;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.log4j.Logger;

import controller.Controller;
import core.BlockChain;
import core.account.Account;
import core.crypto.Base58;
import core.transaction.R_Send;
import core.transaction.Transaction;
import datachain.DCSet;
import network.message.Message;
import network.message.TelegramMessage;
import ntp.NTP;
import org.jsoup.Connection;

public class TelegramManager extends Thread {
    /**
     * count telegrams
     */
    private static final int MAX_HANDLED_TELEGRAMS_SIZE = BlockChain.HARD_WORK ? 1 << 20 : 1 << 16;
    /**
     * time to live telegram
     */
    private static final int KEEP_TIME = 60000 * 60 * (BlockChain.HARD_WORK ? 2 : 8);
    static Logger LOGGER = Logger.getLogger(TelegramManager.class.getName());
    private Network network;
    private boolean isRun;
    // pool of messages
    private Map<String, TelegramMessage> handledTelegrams;
    // timestamp lists for clear
    private TreeMap<Long, List<TelegramMessage>> telegramsForTime;
    // lists for RECIPIENTS only address
    private Map<String, List<TelegramMessage>> telegramsForAddress;
    // counts for CREATORs COUNT
    private Map<String, Integer> telegramsCounts;

    public TelegramManager(Network network) {

        this.network = network;
        this.handledTelegrams = new HashMap<String, TelegramMessage>();
        this.telegramsForTime = new TreeMap<Long, List<TelegramMessage>>();
        this.telegramsForAddress = new HashMap<String, List<TelegramMessage>>();
        this.telegramsCounts = new HashMap<String, Integer>();

    }

    // GET telegram
    public TelegramMessage getTelegram(String signatureKey) {
        return handledTelegrams.get(signatureKey);
    }

    public Integer TelegramCount(){
        return handledTelegrams.size();
    }

    // GET telegrams for RECIPIENT from TIME
    public List<TelegramMessage> getTelegramsForAddress(String address, long timestamp, String filter) {
        // TelegramMessage telegram;
        List<TelegramMessage> telegrams = new ArrayList<TelegramMessage>();
        // ASK DATABASE FOR A LIST OF PEERS
        if (!Controller.getInstance().isOnStopping()) {
            List<TelegramMessage> telegramsAddress = telegramsForAddress.get(address);
            if (telegramsAddress == null)
                return telegrams;
            for (TelegramMessage telegram : telegramsAddress) {
                Transaction transaction = telegram.getTransaction();
                if (timestamp >= 0 && telegram.getTransaction().getTimestamp() < timestamp)
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

    public void delete (String signatureStr) {

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
        recipients = transaction.getRecipientAccounts();
        if (recipients != null && !recipients.isEmpty()) {
            int i;
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

        timestamp = transaction.getTimestamp();
        List<TelegramMessage> telegrams = this.telegramsForTime.get(timestamp);
        for (TelegramMessage telegram_item : telegrams) {
            if (signatureStr.equals(Base58.encode(telegram_item.getTransaction().getSignature()))) {
                telegrams.remove(signatureStr);
                break;
            }
            if (telegrams.isEmpty())
                this.telegramsForTime.remove(timestamp);
            else
                this.telegramsForTime.put(timestamp, telegrams);

        }
    }

    /**
     * DELETE telegrams from ALL maps
     *
     */
    public List<String> deleteList(List<String> signatures) {

        List<String> left = new ArrayList<>();
        HashSet<Account> recipients;
        String address;
        int i;
        TelegramMessage telegram;
        Transaction transaction;
        long timestamp;
        for (String signatureStr : signatures) {
            telegram = this.handledTelegrams.remove(signatureStr);
            if (telegram == null) {
                left.add(signatureStr);
                continue;
            }

            transaction = telegram.getTransaction();
            byte[] signature = transaction.getSignature();
            recipients = transaction.getRecipientAccounts();
            if (recipients != null && !recipients.isEmpty()) {
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

            timestamp = transaction.getTimestamp();
            List<TelegramMessage> telegrams = this.telegramsForTime.get(timestamp);
            for (TelegramMessage telegram_item : telegrams) {
                if (signatureStr.equals(Base58.encode(telegram_item.getTransaction().getSignature()))) {
                    telegrams.remove(signatureStr);
                    break;
                }

            }
            if (telegrams.isEmpty())
                this.telegramsForTime.remove(timestamp);
            else
                this.telegramsForTime.put(timestamp, telegrams);
        }

        return left;
    }

    /**
     * Remove telegram
     *
     * @param SignTelegram list of telegramMessage
     * @return list not remove signature(not found in list)
     */
    public List<TelegramMessage> deleteTelegram_old(List<TelegramMessage> SignTelegram) {

        // not use list
        List<TelegramMessage> notRemoveTelegram = new ArrayList<>();

        for (TelegramMessage signature : SignTelegram) {
            String signatureKye = Base58.encode((signature).getTransaction().getSignature());
            this.handledTelegrams.remove(signatureKye);
            this.telegramsForTime.remove((signature).getTransaction().getTimestamp());
            this.telegramsForAddress.remove((signature).getTransaction().getRecipientAccounts());
            this.telegramsCounts.remove((signature).getTransaction().getCreator());
        }
        return notRemoveTelegram;
    }

    // GET telegrams for RECIPIENT from TIME
    public List<TelegramMessage> getTelegramsFromTimestamp(long timestamp, String filter) {
        List<TelegramMessage> telegrams = new ArrayList<TelegramMessage>();
        if (!Controller.getInstance().isOnStopping()) {

            SortedMap<Long, List<TelegramMessage>> subMap = telegramsForTime.tailMap(timestamp);
            for (Entry<Long, List<TelegramMessage>> item : subMap.entrySet()) {
                List<TelegramMessage> telegramsTimestamp = item.getValue();
                if (telegramsTimestamp != null) {
                    for (TelegramMessage telegram : telegramsTimestamp) {
                        Transaction transaction = telegram.getTransaction();
                        if (timestamp >= 0 && telegram.getTransaction().getTimestamp() < timestamp)
                            continue;

                        if (filter != null && transaction.getType() == Transaction.SEND_ASSET_TRANSACTION) {
                            String head = ((R_Send) transaction).getHead();
                            if (!filter.equals(head))
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

    public synchronized boolean pipeAddRemove(TelegramMessage telegram, List<TelegramMessage> firstItem,
                                              long timeKey) {

        Transaction transaction;

        if (firstItem == null) {

            transaction = telegram.getTransaction();

            // CHECK IF SIGNATURE IS VALID OR GENESIS TRANSACTION
            Account creator = transaction.getCreator();
            if (creator == null || !transaction.isSignatureValid(DCSet.getInstance())) {
                // DISHONEST PEER
                ///this.network.tryDisconnect(telegram.getSender(), Synchronizer.BAN_BLOCK_TIMES,
                ///		"ban PeerOnError - invalid telegram signature");
                return true;
            }

            long timestamp = transaction.getTimestamp();
            if (timestamp > NTP.getTime() + 10000) {
                // DISHONEST PEER
                ///this.network.tryDisconnect(telegram.getSender(), Synchronizer.BAN_BLOCK_TIMES,
                ///		"ban PeerOnError - invalid telegram timestamp >>");
                return true;
            } else if (30000 + timestamp < NTP.getTime()) {
                // DISHONEST PEER
                ///this.network.tryDisconnect(telegram.getSender(), Synchronizer.BAN_BLOCK_TIMES,
                ///		"ban PeerOnError - invalid telegram timestamp <<");
                return true;
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

            Message old_value = this.handledTelegrams.put(signatureKey, telegram.copy());
            if (old_value != null)
                return true;

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
        int i;
        this.isRun = true;
        TelegramMessage telegram;
        String address;
        HashSet<Account> recipients;

        while (this.isRun && !this.isInterrupted()) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException es) {
                return;
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
    }

    public void halt() {
        this.isRun = false;
    }

}
