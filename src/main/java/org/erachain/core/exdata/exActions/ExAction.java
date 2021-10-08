package org.erachain.core.exdata.exActions;

import org.erachain.core.BlockChain;
import org.erachain.core.account.Account;
import org.erachain.core.block.Block;
import org.erachain.core.crypto.Crypto;
import org.erachain.core.item.assets.AssetCls;
import org.erachain.core.transaction.RSignNote;
import org.erachain.core.transaction.Transaction;
import org.erachain.datachain.DCSet;
import org.erachain.lang.Lang;
import org.json.simple.JSONObject;
import org.mapdb.Fun;

import java.math.BigDecimal;
import java.util.List;

/**
 * Simple pay - for all same amount
 */

public abstract class ExAction<R> {

    static Crypto crypto = Crypto.getInstance();

    public static final int FILTERED_ACCRUALS_TYPE = 0;
    public static final int SIMPLE_PAYOUTS_TYPE = 1;
    public static final int LIST_PAYOUTS_TYPE = 2;

    int type;
    protected int flags;
    protected long assetKey;

    protected DCSet dcSet;
    protected int height;
    protected AssetCls asset;

    protected R results;
    protected BigDecimal totalPay;

    public int resultCode;
    public String errorValue;

    ExAction(int type) {
        this.type = type;
    }

    ExAction(int type, int flags) {
        this.type = type;
        this.flags = flags;
    }

    public int getType() {
        return type;
    }

    public long getAssetKey() {
        return assetKey;
    }

    public AssetCls getAsset() {
        return asset;
    }

    public R getResults() {
        return results;
    }

    public BigDecimal getTotalPay() {
        return totalPay;
    }

    public abstract int getCount();

    public abstract String viewResults(Transaction transactionParent);

    public abstract String viewType();

    public abstract long getTotalFeeBytes();

    public void setDC(DCSet dcSet) {
        if (this.dcSet == null || !this.dcSet.equals(dcSet)) {
            this.dcSet = dcSet;
        }
    }

    public abstract byte[] getDBdata();

    public abstract byte[] toBytes() throws Exception;

    public abstract int length();

    public abstract int getLengthDBData();

    public abstract int parseDBData(byte[] dbData, int position);

    public static ExAction parse(int type, byte[] data, int pos) throws Exception {

        switch (type) {
            case FILTERED_ACCRUALS_TYPE:
                return ExPays.parse(data, pos);
            case SIMPLE_PAYOUTS_TYPE:
                return ExAirDrop.parse(data, pos);
            case LIST_PAYOUTS_TYPE:
                return ExListPays.parse(data, pos);
        }

        throw new Exception("Invalid ExAction type: " + type);

    }

    public static Fun.Tuple2<ExAction, String> parseJSON(JSONObject json) {

        try {
            int type = (Integer) json.get("type");
            switch (type) {
                case FILTERED_ACCRUALS_TYPE:
                    return ExPays.parseJSON_local(json);
                case SIMPLE_PAYOUTS_TYPE:
                    return ExAirDrop.parseJSON_local(json);
                case LIST_PAYOUTS_TYPE:
                    return ExListPays.parseJSON_local(json);
            }
            return new Fun.Tuple2<>(null, "Invalid ExAction type: " + type);
        } catch (Exception e) {
            return new Fun.Tuple2<>(null, e.getMessage());
        }

    }

    /**
     * Version 2 maker for BlockExplorer
     */
    public JSONObject makeJSONforHTML(JSONObject langObj) {
        JSONObject json = toJson();

        json.put("asset", asset.getName());
        json.put("results", getInfoHTML(true));

        json.put("Label_Counter", Lang.T("Counter", langObj));
        json.put("Label_Total_Amount", Lang.T("Total Amount", langObj));
        json.put("Label_Additional_Fee", Lang.T("Additional Fee", langObj));


        return json;

    }

    public JSONObject toJson() {
        JSONObject toJson = new JSONObject();

        toJson.put("type", type);
        toJson.put("typeName", viewType());
        toJson.put("flags", flags);
        toJson.put("assetKey", assetKey);

        if (totalPay != null)
            toJson.put("totalPay", totalPay);

        return toJson;
    }

    public String getInfoHTML(boolean onlyTotal) {
        String out = "<h3>" + Lang.T(viewType()) + "</h3>";
        out += Lang.T("Asset") + ": <b>" + asset.getName() + "<br>";
        out += Lang.T("Count # кол-во") + ": <b>" + getCount()
                + "</b>, " + Lang.T("Additional Fee") + ": <b>" + BlockChain.feeBG(getTotalFeeBytes())
                + "</b>, " + Lang.T("Total") + ": <b>" + totalPay;
        return out;

    }

    /**
     * make calculations of lists and pre-validate it if need
     */
    public abstract int preProcess(int height, Account creator, boolean andPreValid);

    /**
     * make calculations of lists for process / orphan. If before validated it take old results
     *
     * @param transaction
     * @return
     */
    public int preProcess(Transaction transaction) {
        if (results == null)
            return (resultCode = preProcess(transaction.getBlockHeight(), transaction.getCreator(), false));
        return resultCode;
    }

    public abstract void updateItemsKeys(List listTags);

    /**
     * full validate
     *
     * @param rNote
     * @return
     */
    public abstract int isValid(RSignNote rNote);

    public abstract void process(Transaction rNote, Block block);

    public abstract void orphan(Transaction rNote);

    public abstract boolean isInvolved(Account account);

}
