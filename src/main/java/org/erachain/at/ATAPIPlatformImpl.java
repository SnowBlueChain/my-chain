package org.erachain.at;

import com.google.common.primitives.Bytes;
import com.google.common.primitives.Longs;
import org.erachain.core.account.Account;
import org.erachain.core.block.Block;
import org.erachain.core.crypto.Base58;
import org.erachain.core.crypto.Crypto;
import org.erachain.core.transaction.RSend;
import org.erachain.core.transaction.Transaction;
import org.erachain.core.transaction.TransactionAmount;
import org.erachain.datachain.ATTransactionMap;
import org.erachain.datachain.DCSet;
import org.erachain.datachain.TransactionFinalMap;
import org.erachain.datachain.TransactionFinalMapImpl;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import org.mapdb.Fun.Tuple2;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.LinkedHashMap;


//CORE AT API IMPLEMENTATION
public class ATAPIPlatformImpl extends ATAPIImpl {


    private static final Logger LOGGER = LoggerFactory            .getLogger(ATAPIPlatformImpl.class);

    private final static ATAPIPlatformImpl instance = new ATAPIPlatformImpl();

    private final static int FIX_HEIGHT_0 = 142000;

    private DCSet dcSet;

    ATAPIPlatformImpl() {

    }

    public static ATAPIPlatformImpl getInstance() {
        return instance;
    }

    protected static long findTransactionAfterHeight(int startHeight, Account account, int numOfTx, ATMachineState state, DCSet dcSet) {
        //IF STARTHEIGHT IS VALID
        if (startHeight < 0) {
            return 0;
        }

        //STARTHEIGHT SHOULD BE GREATER OR EQUAL THAN CREATION BLOCK HEIGHT
        if (startHeight < state.getCreationBlockHeight()) {
            startHeight = state.getCreationBlockHeight();
        }

        int forkHeight = startHeight; //getForkHeight(dcSet);

        Tuple2<Integer, Integer> atTxT = dcSet.getATTransactionMap().getNextATTransaction(startHeight, numOfTx, account.getAddress());

        int atTxs = dcSet.getATTransactionMap().getATTransactions(startHeight).size();

        Long tx = dcSet.getTransactionFinalMap().getTransactionsAfterTimestamp(startHeight, (numOfTx > atTxs) ? numOfTx - atTxs : 0, account.getShortAddressBytes());
        Tuple2<Integer, Integer> pair = Transaction.parseDBRef(tx);

        if (forkHeight > 0) {
            Tuple2<Integer, Integer> atTxTp = ((ATTransactionMap) dcSet.getATTransactionMap().getParent()).getNextATTransaction(startHeight, numOfTx, account.getAddress());
            int atTxsp = ((ATTransactionMap) dcSet.getATTransactionMap().getParent()).getATTransactions(startHeight).size();

            Long txp = ((TransactionFinalMap) dcSet.getTransactionFinalMap()
                    //.getParentMap() было в DCU - сечас нету в TAB & SUIT
            )
                    .getTransactionsAfterTimestamp(startHeight, (numOfTx > atTxs) ? numOfTx - atTxsp : 0, account.getShortAddressBytes());
            Tuple2<Integer, Integer> pairP = Transaction.parseDBRef(txp);
            if (atTxTp != null && (txp == null || atTxTp.a <= pairP.a) && atTxTp.a < forkHeight) {
                ATTransaction atTx = ((ATTransactionMap) dcSet.getATTransactionMap().getParent()).get(atTxTp);
                if (!atTx.getSender().equalsIgnoreCase(account.getAddress()) && atTx.getRecipient().equalsIgnoreCase(account.getAddress()) && atTx.getAmount() > state.minActivationAmount()) {
                    return ATAPIHelper.getLongTimestamp(atTxTp.a, atTxTp.b + 1);
                }

            } else if (txp != null && pairP.a < forkHeight) {
                atTxs = ((ATTransactionMap) dcSet.getATTransactionMap().getParent()).getATTransactions(pairP.a).size();
                Transaction transaction = ((TransactionFinalMapImpl) dcSet.getTransactionFinalMap()
                        //.getParentMap() было в DCU - сечас нету в TAB & SUIT
                ).get(txp);

                long txAmount = getAmount((Transaction) transaction, new Account(Base58.encode(state.getId())), state.getHeight());

                if (transaction.isInvolved(account) && !transaction.getCreator().getAddress().equals(account.getAddress()) && txAmount >= state.minActivationAmount()) {
                    return ATAPIHelper.getLongTimestamp(pair.a, pair.b + atTxs);
                }
            }
        }
        if (atTxT != null && (tx == null || atTxT.a <= pair.a)) {
            ATTransaction atTx = dcSet.getATTransactionMap().get(atTxT);
            if (!atTx.getSender().equalsIgnoreCase(account.getAddress()) && atTx.getRecipient().equalsIgnoreCase(account.getAddress()) && atTx.getAmount() > state.minActivationAmount()) {
                return ATAPIHelper.getLongTimestamp(atTxT.a, atTxT.b + 1);
            }
        } else if (tx != null) {
            atTxs = dcSet.getATTransactionMap().getATTransactions(pair.a).size();
            Transaction transaction = dcSet.getTransactionFinalMap().get(tx);

            long txAmount = getAmount(transaction, new Account(Base58.encode(state.getId())), state.getHeight());

            if (transaction.isInvolved(account) && !transaction.getCreator().getAddress().equals(account.getAddress()) && txAmount >= state.minActivationAmount()) {
                return ATAPIHelper.getLongTimestamp(pair.a, pair.b + atTxs);
            }
        }

        return 0;

    }

    protected static long getAmount(Transaction tx, Account recipient, int height) {
        if (tx instanceof TransactionAmount) {
            tx = (TransactionAmount) tx;
            byte[] amountB = (height >= FIX_HEIGHT_0) ? tx.getAmount(recipient).unscaledValue().toByteArray() :
                    tx.getAmount(tx.getCreator()).unscaledValue().toByteArray();
            if (amountB.length < 8) {
                byte[] fill = new byte[8 - amountB.length];
                amountB = Bytes.concat(fill, amountB);
            }

            long txAmount = Longs.fromByteArray(amountB);
            return txAmount;
        }
        return 0;
    }

    protected static Object findTransaction(byte[] id, DCSet db) {
        int height = ATAPIHelper.longToHeight(ATAPIHelper.getLong(id));
        int position = ATAPIHelper.longToNumOfTx(ATAPIHelper.getLong(id));

        if (position <= 0) {
            return null;
        }

        int forkHeight = height; //getForkHeight(db);

        //IF NOT FORK
        if (forkHeight == 0 || forkHeight <= height) {
            LinkedHashMap<Tuple2<Integer, Integer>, ATTransaction> atTxs = db.getATTransactionMap().getATTransactions(height);

            if (atTxs.size() >= position) {
                ATTransaction key = atTxs.get(new Tuple2<Integer, Integer>(height, position - 1));
                return key;
            } else {
                return db.getTransactionFinalMap().get(height, position - atTxs.size());
            }
        } else if (forkHeight > height) {
            LinkedHashMap<Tuple2<Integer, Integer>, ATTransaction> atTxs = ((ATTransactionMap) db.getATTransactionMap().getParent()).getATTransactions(height);

            if (atTxs.size() >= position) {
                ATTransaction key = atTxs.get(new Tuple2<Integer, Integer>(height, position - 1));
                return key;
            } else {
                return db.getTransactionFinalMap().get(height, position - atTxs.size());
            }

        }

        return null;
    }

    public void setDBSet(DCSet newDBSet) {
        dcSet = newDBSet;
    }

    @Override
    public long get_Block_Timestamp(ATMachineState state) {

        Block lastBlock = dcSet.getBlockMap().last();
        return ATAPIHelper.getLongTimestamp(lastBlock.getHeight() + 1, 0);
    }

    public long get_Creation_Timestamp(ATMachineState state) {
        return ATAPIHelper.getLongTimestamp(state.getCreationBlockHeight(), 0);
    }

    @Override
    public long get_Last_Block_Timestamp(ATMachineState state) {
        Block block = dcSet.getBlockMap().last();
        return ATAPIHelper.getLongTimestamp(block.getHeight(), 0);
    }

    @Override
    public void put_Last_Block_Hash_In_A(ATMachineState state) {
        byte[] signature = dcSet.getBlockMap().getLastBlockSignature(); //128 BYTES
        byte[] hash = Crypto.getInstance().digest(signature); //32 BYTES

        state.set_A1(Arrays.copyOfRange(hash, 0, 8));
        state.set_A2(Arrays.copyOfRange(hash, 8, 16));
        state.set_A3(Arrays.copyOfRange(hash, 16, 24));
        state.set_A4(Arrays.copyOfRange(hash, 24, 32));
    }

    @Override
    public void A_to_Tx_after_Timestamp(long val, ATMachineState state) {
        int height = ATAPIHelper.longToHeight(val);
        int numOfTx = ATAPIHelper.longToNumOfTx(val);

        byte[] address = state.getId();
        Account account = new Account(Base58.encode(address));

        clear_A(state);
        try {
            long transactionIDNew = findTransactionAfterHeight(height, account, numOfTx, state, dcSet);
            state.set_A1(ATAPIHelper.getByteArray(transactionIDNew));
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
        }

    }

    @Override
    public void A_to_Tx_at_Timestamp(long val, ATMachineState state) {
        clear_A(state);
        state.set_A1(ATAPIHelper.getByteArray(val));
    }

    @Override
    public long get_Type_for_Tx_in_A(ATMachineState state) {
        byte[] id = state.get_A1();
        Object transaction = findTransaction(id, dcSet);

        if (transaction != null) {
            if (!transaction.getClass().equals(ATTransaction.class)) {
                Transaction tx = (Transaction) transaction;
                return (tx.getType() == Transaction.SEND_ASSET_TRANSACTION) ? 1 : 0;
            } else {
                ATTransaction tx = (ATTransaction) transaction;
                return (tx.getMessage().length > 0) ? 1 : 0;

            }
        }
        return -1;
    }

    @Override
    public long get_Amount_for_Tx_in_A(ATMachineState state) {
        byte[] id = state.get_A1();
        Object transaction = findTransaction(id, dcSet);
        long amount = -1;


        if (transaction != null) {
            long txAmount = 0;
            if (!transaction.getClass().equals(ATTransaction.class)) {
                txAmount = getAmount((Transaction) transaction, new Account(Base58.encode(state.getId())), state.getHeight());
            } else {
                txAmount = ((ATTransaction) transaction).getAmount();
            }
            if (state.minActivationAmount() <= txAmount)
                amount = txAmount - state.minActivationAmount();
        }

        return amount;
    }

    @Override
    public long get_Timestamp_for_Tx_in_A(ATMachineState state) {

        byte[] id = state.get_A1();
        Object transaction = findTransaction(id, dcSet);
        if (transaction != null) {
            return ATAPIHelper.getLong(id);
        }
        return -1;
    }

    @Override
    public long get_Random_Id_for_Tx_in_A(ATMachineState state) {
        Transaction transaction = (Transaction) findTransaction(state.get_A1(), dcSet);

        if (transaction != null) {

            int txBlockHeight;
            int blockHeight = dcSet.getBlockMap().last().getHeight() + 1;
            byte[] senderPublicKey = new byte[32];

            if (!transaction.getClass().equals(ATTransaction.class)) {
                txBlockHeight = transaction.getBlockHeightByParentOrLast(dcSet);
                senderPublicKey = transaction.getCreator().getPublicKey();
            } else {
                txBlockHeight = ATAPIHelper.longToHeight(ATAPIHelper.getLong(state.get_A1()));
            }

            if (blockHeight - txBlockHeight < ATConstants.getInstance().BLOCKS_FOR_TICKET(blockHeight)) { //for tests - for real case 1440
                state.setWaitForNumberOfBlocks((int) ATConstants.getInstance().BLOCKS_FOR_TICKET(blockHeight) - (blockHeight - txBlockHeight));
                state.getMachineState().pc -= 7;
                state.getMachineState().stopped = true;
                return 0;
            }

            byte[] sig = dcSet.getBlockMap().getLastBlockSignature();
            ByteBuffer bf = ByteBuffer.allocate(sig.length + Long.SIZE + senderPublicKey.length);
            bf.order(ByteOrder.LITTLE_ENDIAN);

            bf.put(dcSet.getBlockMap().getLastBlockSignature());
            bf.put(state.get_A1());
            bf.put(senderPublicKey);

            byte[] byteTicket = Crypto.getInstance().digest(bf.array());

            long ticket = Math.abs(ATAPIHelper.getLong(Arrays.copyOfRange(byteTicket, 0, 8)));

            return ticket;
        }
        return -1;
    }

    @Override
    public void message_from_Tx_in_A_to_B(ATMachineState state) {
        Object tx = findTransaction(state.get_A1(), dcSet); //25 BYTES


        ByteBuffer b = ByteBuffer.allocate(state.get_B1().length * 4);
        b.order(ByteOrder.LITTLE_ENDIAN);

        if (tx != null) {
            if (tx instanceof RSend) {
                RSend txMessage = (RSend) tx;
                if (txMessage != null) {
                    byte[] message = txMessage.getData();
                    if (message.length <= state.get_B1().length * 4) {
                        b.put(message);
                    }

                }

            } else if (tx.getClass().equals(ATTransaction.class)) {
                ATTransaction txAT = (ATTransaction) tx;
                byte[] message = txAT.getMessage();
                if (message != null && message.length > 0) {
                    b.put(txAT.getMessage());
                }
            }
        }

        b.clear();

        byte[] temp = new byte[8];

        b.get(temp, 0, 8);
        state.set_B1(temp);

        b.get(temp, 0, 8);
        state.set_B2(temp);

        b.get(temp, 0, 8);
        state.set_B3(temp);

        b.get(temp, 0, 8);
        state.set_B4(temp);


    }

    @Override
    public void B_to_Address_of_Tx_in_A(ATMachineState state) {
        Object tx = findTransaction(state.get_A1(), dcSet);

        clear_B(state);

        if (tx != null) {
            byte[] address;
            if (!tx.getClass().equals(ATTransaction.class)) {
                address = Base58.decode(((Transaction) tx).getCreator().getAddress()); //25 BYTES
            } else {
                address = ((ATTransaction) tx).getSenderId();
            }
            address = Bytes.ensureCapacity(address, 32, 0); // 32 BYTES
            clear_B(state);

            state.set_B1(Arrays.copyOfRange(address, 0, 8));
            state.set_B2(Arrays.copyOfRange(address, 8, 16));
            state.set_B3(Arrays.copyOfRange(address, 16, 24));
            state.set_B4(Arrays.copyOfRange(address, 24, 32));
        }

    }

    @Override
    public void B_to_Address_of_Creator(ATMachineState state) {
        byte[] address = state.getCreator(); //25 BYTES
        address = Bytes.ensureCapacity(address, 32, 0); // 32 BYTES

        clear_B(state);

        state.set_B1(Arrays.copyOfRange(address, 0, 8));
        state.set_B2(Arrays.copyOfRange(address, 8, 16));
        state.set_B3(Arrays.copyOfRange(address, 16, 24));
        state.set_B4(Arrays.copyOfRange(address, 24, 32));
    }

    @Override
    public long get_Current_Balance(ATMachineState state) {
        return state.getG_balance();
    }

    @Override
    public long get_Previous_Balance(ATMachineState state) {
        return state.getP_balance();
    }

    @Override
    public void send_to_Address_in_B(long key, long val, ATMachineState state) {

        if (val < 1) return;

        if (val < state.getG_balance()) {

            ByteBuffer b = ByteBuffer.allocate(state.get_B1().length * 4);
            b.order(ByteOrder.LITTLE_ENDIAN);

            b.put(state.get_B1());
            b.put(state.get_B2());
            b.put(state.get_B3());
            b.put(state.get_B4());

            b.clear();

            byte[] finalAddress = new byte[ATConstants.AT_ID_SIZE];

            b.get(finalAddress, 0, finalAddress.length);

            ATTransaction tx = new ATTransaction(state.getId(), finalAddress, key, val, null);
            state.addTransaction(tx);

            state.setG_balance(state.getG_balance() - val);

        } else {
            ByteBuffer b = ByteBuffer.allocate(state.get_B1().length * 4);
            b.order(ByteOrder.LITTLE_ENDIAN);

            b.put(state.get_B1());
            b.put(state.get_B2());
            b.put(state.get_B3());
            b.put(state.get_B4());

            b.clear();

            byte[] finalAddress = new byte[ATConstants.AT_ID_SIZE];

            b.get(finalAddress, 0, finalAddress.length);

            ATTransaction tx = new ATTransaction(state.getId(), finalAddress, key, state.getG_balance(), null);
            state.addTransaction(tx);

            state.setG_balance(0L);
        }
    }

    @Override
    public void send_All_to_Address_in_B(long key, ATMachineState state) {
        ByteBuffer b = ByteBuffer.allocate(state.get_B1().length * 4);
        b.order(ByteOrder.LITTLE_ENDIAN);

        b.put(state.get_B1());
        b.put(state.get_B2());
        b.put(state.get_B3());
        b.put(state.get_B4());

        byte[] recipientBytes = new byte[ATConstants.AT_ID_SIZE];
        b.clear();
        b.get(recipientBytes, 0, ATConstants.AT_ID_SIZE);

        ATTransaction tx = new ATTransaction(state.getId(), recipientBytes, key, state.getG_balance(), null);
        state.addTransaction(tx);

        state.setG_balance(0L);
    }

    @Override
    public void send_Old_to_Address_in_B(ATMachineState state) {

        ByteBuffer b = ByteBuffer.allocate(state.get_B1().length * 4);
        b.order(ByteOrder.LITTLE_ENDIAN);

        b.put(state.get_B1());
        b.put(state.get_B2());
        b.put(state.get_B3());
        b.put(state.get_B4());

        b.clear();

        byte[] finalAddress = new byte[ATConstants.AT_ID_SIZE];

        b.get(finalAddress, 0, finalAddress.length);

        if (state.getP_balance() > state.getG_balance()) {
            ATTransaction tx = new ATTransaction(state.getId(), finalAddress, Transaction.FEE_KEY, state.getG_balance(), null);
            state.addTransaction(tx);

            state.setG_balance(0L);
            state.setP_balance(0L);

        } else {
            ATTransaction tx = new ATTransaction(state.getId(), finalAddress, Transaction.FEE_KEY, state.getP_balance(), null);
            state.addTransaction(tx);

            state.setG_balance(state.getG_balance() - state.getP_balance());
            state.setP_balance(0l);

        }
    }

    //Send to B address the message stored in A
    @Override
    public void send_A_to_Address_in_B(ATMachineState state) {
        ByteBuffer a = ByteBuffer.allocate(state.get_A1().length * 4);
        a.order(ByteOrder.LITTLE_ENDIAN);
        a.put(state.get_A1());
        a.put(state.get_A2());
        a.put(state.get_A3());
        a.put(state.get_A4());
        a.clear();

        ByteBuffer b = ByteBuffer.allocate(state.get_B1().length * 4);
        b.order(ByteOrder.LITTLE_ENDIAN);

        b.put(state.get_B1());
        b.put(state.get_B2());
        b.put(state.get_B3());
        b.put(state.get_B4());

        b.clear();

        byte[] finalAddress = new byte[ATConstants.AT_ID_SIZE];

        b.get(finalAddress, 0, finalAddress.length);

        ATTransaction tx = new ATTransaction(state.getId(), finalAddress, Transaction.FEE_KEY, 0L, a.array());
        state.addTransaction(tx);

    }


	/*
	public static int getForkHeight(DCSet db)
	{
		//CHECK IF FORK
		if ( db.getBlocksHeadMap().getParentList() != null )
		{
			//FIND FORKHEIGHT
			if ( db.getBlocksHeadMap().getList().isEmpty()  )
			{
				return db.getBlocksHeadMap().getLastBlock().getHeight(db) + 1;
			}
			else
			{
				//return Collections.min(db.getHeightMap().getValuesAll());
				return db.getBlockSignsMap().getStartedInForkHeight();
			}
		}
		return 0;
	}
	*/

    public long add_Minutes_to_Timestamp(long val1, long val2, ATMachineState state) {
        int height = ATAPIHelper.longToHeight(val1);
        int numOfTx = ATAPIHelper.longToNumOfTx(val1);
        int addHeight = height + (int) (val2 / ATConstants.getInstance().AVERAGE_BLOCK_MINUTES(dcSet.getBlockMap().last().getHeight()));
        return ATAPIHelper.getLongTimestamp(addHeight, numOfTx);
    }

}
