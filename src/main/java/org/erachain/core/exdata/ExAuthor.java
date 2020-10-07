package org.erachain.core.exdata;

import com.google.common.primitives.Ints;
import com.google.common.primitives.Longs;
import org.erachain.core.transaction.Transaction;
import org.erachain.datachain.DCSet;
import org.json.simple.JSONObject;

import java.nio.charset.StandardCharsets;

public class ExAuthor {

    public static final byte BASE_LENGTH = 12;

    /**
     *
     */
    protected final byte flags;

    /**
     * 0 - дополнение, см. LINK_APPENDIX_TYPE...
     */
    protected final byte[] memoBytes;
    protected String memo;

    /**
     * Ссылка на персону
     */
    protected final long ref;

    /**
     * Доля или вклад
     */
    protected final int share;

    public ExAuthor(byte flags, int share, long ref, String memo) {
        this.flags = flags;
        this.share = share;
        this.ref = ref;
        this.memo = memo;
        memoBytes = memo == null || memo.isEmpty() ? null : memo.getBytes(StandardCharsets.UTF_8);
    }

    public ExAuthor(byte flags, int share, long ref, byte[] memoBytes) {
        this.memoBytes = memoBytes;
        this.flags = flags;
        this.share = share;
        this.ref = ref;
    }

    public ExAuthor(byte[] data) {
        this.flags = data[0];
        this.share = Ints.fromBytes((byte) 0, (byte) 0, data[2], data[3]);
        byte[] refBuf = new byte[Longs.BYTES];
        System.arraycopy(data, 4, refBuf, 0, Long.BYTES);
        ref = Longs.fromByteArray(refBuf);
        int memoLen = data[1];
        this.memoBytes = new byte[memoLen];
        System.arraycopy(data, 4, refBuf, 0, Long.BYTES);
    }

    public byte getFlags() {
        return flags;
    }

    public long getRef() {
        return ref;
    }

    public String getMemo() {
        if (memo == null) {
            if (memoBytes == null || memoBytes.length == 0)
                return null;
            else
                memo = new String(this.memoBytes, StandardCharsets.UTF_8);
        }
        return memo;
    }

    public int getShare() {
        return share;
    }

    public JSONObject makeJSONforHTML() {
        return toJson();
    }

    public JSONObject toJson() {
        JSONObject json = new JSONObject();
        json.put("memo", getMemo());
        json.put("flags", flags);
        json.put("share", share);
        json.put("ref", Transaction.viewDBRef(ref));
        return json;
    }

    public byte[] toBytes() {
        byte[] data = new byte[BASE_LENGTH + memoBytes.length];
        data[0] = flags;
        data[1] = (byte) memoBytes.length;
        data[2] = (byte) (share >> 8);
        data[3] = (byte) share;
        System.arraycopy(Longs.toByteArray(ref), 0, data, 4, Long.BYTES);
        System.arraycopy(memoBytes, 0, data, BASE_LENGTH, memoBytes.length);

        return data;
    }

    public static ExAuthor parse(byte[] data) throws Exception {
        return new ExAuthor(data);
    }

    public int length() {
        return BASE_LENGTH + memoBytes.length;
    }

    public int isValid(DCSet dcSet) {
        if (share > 1000 || share < 0) {
            return Transaction.INVALID_AMOUNT;
        }

        if (memoBytes.length > 255)
            return Transaction.INVALID_DATA_LENGTH;

        if (!dcSet.getItemPersonMap().contains(ref))
            return Transaction.ITEM_PERSON_NOT_EXIST;

        return Transaction.VALIDATE_OK;
    }
}
