package org.erachain.core.transaction;

import com.google.common.primitives.Bytes;
import com.google.common.primitives.Longs;
import org.erachain.core.account.Account;
import org.erachain.core.block.Block;
import org.erachain.core.crypto.Base58;
import org.erachain.core.crypto.Crypto;
import org.json.simple.JSONObject;
import org.mapdb.Fun.Tuple3;
import org.mapdb.Fun.Tuple4;
import org.mapdb.Fun.Tuple5;

import java.util.Arrays;
import java.util.HashSet;

public class GenesisCertifyPersonRecord extends Genesis_Record {

    private static final byte TYPE_ID = (byte) Transaction.GENESIS_CERTIFY_PERSON_TRANSACTION;
    private static final String NAME_ID = "GENESIS Certify Person";
    private static final int RECIPIENT_LENGTH = TransactionAmount.RECIPIENT_LENGTH;


    private static final int BASE_LENGTH = Genesis_Record.BASE_LENGTH + RECIPIENT_LENGTH + KEY_LENGTH;

    private Account recipient;
    private long key;

    public GenesisCertifyPersonRecord(Account recipient, long key) {
        super(TYPE_ID, NAME_ID);
        this.recipient = recipient;
        this.key = key;
        this.generateSignature();
    }

    //GETTERS/SETTERS
    //public static String getName() { return NAME; }

    public static Transaction Parse(byte[] data) throws Exception {

        //CHECK IF WE MATCH BLOCK LENGTH
        if (data.length < BASE_LENGTH) {
            throw new Exception("Data does not match block length: " + data.length + " in " + NAME_ID);
        }

        // READ TYPE
        //byte[] typeBytes = Arrays.copyOfRange(data, 0, SIMPLE_TYPE_LENGTH);
        int position = SIMPLE_TYPE_LENGTH;

        //READ RECIPIENT
        byte[] recipientBytes = Arrays.copyOfRange(data, position, position + RECIPIENT_LENGTH);
        Account recipient = new Account(Base58.encode(recipientBytes));
        position += RECIPIENT_LENGTH;

        //READ KEY
        byte[] keyBytes = Arrays.copyOfRange(data, position, position + KEY_LENGTH);
        long key = Longs.fromByteArray(keyBytes);
        position += KEY_LENGTH;

        return new GenesisCertifyPersonRecord(recipient, key);
    }

    public Account getRecipient() {
        return this.recipient;
    }

    @Override
    public long getKey() {
        return this.key;
    }

    //PARSE/CONVERT

    @SuppressWarnings("unchecked")
    @Override
    public JSONObject toJson() {
        //GET BASE
        JSONObject transaction = super.toJson();

        //ADD CREATOR/RECIPIENT/AMOUNT/ASSET
        transaction.put("recipient", this.recipient.getAddress());
        transaction.put("person", this.key);

        return transaction;
    }

    //@Override
    @Override
    public byte[] toBytes(int forDeal, boolean withSignature) {
        byte[] data = super.toBytes(forDeal, withSignature);

        //WRITE RECIPIENT
        data = Bytes.concat(data, Base58.decode(this.recipient.getAddress()));

        //WRITE KEY
        byte[] keyBytes = Longs.toByteArray(this.key);
        keyBytes = Bytes.ensureCapacity(keyBytes, KEY_LENGTH, 0);
        data = Bytes.concat(data, keyBytes);

        return data;
    }

    @Override
    public int getDataLength(int forDeal, boolean withSignature) {

        return BASE_LENGTH;
    }


    //VALIDATE

    @Override
    public int isValid(int asDeal, long flags) {

        //CHECK IF RECIPIENT IS VALID ADDRESS
        if (!Crypto.getInstance().isValidAddress(this.recipient.getAddress())) {
            return INVALID_ADDRESS;
        }

        if (!this.dcSet.getItemPersonMap().contains(this.key)) {
            return Transaction.ITEM_PERSON_NOT_EXIST;
        }

        return VALIDATE_OK;
    }

    //PROCESS/ORPHAN

    @Override
    public void process(Block block, int asDeal) {

        //Block block = new GenesisBlock();
        int transactionIndex = -1;
        int blockIndex = -1;
        //Block block = this.getBlock(db);// == null (((
        if (block == null) {
            blockIndex = this.dcSet.getBlockMap().last().getHeight();
        } else {
            blockIndex = block.getHeight();
            if (blockIndex < 1) {
                // if block not is confirmed - get last block + 1
                blockIndex = this.dcSet.getBlockMap().last().getHeight() + 1;
            }
            //transactionIndex = this.getSeqNo(db);
            transactionIndex = block.getTransactionSeq(signature);
        }

        //UPDATE RECIPIENT
        Tuple5<Long, Long, byte[], Integer, Integer> itemP =
                new Tuple5<Long, Long, byte[], Integer, Integer>
                        (timestamp, Long.MAX_VALUE, null, blockIndex, transactionIndex);

        // SET ALIVE PERSON for DURATION permanent
        ///db.getPersonStatusMap().addItem(this.key, StatusCls.ALIVE_KEY, itemP);

        // SET PERSON ADDRESS - end date as timestamp
        Tuple4<Long, Integer, Integer, Integer> itemA = new Tuple4<Long, Integer, Integer, Integer>(this.key, Integer.MAX_VALUE, blockIndex, transactionIndex);
        Tuple3<Integer, Integer, Integer> itemA1 = new Tuple3<Integer, Integer, Integer>(0, blockIndex, transactionIndex);
        this.dcSet.getAddressPersonMap().addItem(this.recipient.getAddress(), itemA);
        this.dcSet.getPersonAddressMap().addItem(this.key, this.recipient.getAddress(), itemA1);

        //UPDATE REFERENCE OF RECIPIENT
        this.recipient.setLastTimestamp(this.timestamp, this.dcSet);
    }

    @Override
    public void orphan(int asDeal) {

        // UNDO ALIVE PERSON for DURATION
        //db.getPersonStatusMap().removeItem(this.key, StatusCls.ALIVE_KEY);

        //UPDATE RECIPIENT
        this.dcSet.getAddressPersonMap().removeItem(this.recipient.getAddress());
        this.dcSet.getPersonAddressMap().removeItem(this.key, this.recipient.getAddress());

        //UPDATE REFERENCE OF CREATOR
        // not needthis.creator.setLastReference(this.reference, db);
        //UPDATE REFERENCE OF RECIPIENT
        this.recipient.removeLastTimestamp(this.dcSet);
    }

    //REST

    @Override
    public HashSet<Account> getRecipientAccounts() {
        HashSet<Account> accounts = new HashSet<>();
        accounts.add(this.recipient);
        return accounts;
    }

    @Override
    public boolean isInvolved(Account account) {
        String address = account.getAddress();

        if (address.equals(recipient.getAddress())) {
            return true;
        }

        return false;
    }

}