package org.erachain.core;

import org.erachain.controller.Controller;
import org.erachain.core.transaction.Transaction;
import org.erachain.datachain.DCSet;
import org.erachain.datachain.TransactionTabImpl;
import org.erachain.network.message.TransactionMessage;
import org.erachain.ntp.NTP;
import org.erachain.utils.MonitoredThread;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class TransactionsPool extends MonitoredThread {

    private final static boolean USE_MONITOR = false;
    private static final boolean LOG_UNCONFIRMED_PROCESS = BlockChain.DEVELOP_USE? false : false;
    private boolean runned;

    private static final Logger LOGGER = LoggerFactory.getLogger(TransactionsPool.class.getSimpleName());

    private static final int QUEUE_LENGTH = BlockChain.MAX_BLOCK_SIZE_GEN;
    BlockingQueue<Object> blockingQueue = new ArrayBlockingQueue<Object>(QUEUE_LENGTH);

    private Controller controller;
    private BlockChain blockChain;
    private DCSet dcSet;
    private TransactionTabImpl utxMap;
    private boolean needClearMap;

    public TransactionsPool(Controller controller, BlockChain blockChain, DCSet dcSet) {
        this.controller = controller;
        this.blockChain = blockChain;
        this.dcSet = dcSet;
        this.utxMap = dcSet.getTransactionTab();

        this.setName("Transactions Pool[" + this.getId() + "]");

        this.start();
    }

    /**
     * @param item
     */
    public boolean offerMessage(Object item) {
        boolean result = blockingQueue.offer(item);
        if (!result) {
            this.controller.network.missedTransactions.incrementAndGet();
        }
        return result;
    }

    public synchronized void needClear() {
        needClearMap = true;
    }

    private int clearCount;
    private long pointClear;
    public void processMessage(Object item) {

        if (item == null)
            return;

        if (item instanceof Transaction) {
            // ADD TO UNCONFIRMED TRANSACTIONS
            utxMap.add((Transaction) item);

        } else {

            long timeCheck = System.nanoTime();
            long onMessageProcessTiming = timeCheck;

            TransactionMessage transactionMessage = (TransactionMessage) item;

            // GET TRANSACTION
            Transaction transaction = transactionMessage.getTransaction();

            // CHECK IF SIGNATURE IS VALID ////// ------- OR GENESIS TRANSACTION
            if (transaction.getCreator() == null
                    || !transaction.isSignatureValid(DCSet.getInstance())) {
                // DISHONEST PEER
                this.controller.banPeerOnError(transactionMessage.getSender(), "invalid transaction signature");

                return;
            }

            // DEADTIME
            if (transaction.getDeadline() < this.blockChain.getTimestamp(this.dcSet)) {
                // so OLD transaction
                return;
            }

            if (LOG_UNCONFIRMED_PROCESS) {
                timeCheck = System.currentTimeMillis() - timeCheck;
                if (timeCheck > 10) {
                    LOGGER.debug("TRANSACTION_TYPE proccess 1 period: " + timeCheck);
                }
            }

            // ALREADY EXIST
            byte[] signature = transaction.getSignature();

            if (LOG_UNCONFIRMED_PROCESS)
                timeCheck = System.currentTimeMillis();

            if (utxMap.contains(signature)) {
                if (LOG_UNCONFIRMED_PROCESS) {
                    timeCheck = System.currentTimeMillis() - timeCheck;
                    if (timeCheck > 20) {
                        LOGGER.debug("TRANSACTION_TYPE process CONTAINS in UNC period: " + timeCheck);
                    }
                }
                return;
            }
            if (LOG_UNCONFIRMED_PROCESS) {
                timeCheck = System.currentTimeMillis() - timeCheck;
                if (timeCheck > 20) {
                    LOGGER.debug("TRANSACTION_TYPE proccess CONTAINS in UNC period: " + timeCheck);
                }
            }

            if (LOG_UNCONFIRMED_PROCESS)
                timeCheck = System.currentTimeMillis();

            if (this.dcSet.getTransactionFinalMapSigns().contains(signature) || this.controller.isOnStopping()) {
                return;
            }

            if (LOG_UNCONFIRMED_PROCESS) {
                timeCheck = System.currentTimeMillis() - timeCheck;
                if (timeCheck > 30) {
                    LOGGER.debug("TRANSACTION_TYPE proccess CONTAINS in FINAL period: " + timeCheck);
                }
            }

            // ADD TO UNCONFIRMED TRANSACTIONS
            utxMap.add(transaction);

            if (LOG_UNCONFIRMED_PROCESS) {
                timeCheck = System.currentTimeMillis() - timeCheck;
                if (timeCheck > 30) {
                    LOGGER.debug("TRANSACTION_TYPE proccess ADD period: " + timeCheck);
                }
            }

            // время обработки считаем тут
            onMessageProcessTiming = System.nanoTime() - onMessageProcessTiming;
            if (onMessageProcessTiming < 999999999999l) {
                // при переполнении может быть минус
                // в миеросекундах подсчет делаем
                onMessageProcessTiming /= 1000;
                this.controller.unconfigmedMessageTimingAverage = ((this.controller.unconfigmedMessageTimingAverage << 8)
                        + onMessageProcessTiming - this.controller.unconfigmedMessageTimingAverage) >> 8;
            }

            // проверяем на переполнение пула чтобы лишние очистить
            if (++clearCount > 1000 && System.currentTimeMillis() - pointClear
                    > BlockChain.GENERATING_MIN_BLOCK_TIME_MS(transaction.getTimestamp()) << 2) {

                clearCount = 0;

                if (controller.isStatusOK()) {
                    if (utxMap.size() > BlockChain.MAX_UNCONFIGMED_MAP_SIZE) {
                        utxMap.clearByDeadTimeAndLimit(NTP.getTime(), true);
                    }
                } else {
                    // если идет синхронизация, то удаляем все что есть не на текущее время
                    // и так как даже если мы вот-вот засинхримся мы все равно блок не сможем сразу собрать
                    // из-за мягкой синхронизации с сетью - а значит и нам не нужно заботиться об удаленных трнзакциях
                    // у нас - они будут включены другими нодами которые полностью в синхре
                    // мы выстыпаем лишь как ретрнслятор - при этом у нас запас по времени хранения все равно должен быть
                    // чтобы помнить какие транзакции мы уже словили и ретранслировали
                    if (utxMap.size() > BlockChain.MAX_BLOCK_SIZE_GEN >> 2) {
                        utxMap.clearByDeadTimeAndLimit(NTP.getTime(), true);
                    }
                }

                pointClear = System.currentTimeMillis();
            }

            // если мы не в синхронизации - так как мы тогда
            // не знаем время текущее цепочки и не понимаем можно ли борадкастить дальше трнзакцию
            // так как непонятно - протухла она или нет

            // BROADCAST
            controller.network.broadcast(transactionMessage, false);
        }

    }

    public void run() {

        long poinClear = 0;

        runned = true;
        //Message message;
        while (runned) {

            if (needClearMap) {
                /////// CLEAR
                try {
                    boolean needFlush = System.currentTimeMillis() - poinClear - 1000 > BlockChain.GENERATING_MIN_BLOCK_TIME_MS(BlockChain.VERS_30SEC + 1) << 2;
                    // try repopulate UTX table
                    if (needFlush) {
                        LOGGER.debug("try CLEAR UTXs");
                        poinClear = System.currentTimeMillis();
                        int height = dcSet.getBlocksHeadsMap().size();
                        int sizeUTX = utxMap.size();
                        LOGGER.debug("try CLEAR UTXs, size: " + sizeUTX);
                        // нужно скопировать из таблици иначе после закрытия ее ошибка обращения
                        // так .values() выдает не отдельный массив а объект базы данных!
                        Transaction[] items = utxMap.values().toArray(new Transaction[]{});
                        utxMap.clear();
                        long timestamp = Controller.getInstance().getBlockChain().getTimestamp(height);
                        int countDeleted = 0;
                        if (sizeUTX < BlockChain.MAX_UNCONFIGMED_MAP_SIZE) {
                            for (Transaction item : items) {
                                if (timestamp > item.getDeadline()) {
                                    countDeleted++;
                                    continue;
                                }
                                utxMap.add(item);
                            }
                        } else {
                            // переполненение - удалим все старые
                            int i = sizeUTX;
                            Transaction item;
                            do {
                                item = items[--i];
                                if (timestamp > item.getDeadline())
                                    continue;
                                utxMap.add(item);
                            } while (sizeUTX - i < BlockChain.MAX_UNCONFIGMED_MAP_SIZE);
                            countDeleted = sizeUTX - i;
                        }

                        LOGGER.debug("ADDED UTXs: " + utxMap.size() + " for " + (System.currentTimeMillis() - poinClear)
                                + " ms, DELETED:  " + countDeleted);

                    }
                } catch (OutOfMemoryError e) {
                    LOGGER.error(e.getMessage(), e);
                    Controller.getInstance().stopAll(56);
                    return;
                } catch (IllegalMonitorStateException e) {
                    break;
                } catch (Exception e) {
                    LOGGER.error(e.getMessage(), e);
                } catch (Throwable e) {
                    LOGGER.error(e.getMessage(), e);
                }
            }

            // PROCESS
            try {
                processMessage(blockingQueue.take());
            } catch (OutOfMemoryError e) {
                LOGGER.error(e.getMessage(), e);
                Controller.getInstance().stopAll(56);
                return;
            } catch (IllegalMonitorStateException e) {
                break;
            } catch (InterruptedException e) {
                break;
            } catch (Exception e) {
                LOGGER.error(e.getMessage(), e);
            } catch (Throwable e) {
                LOGGER.error(e.getMessage(), e);
            }

        }

        LOGGER.info("Transactions Pool halted");
    }

    public void halt() {
        this.runned = false;
    }

}
