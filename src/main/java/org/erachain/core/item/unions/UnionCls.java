package org.erachain.core.item.unions;

//import java.math.BigDecimal;

import com.google.common.primitives.Bytes;
import com.google.common.primitives.Longs;
import org.erachain.core.BlockChain;
import org.erachain.core.account.PublicKeyAccount;
import org.erachain.core.item.ItemCls;
import org.erachain.core.transaction.Transaction;
import org.erachain.datachain.DCSet;
import org.erachain.datachain.IssueItemMap;
import org.erachain.datachain.ItemMap;
import org.erachain.settings.Settings;
import org.erachain.utils.DateTimeFormat;
import org.json.simple.JSONObject;

public abstract class UnionCls extends ItemCls {

    public static final long START_KEY = BlockChain.SIDE_MODE ? 1L << 14 : 1000L;

    public static final int UNION = 1;

    protected static final int BIRTHDAY_LENGTH = ItemCls.TIMESTAMP_LENGTH;
    protected static final int PARENT_LENGTH = Transaction.KEY_LENGTH;
    protected static final int BASE_LENGTH = BIRTHDAY_LENGTH + PARENT_LENGTH;

    // TODO add setTemplate - document for birth union
    protected long birthday; // timestamp
    protected long parent; // parent union

    public UnionCls(byte[] typeBytes, PublicKeyAccount owner, String name, long birthday, long parent, byte[] icon, byte[] image, String description) {
        super(typeBytes, owner, name, icon, image, description);
        this.birthday = birthday;
        this.parent = parent;

    }

    public UnionCls(int type, PublicKeyAccount owner, String name, long birthday, long parent, byte[] icon, byte[] image, String description) {
        this(new byte[TYPE_LENGTH], owner, name, birthday, parent, icon, image, description);
        this.typeBytes[0] = (byte) type;
    }

    //GETTERS/SETTERS
    public int getItemType() {
        return ItemCls.UNION_TYPE;
    }

    @Override
    public long getStartKey() {
        long startKey = BlockChain.startKeys[ItemCls.UNION_TYPE];

        if (BlockChain.MAIN_MODE || startKey > 0 && startKey < START_KEY)
            return START_KEY;

        return startKey;
    }

    public String getItemTypeName() {
        return "union";
    }

    public long getBirthday() {
        return this.birthday;
    }

    public String getBirthdayStr() {
        return DateTimeFormat.timestamptoString(this.birthday, Settings.getInstance().getBirthTimeFormat(), "UTC");
    }

    public long getParent() {
        return this.parent;
    }

    @Override
    public ItemMap getDBMap(DCSet db) {
        return db.getItemUnionMap();
    }

    public IssueItemMap getDBIssueMap(DCSet db) {
        return db.getIssueUnionMap();
    }

    // PARSE
    public byte[] toBytes(boolean includeReference, boolean onlyBody) {

        byte[] data = super.toBytes(includeReference, onlyBody);

        // WRITE BIRTHDAY
        byte[] birthdayBytes = Longs.toByteArray(this.birthday);
        birthdayBytes = Bytes.ensureCapacity(birthdayBytes, BIRTHDAY_LENGTH, 0);
        data = Bytes.concat(data, birthdayBytes);

        // WRITE PARENT
        byte[] parentBytes = Longs.toByteArray(this.parent);
        parentBytes = Bytes.ensureCapacity(parentBytes, PARENT_LENGTH, 0);
        data = Bytes.concat(data, parentBytes);


        return data;
    }


    public int getDataLength(boolean includeReference) {
        return super.getDataLength(includeReference) + BASE_LENGTH;
    }
    //OTHER

    @Override
    public String toString(DCSet db) {
        long key = this.getKey(db);
        return (key < 0 ? "?" : key) + "." + this.typeBytes[0] + " " + this.name
                + " !" + parent + " " + DateTimeFormat.timestamptoString(birthday, "dd-MM-YY", "");
    }

    @Override
    public String getShort(DCSet db) {
        long key = this.getKey(db);
        return (key < 0 ? "?" : key) + "." + this.typeBytes[0] + " "
                + this.name.substring(0, Math.min(this.name.length(), 20))
                + " !" + parent + " " + DateTimeFormat.timestamptoString(birthday, "dd-MM-YY", "");
    }

    @SuppressWarnings("unchecked")
    public JSONObject toJson() {

        JSONObject personJSON = super.toJson();

        // ADD DATA
        personJSON.put("birthday", this.birthday);
        personJSON.put("parent", this.parent);

        return personJSON;
    }


}
