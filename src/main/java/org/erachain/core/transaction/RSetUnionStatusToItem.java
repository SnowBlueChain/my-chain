package org.erachain.core.transaction;

import com.google.common.primitives.Bytes;
import com.google.common.primitives.Longs;
import org.erachain.core.BlockChain;
import org.erachain.core.account.Account;
import org.erachain.core.account.PublicKeyAccount;
import org.erachain.core.block.Block;
import org.erachain.core.item.ItemCls;
import org.erachain.core.item.statuses.StatusCls;
import org.json.simple.JSONObject;
import org.mapdb.Fun.Tuple5;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.HashSet;

// this.end_date == null -> MAX
public class RSetUnionStatusToItem extends Transaction {

    private static final byte TYPE_ID = (byte) Transaction.SET_UNION_STATUS_TO_ITEM_TRANSACTION;
    private static final String NAME_ID = "Set Union Status to Unit";
    private static final int DATE_LENGTH = Transaction.TIMESTAMP_LENGTH; // one year + 256 days max

    private static final int LOAD_LENGTH = 2 * DATE_LENGTH + KEY_LENGTH + KEY_LENGTH + 1 + KEY_LENGTH;
    protected static final int BASE_LENGTH_AS_MYPACK = Transaction.BASE_LENGTH_AS_MYPACK + LOAD_LENGTH;
    protected static final int BASE_LENGTH_AS_PACK = Transaction.BASE_LENGTH_AS_PACK + LOAD_LENGTH;
    protected static final int BASE_LENGTH = Transaction.BASE_LENGTH + LOAD_LENGTH;
    protected static final int BASE_LENGTH_AS_DBRECORD = Transaction.BASE_LENGTH_AS_DBRECORD + LOAD_LENGTH;

    protected Long key; // UNION KEY
    protected Long statusKey; // STATUS KEY
    protected StatusCls status; // STATUS
    protected int itemType; // ITEM TYPE (CAnnot read ITEMS on start DB - need reset ITEM after
    protected Long itemKey; // ITEM KEY
    protected long beg_date;
    protected long end_date = Long.MAX_VALUE;

    public RSetUnionStatusToItem(byte[] typeBytes, PublicKeyAccount creator, byte feePow, long key, int itemType, long itemKey,
                                 Long beg_date, Long end_date, long timestamp, Long reference) {
        super(typeBytes, NAME_ID, creator, feePow, timestamp, reference);

        this.key = key;
        this.itemType = itemType;
        this.itemKey = itemKey;
        if (beg_date == null || beg_date == 0) beg_date = Long.MIN_VALUE;
        this.beg_date = beg_date;
        if (end_date == null) end_date = Long.MAX_VALUE;
        this.end_date = end_date;
    }

    public RSetUnionStatusToItem(PublicKeyAccount creator, byte feePow, long key, int itemType, long itemKey,
                                 Long beg_date, Long end_date, long timestamp, Long reference) {
        this(new byte[]{TYPE_ID, (byte) 0, 0, 0}, creator, feePow, key, itemType, itemKey,
                beg_date, end_date, timestamp, reference);
    }

    // set default date
    public RSetUnionStatusToItem(PublicKeyAccount creator, byte feePow, long key, int itemType, long itemKey,
                                 long timestamp, Long reference) {
        this(new byte[]{TYPE_ID, (byte) 0, 0, 0}, creator, feePow, key, itemType, itemKey,
                Long.MIN_VALUE, Long.MAX_VALUE, timestamp, reference);
    }

    public RSetUnionStatusToItem(byte[] typeBytes, PublicKeyAccount creator, byte feePow, long key, int itemType, long itemKey,
                                 Long beg_date, Long end_date, long timestamp, Long reference, byte[] signature) {
        this(typeBytes, creator, feePow, key, itemType, itemKey,
                beg_date, end_date, timestamp, reference);
        this.signature = signature;
    }
    public RSetUnionStatusToItem(byte[] typeBytes, PublicKeyAccount creator, byte feePow, long key, int itemType, long itemKey,
                                 Long beg_date, Long end_date, long timestamp, Long reference, byte[] signature, long feeLong) {
        this(typeBytes, creator, feePow, key, itemType, itemKey,
                beg_date, end_date, timestamp, reference);
        this.signature = signature;
        this.fee = BigDecimal.valueOf(feeLong, BlockChain.FEE_SCALE);
    }

    // as pack
    public RSetUnionStatusToItem(byte[] typeBytes, PublicKeyAccount creator, long key, int itemType, long itemKey,
                                 Long beg_date, Long end_date, byte[] signature) {
        this(typeBytes, creator, (byte) 0, key, itemType, itemKey,
                beg_date, end_date, 0l, null);
        this.signature = signature;
    }

    public RSetUnionStatusToItem(PublicKeyAccount creator, byte feePow, long key, int itemType, long itemKey,
                                 Long beg_date, Long end_date, long timestamp, Long reference, byte[] signature) {
        this(new byte[]{TYPE_ID, (byte) 0, 0, 0}, creator, feePow, key, itemType, itemKey,
                beg_date, end_date, timestamp, reference);
    }

    // as pack
    public RSetUnionStatusToItem(PublicKeyAccount creator, long key, int itemType, long itemKey,
                                 Long beg_date, Long end_date, byte[] signature) {
        this(new byte[]{TYPE_ID, (byte) 0, (byte) 0, 0}, creator, (byte) 0, key, itemType, itemKey,
                beg_date, end_date, 0l, null);
    }

    //GETTERS/SETTERS

    //public static String getName() { return "Send"; }

    public StatusCls getStatus() {
        if (statusKey == null) {
            status = (StatusCls) ItemCls.getItem(dcSet, ItemCls.STATUS_TYPE, this.key);
        }
        return status;
    }

    @Override
    public String getTitle() {
        return getStatus().getName();
    }

    // releaserReference = null - not a pack
    // releaserReference = reference for releaser account - it is as pack
    public static Transaction Parse(byte[] data, int asDeal) throws Exception {
        //boolean asPack = releaserReference != null;

        //CHECK IF WE MATCH BLOCK LENGTH
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
        byte[] signature = Arrays.copyOfRange(data, position, position + SIGNATURE_LENGTH);
        position += SIGNATURE_LENGTH;

        long feeLong = 0;
        if (asDeal == FOR_DB_RECORD) {
            // READ FEE
            byte[] feeBytes = Arrays.copyOfRange(data, position, position + FEE_LENGTH);
            feeLong = Longs.fromByteArray(feeBytes);
            position += FEE_LENGTH;
        }

        //READ STATUS KEY
        byte[] keyBytes = Arrays.copyOfRange(data, position, position + KEY_LENGTH);
        long key = Longs.fromByteArray(keyBytes);
        position += KEY_LENGTH;

        //READ ITEM
        // ITEM TYPE
        Byte itemType = data[position];
        position++;
        // ITEM KEY
        byte[] itemKeyBytes = Arrays.copyOfRange(data, position, position + KEY_LENGTH);
        long itemKey = Longs.fromByteArray(itemKeyBytes);
        position += KEY_LENGTH;
        //ItemCls item = Controller.getInstance().getItem(itemType.intValue(), itemKey);	// error!

        // READ BEGIN DATE
        byte[] beg_dateBytes = Arrays.copyOfRange(data, position, position + DATE_LENGTH);
        Long beg_date = Longs.fromByteArray(beg_dateBytes);
        position += DATE_LENGTH;

        // READ END DATE
        byte[] end_dateBytes = Arrays.copyOfRange(data, position, position + DATE_LENGTH);
        Long end_date = Longs.fromByteArray(end_dateBytes);
        position += DATE_LENGTH;

        if (asDeal > Transaction.FOR_MYPACK) {
            return new RSetUnionStatusToItem(typeBytes, creator, feePow, key, itemType, itemKey,
                    beg_date, end_date, timestamp, reference, signature, feeLong);
        } else {
            return new RSetUnionStatusToItem(typeBytes, creator, key, itemType, itemKey,
                    beg_date, end_date, signature);
        }

    }

    @Override
    public long getKey() {
        return this.key;
    }

    public int getItemType() {
        return this.itemType;
    }

    public long getItemKey() {
        return this.itemKey;
    }

    public Long getBeginDate() {
        return this.beg_date;
    }

    public Long getEndDate() {
        return this.end_date;
    }

    @Override
    public boolean hasPublicText() {
        return false;
    }

    @SuppressWarnings("unchecked")
    @Override
    public JSONObject toJson() {
        //GET BASE
        JSONObject transaction = this.getJsonBase();

        //ADD CREATOR/SERVICE/DATA
        transaction.put("key", this.key);
        transaction.put("itemType", this.itemType);
        transaction.put("itemKey", this.itemKey);
        transaction.put("begin_date", this.beg_date);
        transaction.put("end_date", this.end_date);

        return transaction;
    }

    //@Override
    @Override
    public byte[] toBytes(int forDeal, boolean withSignature) {

        byte[] data = super.toBytes(forDeal, withSignature);

        //WRITE STATUS KEY
        byte[] keyBytes = Longs.toByteArray(this.key);
        keyBytes = Bytes.ensureCapacity(keyBytes, KEY_LENGTH, 0);
        data = Bytes.concat(data, keyBytes);

        //WRITE ITEM KEYS
        // TYPE
        byte[] itemTypeKeyBytes = new byte[1];
        itemTypeKeyBytes[0] = (byte) this.itemType;
        data = Bytes.concat(data, itemTypeKeyBytes);
        // KEY
        byte[] itemKeyBytes = Longs.toByteArray(this.itemKey);
        keyBytes = Bytes.ensureCapacity(itemKeyBytes, KEY_LENGTH, 0);
        data = Bytes.concat(data, keyBytes);

        //WRITE BEGIN DATE
        data = Bytes.concat(data, Longs.toByteArray(this.beg_date));

        //WRITE END DATE
        data = Bytes.concat(data, Longs.toByteArray(this.end_date));

        return data;
    }

    @Override
    public int getDataLength(int forDeal, boolean withSignature) {
        // not include reference

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

    @Override
    public int isValid(int asDeal, long flags) {

        int result = super.isValid(asDeal, flags);
        if (result != Transaction.VALIDATE_OK) return result;

        //CHECK END_DAY
        if (end_date < 0) {
            return INVALID_DATE;
        }

        if (!this.dcSet.getItemStatusMap().contains(this.key)) {
            return Transaction.ITEM_STATUS_NOT_EXIST;
        }

        if (this.itemType != ItemCls.PERSON_TYPE
                && this.itemType != ItemCls.ASSET_TYPE
                && this.itemType != ItemCls.STATUS_TYPE)
            return ITEM_DOES_NOT_UNITED;

        ItemCls item = this.dcSet.getItem_Map(this.itemType).get(this.itemKey);
        if (item == null) {
            return Transaction.ITEM_DOES_NOT_EXIST;
        }

        BigDecimal balERA = this.creator.getBalanceUSE(RIGHTS_KEY, this.dcSet);
        if (balERA.compareTo(BlockChain.MIN_REGISTRATING_BALANCE_10_BD) < 0)
            return Transaction.NOT_ENOUGH_ERA_USE_10;

        return Transaction.VALIDATE_OK;
    }

    //PROCESS/ORPHAN

    @Override
    public void process(Block block, int asDeal) {

        //UPDATE SENDER
        super.process(block, asDeal);

        // pack additional data
        byte[] a_data = new byte[0];//this.value1;

        //Block block = db.getBlocksHeadMap().getLastBlock();
        //int blockIndex = block.getHeight(db);
        //int transactionIndex = block.getTransactionIndex(signature);

        Tuple5<Long, Long, byte[], Integer, Integer> itemP = new Tuple5<Long, Long, byte[], Integer, Integer>
                (
                        beg_date, end_date,
                        a_data,
                        this.height, this.seqNo
                );

        // SET UNION to ITEM for DURATION
        if (this.itemType == ItemCls.PERSON_TYPE)
            this.dcSet.getPersonUnionMap().addItem(this.itemKey, this.key, itemP);
        else if (this.itemType == ItemCls.ASSET_TYPE)
            this.dcSet.getAssetUnionMap().addItem(this.itemKey, this.key, itemP);
        else if (this.itemType == ItemCls.STATUS_TYPE)
            this.dcSet.getStatusUnionMap().addItem(this.itemKey, this.key, itemP);

    }

    @Override
    public void orphan(Block block, int asDeal) {

        //UPDATE SENDER
        super.orphan(block, asDeal);

        // UNDO ALIVE PERSON for DURATION
        if (this.itemType == ItemCls.PERSON_TYPE)
            this.dcSet.getPersonUnionMap().removeItem(this.itemKey, this.key);
        else if (this.itemType == ItemCls.ASSET_TYPE)
            this.dcSet.getAssetUnionMap().removeItem(this.itemKey, this.key);
        else if (this.itemType == ItemCls.STATUS_TYPE)
            this.dcSet.getStatusUnionMap().removeItem(this.itemKey, this.key);

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