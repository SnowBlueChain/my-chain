package org.erachain.core.transaction;

import com.google.common.primitives.Bytes;
import com.google.common.primitives.Longs;
import org.erachain.core.BlockChain;
import org.erachain.core.account.Account;
import org.erachain.core.account.PrivateKeyAccount;
import org.erachain.core.account.PublicKeyAccount;
import org.erachain.core.block.Block;
import org.erachain.core.exdata.exLink.ExLink;
import org.erachain.core.item.ItemCls;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.HashSet;

public abstract class IssueItemRecord extends Transaction implements Itemable {

    static Logger LOGGER = LoggerFactory.getLogger(IssueItemRecord.class.getName());

    //private static final int BASE_LENGTH = Transaction.BASE_LENGTH;

    protected ItemCls item;
    protected Long key = 0L;

    public IssueItemRecord(byte[] typeBytes, String NAME_ID, PublicKeyAccount creator, ExLink linkTo, ItemCls item, byte feePow, long timestamp, Long reference) {
        super(typeBytes, NAME_ID, creator, linkTo, feePow, timestamp, reference);
        this.item = item;
        if (item.getKey() != 0)
            key = item.getKey();
    }

    public IssueItemRecord(byte[] typeBytes, String NAME_ID, PublicKeyAccount creator, ExLink linkTo, ItemCls item, byte feePow, long timestamp, Long reference, byte[] signature) {
        this(typeBytes, NAME_ID, creator, linkTo, item, feePow, timestamp, reference);
        this.signature = signature;
        this.item.setReference(signature, seqNo);
    }

    public IssueItemRecord(byte[] typeBytes, String NAME_ID, PublicKeyAccount creator, ExLink linkTo, ItemCls item, byte[] signature) {
        this(typeBytes, NAME_ID, creator, linkTo, item, (byte) 0, 0L, null);
        this.signature = signature;
        this.item.setReference(signature, seqNo);
    }

    //GETTERS/SETTERS
    //public static String getName() { return "Issue Item"; }

    @Override
    public ItemCls getItem() {
        return this.item;
    }

    /** нужно для отображение в блокэксплорере
     *  - не участвует в Протоколе, так как перед выпуском неизвестно его значение
     * @return
     */
    @Override
    public long getKey() {
        return key == null ? (isWiped() ? 0 : null) // выдаст ошибку специально если боевая и NULL, see issues/1347
                : key;
    }

    @Override
    public String getTitle() {
        return this.item.getName();
    }

    @Override
    public void makeItemsKeys() {
        if (key == null || key == 0)
            return;

        if (creatorPersonDuration != null) {
            // запомним что тут две сущности
            itemsKeys = new Object[][]{
                    new Object[]{ItemCls.PERSON_TYPE, creatorPersonDuration.a},
                    new Object[]{item.getItemType(), key}
            };
        } else {
            itemsKeys = new Object[][]{
                    new Object[]{item.getItemType(), key}
            };
        }
    }

    /**
     * нельзя вызывать для Форка и для isWIPED
     */
    public void updateFromStateDB() {
        if (this.dbRef == 0) {
            // неподтвержденная транзакция не может быть обновлена
            return;
        }

        if (key == null || key == 0) {
            // эта транзакция взята как скелет из набора блока
            // найдем сохраненную транзакцию - в ней есь Номер Сути
            IssueItemRecord issueItemRecord = (IssueItemRecord) dcSet.getTransactionFinalMap().get(this.dbRef);
            key = issueItemRecord.getKey();
            item.setKey(key);
        } else if (item.getKey() == 0) {
            item.setKey(key);
        }
    }

    @Override
    public String viewItemName() {
        return item.toString();
    }

    public String getItemDescription() {
        return item.getDescription();
    }

    @Override
    public boolean hasPublicText() {
        return true;
    }

    //@Override
    @Override
    public void sign(PrivateKeyAccount creator, int forDeal) {
        super.sign(creator, forDeal);
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

        if (forDeal == FOR_DB_RECORD) {
            if (key == null) {
                // для неподтвержденных когда еще номера нету
                data = Bytes.concat(data, new byte[KEY_LENGTH]);
            } else {
                byte[] keyBytes = Longs.toByteArray(key);
                keyBytes = Bytes.ensureCapacity(keyBytes, KEY_LENGTH, 0);
                data = Bytes.concat(data, keyBytes);
            }

        }

        return data;
    }

    @Override
    public int getDataLength(int forDeal, boolean withSignature) {

        int base_len;

        if (true) {
            base_len = super.getDataLength(forDeal, withSignature);
            if (forDeal == FOR_DB_RECORD)
                base_len += KEY_LENGTH;
        } else {
            if (forDeal == FOR_MYPACK)
                base_len = BASE_LENGTH_AS_MYPACK;
            else if (forDeal == FOR_PACK)
                base_len = BASE_LENGTH_AS_PACK;
            else if (forDeal == FOR_DB_RECORD)
                base_len = BASE_LENGTH_AS_DBRECORD + KEY_LENGTH;
            else
                base_len = BASE_LENGTH;

            if (exLink != null)
                base_len += exLink.length();

            if (!withSignature)
                base_len -= SIGNATURE_LENGTH;

        }

        // not include item reference
        return base_len + this.item.getDataLength(false);

    }

    //VALIDATE

    //@Override
    @Override
    public int isValid(int forDeal, long flags) {

        if (height < BlockChain.ALL_VALID_BEFORE) {
            return VALIDATE_OK;
        }

        if (BlockChain.startKeys[this.item.getItemType()] < 0) {
            if (this.item.isNovaAsset(this.creator, this.dcSet) <= 0) {
                return INVALID_ISSUE_PROHIBITED;
            }
        }

        //CHECK NAME LENGTH
        String name = this.item.getName();
        // TEST ONLY CHARS
        int nameLen = name.length();

        if (nameLen < item.getMinNameLen()) {
            // IF is NEW NOVA
            if (this.item.isNovaAsset(this.creator, this.dcSet) <= 0) {
                errorValue = "" + nameLen;
                return INVALID_NAME_LENGTH_MIN;
            }
        }

        // TEST ALL BYTES for database FIELD
        if (name.getBytes(StandardCharsets.UTF_8).length > ItemCls.MAX_NAME_LENGTH) {
            errorValue = "" + nameLen;
            return INVALID_NAME_LENGTH_MAX;
        }

        //CHECK ICON LENGTH
        int iconLength = this.item.getIcon().length;
        if (iconLength < 0) {
            return INVALID_ICON_LENGTH_MIN;
        }
        if (iconLength > ItemCls.MAX_ICON_LENGTH) {
            errorValue = "" + iconLength;
            return INVALID_ICON_LENGTH_MAX;
        }

        //CHECK IMAGE LENGTH
        int imageLength = this.item.getImage().length;
        if (imageLength < 0) {
            return INVALID_IMAGE_LENGTH_MIN;
        }
        if (imageLength > ItemCls.MAX_IMAGE_LENGTH) {
            errorValue = "" + imageLength;
            return INVALID_IMAGE_LENGTH_MAX;
        }

        //CHECK DESCRIPTION LENGTH
        int descriptionLength = this.item.getDescription().getBytes(StandardCharsets.UTF_8).length;
        if (descriptionLength > Transaction.MAX_DATA_BYTES_LENGTH) {
            errorValue = "" + descriptionLength;
            return INVALID_DESCRIPTION_LENGTH_MAX;
        }

        return super.isValid(forDeal, flags);

    }

    //PROCESS/ORPHAN
    //@Override
    @Override
    public void process(Block block, int forDeal) {

        //UPDATE CREATOR
        super.process(block, forDeal);

        this.item.setReference(this.signature, seqNo);

        //INSERT INTO DATABASE
        key = this.item.insertToMap(this.dcSet, this.item.getStartKey());

    }

    //@Override
    @Override
    public void orphan(Block block, int forDeal) {
        //UPDATE CREATOR
        super.orphan(block, forDeal);

        //DELETE FROM DATABASE
        key = this.item.deleteFromMap(this.dcSet, item.getStartKey());
    }

    @Override
    public HashSet<Account> getInvolvedAccounts() {
        HashSet<Account> accounts = this.getRecipientAccounts();
        accounts.add(this.creator);
        return accounts;
    }

    @Override
    public HashSet<Account> getRecipientAccounts() {
        HashSet<Account> accounts = new HashSet<>(3, 1);
        if (!this.item.getOwner().equals(this.creator)) {
            accounts.add(this.item.getOwner());
        }
        return accounts;
    }

    @Override
    public boolean isInvolved(Account account) {

        if (account.equals(this.creator) || account.equals(this.item.getOwner())) {
            return true;
        }

        return false;
    }

}
