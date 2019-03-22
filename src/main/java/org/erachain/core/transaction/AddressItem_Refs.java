package org.erachain.core.transaction;

import com.google.common.primitives.Bytes;
import org.erachain.core.BlockChain;
import org.erachain.core.account.Account;
import org.erachain.core.account.PrivateKeyAccount;
import org.erachain.core.account.PublicKeyAccount;
import org.erachain.core.block.Block;
import org.erachain.core.item.ItemCls;
import org.json.simple.JSONObject;

import java.nio.charset.StandardCharsets;
import java.util.HashSet;

public abstract class AddressItem_Refs extends Transaction {

    private ItemCls item;

    public static final long START_KEY = 1000l; // << 20;

    public AddressItem_Refs(byte[] typeBytes, String NAME_ID, PublicKeyAccount creator, ItemCls item, byte feePow, long timestamp, Long reference) {
        super(typeBytes, NAME_ID, creator, feePow, timestamp, reference);
        this.item = item;
    }

    /*
    public AddressItemRefs(byte[] typeBytes, String NAME_ID, PublicKeyAccount creator, ItemCls item, byte feePow, long timestamp, Long reference, byte[] signature)
    {
        this(typeBytes, NAME_ID, creator, item, feePow, timestamp, reference);
        this.signature = signature;
        if (item.getReference() == null) item.setReference(signature); // set reference
        //item.resolveKey(DBSet.getInstance());
        ///// if (timestamp > 1000 ) setDB; // not asPaack
    }
     */
    public AddressItem_Refs(byte[] typeBytes, String NAME_ID, PublicKeyAccount creator, ItemCls item, byte[] signature) {
        this(typeBytes, NAME_ID, creator, item, (byte) 0, 0l, null);
        this.signature = signature;
        if (this.item.getReference() == null) this.item.setReference(signature);
        //item.resolveKey(DBSet.getInstance());
    }

    //GETTERS/SETTERS
    //public static String getName() { return "Issue Item"; }

    public ItemCls getItem() {
        return this.item;
    }

    @Override
    public String viewItemName() {
        return item.toString();
    }

    //@Override
    @Override
    public void sign(PrivateKeyAccount creator, int forDeal) {
        super.sign(creator, forDeal);
        // in IMPRINT reference already setted before sign
        if (this.item.getReference() == null) this.item.setReference(this.signature);
    }

    //PARSE CONVERT


    @SuppressWarnings("unchecked")
    @Override
    public JSONObject toJson() {
        //GET BASE
        JSONObject transaction = this.getJsonBase();

        //ADD CREATOR/NAME/DISCRIPTION/QUANTITY/DIVISIBLE
        transaction.put("item", this.item.toJson());

        return transaction;
    }

    @Override
    public byte[] toBytes(int forDeal, boolean withSignature) {
        byte[] data = super.toBytes(forDeal, withSignature);

        // without reference
        data = Bytes.concat(data, this.item.toBytes(false, false));

        return data;
    }

    @Override
    public int getDataLength(int forDeal, boolean withSignature) {
        // not include item reference
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

        return base_len + this.item.getDataLength(false);
    }

    //VALIDATE

    //@Override
    @Override
    public int isValid(int asDeal, long flags) {

        //CHECK NAME LENGTH
        int nameLength = this.item.getName().getBytes(StandardCharsets.UTF_8).length;
        if (nameLength > ItemCls.MAX_NAME_LENGTH || nameLength < item.getMinNameLen()) {
            return INVALID_NAME_LENGTH;
        }

        //CHECK DESCRIPTION LENGTH
        int descriptionLength = this.item.getDescription().getBytes(StandardCharsets.UTF_8).length;
        if (descriptionLength > BlockChain.MAX_REC_DATA_BYTES) {
            return INVALID_DESCRIPTION_LENGTH;
        }

        return super.isValid(asDeal, flags);

    }

    //PROCESS/ORPHAN

    //@Override
    @Override
    public void process(Block block, int asDeal) {
        //UPDATE CREATOR
        super.process(block, asDeal);

        // SET REFERENCE if not setted before (in Imprint it setted)
        if (this.item.getReference() == null) this.item.setReference(this.signature);

        //INSERT INTO DATABASE
        this.item.insertToMap(this.dcSet, START_KEY);

    }

    //@Override
    @Override
    public void orphan(Block block, int asDeal) {
        //UPDATE CREATOR
        super.orphan(block, asDeal);

        //DELETE FROM DATABASE
        long key = this.item.removeFromMap(this.dcSet, START_KEY);
    }

    @Override
    public HashSet<Account> getInvolvedAccounts() {
        return this.getRecipientAccounts();
    }

    @Override
    public HashSet<Account> getRecipientAccounts() {
        HashSet<Account> accounts = new HashSet<>();
        accounts.add(this.creator);
        return accounts;
    }

    @Override
    public boolean isInvolved(Account account) {
        String address = account.getAddress();

        if (address.equals(this.creator.getAddress())) {
            return true;
        }

        return false;
    }

    @Override
    public long calcBaseFee() {

        long add_fee = 0;
        long len = this.getItem().getName().length();
        if (len < 10) {
            add_fee = 3 ^ (10 - len) * 100;
        }

        return calcCommonFee() + BlockChain.FEE_PER_BYTE * (500 + add_fee);
    }
}
