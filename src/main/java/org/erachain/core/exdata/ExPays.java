package org.erachain.core.exdata;

import com.google.common.primitives.Ints;
import com.google.common.primitives.Longs;
import org.erachain.controller.Controller;
import org.erachain.core.BlockChain;
import org.erachain.core.account.Account;
import org.erachain.core.account.PublicKeyAccount;
import org.erachain.core.block.Block;
import org.erachain.core.item.assets.AssetCls;
import org.erachain.core.item.assets.Order;
import org.erachain.core.item.persons.PersonCls;
import org.erachain.core.transaction.RSignNote;
import org.erachain.core.transaction.Transaction;
import org.erachain.core.transaction.TransactionAmount;
import org.erachain.datachain.DCSet;
import org.erachain.datachain.ItemAssetBalanceMap;
import org.erachain.datachain.TransactionFinalMapImpl;
import org.erachain.dbs.IteratorCloseable;
import org.erachain.lang.Lang;
import org.json.simple.JSONObject;
import org.mapdb.Fun;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * StandardCharsets.UTF_8 JSON "TM" - template key "PR" - template params
 * "HS" - Hashes "MS" - message
 * <p>
 * PARAMS template:TemplateCls param_keys: [id:text] hashes_Set: [name:hash]
 * mess: message title: Title file_Set: [file Name, ZIP? , file byte[]]
 */

public class ExPays {

    public static final byte BASE_LENGTH = 4 + 3;

    public static final int MAX_COUNT = Integer.MAX_VALUE >> 1;
    private static final byte AMOUNT_FLAG_MASK = -128;
    private static final byte AMOUNT_MIN_FLAG_MASK = 64;
    private static final byte AMOUNT_MAX_FLAG_MASK = 32;
    private static final byte BALANCE_FLAG_MASK = 16;
    private static final byte BALANCE_AMOUNT_MIN_FLAG_MASK = 8;
    private static final byte BALANCE_AMOUNT_MAX_FLAG_MASK = 4;
    private static final byte ACTIVE_START_FLAG_MASK = 2;
    private static final byte ACTIVE_END_FLAG_MASK = 1;

    public static final byte PAYMENT_METHOD_TOTAL = 0; // by TOTAL
    public static final byte PAYMENT_METHOD_COEFF = 1; // by coefficient
    public static final byte PAYMENT_METHOD_ABSOLUTE = 2; // by ABSOLUTE VALUE

    public static final byte FILTER_PERSON_NONE = 0;
    public static final byte FILTER_PERSON_ONLY = 1;
    public static final byte FILTER_PERSON_ONLY_MAN = 2;
    public static final byte FILTER_PERSON_ONLY_WOMAN = 3;

    private static final byte NOT_FILTER_PERSONS = -1; //
    private static final byte NOT_FILTER_GENDER = -2; //

    private static final Logger LOGGER = LoggerFactory.getLogger(ExPays.class);

    public static final String FILTER_PERS_ALL = "All";
    public static final String FILTER_PERS_ONLY = "Only certified addresses";
    public static final String FILTER_PERS_MAN = "Only for Men";
    public static final String FILTER_PERS_WOMAN = "Only for Women";

    /**
     * 0 - version; 1..3 - flags;
     */
    private int flags; // 4

    private Long assetKey; // 12
    private int balancePos; // 13
    private boolean backward; // 14
    private int payMethod; // 15 0 - by Total, 1 - by Percent
    private BigDecimal payMethodValue; // 17
    private BigDecimal amountMin; // 19
    private BigDecimal amountMax; //21

    private Long filterAssetKey; // 29
    private int filterBalancePos; //30
    private int filterBalanceSide; //31
    private BigDecimal filterBalanceMIN; // 33
    private BigDecimal filterBalanceMAX; // 34

    private int filterTXType; // 36
    private Long filterTXStartSeqNo; // 44
    public Long filterTXEndSeqNo; // 52

    private final int filterByGender; // 53 = gender or all
    public boolean selfPay; // 54

    /////////////////
    DCSet dcSet;
    private int height;
    AssetCls asset;
    int payAction;
    AssetCls filterAsset;
    /**
     * recipient + balance + payout
     */
    public List<Fun.Tuple3<Account, BigDecimal, BigDecimal>> filteredPayouts;
    private int filteredPayoutsCount;
    private BigDecimal totalPay;
    private long totalFeeBytes;
    private int maxIndex;
    private BigDecimal maxBal;

    public String errorValue;


    /**
     * make FLAGS internal
     *  @param flags
     * @param assetKey
     * @param balancePos
     * @param backward
     * @param payMethod
     * @param payMethodValue
     * @param amountMin
     * @param amountMax
     * @param filterAssetKey
     * @param filterBalancePos
     * @param filterBalanceSide
     * @param filterBalanceMIN
     * @param filterBalanceMAX
     * @param filterTXType
     * @param filterTXStartSeqNo
     * @param filterTXEndSeqNo
     * @param filterByGender
     * @param selfPay
     */
    public ExPays(int flags, Long assetKey, int balancePos, boolean backward, int payMethod, BigDecimal payMethodValue, BigDecimal amountMin, BigDecimal amountMax,
                  Long filterAssetKey, int filterBalancePos, int filterBalanceSide,
                  BigDecimal filterBalanceMIN, BigDecimal filterBalanceMAX,
                  int filterTXType, Long filterTXStartSeqNo, Long filterTXEndSeqNo,
                  int filterByGender, boolean selfPay) {
        this.flags = flags;

        if (true || // запретить без действий по активу - так как это не письма явно - письма отдельно!
                assetKey != null && assetKey != 0L) {
            this.flags |= AMOUNT_FLAG_MASK;
            this.assetKey = assetKey;
            this.balancePos = balancePos;
            this.backward = backward;
            this.payMethod = payMethod;
            this.payMethodValue = payMethodValue;

            if (payMethod != PAYMENT_METHOD_ABSOLUTE) {
                if (amountMin != null) {
                    this.flags |= AMOUNT_MIN_FLAG_MASK;
                    this.amountMin = amountMin;
                }
                if (amountMax != null) {
                    this.flags |= AMOUNT_MAX_FLAG_MASK;
                    this.amountMax = amountMax;
                }
            }
        }

        if (true || // запретить без фильтрации по активу - так как это не письма явно - письма отдельно!
                filterAssetKey != null && filterAssetKey != 0L) {
            this.flags |= BALANCE_FLAG_MASK;
            this.filterAssetKey = filterAssetKey;
            this.filterBalancePos = filterBalancePos;
            this.filterBalanceSide = filterBalanceSide;
            if (filterBalanceMIN != null) {
                this.flags |= BALANCE_AMOUNT_MIN_FLAG_MASK;
                this.filterBalanceMIN = filterBalanceMIN;
            }
            if (filterBalanceMAX != null) {
                this.flags |= BALANCE_AMOUNT_MAX_FLAG_MASK;
                this.filterBalanceMAX = filterBalanceMAX;
            }
        }

        this.filterTXType = filterTXType;

        if (filterTXStartSeqNo != null) {
            this.flags |= ACTIVE_START_FLAG_MASK;
            this.filterTXStartSeqNo = filterTXStartSeqNo;
        }
        if (filterTXEndSeqNo != null) {
            this.flags |= ACTIVE_END_FLAG_MASK;
            this.filterTXEndSeqNo = filterTXEndSeqNo;
        }

        this.filterByGender = filterByGender;
        this.selfPay = selfPay;
    }

    public ExPays(int flags, Long assetKey, int balancePos, boolean backward, int payMethod, BigDecimal payMethodValue, BigDecimal amountMin, BigDecimal amountMax,
                  Long filterAssetKey, int filterBalancePos, int filterBalanceSide,
                  BigDecimal filterBalanceMIN, BigDecimal filterBalanceMAX,
                  int filterTXType, Long filterTXStartSeqNo, Long filterTXEndSeqNo,
                  int filterByGender, boolean selfPay,
                  int filteredPayoutsCount, BigDecimal totalPay, long totalFeeBytes) {
        this(flags, assetKey, balancePos, backward, payMethod, payMethodValue, amountMin, amountMax,
                filterAssetKey, filterBalancePos, filterBalanceSide,
                filterBalanceMIN, filterBalanceMAX,
                filterTXType, filterTXStartSeqNo, filterTXEndSeqNo,
                filterByGender, selfPay);

        this.filteredPayoutsCount = filteredPayoutsCount;
        this.totalPay = totalPay;
        this.totalFeeBytes = totalFeeBytes;
    }

    public List<Fun.Tuple3<Account, BigDecimal, BigDecimal>> getFilteredPayouts(Transaction statement) {
        if (filteredPayouts == null) {
            filteredPayoutsCount = makeFilterPayList(statement, false);
            if (payMethod == PAYMENT_METHOD_TOTAL) {
                calcPayoutsForMethodTotal();
            }
        }
        return filteredPayouts;
    }

    public List<Fun.Tuple3<Account, BigDecimal, BigDecimal>> precalcFilteredPayouts(int height, Account creator) {
        filteredPayoutsCount = makeFilterPayList(dcSet, height, asset, creator, false);
        if (payMethod == PAYMENT_METHOD_TOTAL) {
            calcPayoutsForMethodTotal();
        }
        return filteredPayouts;
    }

    public int getFilteredPayoutsCount() {
        return filteredPayoutsCount;
    }

    public BigDecimal getTotalPay() {
        return totalPay;
    }

    public long getTotalFeeBytes() {
        return totalFeeBytes;
    }

    public static String viewPayMethod(int mode) {
        return "PAY_METHOD_" + mode;
    }

    public static String viewFilterPersMode(int mode) {
        switch (mode) {
            case 0:
                return "All";
            case 1:
                return "Only certified addresses";
            case 2:
                return "Only for Men";
            case 3:
                return "Only for Women";
        }
        return "--";
    }

    public void calcTotalFeeBytes() {
        totalFeeBytes = (hasFilterActive() ? 30L : 10L) * filteredPayoutsCount;
    }

    public boolean hasAmount() {
        return (this.flags & AMOUNT_FLAG_MASK) != 0;
    }

    public boolean hasAmountMin() {
        return (this.flags & AMOUNT_MIN_FLAG_MASK) != 0;
    }

    public boolean hasAmountMax() {
        return (this.flags & AMOUNT_MAX_FLAG_MASK) != 0;
    }

    public boolean hasAssetFilter() {
        return (this.flags & BALANCE_FLAG_MASK) != 0;
    }

    public boolean hasAssetFilterBalMIN() {
        return (this.flags & BALANCE_AMOUNT_MIN_FLAG_MASK) != 0;
    }

    public boolean hasAssetFilterBalMAX() {
        return (this.flags & BALANCE_AMOUNT_MAX_FLAG_MASK) != 0;
    }


    /**
     * Используется ли Итераторы дополнительные для вычисления активности? Нужно для вычисления Комиссии
     *
     * @return
     */
    public boolean hasFilterActive() {
        return filterTXType != 0 || hasTXTypeFilterActiveStart() || hasTXTypeFilterActiveEnd();
    }

    public boolean hasTXTypeFilterActiveStart() {
        return (this.flags & ACTIVE_START_FLAG_MASK) != 0;
    }

    public boolean hasTXTypeFilterActiveEnd() {
        return (this.flags & ACTIVE_END_FLAG_MASK) != 0;
    }

    public void setDC(DCSet dcSet) {
        if (this.dcSet == null || !this.dcSet.equals(dcSet)) {
            this.dcSet = dcSet;
            if (hasAmount()) {
                this.asset = this.dcSet.getItemAssetMap().get(this.assetKey);
            }
        }
    }

    public int parseDBData(byte[] dbData, int position) {
        filteredPayoutsCount = Ints.fromByteArray(Arrays.copyOfRange(dbData, position, position + Integer.BYTES));
        position += Integer.BYTES;

        totalFeeBytes = Longs.fromByteArray(Arrays.copyOfRange(dbData, position, position + Long.BYTES));
        position += Long.BYTES;

        int len = dbData[position++];
        if (len == 0) {
            totalPay = null;
        } else {
            int scale = dbData[position++];
            totalPay = new BigDecimal(new BigInteger(Arrays.copyOfRange(dbData, position, position + len)), scale);
        }

        position += len;

        return position;

    }

    public int getLengthDBData() {
        return Integer.BYTES + Long.BYTES
                + (totalPay == null ? 1 : 2 + totalPay.unscaledValue().toByteArray().length);
    }

    public byte[] getDBdata() {

        byte[] buff;
        byte[] dbData;

        if (totalPay == null) {
            dbData = new byte[Integer.BYTES + Long.BYTES + 1];
            buff = null;
        } else {
            buff = this.totalPay.unscaledValue().toByteArray();
            dbData = new byte[Integer.BYTES + Long.BYTES + 2 + buff.length];
        }

        int pos = 0;
        System.arraycopy(Ints.toByteArray(filteredPayoutsCount), 0, dbData, pos, Integer.BYTES);
        pos += Integer.BYTES;
        System.arraycopy(Longs.toByteArray(totalFeeBytes), 0, dbData, pos, Long.BYTES);
        pos += Long.BYTES;
        if (totalPay == null) {
            dbData[pos++] = (byte) 0;
            return dbData;
        }

        dbData[pos++] = (byte) buff.length;

        dbData[pos++] = (byte) this.totalPay.scale();
        System.arraycopy(buff, 0, dbData, pos, buff.length);

        return dbData;

    }

    public byte[] toBytes() throws Exception {

        ByteArrayOutputStream outStream = new ByteArrayOutputStream();

        outStream.write(Ints.toByteArray(flags));

        byte[] buff;
        if (hasAmount()) {
            outStream.write(Longs.toByteArray(this.assetKey));

            buff = new byte[]{(byte) balancePos, (byte) (backward ? 1 : 0), (byte) payMethod};
            outStream.write(buff);

            outStream.write(this.payMethodValue.scale());
            buff = this.payMethodValue.unscaledValue().toByteArray();
            outStream.write(buff.length);
            outStream.write(buff);

            if (hasAmountMin()) {
                outStream.write(this.amountMin.scale());
                buff = this.amountMin.unscaledValue().toByteArray();
                outStream.write(buff.length);
                outStream.write(buff);
            }

            if (hasAmountMax()) {
                outStream.write(this.amountMax.scale());
                buff = this.amountMax.unscaledValue().toByteArray();
                outStream.write(buff.length);
                outStream.write(buff);
            }

        }

        if (hasAssetFilter()) {
            outStream.write(Longs.toByteArray(this.filterAssetKey));
            buff = new byte[]{(byte) filterBalancePos, (byte) filterBalanceSide};
            outStream.write(buff);

            if (hasAssetFilterBalMIN()) {
                outStream.write(this.filterBalanceMIN.scale());
                buff = this.filterBalanceMIN.unscaledValue().toByteArray();
                outStream.write(buff.length);
                outStream.write(buff);
            }

            if (hasAssetFilterBalMAX()) {
                outStream.write(this.filterBalanceMAX.scale());
                buff = this.filterBalanceMAX.unscaledValue().toByteArray();
                outStream.write(buff.length);
                outStream.write(buff);
            }
        }

        if (hasTXTypeFilterActiveStart()) {
            outStream.write(Longs.toByteArray(this.filterTXStartSeqNo));
        }
        if (hasTXTypeFilterActiveEnd()) {
            outStream.write(Longs.toByteArray(this.filterTXEndSeqNo));
        }

        outStream.write(new byte[]{(byte) filterTXType, (byte) filterByGender, (byte) (selfPay ? 1 : 0)});

        return outStream.toByteArray();

    }

    public int length() {
        int len = BASE_LENGTH;

        if (hasAmount()) {
            len += Transaction.KEY_LENGTH + 3
                    + payMethodValue.unscaledValue().toByteArray().length + 2
                    + (hasAmountMin() ? amountMin.unscaledValue().toByteArray().length + 2 : 0)
                    + (hasAmountMax() ? amountMax.unscaledValue().toByteArray().length + 2 : 0);
        }

        if (hasAssetFilter()) {
            len += Transaction.KEY_LENGTH + 2
                    + (hasAssetFilterBalMIN() ? filterBalanceMIN.unscaledValue().toByteArray().length + 2 : 0)
                    + (hasAssetFilterBalMAX() ? filterBalanceMAX.unscaledValue().toByteArray().length + 2 : 0);
        }

        if (hasTXTypeFilterActiveStart()) {
            len += Long.BYTES;
        }
        if (hasTXTypeFilterActiveEnd()) {
            len += Long.BYTES;
        }

        return len;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static ExPays parse(byte[] data, int position) throws Exception {

        int scale;
        int len;

        int flags = Ints.fromByteArray(Arrays.copyOfRange(data, position, position + Integer.BYTES));
        position += Integer.BYTES;

        Long assetKey = null;
        int balancePos = 0;
        boolean backward = false;
        BigDecimal amountMin = null;
        BigDecimal amountMax = null;
        int payMethod = 0;
        BigDecimal payMethodValue = null;

        if ((flags & AMOUNT_FLAG_MASK) != 0) {
            assetKey = Longs.fromByteArray(Arrays.copyOfRange(data, position, position + Long.BYTES));
            position += Long.BYTES;

            balancePos = data[position++];
            backward = data[position++] > 0;
            payMethod = data[position++];

            scale = data[position++];
            len = data[position++];
            payMethodValue = new BigDecimal(new BigInteger(Arrays.copyOfRange(data, position, position + len)), scale);
            position += len;

            if ((flags & AMOUNT_MIN_FLAG_MASK) != 0) {
                scale = data[position++];
                len = data[position++];
                amountMin = new BigDecimal(new BigInteger(Arrays.copyOfRange(data, position, position + len)), scale);
                position += len;
            }

            if ((flags & AMOUNT_MAX_FLAG_MASK) != 0) {
                scale = data[position++];
                len = data[position++];
                amountMax = new BigDecimal(new BigInteger(Arrays.copyOfRange(data, position, position + len)), scale);
                position += len;
            }

        }

        Long filterAssetKey = null;
        int filterBalancePos = 0;
        int filterBalanceSide = 0;
        BigDecimal filterBalanceLessThen = null;
        BigDecimal filterBalanceMoreThen = null;

        if ((flags & BALANCE_FLAG_MASK) != 0) {
            filterAssetKey = Longs.fromByteArray(Arrays.copyOfRange(data, position, position + Long.BYTES));
            position += Long.BYTES;

            filterBalancePos = data[position++];
            filterBalanceSide = data[position++];

            if ((flags & BALANCE_AMOUNT_MIN_FLAG_MASK) != 0) {
                scale = data[position++];
                len = data[position++];
                filterBalanceMoreThen = new BigDecimal(new BigInteger(Arrays.copyOfRange(data, position, position + len)), scale);
                position += len;
            }

            if ((flags & BALANCE_AMOUNT_MAX_FLAG_MASK) != 0) {
                scale = data[position++];
                len = data[position++];
                filterBalanceLessThen = new BigDecimal(new BigInteger(Arrays.copyOfRange(data, position, position + len)), scale);
                position += len;
            }

        }

        Long filterTXStart = null;
        Long filterTXEnd = null;

        if ((flags & ACTIVE_START_FLAG_MASK) != 0) {
            filterTXStart = Longs.fromByteArray(Arrays.copyOfRange(data, position, position + Long.BYTES));
            position += Long.BYTES;
        }
        if ((flags & ACTIVE_END_FLAG_MASK) != 0) {
            filterTXEnd = Longs.fromByteArray(Arrays.copyOfRange(data, position, position + Long.BYTES));
            position += Long.BYTES;
        }

        int filterTXType = data[position++];
        int filterByPerson = data[position++];
        boolean selfPay = data[position++] > 0;

        return new ExPays(flags, assetKey, balancePos, backward, payMethod, payMethodValue, amountMin, amountMax,
                filterAssetKey, filterBalancePos, filterBalanceSide,
                filterBalanceMoreThen, filterBalanceLessThen,
                filterTXType, filterTXStart, filterTXEnd,
                filterByPerson, selfPay);
    }

    /**
     * @param assetKey
     * @param balancePos
     * @param backward
     * @param payMethod
     * @param payMethodValue
     * @param amountMin
     * @param amountMax
     * @param filterAssetKey
     * @param filterBalancePos
     * @param filterBalanceSide
     * @param filterBalanceMoreThen
     * @param filterBalanceLessThen
     * @param filterTXType
     * @param filterTXStartStr      as SeqNo: 123-1
     * @param filterTXEndStr        as SeqNo: 123-1
     * @param filterByPerson
     * @param selfPay
     * @return
     */
    public static Fun.Tuple2<ExPays, String> make(Long assetKey, int balancePos, boolean backward,
                                                  int payMethod, String payMethodValue, String amountMin, String amountMax,
                                                  Long filterAssetKey, int filterBalancePos, int filterBalanceSide,
                                                  String filterBalanceMoreThen, String filterBalanceLessThen,
                                                  int filterTXType, String filterTXStartStr, String filterTXEndStr,
                                                  int filterByPerson, boolean selfPay) {

        int steep = 0;
        BigDecimal amountMinBG;
        BigDecimal amountMaxBG;
        BigDecimal payMethodValueBG;
        BigDecimal filterBalanceMoreThenBG;
        BigDecimal filterBalanceLessThenBG;
        Long filterTXStartSeqNo;
        Long filterTXEndSeqNo;
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:00");

        Controller cntr = Controller.getInstance();
        BlockChain chain = cntr.getBlockChain();

        try {
            amountMinBG = amountMin == null || amountMin.isEmpty() ? null : new BigDecimal(amountMin);
            ++steep;
            amountMaxBG = amountMax == null || amountMax.isEmpty() ? null : new BigDecimal(amountMax);
            ++steep;
            payMethodValueBG = payMethodValue == null || payMethodValue.isEmpty() ? null : new BigDecimal(payMethodValue);
            ++steep;
            filterBalanceMoreThenBG = filterBalanceMoreThen == null || filterBalanceMoreThen.isEmpty() ? null : new BigDecimal(filterBalanceMoreThen);
            ++steep;
            filterBalanceLessThenBG = filterBalanceLessThen == null || filterBalanceLessThen.isEmpty() ? null : new BigDecimal(filterBalanceLessThen);
            ++steep;
            if (filterTXStartStr == null || filterTXStartStr.isEmpty()) {
                filterTXStartSeqNo = null;
            } else {
                filterTXStartSeqNo = Transaction.parseDBRef(filterTXStartStr);
                if (filterTXStartSeqNo == null) {
                    try {
                        Date parsedDate = dateFormat.parse(filterTXStartStr);
                        Timestamp timestamp = new java.sql.Timestamp(parsedDate.getTime());
                        filterTXStartSeqNo = timestamp.getTime();
                    } catch (Exception e) {
                        filterTXStartSeqNo = Long.parseLong(filterTXStartStr) * 1000L;
                    }
                    filterTXStartSeqNo = Transaction.makeDBRef(chain.getHeightOnTimestampMS(filterTXStartSeqNo), 0);
                }
            }

            ++steep;
            if (filterTXEndStr == null || filterTXEndStr.isEmpty()) {
                filterTXEndSeqNo = null;
            } else {
                filterTXEndSeqNo = Transaction.parseDBRef(filterTXEndStr);
                if (filterTXEndSeqNo == null) {
                    try {
                        Date parsedDate = dateFormat.parse(filterTXEndStr);
                        Timestamp timestamp = new java.sql.Timestamp(parsedDate.getTime());
                        filterTXEndSeqNo = timestamp.getTime();
                    } catch (Exception e) {
                        filterTXEndSeqNo = Long.parseLong(filterTXEndStr) * 1000L;
                    }
                    filterTXEndSeqNo = Transaction.makeDBRef(chain.getHeightOnTimestampMS(filterTXEndSeqNo), 0);
                }
            }
        } catch (Exception e) {
            String error;
            switch (steep) {
                case 0:
                    error = "Wrong amountMin";
                    break;
                case 1:
                    error = "Wrong amountMax";
                    break;
                case 2:
                    error = "Wrong payMethodValue";
                    break;
                case 3:
                    error = "Wrong filterBalanceMoreThen";
                    break;
                case 4:
                    error = "Wrong filterBalanceLessThen";
                    break;
                case 5:
                    error = "Wrong filterTXStartStr";
                    break;
                case 6:
                    error = "Wrong filterTXEndStr";
                    break;
                default:
                    error = e.getMessage();
            }
            return new Fun.Tuple2<>(null, error);
        }

        if (assetKey == null || assetKey == 0L) {
            return new Fun.Tuple2<>(null, "Wrong assetKey (null or ZERO)");
        } else if (filterAssetKey == null || filterAssetKey == 0L) {
            return new Fun.Tuple2<>(null, "Wrong filterAssetKey (null or ZERO)");
        } else if (payMethodValueBG == null || payMethodValueBG.signum() == 0) {
            return new Fun.Tuple2<>(null, "Wrong payMethodValue (null or ZERO)");
        }

        int flags = 0;
        return new Fun.Tuple2<>(new ExPays(flags, assetKey, balancePos, backward, payMethod, payMethodValueBG, amountMinBG, amountMaxBG,
                filterAssetKey, filterBalancePos, filterBalanceSide,
                filterBalanceMoreThenBG, filterBalanceLessThenBG,
                filterTXType, filterTXStartSeqNo, filterTXEndSeqNo,
                filterByPerson, selfPay), null);

    }

    /**
     * Version 2 maker for BlockExplorer
     */
    public JSONObject makeJSONforHTML(JSONObject langObj) {
        JSONObject json = toJson();

        json.put("asset", asset.getName());
        if (filterAssetKey != null && filterAssetKey > 0L) {
            if (filterAsset == null)
                filterAsset = dcSet.getItemAssetMap().get(filterAssetKey);
            json.put("filterAsset", filterAsset.getName());
        }

        if (filteredPayoutsCount > 0) {
            json.put("Label_Counter", Lang.T("Counter", langObj));
            json.put("Label_Total_Amount", Lang.T("Total Amount", langObj));
            json.put("Label_Additional_Fee", Lang.T("Additional Fee", langObj));

        }

        json.put("payMethodName", Lang.T(ExPays.viewPayMethod(payMethod), langObj));
        json.put("balancePosName", Lang.T(Account.balancePositionName(balancePos), langObj));
        json.put("filterBalancePosName", Lang.T(Account.balancePositionName(filterBalancePos), langObj));
        json.put("filterBalanceSideName", Lang.T(Account.balanceSideName(filterBalanceSide), langObj));
        json.put("filterTXTypeName", Lang.T(Transaction.viewTypeName(filterTXType), langObj));
        json.put("filterByGenderName", Lang.T(viewFilterPersMode(filterTXType), langObj));

        json.put("Label_Action_for_Asset", Lang.T("Action for Asset", langObj));
        json.put("Label_assetKey", Lang.T("Asset", langObj));
        json.put("Label_balancePos", Lang.T("Balance Position", langObj));
        json.put("Label_backward", Lang.T("Backward", langObj));

        json.put("Label_payMethod", Lang.T("Method of calculation", langObj));
        json.put("Label_payMethodValue", Lang.T("Value", langObj));
        json.put("Label_amountMin", Lang.T("Minimal Payout", langObj));
        json.put("Label_amountMax", Lang.T("Maximum Payout", langObj));

        json.put("Label_Filter_By_Asset_and_Balance", Lang.T("Filter By Asset and Balance", langObj));
        json.put("Label_balanceSide", Lang.T("Balance Side", langObj));
        json.put("Label_filterBalanceMIN", Lang.T("More or Equal", langObj));
        json.put("Label_filterBalanceMAX", Lang.T("Less or Equal", langObj));
        json.put("Label_Filter_by_Actions_and_Period", Lang.T("Filter by Actions and Period", langObj));
        json.put("Label_filterTXType", Lang.T("Action", langObj));
        json.put("Label_filterTXStartSeqNo", Lang.T("Height start", langObj));
        json.put("Label_filterTXEndSeqNo", Lang.T("Height end", langObj));

        json.put("Label_Filter_by_Persons", Lang.T("Filter by Persons", langObj));
        json.put("Label_filterByGender", Lang.T("Gender", langObj));
        json.put("Label_selfPay", Lang.T("Payout to Self too", langObj));

        json.put("Label_", Lang.T("", langObj));
        return json;

    }

    public JSONObject toJson() {

        JSONObject toJson = new JSONObject();

        toJson.put("flags", flags);
        toJson.put("assetKey", assetKey);
        toJson.put("balancePos", balancePos);
        toJson.put("backward", backward);

        toJson.put("payMethod", payMethod);
        toJson.put("payMethodValue", payMethodValue.toPlainString());
        if (payMethod != PAYMENT_METHOD_ABSOLUTE) {
            toJson.put("amountMin", amountMin);
            toJson.put("amountMax", amountMax);
        }

        toJson.put("filterAssetKey", filterAssetKey);
        toJson.put("filterBalancePos", filterBalancePos);
        toJson.put("filterBalanceSide", filterBalanceSide);
        if (hasAssetFilterBalMIN())
            toJson.put("filterBalanceMIN", filterBalanceMIN);
        if (hasAssetFilterBalMAX())
            toJson.put("filterBalanceMAX", filterBalanceMAX);

        toJson.put("filterTXType", filterTXType);
        if (hasTXTypeFilterActiveStart())
            toJson.put("filterTXStartSeqNo", filterTXStartSeqNo);
        if (hasTXTypeFilterActiveEnd())
            toJson.put("filterTXEndSeqNo", filterTXEndSeqNo);

        toJson.put("filterByGender", filterByGender);
        toJson.put("selfPay", selfPay);

        if (filteredPayoutsCount > 0) {
            toJson.put("filteredPayoutsCount", filteredPayoutsCount);
            toJson.put("totalPay", totalPay.toPlainString());
            toJson.put("totalFeeBytes", totalFeeBytes);
            toJson.put("totalFee", BlockChain.feeBG(totalFeeBytes).toPlainString());
        }

        return toJson;
    }

    public boolean calcPayoutsForMethodTotal() {

        if (filteredPayoutsCount == 0)
            return false;

        // нужно подсчитать выплаты по общей сумме балансов
        int scale = asset.getScale();
        BigDecimal totalBalances = totalPay;
        if (totalBalances.signum() == 0)
            // возможно это просто высылка писем всем - без перечислений
            return false;

        // плдсчитаем ьолее точно сумму к выплате - по коэффициентам она скруглится
        totalPay = BigDecimal.ZERO;
        BigDecimal coefficient = payMethodValue.divide(totalBalances,
                scale + Order.powerTen(totalBalances) + 3, RoundingMode.HALF_DOWN);
        Fun.Tuple3 item;
        BigDecimal amount;
        maxBal = BigDecimal.ZERO;
        for (int index = 0; index < filteredPayoutsCount; index++) {
            item = filteredPayouts.get(index);
            amount = (BigDecimal) item.b;
            amount = amount.multiply(coefficient).setScale(scale, RoundingMode.DOWN);
            totalPay = totalPay.add(amount);
            filteredPayouts.set(index, new Fun.Tuple3(item.a, item.b, amount));

            if (maxBal.compareTo(amount.abs()) < 0) {
                // запомним максимальное для скидывания остатка
                maxBal = amount.abs();
                maxIndex = index;
            }
        }

        BigDecimal totalDiff = payMethodValue.subtract(totalPay);
        if (totalDiff.signum() != 0) {
            // есть нераспределенный остаток
            Fun.Tuple3<Account, BigDecimal, BigDecimal> maxItem = filteredPayouts.get(maxIndex);
            filteredPayouts.set(maxIndex, new Fun.Tuple3(maxItem.a, maxItem.b, maxItem.c.add(totalDiff)));

            totalPay = payMethodValue;
        }

        return true;
    }

    public int isValid(RSignNote rNote) {

        if (hasAmount()) {
            if (this.assetKey == null || this.assetKey == 0L) {
                errorValue = "Payouts: assetKey == null or ZERO";
                return Transaction.INVALID_ITEM_KEY;
            } else if (this.balancePos < TransactionAmount.ACTION_SEND || this.balancePos > TransactionAmount.ACTION_SPEND) {
                errorValue = "Payouts: balancePos out off range";
                return Transaction.INVALID_AMOUNT;
            } else if (this.payMethodValue == null || payMethodValue.signum() == 0) {
                errorValue = "Payouts: payMethodValue == null";
                return Transaction.INVALID_AMOUNT;
            } else if (payMethodValue.signum() < 0) {
                errorValue = "Payouts: payMethodValue < 0";
                return Transaction.INVALID_AMOUNT;
            }
        }

        if (hasAssetFilter()) {
            if (this.filterAssetKey == null || this.filterAssetKey == 0L) {
                errorValue = "Payouts: filterAssetKey == null or ZERO";
                return Transaction.INVALID_ITEM_KEY;
            } else if (this.filterBalancePos < TransactionAmount.ACTION_SEND || this.filterBalancePos > TransactionAmount.ACTION_SPEND) {
                errorValue = "Payouts: filterBalancePos";
                return Transaction.INVALID_BACKWARD_ACTION;
            } else if (this.filterBalanceSide < Account.BALANCE_SIDE_DEBIT || this.filterBalanceSide > Account.BALANCE_SIDE_CREDIT) {
                errorValue = "Payouts: filterBalanceSide";
                return Transaction.INVALID_BACKWARD_ACTION;
            }
        }

        if (this.filterTXType != 0 && !Transaction.isValidTransactionType(this.filterTXType)) {
            errorValue = "Payouts: filterTXType= " + filterTXType;
            return Transaction.INVALID_TRANSACTION_TYPE;
        }

        if (assetKey != null && filterAssetKey != null
                && assetKey.equals(filterAssetKey)
                && balancePos == filterBalancePos) {
            // при откате невозможно тогда будет правильно рассчитать - так как съехала общая сумма
            errorValue = "Payouts: assetKey == filterAssetKey && balancePos == filterBalancePos";
            return Transaction.INVALID_TRANSFER_TYPE;
        }

        filteredPayoutsCount = makeFilterPayList(rNote, true);

        if (filteredPayoutsCount < 0) {
            // ERROR on make LIST
            return -filteredPayoutsCount;

        } else if (filteredPayoutsCount > 0) {
            height = rNote.getBlockHeight();

            if (filterTXType == PAYMENT_METHOD_TOTAL) {
                // просчитаем значения для точного округления Общей Суммы
                if (!calcPayoutsForMethodTotal())
                    // не удалось просчитать значения
                    return Transaction.VALIDATE_OK;
            }

            Account recipient = filteredPayouts.get(0).a;
            PublicKeyAccount creator = rNote.getCreator();
            byte[] signature = rNote.getSignature();

            // возьмем знаки (минус) для создания позиции баланса такой
            Fun.Tuple2<Integer, Integer> signs = Account.getSignsForBalancePos(balancePos);
            long key = signs.a * assetKey;

            // комиссию не проверяем так как она не правильно считается внутри?
            long actionFlags = Transaction.NOT_VALIDATE_FLAG_FEE;

            BigDecimal totalFeeBG = rNote.getFee();
            int result;
            // проверим как будто всю сумму одному переводим - с учетом комиссии полной
            result = TransactionAmount.isValidAction(dcSet, height, creator, signature,
                    key, asset, signs.b > 0 ? totalPay : totalPay.negate(), recipient,
                    backward, totalFeeBG, null, false, actionFlags);
            if (result != Transaction.VALIDATE_OK) {
                errorValue = "Payouts: totalPay + totalFee = " + totalPay.toPlainString() + " / " + totalFeeBG.toPlainString();
                return result;
            }

            ////////// TODO NEED CHECK ALL
            boolean needCheckAllList = false;
            if (needCheckAllList) {

                for (Fun.Tuple3 item : filteredPayouts) {

                    recipient = (Account) item.a;
                    if (recipient == null)
                        break;
                    BigDecimal amount = (BigDecimal) item.c;

                    result = TransactionAmount.isValidAction(dcSet, height, creator, signature,
                            key, asset, signs.b > 0 ? amount : amount.negate(), recipient,
                            backward, BigDecimal.ZERO, null, false, actionFlags);

                    if (result != Transaction.VALIDATE_OK) {
                        errorValue = "Payouts: " + amount.toPlainString() + " -> " + recipient.getAddress();
                        return result;
                    }

                }
            }
        }

        return Transaction.VALIDATE_OK;
    }

    public int makeFilterPayList(DCSet dcSet, int height, AssetCls asset, Account creator, boolean andValidate) {

        filteredPayouts = new ArrayList<>();

        int scale = asset.getScale();

        boolean onlyPerson = filterByGender > FILTER_PERSON_NONE;
        int gender = filterByGender - FILTER_PERSON_ONLY_MAN;
        byte[] accountFrom = creator.getShortAddressBytes();

        ItemAssetBalanceMap balancesMap = dcSet.getAssetBalanceMap();
        TransactionFinalMapImpl txMap = dcSet.getTransactionFinalMap();

        // определим - меняется ли позиция баланса если направление сменим
        // это нужно чтобы отсекать смену знака у балансов для тек активов у кого меняется позиция от знака
        // настроим данные платежа по знакам Актива ИКоличества, так как величина коэффициента способа всегда положительная
        Fun.Tuple2<Integer, Integer> signs = Account.getSignsForBalancePos(balancePos);
        int balancePosDirect = Account.balancePosition(assetKey * signs.a, new BigDecimal(signs.b), false, asset.isSelfManaged());
        int balancePosBackward = Account.balancePosition(assetKey * signs.a, new BigDecimal(signs.b), true, asset.isSelfManaged());
        int filterBySigNum;
        if (balancePosDirect != balancePosBackward) {
            if (balancePosDirect == TransactionAmount.ACTION_SPEND) {
                // используем только отрицательные балансы
                filterBySigNum = -1;
            } else {
                // используем только положительные балансы
                filterBySigNum = 1;
            }
        } else {
            filterBySigNum = 0;
        }

        boolean reversedBalancesInPosition = asset.isReverseBalancePos(balancePos);
        // сменим знак балансов для отрицательных
        if (reversedBalancesInPosition) {
            filterBySigNum *= -1;
        }

        byte[] key;
        BigDecimal balance;
        BigDecimal payout;
        BigDecimal totalBalances = BigDecimal.ZERO;

        int count = 0;

        Fun.Tuple4<Long, Integer, Integer, Integer> addressDuration;
        Long myPersonKey = null;
        if (onlyPerson && !selfPay) {
            addressDuration = dcSet.getAddressPersonMap().getItem(accountFrom);
            if (addressDuration != null) {
                myPersonKey = addressDuration.a;
            }
        } else {
            myPersonKey = null;
        }

        boolean isPerson = creator.isPerson(dcSet, height);

        HashSet<Long> usedPersons = new HashSet<>();
        PersonCls person;
        byte[] assetOwner = asset.getOwner().getShortAddressBytes();

        boolean hasAssetFilter = hasAssetFilter();
        try (IteratorCloseable<byte[]> iterator = balancesMap.getIteratorByAsset(hasAssetFilter ? filterAssetKey : AssetCls.FEE_KEY)) {
            while (iterator.hasNext()) {
                key = iterator.next();

                balance = Account.balanceInPositionAndSide(balancesMap.get(key), filterBalancePos, filterBalanceSide);
                if (filterBySigNum != 0 && balance.signum() != 0 && filterBySigNum != balance.signum()) {
                    // произошла смена направления для актива у котро меняется Позиция баланса - пропускаем такое
                    continue;
                }

                if (hasAssetFilter && filterBalanceMIN != null && balance.compareTo(filterBalanceMIN) < 0
                        || filterBalanceMAX != null && balance.compareTo(filterBalanceMAX) > 0)
                    continue;

                byte[] recipientShort = ItemAssetBalanceMap.getShortAccountFromKey(key);
                if (Arrays.equals(assetOwner, recipientShort))
                    // создателю актива не даем ничего никогда
                    continue;

                if (onlyPerson) {
                    // так как тут сортировка по убыванию значит первым встретится тот счет на котром больше всего актива
                    // - он и будет выбран куда 1 раз пошлем актив свой
                    addressDuration = dcSet.getAddressPersonMap().getItem(recipientShort);
                    if (addressDuration == null)
                        continue;
                    if (usedPersons.contains(addressDuration.a))
                        continue;

                    if (!selfPay && myPersonKey != null && myPersonKey.equals(addressDuration.a)) {
                        // сами себе не платим?
                        continue;
                    }

                    person = (PersonCls) dcSet.getItemPersonMap().get(addressDuration.a);

                    if (gender >= 0 && person.getGender() != gender) {
                        continue;
                    }

                } else {

                    if (!selfPay && Arrays.equals(accountFrom, recipientShort)) {
                        // сами себе не платим?
                        continue;
                    }

                    addressDuration = null;
                    person = null;
                }

                Account recipient = new Account(recipientShort);

                /// если задано то проверим - входит ли в в диапазон
                // - собранные блоки учитываем? да - иначе долго будет делать поиск
                if (filterTXType != 0 || filterTXStartSeqNo != null || filterTXEndSeqNo != null) {
                    // на счете должна быть активность в заданном диапазоне для данного типа
                    if (!txMap.isCreatorWasActive(recipientShort, filterTXStartSeqNo, filterTXType, filterTXEndSeqNo))
                        continue;
                }

                // IF send from PERSON to ANONYMOUS
                if (hasAssetFilter && andValidate && !TransactionAmount.isValidPersonProtect(dcSet, height, recipient,
                        isPerson, assetKey, balancePos,
                        asset)) {
                    errorValue = recipient.getAddress();
                    return -Transaction.RECEIVER_NOT_PERSONALIZED;
                }

                if (!hasAssetFilter) {
                    payout = null;
                } else {
                    switch (payMethod) {
                        case PAYMENT_METHOD_COEFF:
                            // нужно вычислить сразу сколько шлем
                            payout = balance.multiply(payMethodValue).setScale(scale, RoundingMode.HALF_DOWN);
                            totalBalances = totalBalances.add(payout);
                            break;
                        case PAYMENT_METHOD_ABSOLUTE:
                            payout = payMethodValue.setScale(scale, RoundingMode.HALF_DOWN);
                            break;
                        default:
                            payout = null;
                            totalBalances = totalBalances.add(balance);
                    }
                }

                // не проверяем на 0 - так это может быть рассылка писем всем
                filteredPayouts.add(new Fun.Tuple3(recipient, balance, payout));

                count++;
                if (andValidate && count > MAX_COUNT) {
                    errorValue = "MAX count over: " + MAX_COUNT;
                    return -Transaction.INVALID_BLOCK_TRANS_SEQ_ERROR;
                }

                if (onlyPerson) {
                    // учтем что такой персоне давали
                    usedPersons.add(addressDuration.a);
                }

            }

        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
        }

        switch (payMethod) {
            case PAYMENT_METHOD_ABSOLUTE:
                totalPay = payMethodValue.multiply(new BigDecimal(count));
                break;
            default:
                totalPay = totalBalances;
        }

        filteredPayoutsCount = count;
        calcTotalFeeBytes();
        return count;

    }

    public int makeFilterPayList(Transaction transaction, boolean andValidate) {
        return makeFilterPayList(transaction.getDCSet(), height, asset, transaction.getCreator(), andValidate);
    }

    public void processBody(Transaction rNote, boolean asOrphan, Block block) {
        PublicKeyAccount creator = rNote.getCreator();

        if (hasAssetFilter()) {
            boolean isDirect = asset.isDirectBalances();
            long absKey = assetKey;

            // возьмем знаки (минус) для создания позиции баланса такой
            Fun.Tuple2<Integer, Integer> signs = Account.getSignsForBalancePos(balancePos);
            Long actionPayKey = signs.a * assetKey;
            boolean isAmountNegate;
            BigDecimal actionPayAmount;
            boolean incomeReverse = balancePos == Account.BALANCE_POS_HOLD;
            boolean reversedBalancesInPosition = asset.isReverseBalancePos(balancePos);
            boolean backwardAction;

            Account recipient;
            for (Fun.Tuple3 item : filteredPayouts) {

                recipient = (Account) item.a;
                if (recipient == null)
                    break;
                actionPayAmount = (BigDecimal) item.c;

                isAmountNegate = actionPayAmount.signum() < 0;
                backwardAction = (reversedBalancesInPosition ^ backward) ^ isAmountNegate;

                if (!asOrphan && block != null) {
                    rNote.addCalculated(block, recipient, absKey, actionPayAmount,
                            asset.viewAssetTypeAction(backwardAction, balancePos, asset.getOwner().equals(creator)));
                }

                // сбросим направлени от фильтра
                actionPayAmount = actionPayAmount.abs();
                // зазадим направление от Действия нашего
                actionPayAmount = signs.b > 0 ? actionPayAmount : actionPayAmount.negate();

                TransactionAmount.processAction(dcSet, asOrphan, creator, recipient, balancePos, absKey,
                        asset, actionPayKey, actionPayAmount, backwardAction,
                        incomeReverse);


            }
        }

    }

    public void process(Transaction rNote, Block block) {

        if (filteredPayouts == null) {
            filteredPayoutsCount = makeFilterPayList(rNote, false);
        }

        if (filteredPayoutsCount == 0)
            return;

        if (payMethod == PAYMENT_METHOD_TOTAL) {
            if (!calcPayoutsForMethodTotal())
                // не удалось просчитать значения
                return;
        }

        height = rNote.getBlockHeight();
        asset = dcSet.getItemAssetMap().get(assetKey);

        processBody(rNote, false, block);

    }

    public void orphan(Transaction rNote) {

        if (filteredPayouts == null) {
            filteredPayoutsCount = makeFilterPayList(rNote, false);
        }

        if (filteredPayoutsCount == 0)
            return;

        if (payMethod == PAYMENT_METHOD_TOTAL) {
            if (payMethod == PAYMENT_METHOD_TOTAL) {
                if (!calcPayoutsForMethodTotal())
                    // не удалось просчитать значения
                    return;
            }
        }

        height = rNote.getBlockHeight();
        asset = dcSet.getItemAssetMap().get(assetKey);

        processBody(rNote, true, null);

    }

}
