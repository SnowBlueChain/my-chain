package org.erachain.core.transaction;

import com.google.common.primitives.Bytes;
import com.google.common.primitives.Ints;
import com.google.common.primitives.Longs;
import org.erachain.controller.Controller;
import org.erachain.core.BlockChain;
import org.erachain.core.account.Account;
import org.erachain.core.account.PrivateKeyAccount;
import org.erachain.core.account.PublicKeyAccount;
import org.erachain.core.block.Block;
import org.erachain.core.block.GenesisBlock;
import org.erachain.core.blockexplorer.ExplorerJsonLine;
import org.erachain.core.crypto.Base58;
import org.erachain.core.crypto.Crypto;
import org.erachain.core.item.ItemCls;
import org.erachain.core.item.assets.AssetCls;
import org.erachain.core.item.persons.PersonCls;
import org.erachain.datachain.DCSet;
import org.erachain.datachain.TransactionFinalMapImpl;
import org.erachain.settings.Settings;
import org.erachain.utils.DateTimeFormat;
import org.json.simple.JSONObject;
import org.mapdb.Fun;
import org.mapdb.Fun.Tuple2;
import org.mapdb.Fun.Tuple3;
import org.mapdb.Fun.Tuple4;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.*;

// import org.slf4j.LoggerFactory;

//import java.math.RoundingMode;
//import java.math.MathContext;
//import java.util.Comparator;
//import javax.swing.JFrame;
//import javax.swing.JOptionPane;
//import org.erachain.lang.Lang;
//import org.erachain.settings.Settings;

public abstract class Transaction implements ExplorerJsonLine {


    /*
     *  SEE in concrete TRANSACTIONS
     * public static final byte[][] VALID_RECORDS = new byte[][]{
     * };
     */

    public static final int BALANCE_SIDE_DEBIT = 1;
    public static final int BALANCE_SIDE_LEFT = 2;
    public static final int BALANCE_SIDE_CREDIT = 3;
    public static final int BALANCE_SIDE_FORGED = 4;

    // toBYTE & PARSE fields for different DEALs
    public static final int FOR_MYPACK = 1; // not use this.timestamp & this.feePow
    public static final int FOR_PACK = 2; // not use feePow
    public static final int FOR_NETWORK = 3; // use all (but not calcalated)
    public static final int FOR_DB_RECORD = 4; // use all + calcalated fields (FEE, BlockNo + SeqNo)

    // FLAGS for VALIDATING
    public static final long NOT_VALIDATE_FLAG_FEE = 1l;
    public static final long NOT_VALIDATE_FLAG_PERSONAL = 2l;
    public static final long NOT_VALIDATE_FLAG_PUBLIC_TEXT = 4l;
    public static final long NOT_VALIDATE_FLAG_BALANCE = 8l;
    public static final long NOT_VALIDATE_KEY_COLLISION = 16l;

    // VALIDATION CODE
    public static final int VALIDATE_OK = 1;
    public static final int FUTURE_ABILITY = 2;
    public static final int INVALID_WALLET_ADDRESS = 3;
    public static final int INVALID_MAKER_ADDRESS = 5;
    public static final int INVALID_REFERENCE = 6;
    /**
     * Если откат был в ДЕВЕЛОПе и в этом блоке была первая транзакция то потом откат
     */
    public static final int INVALID_TIMESTAMP = 7;
    public static final int INVALID_ADDRESS = 8;
    public static final int INVALID_FEE_POWER = 9;
    public static final int NOT_ENOUGH_FEE = 10;
    public static final int NO_BALANCE = 11;
    public static final int INVALID_PUBLIC_KEY = 12;
    public static final int INVALID_RAW_DATA = 13;
    public static final int INVALID_DATE = 14;
    public static final int INVALID_CREATOR = 15; // for some reasons that
    // creator is invalid (same
    // as trade order)
    public static final int INVALID_SIGNATURE = 16;
    public static final int NO_DEBT_BALANCE = 17;
    public static final int NO_HOLD_BALANCE = 18;
    public static final int INVALID_TRANSFER_TYPE = 19;
    public static final int NOT_ENOUGH_RIGHTS = 20;
    public static final int OWNER_NOT_PERSONALIZED = 21;
    public static final int ACCOUNT_ALREADY_PERSONALIZED = 23;
    public static final int TRANSACTION_DOES_NOT_EXIST = 24;
    public static final int CREATOR_NOT_PERSONALIZED = 25;
    public static final int RECEIVER_NOT_PERSONALIZED = 26;
    public static final int INVALID_CLAIM_RECIPIENT = 27;
    public static final int INVALID_CLAIM_DEBT_RECIPIENT = 28;
    public static final int INVALID_RECEIVER = 29;

    // ASSETS
    public static final int INVALID_QUANTITY = 30;

    public static final int INVALID_AMOUNT_IS_NULL = 31;
    public static final int NEGATIVE_AMOUNT = 32;
    public static final int INVALID_AMOUNT = 33;
    public static final int INVALID_RETURN = 34;
    public static final int HAVE_EQUALS_WANT = 35;
    public static final int ORDER_DOES_NOT_EXIST = 36;
    public static final int INVALID_ORDER_CREATOR = 37;
    public static final int INVALID_PAYMENTS_LENGTH = 38;
    public static final int NEGATIVE_PRICE = 39;
    public static final int INVALID_PRICE = 40;
    public static final int INVALID_CREATION_BYTES = 41;
    public static final int INVALID_TAGS_LENGTH = 42;
    public static final int INVALID_TYPE_LENGTH = 43;
    public static final int NOT_MOVABLE_ASSET = 44;
    public static final int NOT_DEBT_ASSET = 45;
    public static final int INVALID_ACCOUNTING_PAIR = 46;
    public static final int INVALID_HOLD_DIRECTION = 47;
    public static final int INVALID_ECXHANGE_PAIR = 48;

    public static final int NO_INCLAIM_BALANCE = 49;

    public static final int HASH_ALREDY_EXIST = 51;

    public static final int INVALID_CLAIM_DEBT_CREATOR = 61;

    public static final int NOT_ENOUGH_ERA_OWN_10 = 101;
    public static final int NOT_ENOUGH_ERA_USE_10 = 102;
    public static final int NOT_ENOUGH_ERA_OWN_100 = 103;
    public static final int NOT_ENOUGH_ERA_USE_100 = 104;
    public static final int NOT_ENOUGH_ERA_OWN_1000 = 105;
    public static final int NOT_ENOUGH_ERA_USE_1000 = 106;

    public static final int INVALID_BACKWARD_ACTION = 117;
    public static final int NOT_SELF_PERSONALIZY = 118;
    public static final int PUB_KEY_NOT_PERSONALIZED = 119;

    public static final int INVALID_ISSUE_PROHIBITED = 150;
    public static final int INVALID_NAME_LENGTH_MIN = 151;
    public static final int INVALID_NAME_LENGTH_MAX = 152;
    public static final int INVALID_ICON_LENGTH_MIN = 153;
    public static final int INVALID_ICON_LENGTH_MAX = 154;
    public static final int INVALID_IMAGE_LENGTH_MIN = 155;
    public static final int INVALID_IMAGE_LENGTH_MAX = 156;
    public static final int INVALID_DESCRIPTION_LENGTH_MIN = 157;
    public static final int INVALID_DESCRIPTION_LENGTH_MAX = 158;
    public static final int INVALID_VALUE_LENGTH_MIN = 159;
    public static final int INVALID_VALUE_LENGTH_MAX = 160;
    public static final int INVALID_TITLE_LENGTH_MIN = 161;
    public static final int INVALID_TITLE_LENGTH_MAX = 162;

    public static final int NOT_DEBTABLE_ASSET = 171;
    public static final int NOT_HOLDABLE_ASSET = 172;
    public static final int NOT_SPENDABLE_ASSET = 173;

    /**
     * Прровека на коллизию ключа по подписи - проверяем только если усекаем его и нетпроверки на двойную трату -
     * BlockChain#CHECK_DOUBLE_SPEND_DEEP
     */
    public static final int KEY_COLLISION = 194;

    public static final int INVALID_MESSAGE_FORMAT = 195;
    public static final int INVALID_MESSAGE_LENGTH = 196;
    public static final int UNKNOWN_PUBLIC_KEY_FOR_ENCRYPT = 197;


    // ITEMS
    public static final int INVALID_ITEM_KEY = 201;
    public static final int INVALID_ITEM_VALUE = 202;
    public static final int ITEM_DOES_NOT_EXIST = 203;
    public static final int ITEM_ASSET_NOT_EXIST = 204;
    public static final int ITEM_IMPRINT_DOES_NOT_EXIST = 205;
    public static final int ITEM_TEMPLATE_NOT_EXIST = 206;
    public static final int ITEM_PERSON_NOT_EXIST = 207;
    public static final int ITEM_STATUS_NOT_EXIST = 208;
    public static final int ITEM_UNION_NOT_EXIST = 209;
    public static final int ITEM_DOES_NOT_STATUSED = 210;
    public static final int ITEM_DOES_NOT_UNITED = 211;
    public static final int ITEM_DUPLICATE_KEY = 212;
    public static final int ITEM_DUPLICATE = 213;
    public static final int INVALID_TIMESTAMP_START = 214;
    public static final int INVALID_TIMESTAMP_END = 215;

    public static final int ITEM_PERSON_IS_DEAD = 235;
    public static final int AMOUNT_LENGHT_SO_LONG = 236;
    public static final int AMOUNT_SCALE_SO_BIG = 237;
    public static final int AMOUNT_SCALE_WRONG = 238;

    public static final int ITEM_PERSON_LATITUDE_ERROR = 250;
    public static final int ITEM_PERSON_LONGITUDE_ERROR = 251;
    public static final int ITEM_PERSON_RACE_ERROR = 252;
    public static final int ITEM_PERSON_GENDER_ERROR = 253;
    public static final int ITEM_PERSON_SKIN_COLOR_ERROR = 254;
    public static final int ITEM_PERSON_EYE_COLOR_ERROR = 255;
    public static final int ITEM_PERSON_HAIR_COLOR_ERROR = 256;
    public static final int ITEM_PERSON_HEIGHT_ERROR = 257;
    public static final int ITEM_PERSON_OWNER_SIGNATURE_INVALID = 258;

    // NAMES
    public static final int NAME_DOES_NOT_EXIST = 5060;
    public static final int NAME_ALREADY_REGISTRED = 5061;
    public static final int NAME_ALREADY_ON_SALE = 5062;
    public static final int NAME_NOT_FOR_SALE = 5063;
    public static final int BUYER_ALREADY_OWNER = 5064;
    public static final int NAME_NOT_LOWER_CASE = 5065;
    public static final int NAME_WITH_SPACE = 5066;

    public static final int CREATOR_NOT_OWNER = 367;
    public static final int NAME_KEY_ALREADY_EXISTS = 368;
    public static final int NAME_KEY_NOT_EXISTS = 369;
    public static final int LAST_KEY_IS_DEFAULT_KEY = 370;

    // POLL
    public static final int INVALID_OPTIONS_LENGTH = 380;
    public static final int INVALID_OPTION_LENGTH = 381;
    public static final int DUPLICATE_OPTION = 382;
    public static final int POLL_ALREADY_CREATED = 383;
    public static final int POLL_ALREADY_HAS_VOTES = 384;
    public static final int POLL_NOT_EXISTS = 385;
    public static final int POLL_OPTION_NOT_EXISTS = 386;
    public static final int ALREADY_VOTED_FOR_THAT_OPTION = 387;
    public static final int INVALID_DATA_LENGTH = 388;
    public static final int INVALID_DATA = 389;
    public static final int INVALID_PARAMS_LENGTH = 390;
    public static final int INVALID_URL_LENGTH = 391;
    public static final int INVALID_HEAD_LENGTH = 392;
    public static final int INVALID_DATA_FORMAT = 393;

    public static final int PRIVATE_KEY_NOT_FOUND = 530;
    public static final int INVALID_UPDATE_VALUE = 540;
    public static final int INVALID_TRANSACTION_TYPE = 550;
    public static final int INVALID_BLOCK_HEIGHT = 599;
    public static final int INVALID_BLOCK_TRANS_SEQ_ERROR = 501;
    public static final int TELEGRAM_DOES_NOT_EXIST = 541;
    public static final int NOT_YET_RELEASED = 599;
    public static final int AT_ERROR = 600; // END error for org.erachain.api.ApiErrorFactory.ERROR

    // 
    // TYPES *******
    // universal
    public static final int EXTENDED = 0;
    // genesis
    public static final int GENESIS_ISSUE_ASSET_TRANSACTION = 1;
    public static final int GENESIS_ISSUE_TEMPLATE_TRANSACTION = 2;
    public static final int GENESIS_ISSUE_PERSON_TRANSACTION = 3;
    public static final int GENESIS_ISSUE_STATUS_TRANSACTION = 4;
    public static final int GENESIS_ISSUE_UNION_TRANSACTION = 5;
    public static final int GENESIS_SEND_ASSET_TRANSACTION = 6;
    public static final int GENESIS_SIGN_NOTE_TRANSACTION = 7;
    public static final int GENESIS_CERTIFY_PERSON_TRANSACTION = 8;
    public static final int GENESIS_ASSIGN_STATUS_TRANSACTION = 9;
    public static final int GENESIS_ADOPT_UNION_TRANSACTION = 10;
    // ISSUE ITEMS
    public static final int ISSUE_ASSET_TRANSACTION = 21;
    public static final int ISSUE_IMPRINT_TRANSACTION = 22;
    public static final int ISSUE_TEMPLATE_TRANSACTION = 23;
    public static final int ISSUE_PERSON_TRANSACTION = 24;
    public static final int ISSUE_STATUS_TRANSACTION = 25;
    public static final int ISSUE_UNION_TRANSACTION = 26;
    public static final int ISSUE_STATEMENT_TRANSACTION = 27;
    public static final int ISSUE_POLL_TRANSACTION = 28;
    // SEND ASSET
    public static final int SEND_ASSET_TRANSACTION = 31;
    // OTHER
    public static final int SIGN_NOTE_TRANSACTION = 35;
    public static final int CERTIFY_PUB_KEYS_TRANSACTION = 36;
    public static final int SET_STATUS_TO_ITEM_TRANSACTION = 37;
    public static final int SET_UNION_TO_ITEM_TRANSACTION = 38;
    public static final int SET_UNION_STATUS_TO_ITEM_TRANSACTION = 39;
    // confirm other transactions
    public static final int VOUCH_TRANSACTION = 40;
    // HASHES
    public static final int HASHES_RECORD = 41;
    // exchange of assets
    public static final int CREATE_ORDER_TRANSACTION = 50;
    public static final int CANCEL_ORDER_TRANSACTION = 51;
    // voting
    public static final int CREATE_POLL_TRANSACTION = 61;
    public static final int VOTE_ON_POLL_TRANSACTION = 62;
    public static final int VOTE_ON_ITEM_POLL_TRANSACTION = 63;
    public static final int RELEASE_PACK = 70;

    public static final int CALCULATED_TRANSACTION = 100;

    // old
    public static final int REGISTER_NAME_TRANSACTION = 6 + 130;
    public static final int UPDATE_NAME_TRANSACTION = 7 + 130;
    public static final int SELL_NAME_TRANSACTION = 8 + 130;
    public static final int CANCEL_SELL_NAME_TRANSACTION = 9 + 130;
    public static final int BUY_NAME_TRANSACTION = 10 + 130;
    public static final int ARBITRARY_TRANSACTION = 12 + 130;
    public static final int MULTI_PAYMENT_TRANSACTION = 13 + 130;
    public static final int DEPLOY_AT_TRANSACTION = 14 + 130;
    // FEE PARAMETERS
    public static final long RIGHTS_KEY = AssetCls.ERA_KEY;

    // public static final int ACCOUNTING_TRANSACTION = 26;
    // public static final int JSON_TRANSACTION = 27;
    public static final long FEE_KEY = AssetCls.FEE_KEY;

    // FEE PARAMETERS public static final int FEE_PER_BYTE = 1;
    // protected static final int PROP_LENGTH = 2; // properties
    public static final int TIMESTAMP_LENGTH = 8;

    // RELEASES
    // private static final long ASSETS_RELEASE = 0l;
    // private static final long POWFIX_RELEASE = 0L; // Block Version 3 //
    // 2016-02-25T19:00:00+00:00
    // public static final int REFERENCE_LENGTH = Crypto.SIGNATURE_LENGTH;
    public static final int REFERENCE_LENGTH = TIMESTAMP_LENGTH;

    /*
     * / public static long getVOTING_RELEASE() {
     * if(Settings.getInstance().isTestnet()) { return
     * Settings.getInstance().getGenesisStamp(); } return VOTING_RELEASE; }
     *
     * public static long getARBITRARY_TRANSACTIONS_RELEASE() {
     * if(Settings.getInstance().isTestnet()) { return
     * Settings.getInstance().getGenesisStamp(); } return
     * ARBITRARY_TRANSACTIONS_RELEASE; }
     *
     * public static int getAT_BLOCK_HEIGHT_RELEASE() {
     * if(Settings.getInstance().isTestnet()) { return 1; } return
     * AT_BLOCK_HEIGHT_RELEASE; }
     *
     * public static int getMESSAGE_BLOCK_HEIGHT_RELEASE() {
     * if(Settings.getInstance().isTestnet()) { return 1; } return
     * MESSAGE_BLOCK_HEIGHT_RELEASE; }
     *
     * public static long getASSETS_RELEASE() {
     * if(Settings.getInstance().isTestnet()) { return
     * Settings.getInstance().getGenesisStamp(); } return ASSETS_RELEASE; }
     *
     * public static long getPOWFIX_RELEASE() {
     * if(Settings.getInstance().isTestnet()) { return
     * Settings.getInstance().getGenesisStamp(); } return POWFIX_RELEASE; }
     */
    public static final int KEY_LENGTH = 8;
    // not need now protected static final int FEE_LENGTH = 8;
    public static final int SIGNATURE_LENGTH = Crypto.SIGNATURE_LENGTH;
    protected static final int TODO_h1 = 69000;
    // PROPERTIES LENGTH
    protected static final int SIMPLE_TYPE_LENGTH = 1;
    public static final int TYPE_LENGTH = 4;
    protected static final int HEIGHT_LENGTH = 4;
    public static final int DATA_JSON_PART_LENGTH = 4;
    public static final int DATA_VERSION_PART_LENGTH = 6;
    public static final int DATA_TITLE_PART_LENGTH = 4;
    protected static final int DATA_NUM_FILE_LENGTH = 4;
    protected static final int SEQ_LENGTH = 4;
    public static final int DATA_SIZE_LENGTH = 4;
    public static final int ENCRYPTED_LENGTH = 1;
    public static final int IS_TEXT_LENGTH = 1;
    protected static final int FEE_POWER_LENGTH = 1;
    public static final int FEE_LENGTH = 8;
    // protected static final int HKEY_LENGTH = 20;
    public static final int CREATOR_LENGTH = PublicKeyAccount.PUBLIC_KEY_LENGTH;
    protected static final int BASE_LENGTH_AS_MYPACK = TYPE_LENGTH;
    protected static final int BASE_LENGTH_AS_PACK = BASE_LENGTH_AS_MYPACK + TIMESTAMP_LENGTH
            + CREATOR_LENGTH + SIGNATURE_LENGTH;
    protected static final int BASE_LENGTH = BASE_LENGTH_AS_PACK + FEE_POWER_LENGTH + REFERENCE_LENGTH;
    protected static final int BASE_LENGTH_AS_DBRECORD = BASE_LENGTH + FEE_LENGTH;

    /**
     * Используется для разделения строки поисковых слов для всех трнзакций.<br>
     * % и @ и # - пусть они будут служебные и по ним не делать разделения
     * так чтобы можно было найти @P указатель на персон например
     * % - это указатель на параметр например иак - %1
     */
    public static String SPLIT_CHARS = "[!?/_., \\~`+&^№*()<>\\\"\\'|\\[\\]{}=;:\\\\]";

    // in pack toByte and Parse - reference not included
    static Logger LOGGER = LoggerFactory.getLogger(Transaction.class.getName());

    protected DCSet dcSet;
    protected String TYPE_NAME = "unknown";
    // protected int type;
    protected byte[] typeBytes;

    protected int height;
    protected int seqNo;
    protected long dbRef; // height + SeqNo

    // TODO REMOVE REFERENCE - use TIMESTAMP as reference
    protected Long reference = 0l;
    protected BigDecimal fee = BigDecimal.ZERO; // - for genesis
    // transactions
    // protected BigDecimal fee = new BigDecimal.valueOf(999000);
    protected byte feePow = 0;
    protected byte[] signature;
    protected long timestamp;
    protected PublicKeyAccount creator;
    protected Fun.Tuple4<Long, Integer, Integer, Integer> creatorPersonDuration;
    protected PersonCls creatorPerson;

    protected Object[][] itemsKeys;

    /**
     * если да то значит взята из Пула трнзакций и на двойную трату проверялась
     */
    public boolean checkedByPool;

    // need for genesis
    protected Transaction(byte type, String type_name) {
        this.typeBytes = new byte[]{type, 0, 0, 0}; // for GENESIS
        this.TYPE_NAME = type_name;
    }

    protected Transaction(byte[] typeBytes, String type_name, PublicKeyAccount creator, byte feePow, long timestamp,
                          Long reference) {
        this.typeBytes = typeBytes;
        this.TYPE_NAME = type_name;
        this.creator = creator;
        // this.props = props;
        this.timestamp = timestamp;
        this.reference = reference;
        if (feePow < 0)
            feePow = 0;
        else if (feePow > BlockChain.FEE_POW_MAX)
            feePow = BlockChain.FEE_POW_MAX;
        this.feePow = feePow;
    }

    protected Transaction(byte[] typeBytes, String type_name, PublicKeyAccount creator, byte feePow, long timestamp,
                          Long reference, byte[] signature) {
        this(typeBytes, type_name, creator, feePow, timestamp, reference);
        this.signature = signature;
    }

    public static int getVersion(byte[] typeBytes) {
        return Byte.toUnsignedInt(typeBytes[1]);
    }


    public static Transaction findByHeightSeqNo(DCSet db, int height, int seq) {
        return db.getTransactionFinalMap().get(height, seq);
    }

    @Override
    public int hashCode() {
        return Ints.fromByteArray(signature);
    }

    @Override
    public boolean equals(Object transaction) {
        if (transaction instanceof Transaction)
            return Arrays.equals(this.signature, ((Transaction) transaction).signature);
        return false;
    }

    public boolean trueEquals(Object transaction) {
        if (transaction == null)
            return false;
        else if (transaction instanceof Transaction)
            return Arrays.equals(this.toBytes(FOR_NETWORK, true),
                    ((Transaction) transaction).toBytes(FOR_NETWORK, true));
        return false;
    }

    // reference in Map - or as signatire or as BlockHeight + seqNo
    public static Transaction findByDBRef(DCSet db, byte[] dbRef) {

        if (dbRef == null)
            return null;

        Long key;
        if (dbRef.length > 20) {
            // soft or hard confirmations
            key = db.getTransactionFinalMapSigns().get(dbRef);
            if (key == null) {
                return db.getTransactionTab().get(dbRef);
            }
        } else {
            int heightBlock = Ints.fromByteArray(Arrays.copyOfRange(dbRef, 0, 4));
            int seqNo = Ints.fromByteArray(Arrays.copyOfRange(dbRef, 4, 8));
            key = Transaction.makeDBRef(heightBlock, seqNo);

        }

        return db.getTransactionFinalMap().get(key);

    }

    public static Map<String, Map<Long, BigDecimal>> subAssetAmount(Map<String, Map<Long, BigDecimal>> allAssetAmount,
                                                                    String address, Long assetKey, BigDecimal amount) {
        return addAssetAmount(allAssetAmount, address, assetKey, BigDecimal.ZERO.subtract(amount));
    }

    public static Map<String, Map<Long, BigDecimal>> addAssetAmount(Map<String, Map<Long, BigDecimal>> allAssetAmount,
                                                                    String address, Long assetKey, BigDecimal amount) {
        Map<String, Map<Long, BigDecimal>> newAllAssetAmount;
        if (allAssetAmount != null) {
            newAllAssetAmount = new LinkedHashMap<String, Map<Long, BigDecimal>>(allAssetAmount);
        } else {
            newAllAssetAmount = new LinkedHashMap<String, Map<Long, BigDecimal>>();
        }

        Map<Long, BigDecimal> newAssetAmountOfAddress;

        if (!newAllAssetAmount.containsKey(address)) {
            newAssetAmountOfAddress = new LinkedHashMap<Long, BigDecimal>();
            newAssetAmountOfAddress.put(assetKey, amount);

            newAllAssetAmount.put(address, newAssetAmountOfAddress);
        } else {
            if (!newAllAssetAmount.get(address).containsKey(assetKey)) {
                newAssetAmountOfAddress = new LinkedHashMap<Long, BigDecimal>(newAllAssetAmount.get(address));
                newAssetAmountOfAddress.put(assetKey, amount);

                newAllAssetAmount.put(address, newAssetAmountOfAddress);
            } else {
                newAssetAmountOfAddress = new LinkedHashMap<Long, BigDecimal>(newAllAssetAmount.get(address));
                BigDecimal newAmount = newAllAssetAmount.get(address).get(assetKey).add(amount);
                newAssetAmountOfAddress.put(assetKey, newAmount);

                newAllAssetAmount.put(address, newAssetAmountOfAddress);
            }
        }

        return newAllAssetAmount;
    }

    public static Map<String, Map<Long, Long>> addStatusTime(Map<String, Map<Long, Long>> allStatusTime, String address,
                                                             Long assetKey, Long time) {
        Map<String, Map<Long, Long>> newAllStatusTime;
        if (allStatusTime != null) {
            newAllStatusTime = new LinkedHashMap<String, Map<Long, Long>>(allStatusTime);
        } else {
            newAllStatusTime = new LinkedHashMap<String, Map<Long, Long>>();
        }

        Map<Long, Long> newStatusTimetOfAddress;

        if (!newAllStatusTime.containsKey(address)) {
            newStatusTimetOfAddress = new LinkedHashMap<Long, Long>();
            newStatusTimetOfAddress.put(assetKey, time);

            newAllStatusTime.put(address, newStatusTimetOfAddress);
        } else {
            if (!newAllStatusTime.get(address).containsKey(assetKey)) {
                newStatusTimetOfAddress = new LinkedHashMap<Long, Long>(newAllStatusTime.get(address));
                newStatusTimetOfAddress.put(assetKey, time);

                newAllStatusTime.put(address, newStatusTimetOfAddress);
            } else {
                newStatusTimetOfAddress = new LinkedHashMap<Long, Long>(newAllStatusTime.get(address));
                Long newTime = newAllStatusTime.get(address).get(assetKey) + time;
                newStatusTimetOfAddress.put(assetKey, newTime);

                newAllStatusTime.put(address, newStatusTimetOfAddress);
            }
        }

        return newAllStatusTime;
    }

    // GETTERS/SETTERS

    public void setHeightSeq(int height, int seqNo) {
        this.dbRef = makeDBRef(height, seqNo);
        this.height = height;
        this.seqNo = seqNo;
    }

    // NEED FOR DB SECONDATY KEYS
    // see org.mapdb.Bind.secondaryKeys
    public void setDC(DCSet dcSet) {
        this.dcSet = dcSet;

        if (BlockChain.TEST_DB == 0 && creator != null) {
            creatorPersonDuration = creator.getPersonDuration(dcSet);
            if (creatorPersonDuration != null) {
                creatorPerson = (PersonCls) dcSet.getItemPersonMap().get(creatorPersonDuration.a);
            }
        }

        makeItemsKeys();

    }

    public void setDC_HeightSeq(DCSet dcSet) {
        setDC(dcSet);

        if (this.typeBytes[0] == Transaction.CALCULATED_TRANSACTION) {

        }

        Long dbRef2 = dcSet.getTransactionFinalMapSigns().get(this.signature);
        if (dbRef2 == null)
            return;

        this.dbRef = dbRef2;
        Tuple2<Integer, Integer> pair = Transaction.parseDBRef(dbRef2);
        this.height = pair.a;
        this.seqNo = pair.b;
    }

    public void setDC(DCSet dcSet, int asDeal, int blockHeight, int seqNo) {
        setDC(dcSet);
        this.height = blockHeight; //this.getBlockHeightByParentOrLast(dcSet);
        this.seqNo = seqNo;
        this.dbRef = Transaction.makeDBRef(height, seqNo);
        if (asDeal > Transaction.FOR_PACK && (this.fee == null || this.fee.signum() == 0))
            this.calcFee();
    }

    public boolean noDCSet() {
        return this.dcSet == null;
    }

    public int getType() {
        return Byte.toUnsignedInt(this.typeBytes[0]);
    }

    public int getVersion() {
        return Byte.toUnsignedInt(this.typeBytes[1]);
    }

    public byte[] getTypeBytes() {
        return this.typeBytes;
    }

    public PublicKeyAccount getCreator() {
        return this.creator;
    }

    public List<PublicKeyAccount> getPublicKeys() {
        return null;
    }

    public Long getTimestamp() {
        return this.timestamp;
    }

    public static Long getTimestampByDBRef(Long dbRef) {
        Tuple2<Integer, Integer> key = parseDBRef(dbRef);
        BlockChain blockChain = Controller.getInstance().getBlockChain();
        return blockChain.getTimestamp(key.a) + key.b;
    }

    // for test signature only!!!
    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public long getDeadline() {
        return this.timestamp + BlockChain.UNCONFIRMED_DEADTIME_MS(this.timestamp);
    }

    /// tyutuy jhg jhg jg j
    /*
     * // TIME public Long viewTime() { return 0L; } public Long getTime() {
     * return this.viewTime(); } public Long viewTime(Account account) { return
     * 0L; } public Long getTime(Account account) { return
     * this.viewTime(account); }
     */
    public long getKey() {
        return 0l;
    }

    public Object[][] getItemsKeys() {
        return itemsKeys;
    }

    public long getAbsKey() {
        long key = this.getKey();
        if (key < 0)
            return -key;
        return key;
    }

    public String getTypeKey() {
        return "";
    }

    public BigDecimal getAmount() {
        return BigDecimal.ZERO;
    }

    public BigDecimal getAmount(Account account) {
        return BigDecimal.ZERO;
    }

    public BigDecimal getAmount(String account) {
        return BigDecimal.ZERO;
    }

    public BigDecimal getFee(String address) {

        if (this.creator != null)
            if (this.creator.getAddress().equals(address))
                return this.fee;
        return BigDecimal.ZERO;
    }

    public BigDecimal getFee(Account account) {
        return this.getFee(account.getAddress());
    }

    public BigDecimal getFee() {
        return this.fee;
    }

    public long getFeeLong() {
        return this.fee.unscaledValue().longValue();
    }

    public String getTitle() {
        return "";
    }

    public void makeItemsKeys() {
        if (creatorPersonDuration != null) {
            itemsKeys = new Object[][]{
                    new Object[]{ItemCls.PERSON_TYPE, creatorPersonDuration.a}
            };
        }
    }

    public static String[] tags(String tags, String words, Object[][] itemsKeys) {
        if (words != null)
            tags += " " + words;

        String[] tagsWords = tags.toLowerCase().split(SPLIT_CHARS);

        if (itemsKeys == null || itemsKeys.length == 0)
            return tagsWords;

        String[] tagsArray = new String[tagsWords.length + itemsKeys.length];

        System.arraycopy(tagsWords, 0, tagsArray, 0, tagsWords.length);
        for (int i = tagsWords.length; i < tagsArray.length; i++) {
            try {
                Object[] itemKey = itemsKeys[i - tagsWords.length];
                tagsArray[i] = ItemCls.getItemTypeChar((int) itemKey[0], (Long) itemKey[1]).toLowerCase();
            } catch (Exception e) {
                LOGGER.error("itemsKeys[" + i + "] = " + itemsKeys[i - tagsWords.length].toString());
                throw (e);
            }
        }
        return tagsArray;
    }

    /**
     * При удалении - транзакция то берется из базы для создания индексов к удалению.
     * И она скелет - нужно базу данных задать и водтянуть номера сущностей и все заново просчитать чтобы правильно удалить метки.
     * Для этого проверку делаем в таблтцк при создании индексов
     *
     * @return
     */
    public String[] getTags() {
        try {
            return tags(viewTypeName(), getTitle(), itemsKeys);
        } catch (Exception e) {
            LOGGER.error(toString() + " - itemsKeys.len: " + itemsKeys.length);
            throw e;
        }
    }

    /*
     * public Long getReference() { return this.reference; }
     */

    public byte getFeePow() {
        return this.feePow;
    }

    public long getAssetKey() {
        return 0l;
    }

    public AssetCls getAsset() {
        return null;
    }

    public byte[] getSignature() {
        return this.signature;
    }

    public long getReference() {
        return this.reference;
    }

    public List<byte[]> getOtherSignatures() {
        return null;
    }

    /**
     * Постраничный поиск по строке поиска
     *
     * @param offest
     * @param filterStr
     * @param useForge
     * @param pageSize
     * @param fromID
     * @param fillFullPage
     * @return
     */
    public static Tuple3<Long, Long, List<Transaction>> searchTransactions(
            DCSet dcSet, String filterStr, boolean useForge, int pageSize, Long fromID, int offset, boolean fillFullPage) {

        List<Transaction> transactions = new ArrayList<>();

        TransactionFinalMapImpl map = dcSet.getTransactionFinalMap();

        if (filterStr != null && !filterStr.isEmpty()) {
            if (Base58.isExtraSymbols(filterStr)) {
                try {
                    Long dbRef = parseDBRef(filterStr);
                    if (dbRef != null) {
                        Transaction one = map.get(dbRef);
                        if (one != null) {
                            transactions.add(one);
                        }
                    }
                } catch (Exception e1) {
                }

            } else {
                try {
                    byte[] signature = Base58.decode(filterStr);
                    Transaction one = map.get(signature);
                    if (one != null) {
                        transactions.add(one);
                    }
                } catch (Exception e2) {
                }
            }
        }

        if (filterStr == null) {
            transactions = map.getTransactionsFromID(fromID, offset, pageSize, !useForge, fillFullPage);
        } else {
            transactions.addAll(map.getTransactionsByTitleFromID(filterStr, fromID,
                    offset, pageSize, fillFullPage));
        }

        if (transactions.isEmpty()) {
            // возможно вниз вышли за границу
            return new Tuple3<>(fromID, null, transactions);
        } else {
            return new Tuple3<>(
                    // включим ссылки на листание вверх
                    transactions.get(0).dbRef,
                    // это не самый конец - включим листание вниз
                    transactions.get(transactions.size() - 1).dbRef,
                    transactions);
        }

    }


    /**
     * Общий для всех проверка на допуск публичного сообщения
     *
     * @param title
     * @param message
     * @param isText
     * @param isEncrypted
     * @return
     */
    public static boolean hasPublicText(String title, byte[] message, boolean isText, boolean isEncrypted) {
        String[] words = title.split(Transaction.SPLIT_CHARS);
        int length = 0;
        for (String word : words) {
            word = word.trim();
            if (Base58.isExtraSymbols(word)) {
                // все слова сложим по длинне
                length += word.length();
                if (length > (BlockChain.TEST_MODE ? 100 : 100))
                    return true;
            }
        }

        if (message == null || message.length == 0)
            return false;

        if (isText && !isEncrypted) {
            String text = new String(message, StandardCharsets.UTF_8);
            if (text.contains(" ") || text.contains("_"))
                return true;
        }
        return false;
    }

    public abstract boolean hasPublicText();

    public int getJobLevel() {
        return 0;
    }

    public int calcCommonFee() {

        int len = this.getDataLength(Transaction.FOR_NETWORK, true);

        /*
        int anonimous = 0;
        if (anonimous > 0) {
            len *= anonimous;
        }

        int minLen = getJobLevel();
        if (this.height < BlockChain.VERS_4_11 && BlockChain.VERS_4_11_USE_OLD_FEE)
            return len * BlockChain.FEE_PER_BYTE_4_10;

        if (len < minLen)
            len = minLen;
        */

        return len * BlockChain.FEE_PER_BYTE;

    }

    // get fee
    public long calcBaseFee() {
        return calcCommonFee();
    }

    // calc FEE by recommended and feePOW
    public void calcFee() {

        long fee_long = calcBaseFee();
        BigDecimal fee = new BigDecimal(fee_long).multiply(BlockChain.FEE_RATE).setScale(BlockChain.FEE_SCALE, BigDecimal.ROUND_UP);

        if (this.feePow > 0) {
            this.fee = fee.multiply(new BigDecimal(BlockChain.FEE_POW_BASE).pow(this.feePow)).setScale(BlockChain.FEE_SCALE, BigDecimal.ROUND_UP);
        } else {
            this.fee = fee;
        }
    }

    // GET forged FEE without invited FEE
    public long getForgedFee() {
        long fee = this.fee.unscaledValue().longValue();
        long fee_invited = this.getInvitedFee();
        return fee - fee_invited;
    }

    // GET only INVITED FEE
    public long getInvitedFee() {

        if (!BlockChain.REFERAL_BONUS_FOR_PERSON(height)) {
            // SWITCH OFF REFERRAL
            return 0l;
        }

        Tuple4<Long, Integer, Integer, Integer> personDuration = creator.getPersonDuration(this.dcSet);
        if (personDuration == null
                || personDuration.a <= BlockChain.BONUS_STOP_PERSON_KEY) {
            // ANONYMOUS or ME
            return 0l;
        }

        long fee = this.fee.unscaledValue().longValue();

        // Если слишком большая комиссия, то и награду чуток увеличим
        if (fee > BlockChain.BONUS_REFERAL << 4)
            return BlockChain.BONUS_REFERAL << 1;
        else if (fee < BlockChain.BONUS_REFERAL << 1) {
            // стандартно если обычная то половину отправим на подарки
            return fee >> 1;
        }

        // если повышенная то не будем изменять
        return BlockChain.BONUS_REFERAL;
    }

    public BigDecimal feeToBD(int fee) {
        return BigDecimal.valueOf(fee, BlockChain.FEE_SCALE);
    }

    /*
    public Block getBlock(DCSet db) {

        if (this.block != null)
            return block;

        if (this.height <= 0) {
            Long key = db.getTransactionFinalMapSigns().get(this.signature);
            if (key == null)
                return null;

            Tuple2<Integer, Integer> pair = Transaction.parseDBRef(key);
            this.height = pair.a;
        }

        this.block = db.getBlockMap().get(this.height);

        return block;
    }
    */

    public Tuple2<Integer, Integer> getHeightSeqNo() {
        return new Tuple2<Integer, Integer>(this.height, this.seqNo);
    }

    public int getBlockHeight() {

        if (this.height > 0)
            return this.height;

        return -1;
    }

    // get current or last
    public int getBlockHeightByParentOrLast(DCSet dc) {

        if (this.height > 0)
            return this.height;

        return dc.getBlocksHeadsMap().size() + 1;
    }

    public int getSeqNo() {
        return this.seqNo;
    }

    public long getDBRef() {
        return this.dbRef;
    }

    public byte[] getDBRefAsBytes() {
        return Longs.toByteArray(this.dbRef);
    }

    // reference in Map - or as signatire or as BlockHeight + seqNo
    public byte[] getDBRef(DCSet db) {
        if (this.getConfirmations(db) < BlockChain.MAX_ORPHAN) {
            // soft or hard confirmations
            return this.signature;
        }

        int bh = this.getBlockHeight();
        if (bh < 1)
            // not in chain
            return null;

        byte[] ref = Ints.toByteArray(bh);
        Bytes.concat(ref, Ints.toByteArray(this.getSeqNo()));
        return ref;

    }

    // reference in Map - or as signatire or as BlockHeight + seqNo
    public static Long makeDBRef(int height, int seqNo) {

        byte[] ref = Ints.toByteArray(height);
        return Longs.fromByteArray(Bytes.concat(ref, Ints.toByteArray(seqNo)));

    }

    public static Long makeDBRef(Tuple2<Integer, Integer> dbRef) {

        byte[] ref = Ints.toByteArray(dbRef.a);
        return Longs.fromByteArray(Bytes.concat(ref, Ints.toByteArray(dbRef.b)));

    }

    public static Long parseDBRef(String refStr) {
        if (refStr == null)
            return null;

        try {
            String[] strA = refStr.split("\\-");
            int height = Integer.parseInt(strA[0]);
            int seq = Integer.parseInt(strA[1]);
            byte[] ref = Ints.toByteArray(height);
            return Longs.fromByteArray(Bytes.concat(ref, Ints.toByteArray(seq)));
        } catch (Exception e) {
            try {
                return Long.parseLong(refStr);
            } catch (Exception e1) {
            }
        }
        return null;
    }

    public static Tuple2<Integer, Integer> parseDBRef(Long dbRef) {

        byte[] bytes = Longs.toByteArray(dbRef);

        int blockHeight = Ints.fromByteArray(Arrays.copyOfRange(bytes, 0, 4));
        int seqNo = Ints.fromByteArray(Arrays.copyOfRange(bytes, 4, 8));

        return new Tuple2<Integer, Integer>(blockHeight, seqNo);

    }

    public static int parseDBRefHeight(long dbRef) {
        return (int) (dbRef >> 32);
    }

    public boolean addCalculated(Block block, Account creator, long assetKey, BigDecimal amount,
                                 String message) {

        if (block != null && block.txCalculated != null) {
            block.txCalculated.add(new RCalculated(creator, assetKey, amount,
                    message, this.dbRef));
            return true;
        }
        return false;
    }

    ////
    // VIEW
    public String viewType() {
        return Byte.toUnsignedInt(typeBytes[0]) + "." + Byte.toUnsignedInt(typeBytes[1]);
    }

    public String viewTypeName() {
        return TYPE_NAME;
    }

    public String viewProperies() {
        return Byte.toUnsignedInt(typeBytes[2]) + "." + Byte.toUnsignedInt(typeBytes[3]);
    }

    public String viewSubTypeName() {
        return "";
    }

    public String viewFullTypeName() {
        String sub = viewSubTypeName();
        return sub.length() > 0 ? viewTypeName() + ":" + sub : viewTypeName();
    }

    public static String viewDBRef(long dbRef) {

        byte[] bytes = Longs.toByteArray(dbRef);

        int blockHeight = Ints.fromByteArray(Arrays.copyOfRange(bytes, 0, 4));
        int seqNo = Ints.fromByteArray(Arrays.copyOfRange(bytes, 4, 8));

        return blockHeight + "-" + seqNo;

    }

    public static String viewDBRef(int blockHeight, int seqNo) {
        return blockHeight + "-" + seqNo;
    }

    public String viewHeightSeq() {
        return this.height + "-" + this.seqNo;
    }

    public String viewAmount(Account account) {
        return account == null ? "" : viewAmount(account.getAddress());
    }

    public String viewAmount(String address) {
        return "";
    }

    public String viewCreator() {
        return creator == null ? "GENESIS" : creator.getPersonAsString();
    }

    public String viewRecipient() {
        return "";
    }

    /*
     * public String viewReference() { //return
     * reference==null?"null":Base58.encode(reference); return
     * reference==null?"null":"" + reference; }
     */
    public String viewSignature() {
        return signature == null ? "null" : Base58.encode(signature);
    }

    public String viewTimestamp() {
        return timestamp < 1000 ? "null" : DateTimeFormat.timestamptoString(timestamp);
    }

    public int viewSize(int forDeal) {
        return getDataLength(forDeal, true);
    }

    // PARSE/CONVERT

    public String viewFeeLong() {
        return feePow + ":" + this.fee.unscaledValue().longValue();
    }

    public String viewFeeAndFiat() {

        String text = fee.toString();
        if (true) {
            BigDecimal compu_rate = new BigDecimal(Settings.getInstance().getCompuRate());
            AssetCls asset = Controller.getInstance().getAsset(Settings.getInstance().getCompuRateAsset());
            if (asset == null)
                asset = Controller.getInstance().getAsset(840L); // ISO-USD

            if (asset == null)
                asset = Controller.getInstance().getAsset(1L); // ERA

            if (compu_rate.signum() > 0) {
                BigDecimal fee_fiat = fee.multiply(compu_rate).setScale(asset.getScale(), BigDecimal.ROUND_HALF_UP);
                text += " (" + fee_fiat.toString() + asset.getTickerName() + ")";
            }

        } else {
            Fun.Tuple2<BigDecimal, String> compu_rate = Controller.COMPU_RATES.get(Settings.getInstance().getLang());
            if (compu_rate == null) {
                compu_rate = Controller.COMPU_RATES.get("en");
            }
            if (compu_rate != null && compu_rate.a.signum() > 0) {
                BigDecimal fee_fiat = fee.multiply(compu_rate.a).setScale(compu_rate.a.scale(), BigDecimal.ROUND_HALF_UP);
                text += " (" + compu_rate.b + fee_fiat.toString() + ")";
            }
        }

        return text;
    }

    public String viewItemName() {
        return "";
    }

    public String viewAmount() {
        return "";
    }

    @SuppressWarnings("unchecked")
    protected JSONObject getJsonBase() {

        DCSet localDCSet = DCSet.getInstance();

        JSONObject transaction = new JSONObject();

        transaction.put("type", Byte.toUnsignedInt(this.typeBytes[0]));
        transaction.put("record_type", this.viewTypeName());
        transaction.put("confirmations", this.getConfirmations(localDCSet));
        transaction.put("type_name", this.viewTypeName());
        transaction.put("sub_type_name", this.viewSubTypeName());

        int height;
        if (this.creator == null) {
            transaction.put("creator", "genesis");
            transaction.put("signature", "genesis");
            height = 1;
        } else {
            transaction.put("publickey", Base58.encode(this.creator.getPublicKey()));
            transaction.put("creator", this.creator.getAddress());
            transaction.put("signature", this.signature == null ? "null" : Base58.encode(this.signature));
            transaction.put("fee", this.fee.toPlainString());
            transaction.put("timestamp", this.timestamp < 1000 ? "null" : this.timestamp);
            transaction.put("version", Byte.toUnsignedInt(this.typeBytes[1]));
            transaction.put("property1", Byte.toUnsignedInt(this.typeBytes[2]));
            transaction.put("property2", Byte.toUnsignedInt(this.typeBytes[3]));
        }

        if (this.height > 0) {
            transaction.put("height", this.height);
            transaction.put("sequence", this.seqNo);
            transaction.put("seqNo", viewHeightSeq());
            if (isWiped()) {
                transaction.put("wiped", true);
            }
        }

        transaction.put("size", this.viewSize(Transaction.FOR_NETWORK));
        return transaction;
    }

    public JSONObject jsonForExplorerPage(JSONObject langObj) {
        return toJson();
    }

    public abstract JSONObject toJson();

    @SuppressWarnings("unchecked")
    public JSONObject rawToJson() {

        DCSet localDCSet = DCSet.getInstance();
        JSONObject transaction = new JSONObject();

        transaction.put("confirmations", this.getConfirmations(localDCSet));

        int height;
        if (this.creator == null) {
            height = 1;
        } else {
            height = this.getBlockHeight();
            transaction.put("publickey", Base58.encode(this.creator.getPublicKey()));
        }

        if (height > 0) {
            transaction.put("seqNo", this.getSeqNo());
            transaction.put("height", height);
        }

        boolean isSigned = this.signature != null;
        transaction.put("signature", isSigned ? Base58.encode(this.signature) : "null");

        transaction.put("raw", Base58.encode(this.toBytes(FOR_NETWORK, isSigned)));

        return transaction;
    }

    public void sign(PrivateKeyAccount creator, int forDeal) {

        // use this.reference in any case and for Pack too
        // but not with SIGN
        boolean withSign = false;
        byte[] data = this.toBytes(forDeal, false);
        if (data == null)
            return;

        if (BlockChain.SIDE_MODE) {
            // чтобы из других цепочек не срабатывало
            data = Bytes.concat(data, Controller.getInstance().blockChain.getGenesisBlock().getSignature());
        } else {
            // чтобы из TestNEt не сработало
            int port = BlockChain.NETWORK_PORT;
            data = Bytes.concat(data, Ints.toByteArray(port));
        }

        this.signature = Crypto.getInstance().sign(creator, data);
    }

    /*
     * public boolean isValidated() { for ( byte[] wiped:
     * BlockChain.VALID_RECORDS) { byte[] sign = wiped; if
     * (Arrays.equals(this.signature, sign)) { return true; } } return false; }
     */

    // VALIDATE

    // releaserReference == null - not as pack
    // releaserReference = reference of releaser - as pack
    public byte[] toBytes(int forDeal, boolean withSignature) {

        //boolean asPack = releaserReference != null;

        byte[] data = new byte[0];

        // WRITE TYPE
        data = Bytes.concat(data, this.typeBytes);

        if (forDeal > FOR_MYPACK) {
            // WRITE TIMESTAMP
            byte[] timestampBytes = Longs.toByteArray(this.timestamp);
            timestampBytes = Bytes.ensureCapacity(timestampBytes, TIMESTAMP_LENGTH, 0);
            data = Bytes.concat(data, timestampBytes);
        }

        // WRITE REFERENCE - in any case as Pack or not - NOW it reserved FLAGS
        if (this.reference != null) {
            // NULL in imprints
            byte[] referenceBytes = Longs.toByteArray(this.reference);
            referenceBytes = Bytes.ensureCapacity(referenceBytes, REFERENCE_LENGTH, 0);
            data = Bytes.concat(data, referenceBytes);
        }

        // WRITE CREATOR
        data = Bytes.concat(data, this.creator.getPublicKey());

        if (forDeal > FOR_PACK) {
            // WRITE FEE POWER
            byte[] feePowBytes = new byte[1];
            feePowBytes[0] = this.feePow;
            data = Bytes.concat(data, feePowBytes);
        }

        // SIGNATURE
        if (withSignature)
            data = Bytes.concat(data, this.signature);

        if (forDeal == FOR_DB_RECORD) {
            // WRITE FEE
            byte[] feeBytes = Longs.toByteArray(this.fee.unscaledValue().longValue());
            data = Bytes.concat(data, feeBytes);
        }

        return data;

    }

    public abstract int getDataLength(int forDeal, boolean withSignature);

    // PROCESS/ORPHAN
    public boolean isWiped() {
        if (getType() == CALCULATED_TRANSACTION)
            return false;

        return BlockChain.isWiped(this.signature);
    }

    public boolean isSignatureValid(DCSet dcSet) {

        if (this.signature == null || this.signature.length != Crypto.SIGNATURE_LENGTH
                || Arrays.equals(this.signature, new byte[Crypto.SIGNATURE_LENGTH]))
            return false;

        // validation with reference - not as a pack in toBytes - in any case!
        byte[] data = this.toBytes(FOR_NETWORK, false);
        if (data == null)
            return false;

        int height = getBlockHeightByParentOrLast(dcSet);
        if (height < BlockChain.SKIP_VALID_SIGN_BEFORE) {
            // for skip NOT VALID SIGNs
            for (byte[] valid_item : BlockChain.VALID_SIGN) {
                if (Arrays.equals(signature, valid_item)) {
                    if (dcSet.getTransactionFinalMapSigns().contains(signature))
                        return false;
                    else
                        return true;
                }
            }
        }

        if (BlockChain.SIDE_MODE) {
            // чтобы из других цепочек не срабатывало
            data = Bytes.concat(data, Controller.getInstance().blockChain.getGenesisBlock().getSignature());
        } else {
            // чтобы из TestNEt не сработало
            int port = BlockChain.NETWORK_PORT;
            data = Bytes.concat(data, Ints.toByteArray(port));
        }

        if (!Crypto.getInstance().verify(this.creator.getPublicKey(), this.signature, data)) {
            boolean wrong = true;
            for (byte[] item : BlockChain.DISCREDIR_ADDRESSES) {
                if (Arrays.equals(this.creator.getPublicKey(), item)
                        && height < 200000) {
                    byte[] digest = Crypto.getInstance().digest(data);
                    digest = Bytes.concat(digest, digest);
                    if (Arrays.equals(this.signature, digest)) {
                        wrong = false;
                    }
                    break;
                }
            }

            if (wrong)
                return false;

        }

        return true;
    }

    /*
     *  flags
     *   = 1 - not check fee
     *   = 2 - not check person
     *   = 4 - not check PublicText
     */
    public int isValid(int asDeal, long flags) {

        if (height < BlockChain.ALL_VALID_BEFORE) {
            return VALIDATE_OK;
        }

        // CHECK IF REFERENCE IS OK
        //Long reference = asDeal == null ? this.creator.getLastTimestamp(dcSet) : asDeal;
        if (asDeal > Transaction.FOR_MYPACK && height > BlockChain.ALL_BALANCES_OK_TO) {
            if (BlockChain.CHECK_DOUBLE_SPEND_DEEP < 0) {
                /// вообще не проверяем в тесте
                if (BlockChain.TEST_DB == 0 && timestamp < Controller.getInstance().getBlockChain().getTimestamp(height - 1)) {
                    // тут нет проверок на двойную трату поэтому только в текущем блоке транзакции принимаем
                    if (BlockChain.CHECK_BUGS > 0)
                        LOGGER.debug("diff sec: " + (Controller.getInstance().getBlockChain().getTimestamp(height) - timestamp) / 1000);
                    return INVALID_TIMESTAMP;
                }
            } else if (BlockChain.CHECK_DOUBLE_SPEND_DEEP > 0) {
                if (timestamp < Controller.getInstance().getBlockChain().getTimestamp(height - BlockChain.CHECK_DOUBLE_SPEND_DEEP)) {
                    // тут нет проверок на двойную трату поэтому только в текущем блоке транзакции принимаем
                    if (BlockChain.CHECK_BUGS > 0)
                        LOGGER.debug("diff sec: " + (Controller.getInstance().getBlockChain().getTimestamp(height) - timestamp) / 1000);
                    return INVALID_TIMESTAMP;
                }

            } else {
                long[] reference = this.creator.getLastTimestamp(dcSet);
                if (reference != null && reference[0] >= this.timestamp
                        && height > BlockChain.VERS_4_11
                ) {
                    if (BlockChain.TEST_DB == 0) {
                        if (BlockChain.CHECK_BUGS > 1)
                            LOGGER.debug("INVALID TIME!!! REFERENCE: " + DateTimeFormat.timestamptoString(reference[0])
                                    + "  TX[timestamp]: " + viewTimestamp() + " diff: " + (this.timestamp - reference[0])
                                    + " BLOCK time: " + Controller.getInstance().getBlockChain().getTimestamp(height));
                    }

                    return INVALID_TIMESTAMP;
                }
            }
        }

        // CHECK CREATOR
        if (!Crypto.getInstance().isValidAddress(this.creator.getAddressBytes())) {
            return INVALID_ADDRESS;
        }

        int height = this.getBlockHeightByParentOrLast(dcSet);
        //if (height <= 0 || height > 1000)
        //    return INVALID_TIMESTAMP;

        // CHECK IT AFTER isPERSON ! because in ignored in IssuePerson
        // CHECK IF CREATOR HAS ENOUGH FEE MONEY
        if ((flags & NOT_VALIDATE_FLAG_FEE) == 0l
                && height > BlockChain.ALL_BALANCES_OK_TO
                && this.creator.getBalance(dcSet, FEE_KEY).a.b.compareTo(this.fee) < 0) {
            return NOT_ENOUGH_FEE;
        }

        if ((flags & NOT_VALIDATE_FLAG_PUBLIC_TEXT) == 0L
                && this.hasPublicText()
                && !BlockChain.TRUSTED_ANONYMOUS.contains(this.creator.getAddress())
                && !this.creator.isPerson(dcSet, height)) {
            return CREATOR_NOT_PERSONALIZED;
        }

        if (false &&  // теперь не проверяем так как ключ сделал длинный dbs.rocksDB.TransactionFinalSignsSuitRocksDB.KEY_LEN
                (flags & NOT_VALIDATE_KEY_COLLISION) == 0l
                && BlockChain.CHECK_DOUBLE_SPEND_DEEP == 0
                && !checkedByPool // транзакция не существует в ожидании - иначе там уже проверили
                && this.signature != null
                && this.dcSet.getTransactionFinalMapSigns().contains(this.signature)) {
            // потому что мы ключ урезали до 12 байт - могут быть коллизии
            return KEY_COLLISION;
        }

        if (creatorPerson != null && !creatorPerson.isAlive(this.timestamp)) {
            return ITEM_PERSON_IS_DEAD;
        }

        return VALIDATE_OK;

    }

    public void process_gifts_turn(int level, long fee_gift, Account invitedAccount,
                                   long invitedPersonKey, boolean asOrphan,
                                   List<RCalculated> txCalculated, String message) {

        if (fee_gift <= 0L)
            return;

        String messageLevel;

        // CREATOR is PERSON
        // FIND person
        ItemCls person = this.dcSet.getItemPersonMap().get(invitedPersonKey);
        Long inviteredDBRef = this.dcSet.getTransactionFinalMapSigns().get(person.getReference());

        Transaction issueRecord = this.dcSet.getTransactionFinalMap().get(inviteredDBRef);
        Account issuerAccount = issueRecord.getCreator();
        Tuple4<Long, Integer, Integer, Integer> issuerPersonDuration = issuerAccount.getPersonDuration(this.dcSet);
        long issuerPersonKey;
        if (issuerPersonDuration == null) {
            // в тестовой сети возможно что каждый создает с неудостоверенного
            issuerPersonKey = -1;
        } else {
            issuerPersonKey = issuerPersonDuration.a;
        }

        if (issuerPersonKey < 0 // это возможно только для певой персоны и то если не она сама себя зарегала и в ДЕВЕЛОПЕ так что пусть там и будет
                || issuerPersonKey == invitedPersonKey // это возможно только в ДЕВЕЛОПЕ так что пусть там и будет
                || issuerPersonKey <= BlockChain.BONUS_STOP_PERSON_KEY
        ) {
            // break loop
            BigDecimal giftBG = BigDecimal.valueOf(fee_gift, BlockChain.FEE_SCALE);
            invitedAccount.changeBalance(this.dcSet, asOrphan, false, FEE_KEY, giftBG, false, true);
            // учтем что получили бонусы
            invitedAccount.changeCOMPUBonusBalances(dcSet, asOrphan, giftBG, Transaction.BALANCE_SIDE_DEBIT);

            if (txCalculated != null && !asOrphan) {
                messageLevel = message + " top level";
                txCalculated.add(new RCalculated(invitedAccount, FEE_KEY, giftBG,
                        messageLevel, this.dbRef));

            }
            return;
        }

        // IS INVITER ALIVE ???
        PersonCls issuer = (PersonCls) this.dcSet.getItemPersonMap().get(issuerPersonKey);
        if (!issuer.isAlive(this.timestamp)) {
            // SKIP this LEVEL for DEAD persons
            process_gifts_turn(level, fee_gift, issuerAccount, issuerPersonKey, asOrphan, txCalculated, message);
            return;
        }

        if (level > 1) {

            long fee_gift_next = fee_gift >> BlockChain.FEE_INVITED_SHIFT_IN_LEVEL;
            long fee_gift_get = fee_gift - fee_gift_next;

            BigDecimal giftBG = BigDecimal.valueOf(fee_gift_get, BlockChain.FEE_SCALE);
            issuerAccount.changeBalance(this.dcSet, asOrphan, false, FEE_KEY, giftBG, false, true);

            // учтем что получили бонусы
            issuerAccount.changeCOMPUBonusBalances(dcSet, asOrphan, giftBG, Transaction.BALANCE_SIDE_DEBIT);

            if (txCalculated != null && !asOrphan) {
                messageLevel = message + " @P:" + invitedPersonKey + " level." + (1 + BlockChain.FEE_INVITED_DEEP - level);
                txCalculated.add(new RCalculated(issuerAccount, FEE_KEY, giftBG,
                        messageLevel, this.dbRef));
            }

            if (fee_gift_next > 0) {
                process_gifts_turn(--level, fee_gift_next, issuerAccount, issuerPersonKey, asOrphan, txCalculated, message);
            }

        } else {
            // this is END LEVEL
            // GET REST of GIFT
            BigDecimal giftBG = BigDecimal.valueOf(fee_gift, BlockChain.FEE_SCALE);
            issuerAccount.changeBalance(this.dcSet, asOrphan, false, FEE_KEY,
                    BigDecimal.valueOf(fee_gift, BlockChain.FEE_SCALE), false, true);

            // учтем что получили бонусы
            issuerAccount.changeCOMPUBonusBalances(dcSet, asOrphan, giftBG, Transaction.BALANCE_SIDE_DEBIT);

            if (txCalculated != null && !asOrphan) {
                messageLevel = message + " @P:" + invitedPersonKey + " level." + (1 + BlockChain.FEE_INVITED_DEEP - level);
                txCalculated.add(new RCalculated(issuerAccount, FEE_KEY, giftBG,
                        messageLevel, this.dbRef));
            }
        }
    }


    public void process_gifts(int level, long fee_gift, Account creator, boolean asOrphan,
                              List<RCalculated> txCalculated, String message) {

        if (fee_gift <= 0l)
            return;

        Tuple4<Long, Integer, Integer, Integer> personDuration = creator.getPersonDuration(this.dcSet);
        if (personDuration == null
                || personDuration.a <= BlockChain.BONUS_STOP_PERSON_KEY) {

            // если рефералку никому не отдавать то она по сути исчезает - надо это отразить в общем балансе
            GenesisBlock.CREATOR.changeBalance(this.dcSet, !asOrphan, false, FEE_KEY,
                    BigDecimal.valueOf(fee_gift, BlockChain.FEE_SCALE), true, false);

            return;
        }

        process_gifts_turn(level, fee_gift, creator, personDuration.a, asOrphan, txCalculated, message);

    }

    // REST

    // public abstract void process(DLSet db);
    public void process(Block block, int asDeal) {

        if (false
            //this.signature != null && Base58.encode(this.signature)
            //.equals("nQhYYc4tSM2sPLpiceCWGKhdt5MKhu82LrTM9hCKgh3iyQzUiZ8H7s4niZrgy4LR4Zav1zXD7kra4YWRd3Fstd")
        ) {
            int error = 0;
            error++;
        }

        if (asDeal > Transaction.FOR_PACK) {
            // this.calcFee();

            if (this.fee != null && this.fee.compareTo(BigDecimal.ZERO) != 0) {
                // NOT update INCOME balance
                this.creator.changeBalance(this.dcSet, true, false, FEE_KEY, this.fee, true, true);
                // учтем траты
                this.creator.changeCOMPUBonusBalances(this.dcSet, true, this.fee, BALANCE_SIDE_CREDIT);
            }

            // Multi Level Referal
            if (BlockChain.FEE_INVITED_DEEP > 0) {
                long invitedFee = getInvitedFee();
                if (invitedFee > 0) {
                    if (BlockChain.CHECK_BUGS > 3 && height == 3104) {
                        boolean debug = true;
                    }
                    process_gifts(BlockChain.FEE_INVITED_DEEP, invitedFee, this.creator, false,
                            block != null && block.txCalculated != null ?
                                    block.txCalculated : null, "Referal bonus " + "@" + this.viewHeightSeq());
                }
            }

            // UPDATE REFERENCE OF SENDER
            this.creator.setLastTimestamp(new long[]{this.timestamp, dbRef}, this.dcSet);
        }

    }

    public void orphan(Block block, int asDeal) {

        if (false && BlockChain.CHECK_BUGS > 1
            ///&& viewHeightSeq().equals("628853-1") // is forging 628853-1
            //Base58.encode(this.signature)
            //.equals("nQhYYc4tSM2sPLpiceCWGKhdt5MKhu82LrTM9hCKgh3iyQzUiZ8H7s4niZrgy4LR4Zav1zXD7kra4YWRd3Fstd")
        ) {
            int error = 0;
            error++;
        }

        if (asDeal > Transaction.FOR_PACK) {
            if (this.fee != null && this.fee.compareTo(BigDecimal.ZERO) != 0) {
                // NOT update INCOME balance
                this.creator.changeBalance(this.dcSet, false, false, FEE_KEY, this.fee, true, true);
                // учтем траты
                this.creator.changeCOMPUBonusBalances(this.dcSet, false, this.fee, BALANCE_SIDE_CREDIT);

            }

            // calc INVITED FEE
            if (BlockChain.FEE_INVITED_DEEP > 0) {
                long invitedFee = getInvitedFee();
                if (invitedFee > 0)
                    process_gifts(BlockChain.FEE_INVITED_DEEP, invitedFee, this.creator, true,
                            null, null);
            }

            // UPDATE REFERENCE OF SENDER
            // set last transaction signature for this ACCOUNT
            this.creator.removeLastTimestamp(this.dcSet, timestamp);

        }

        // CLEAR all FOOTPRINTS and empty data
        this.dcSet.getVouchRecordMap().delete(dbRef);

    }

    public Transaction copy() {
        try {
            return TransactionFactory.getInstance().parse(this.toBytes(FOR_NETWORK, true), Transaction.FOR_NETWORK);
        } catch (Exception e) {
            return null;
        }
    }

    public abstract HashSet<Account> getInvolvedAccounts();

    /*
     * public boolean isConfirmed() { return
     * this.isConfirmed(DLSet.getInstance()); }
     */

    public abstract HashSet<Account> getRecipientAccounts();

    public abstract boolean isInvolved(Account account);

    // TODO перевести все на проверку height
    // Это используется только в ГУУИ поэтому по высоте можно делать точно
    public boolean isConfirmed(DCSet db) {
        if (height > 0)
            return true;

        if (this.getType() == Transaction.CALCULATED_TRANSACTION) {
            // USE referenced transaction
            return db.getTransactionFinalMap().contains(this.reference);
        }

        return db.getTransactionFinalMapSigns().contains(this.getSignature());
    }

    public int getConfirmations(int chainHeight) {

        if (this.height == 0)
            return 0;

        return 1 + chainHeight - this.height;
    }

    public int getConfirmations(DCSet db) {

        // CHECK IF IN UNCONFIRMED TRANSACTION

        if (this.height == 0)
            return 0;

        return 1 + db.getBlockMap().size() - this.height;

    }

    /**
     * ОЧЕНЬ ВАЖНО чтобы Finalizer мог спокойно удалять их и DCSet.fork
     * иначе Финализер не можеи зацикленные сслки порвать и не очищает HEAP.
     * Возможно можно еще освободить объекты
     */
    public void resetDCSet() {
        dcSet = null;
        itemsKeys = null;
    }

    // ПРОЫЕРЯЛОСЬ! действует в совокупк с Финализе в Блоке
    @Override
    protected void finalize() throws Throwable {
        dcSet = null;
        super.finalize();
    }

    @Override
    public String toString() {
        if (signature == null) {
            return getClass().getName() + ":" + viewFullTypeName();
        }
        return getClass().getName() + ":" + viewFullTypeName() + Base58.encode(signature);
    }

}
