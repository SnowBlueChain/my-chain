package org.erachain.core.transaction;

import com.google.common.primitives.Bytes;
import com.google.common.primitives.Ints;
import com.google.common.primitives.Longs;
import org.erachain.core.BlockChain;
import org.erachain.core.account.Account;
import org.erachain.core.account.PublicKeyAccount;
import org.erachain.core.block.Block;
import org.erachain.core.item.ItemCls;
import org.erachain.core.item.polls.PollCls;
import org.erachain.datachain.DCSet;
import org.json.simple.JSONObject;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.HashSet;

public class VoteOnItemPollTransaction extends Transaction implements Itemable {
    private static final byte TYPE_ID = (byte) VOTE_ON_ITEM_POLL_TRANSACTION;
    private static final String NAME_ID = "Vote on Item Poll";
    private static final int OPTION_SIZE_LENGTH = 4;

    private static final int LOAD_LENGTH = KEY_LENGTH + OPTION_SIZE_LENGTH;
    private static final int BASE_LENGTH_AS_MYPACK = Transaction.BASE_LENGTH_AS_MYPACK + LOAD_LENGTH;
    private static final int BASE_LENGTH_AS_PACK = Transaction.BASE_LENGTH_AS_PACK + LOAD_LENGTH;
    private static final int BASE_LENGTH = Transaction.BASE_LENGTH + LOAD_LENGTH;
    private static final int BASE_LENGTH_AS_DBRECORD = Transaction.BASE_LENGTH_AS_DBRECORD + LOAD_LENGTH;
    public int option;
    private long key;
    private PollCls poll;

    public VoteOnItemPollTransaction(byte[] typeBytes, PublicKeyAccount creator, long pollKey, int option, byte feePow, long timestamp, Long reference) {
        super(typeBytes, NAME_ID, creator, feePow, timestamp, reference);

        this.key = pollKey;
        this.option = option;
    }

    public VoteOnItemPollTransaction(byte[] typeBytes, PublicKeyAccount creator, long pollKey, int option, byte feePow, long timestamp, Long reference, byte[] signature) {
        this(typeBytes, creator, pollKey, option, feePow, timestamp, reference);
        this.signature = signature;
    }
    public VoteOnItemPollTransaction(byte[] typeBytes, PublicKeyAccount creator, long pollKey, int option, byte feePow,
                                     long timestamp, Long reference, byte[] signature, long feeLong) {
        this(typeBytes, creator, pollKey, option, feePow, timestamp, reference);
        this.signature = signature;
        this.fee = BigDecimal.valueOf(feeLong, BlockChain.AMOUNT_DEDAULT_SCALE);
    }

    // as pack
    public VoteOnItemPollTransaction(byte[] typeBytes, PublicKeyAccount creator, long pollKey, int option, Long reference, byte[] signature) {
        this(typeBytes, creator, pollKey, option, (byte) 0, 0l, reference);
        this.signature = signature;
    }

    public VoteOnItemPollTransaction(PublicKeyAccount creator, long pollKey, int option, byte feePow, long timestamp, Long reference, byte[] signature) {
        this(new byte[]{TYPE_ID, 0, 0, 0}, creator, pollKey, option, feePow, timestamp, reference, signature);
    }

    public VoteOnItemPollTransaction(PublicKeyAccount creator, long pollKey, int option, byte feePow, long timestamp, Long reference) {
        this(new byte[]{TYPE_ID, 0, 0, 0}, creator, pollKey, option, feePow, timestamp, reference);
    }

    public VoteOnItemPollTransaction(PublicKeyAccount creator, long pollKey, int option, Long reference) {
        this(new byte[]{TYPE_ID, 0, 0, 0}, creator, pollKey, option, (byte) 0, 0l, reference);
    }

    //GETTERS/SETTERS
    //public static String getName() { return "Vote on Poll"; }

    @Override
    public long getKey() {
        return this.key;
    }

    @Override
    public ItemCls getItem()
    {
        if (poll == null) {
            poll = (PollCls) dcSet.getItemPersonMap().get(key);
        }
        return this.poll;
    }

    public void setDC(DCSet dcSet, int asDeal, int blockHeight, int seqNo) {
        super.setDC(dcSet, asDeal, blockHeight, seqNo);

        this.poll = (PollCls) this.dcSet.getItemPollMap().get(this.key);
    }

    public int getOption() {
        return this.option;
    }

    @Override
    public boolean hasPublicText() {
        return false;
    }

    //PARSE CONVERT

    public static Transaction Parse(byte[] data, int asDeal) throws Exception {

        int test_len;
        if (asDeal == Transaction.FOR_MYPACK) {
            test_len = BASE_LENGTH_AS_MYPACK;
        } else if (asDeal == Transaction.FOR_PACK) {
            test_len = BASE_LENGTH_AS_PACK;
        } else if (asDeal == Transaction.FOR_DB_RECORD) {
            test_len = BASE_LENGTH_AS_DBRECORD;
        } else {
            test_len = BASE_LENGTH;
        }

        if (data.length < test_len) {
            throw new Exception("Data does not match block length " + data.length);
        }

        // READ TYPE
        byte[] typeBytes = Arrays.copyOfRange(data, 0, TYPE_LENGTH);
        int position = TYPE_LENGTH;

        long timestamp = 0;
        if (asDeal > Transaction.FOR_MYPACK) {
            //READ TIMESTAMP
            byte[] timestampBytes = Arrays.copyOfRange(data, position, position + TIMESTAMP_LENGTH);
            timestamp = Longs.fromByteArray(timestampBytes);
            position += TIMESTAMP_LENGTH;
        }

        //READ REFERENCE
        byte[] referenceBytes = Arrays.copyOfRange(data, position, position + REFERENCE_LENGTH);
        Long reference = Longs.fromByteArray(referenceBytes);
        position += REFERENCE_LENGTH;

        //READ CREATOR
        byte[] creatorBytes = Arrays.copyOfRange(data, position, position + CREATOR_LENGTH);
        PublicKeyAccount creator = new PublicKeyAccount(creatorBytes);
        position += CREATOR_LENGTH;

        byte feePow = 0;
        if (asDeal > Transaction.FOR_PACK) {
            //READ FEE POWER
            byte[] feePowBytes = Arrays.copyOfRange(data, position, position + 1);
            feePow = feePowBytes[0];
            position += 1;
        }

        //READ SIGNATURE
        byte[] signatureBytes = Arrays.copyOfRange(data, position, position + SIGNATURE_LENGTH);
        position += SIGNATURE_LENGTH;

        long feeLong = 0;
        if (asDeal == FOR_DB_RECORD) {
            // READ FEE
            byte[] feeBytes = Arrays.copyOfRange(data, position, position + FEE_LENGTH);
            feeLong = Longs.fromByteArray(feeBytes);
            position += FEE_LENGTH;
        }

        /////
        //READ POLL
        byte[] keyBytes = Arrays.copyOfRange(data, position, position + KEY_LENGTH);
        long pollKey = Longs.fromByteArray(keyBytes);
        position += KEY_LENGTH;

        //READ OPTION
        byte[] optionBytes = Arrays.copyOfRange(data, position, position + OPTION_SIZE_LENGTH);
        int option = Ints.fromByteArray(optionBytes);
        position += OPTION_SIZE_LENGTH;

        if (asDeal > Transaction.FOR_MYPACK) {
            return new VoteOnItemPollTransaction(typeBytes, creator, pollKey, option, feePow, timestamp, reference,
                    signatureBytes, feeLong);
        } else {
            return new VoteOnItemPollTransaction(typeBytes, creator, pollKey, option, reference, signatureBytes);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public JSONObject toJson() {
        //GET BASE
        JSONObject transaction = this.getJsonBase();

        transaction.put("key", this.key);
        transaction.put("option", this.option);

        return transaction;
    }

    @Override
    public byte[] toBytes(int forDeal, boolean withSignature) {

        byte[] data = super.toBytes(forDeal, withSignature);

        //WRITE POLL KEY
        byte[] keyBytes = Longs.toByteArray(this.key);
        keyBytes = Bytes.ensureCapacity(keyBytes, KEY_LENGTH, 0);
        data = Bytes.concat(data, keyBytes);

        //WRITE OPTION
        byte[] optionBytes = Ints.toByteArray(this.option);
        optionBytes = Bytes.ensureCapacity(optionBytes, OPTION_SIZE_LENGTH, 0);
        data = Bytes.concat(data, optionBytes);

        return data;
    }

    @Override
    public int getDataLength(int forDeal, boolean withSignature) {
        int base_len;
        if (forDeal == FOR_MYPACK)
            base_len = BASE_LENGTH_AS_MYPACK;
        else if (forDeal == FOR_PACK)
            base_len = BASE_LENGTH_AS_PACK;
        else if (forDeal == FOR_DB_RECORD)
            base_len = BASE_LENGTH_AS_DBRECORD;
        else
            base_len = BASE_LENGTH;

        if (!withSignature)
            base_len -= SIGNATURE_LENGTH;

        return base_len;
    }

    //VALIDATE

    //@Override
    @Override
    public int isValid(int asDeal, long flags) {

        //CHECK POLL EXISTS
        if (poll == null) {
            return POLL_NOT_EXISTS;
        }

        //CHECK OPTION EXISTS
        if (poll.getOptions().size() - 1 < this.option || this.option < 0) {
            return POLL_OPTION_NOT_EXISTS;
        }

        return super.isValid(asDeal, flags);

    }

    //PROCESS/ORPHAN

    public void process(Block block, int asDeal) {
        //UPDATE CREATOR
        super.process(block, asDeal);

        //ADD VOTE TO POLL
        this.dcSet.getVoteOnItemPollMap().addItem(this.key, this.option, new BigInteger(this.creator.getShortAddressBytes()),
                this.getHeightSeqNo());

    }


    //@Override
    @Override
    public void orphan(Block block, int asDeal) {
        //UPDATE CREATOR
        super.orphan(block, asDeal);

        //DELETE VOTE FROM POLL
        this.dcSet.getVoteOnItemPollMap().removeItem(this.key, this.option, new BigInteger(this.creator.getShortAddressBytes()));
    }

    @Override
    public HashSet<Account> getInvolvedAccounts() {
        HashSet<Account> accounts = new HashSet<Account>();
        accounts.add(this.creator);
        return accounts;
    }

    @Override
    public HashSet<Account> getRecipientAccounts() {
        return new HashSet<>();
    }

    @Override
    public boolean isInvolved(Account account) {
        String address = account.getAddress();

        if (address.equals(this.creator.getAddress())) {
            return true;
        }

        return false;
    }

}
