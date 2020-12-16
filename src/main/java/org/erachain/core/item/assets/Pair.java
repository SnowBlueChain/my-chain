package org.erachain.core.item.assets;
// 16/03

import com.google.common.primitives.Bytes;
import com.google.common.primitives.Ints;
import com.google.common.primitives.Longs;
import org.erachain.core.transaction.Transaction;
import org.erachain.datachain.DCSet;
import org.erachain.datachain.TradeMapImpl;
import org.json.simple.JSONObject;
import org.mapdb.Fun.Tuple2;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.Arrays;

public class Pair {

    private static final int ASSET_KEY_LENGTH = Transaction.KEY_LENGTH;
    private static final int AMOUNT_LENGTH = Order.AMOUNT_LENGTH;
    private static final int SCALE_LENGTH = 1;
    private static final int BASE_LENGTH = 2 * ASSET_KEY_LENGTH + 2 * AMOUNT_LENGTH + 2 * SCALE_LENGTH
            + 2 * Integer.BYTES + 2 * AMOUNT_LENGTH;

    private Long assetKey1;
    private Long assetKey2;
    private int haveAssetScale;
    private int wantAssetScale;

    private BigDecimal volumeHave;
    private BigDecimal volumeWant;
    private int countHave;
    private int countWant;

    private BigDecimal bidPrice;
    private BigDecimal askPrice;

    private BigDecimal volume24Have;
    private BigDecimal volume24Want;

    private BigDecimal lastPrice;
    private long lastTime;

    // make trading if two orders is seeked
    public Pair(Long assetKey1, Long assetKey2, int haveAssetScale, int wantAssetScale, BigDecimal bidPrice, BigDecimal askPrice,
                BigDecimal volumeHave, BigDecimal volumeWant,
                int countHave, int countWant, BigDecimal volume24Have, BigDecimal volume24Want, BigDecimal lastPrice, long lastTime) {
        this.assetKey1 = assetKey1;
        this.assetKey2 = assetKey2;
        this.haveAssetScale = haveAssetScale;
        this.wantAssetScale = wantAssetScale;

        this.volumeHave = volumeHave;
        this.volumeWant = volumeWant;

        this.countHave = countHave;
        this.countWant = countWant;

        this.bidPrice = bidPrice;
        this.askPrice = askPrice;

        this.volume24Have = volume24Have;
        this.volume24Want = volume24Want;

        this.lastPrice = lastPrice;
        this.lastTime = lastTime;
    }

    public String viewID() {
        return Transaction.viewDBRef(assetKey1) + "/" + Transaction.viewDBRef(assetKey2);
    }

    public Long getAssetKey1() {
        return this.assetKey1;
    }
    public Long getAssetKey2() {
        return this.assetKey2;
    }

    public static Pair get(DCSet db, Long assetKey1, Long assetKey2) {
        return db.getPairMap().get(TradeMapImpl.key);
    }

    public int getCountHave() {
        return this.countHave;
    }
    public int getCountWant() {
        return this.countWant;
    }

    public BigDecimal getVolumeHave() {
        return this.volumeHave;
    }

    public BigDecimal getVolumeWant() {
        return this.volumeWant;
    }

    public BigDecimal getVolume24Have() {
        return volume24Have;
    }

    public BigDecimal getVolume24Want() {
        return volume24Want;
    }

    public BigDecimal getBidPrice() {
        return this.bidPrice;
    }

    public BigDecimal getAskPrice() {
        return this.askPrice;
    }

    public BigDecimal getLastPrice() {
        return lastPrice;
    }

    public long getLastTime() {
        return lastTime;
    }

    @SuppressWarnings("unchecked")
    public JSONObject toJson(long keyForBuySell) {

        JSONObject pair = new JSONObject();
        pair.put("id", viewID());
        pair.put("assetKey1", Transaction.viewDBRef(assetKey1));
        pair.put("assetKey2", Transaction.viewDBRef(assetKey2));

        pair.put("lastTime", lastTime);

        if (keyForBuySell == 0 || keyForBuySell == assetKey1) {

            if (keyForBuySell != 0) {
                // задана пара и направление можно давать
                pair.put("type", "sell");
            }

            pair.put("countHave", countHave);
            pair.put("countWant", countWant);

            pair.put("volumeHave", volumeHave);
            pair.put("volumeWant", volumeWant);

            pair.put("volume24Have", volume24Have);
            pair.put("volume24Want", volume24Want);

            pair.put("lastPrice", lastPrice);
        } else {
            pair.put("type", "buy");

            pair.put("countHave", countWant);
            pair.put("countWant", countHave);

            pair.put("volumeHave", volumeWant);
            pair.put("volumeWant", volumeHave);

            pair.put("volume24Have", volume24Want);
            pair.put("volume24Want", volume24Have);

            pair.put("bid", askPrice);
            pair.put("ask", bidPrice);

            pair.put("lastPrice", BigDecimal.ONE.divide(lastPrice, wantAssetScale, RoundingMode.HALF_DOWN).stripTrailingZeros());

        }

        return pair;

    }


    //PARSE/CONVERT
    public static Pair parse(byte[] data) throws Exception {
        //CHECK IF CORRECT LENGTH
        if (data.length != BASE_LENGTH) {
            throw new Exception("Data does not match trade length");
        }

        int position = 0;

        //READ ASSET 1 KEY
        byte[] assetKey1Bytes = Arrays.copyOfRange(data, position, position + ASSET_KEY_LENGTH);
        Long assetKey1 = Longs.fromByteArray(assetKey1Bytes);
        position += ASSET_KEY_LENGTH;

        //READ ASSET 2 KEY
        byte[] assetKey2Bytes = Arrays.copyOfRange(data, position, position + ASSET_KEY_LENGTH);
        Long assetKey2 = Longs.fromByteArray(assetKey2Bytes);
        position += ASSET_KEY_LENGTH;

        //READ HAVE SCALE
        byte scaleHave = Arrays.copyOfRange(data, position, position + 1)[0];
        position++;

        //READ WANT SCALE
        byte scaleWant = Arrays.copyOfRange(data, position, position + 1)[0];
        position++;

        //READ HAVE VOLUME
        byte[] volumeHaveBytes = Arrays.copyOfRange(data, position, position + AMOUNT_LENGTH);
        BigDecimal volumeHave = new BigDecimal(new BigInteger(volumeHaveBytes), scaleHave);
        position += AMOUNT_LENGTH;

        //READ WANT VOLUME
        byte[] volumeWantBytes = Arrays.copyOfRange(data, position, position + AMOUNT_LENGTH);
        BigDecimal volumeWant = new BigDecimal(new BigInteger(volumeWantBytes), scaleWant);
        position += AMOUNT_LENGTH;

        //READ COUNT HAVE
        byte[] countHaveBytes = Arrays.copyOfRange(data, position, position + Integer.BYTES);
        int countHave = Ints.fromByteArray(countHaveBytes);
        position += Integer.BYTES;

        //READ COUNT WANT
        byte[] countWantBytes = Arrays.copyOfRange(data, position, position + Integer.BYTES);
        int countWant = Ints.fromByteArray(countWantBytes);
        position += Integer.BYTES;

        //READ BID PRICE
        byte[] bidBytes = Arrays.copyOfRange(data, position, position + AMOUNT_LENGTH);
        BigDecimal bidPrice = new BigDecimal(new BigInteger(bidBytes), scaleHave);
        position += AMOUNT_LENGTH;

        //READ ASK PRICE
        byte[] askBytes = Arrays.copyOfRange(data, position, position + AMOUNT_LENGTH);
        BigDecimal askPrice = new BigDecimal(new BigInteger(askBytes), scaleWant);
        position += AMOUNT_LENGTH;

        //READ AMOUNT WANT
        byte[] amountWantBytes = Arrays.copyOfRange(data, position, position + AMOUNT_LENGTH);
        BigDecimal amountWant = new BigDecimal(new BigInteger(amountWantBytes), scaleWant);
        position += AMOUNT_LENGTH;

        byte haveAssetScale = Arrays.copyOfRange(data, position, position + 1)[0];
        position++;
        byte wantAssetScale = Arrays.copyOfRange(data, position, position + 1)[0];
        position++;

        //READ SEQUENCE
        byte[] sequenceBytes = Arrays.copyOfRange(data, position, position + SEQUENCE_LENGTH);
        int sequence = Ints.fromByteArray(sequenceBytes);

        return new Pair(assetKey1, assetKey2, haveAssetScale, wantAssetScale, aamountHave, amountWant, haveAssetScale, wantAssetScale, sequence);
    }

    public byte[] toBytes() {
        byte[] data = new byte[0];

        //WRITE INITIATOR
        byte[] assetKey1Bytes = Longs.toByteArray(this.assetKey1);
        data = Bytes.concat(data, assetKey1Bytes);

        //WRITE TARGET
        byte[] assetKey2Bytes = Longs.toByteArray(this.assetKey2);
        data = Bytes.concat(data, assetKey2Bytes);

        //WRITE HAVE KEY
        byte[] haveKeyBytes = Longs.toByteArray(this.haveKey);
        data = Bytes.concat(data, haveKeyBytes);

        //WRITE HAVE KEY
        byte[] wantKeyBytes = Longs.toByteArray(this.wantKey);
        data = Bytes.concat(data, wantKeyBytes);

        //WRITE AMOUNT HAVE SCALE
        data = Bytes.concat(data, new byte[]{(byte) this.volumeHave.scale()});

        //WRITE AMOUNT HAVE
        byte[] amountHaveBytes = this.volumeHave.unscaledValue().toByteArray();
        fill = new byte[AMOUNT_LENGTH - amountHaveBytes.length];
        amountHaveBytes = Bytes.concat(fill, amountHaveBytes);
        data = Bytes.concat(data, amountHaveBytes);

        //WRITE AMOUNT WANT SCALE
        data = Bytes.concat(data, new byte[]{(byte) this.volumeWant.scale()});

        //WRITE AMOUNT WANT
        byte[] amountWantBytes = this.volumeWant.unscaledValue().toByteArray();
        fill = new byte[AMOUNT_LENGTH - amountWantBytes.length];
        amountWantBytes = Bytes.concat(fill, amountWantBytes);
        data = Bytes.concat(data, amountWantBytes);

        // ASSETS SCALE
        data = Bytes.concat(data, new byte[]{(byte) this.haveAssetScale});
        data = Bytes.concat(data, new byte[]{(byte) this.wantAssetScale});

        //WRITE SEQUENCE
        byte[] sequenceBytes = Ints.toByteArray(this.sequence);
        data = Bytes.concat(data, sequenceBytes);

        return data;
    }

    public int getDataLength() {
        return BASE_LENGTH;
    }

    //PROCESS/ORPHAN

    public void process_old(DCSet db) {
        Order initiator = this.getInitiatorOrder(db);
        Order target = this.getTargetOrder(db);

        //ADD TRADE TO DATABASE
        db.getTradeMap().put(this);
        if (!db.getTradeMap().contains(new Tuple2<Long, Long>(this.assetKey1, this.assetKey2))) {
            int error = 0;
        }

        //UPDATE FULFILLED HAVE
        initiator.setFulfilledHave(initiator.getFulfilledHave().add(this.volumeWant));
        target.setFulfilledHave(target.getFulfilledHave().add(this.volumeHave));

        //CHECK IF FULFILLED
        if (initiator.isFulfilled()) {
            //REMOVE FROM ORDERS
            db.getOrderMap().delete(initiator);

            //ADD TO COMPLETED ORDERS
            //initiator.setFulfilledWant(initiator.getAmountWant());
            db.getCompletedOrderMap().put(initiator);
        } else {
            //UPDATE ORDER
            db.getOrderMap().put(initiator);
        }

        if (target.isFulfilled()) {
            //REMOVE FROM ORDERS
            db.getOrderMap().delete(target);

            //ADD TO COMPLETED ORDERS
            //target.setFulfilledWant(target.getAmountWant());
            db.getCompletedOrderMap().put(target);
        } else {
            //UPDATE ORDER
            //target.setFulfilledWant(target.getFulfilledWant().add(amountWant));
            db.getOrderMap().put(target);
        }

        //TRANSFER FUNDS
        //initiator.getCreator().setBalance(initiator.getWantAssetKey(), initiator.getCreator().getBalance(db, initiator.getWantAssetKey()).add(this.amountHave), db);
        initiator.getCreator().changeBalance(db, false, false, initiator.getWantAssetKey(), this.volumeHave, false, false, false);
        //target.getCreator().setBalance(target.getWantAssetKey(), target.getCreator().getBalance(db, target.getWantAssetKey()).add(this.amountWant), db);
        target.getCreator().changeBalance(db, false, false, target.getWantAssetKey(), this.volumeWant, false, false, false);
    }

    public void orphan_old(DCSet db) {
        Order initiator = this.getInitiatorOrder(db);
        Order target = this.getTargetOrder(db);

        //REVERSE FUNDS
        //initiator.getCreator().setBalance(initiator.getWantAssetKey(), initiator.getCreator().getBalance(db, initiator.getWantAssetKey()).subtract(this.amountHave), db);
        initiator.getCreator().changeBalance(db, true, false, initiator.getWantAssetKey(), this.volumeHave, false, false, false);
        //target.getCreator().setBalance(target.getWantAssetKey(), target.getCreator().getBalance(db, target.getWantAssetKey()).subtract(this.amountWant), db);
        target.getCreator().changeBalance(db, true, false, target.getWantAssetKey(), this.volumeWant, false, false, false);

        //CHECK IF ORDER IS FULFILLED
        if (initiator.isFulfilled()) {
            //REMOVE FROM COMPLETED ORDERS
            db.getCompletedOrderMap().delete(initiator);
        }
        if (target.isFulfilled()) {
            //DELETE TO COMPLETED ORDERS
            db.getCompletedOrderMap().delete(target);
        }

        //REVERSE FULFILLED
        initiator.setFulfilledHave(initiator.getFulfilledHave().subtract(this.volumeWant));
        target.setFulfilledHave(target.getFulfilledHave().subtract(this.volumeHave));

        //UPDATE ORDERS
        db.getOrderMap().put(initiator);
        db.getOrderMap().put(target);

        //REMOVE FROM DATABASE
        db.getTradeMap().delete(this);
    }

    @Override
    public boolean equals(Object object) {
        if (object instanceof Pair) {
            Pair trade = (Pair) object;

            return (trade.getAssetKey1().equals(this.getAssetKey1()) && trade.getAssetKey2().equals(this.getAssetKey2()));
        }

        return false;
    }

    @Override
    public String toString() {
        return viewID() + " : " + this.haveKey + "/" + this.wantKey;
    }

}
