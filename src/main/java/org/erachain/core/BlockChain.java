package org.erachain.core;

import com.google.common.primitives.Longs;
import org.erachain.controller.Controller;
import org.erachain.core.account.Account;
import org.erachain.core.account.PrivateKeyAccount;
import org.erachain.core.account.PublicKeyAccount;
import org.erachain.core.block.Block;
import org.erachain.core.block.GenesisBlock;
import org.erachain.core.crypto.Base58;
import org.erachain.core.transaction.ArbitraryTransaction;
import org.erachain.core.transaction.Transaction;
import org.erachain.datachain.BlocksHeadsMap;
import org.erachain.datachain.DCSet;
import org.erachain.network.Peer;
import org.erachain.ntp.NTP;
import org.erachain.settings.Settings;
import org.erachain.utils.Pair;
import org.mapdb.DB;
import org.mapdb.Fun.Tuple2;
import org.mapdb.Fun.Tuple3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.*;

/**
 * для обработки цепочки блоков. Запоминает в себе генесиз-блок и базу данных.
 * Поидее именно тут должен быть метод FORK а не в базе данных - и отпочковывание новой цепочки.
 * А блоки должны добавляться в цепочку а не в базу данных напрямую. blockChain.add(BLOCK)
 */
public class BlockChain {

    public static final int TESTS_VERS = 0; // not use TESTs - or a11 (as version)

    /**
     * Задает потолок цепочки
     */
    public static final int BLOCK_COUNT = 0;
    /**
     * DATABASE benchmark test. use start options:<br>
     * erachain.jar -pass=1 -seed=5:new:1 -nogui -opi -nodatawallet -nocalculated -hardwork=[0..10] -dbschain=[rocksdb|mapdb] <br>
     * сколько транзакции в блоке - если больше 0 то запускает тест на старте
     */
    public static final int TEST_DB = 0000;
    // запрет сборки своих блоков в ТЕСТЕ
    public static final boolean STOP_GENERATE_BLOCKS = false;

    /**
     * для каждого счета по времени создания транзакции сохраняется ссылка на транзакцию,
     * что требует создания длинных ключей 20 + 8. Это используется при откатах для восстановления последего значения
     */
    public static final boolean NOT_STORE_REFFS_HISTORY = TEST_DB > 0;

    /**
     * для каждого счета сохраняется последнее время транзакции и потом проверяется на повторы.
     * 0 - все сохранять в базе, N - создать в памяти стек размером N для числа блоков последних
     * и при этом ограничиваем время жизни неподтвержденных на N блоков,
     * -1 - не проверяем вообще - возможно стоит уже запрет на транзакции с одного счета в одном блоке,
     * и при этом разрешены транзакции только по времени за 1 блок.
     * Вдобавок если != 0 то проверки на коллизию ключа (подписи) в TransactionFinalMapSigns не проверяется,
     * что ускоряет работу но воявляется вероятность колллизии - поэтому можно там увеличить длинну ключа если тут != 0
     * ! Вдобавок нужно понимать что если мы проверяем по времени трнзакции то 100% они уже будут иметь уникальные подписи
     * и проверять на уникальность их в Финал не нужно (если нет слишком большой обрезки ключа, see Transaction.KEY_COLLISION)
     */
    public static final int CHECK_DOUBLE_SPEND_DEEP = TEST_DB > 0 ? -1 : 0;

    /**
     * Число счетов для теста базы данных - чем больше тем болше нагрузка и сложнее считать.
     * Если меньше чем TEST_DB то улучшается скорость за счет схлопыания повторнных изменений балансов счетов.
     */
    public static PrivateKeyAccount[] TEST_DB_ACCOUNTS = TEST_DB == 0 ? null : new PrivateKeyAccount[1000];
    public static final boolean NOT_CHECK_SIGNS = TEST_DB > 0 && false;

    static public int CHECK_BUGS = TEST_DB > 0 ? 0 : 5;

    /**
     * если задан - первое подключение к нему
     */
    public static final byte[] START_PEER = null; //new byte[]{(byte)138, (byte)197, (byte)135, (byte)122};

    public static final boolean PERSON_SEND_PROTECT = true;
    //public static final int BLOCK_COUNT = 10000; // max count Block (if =<0 to the moon)

    public static final boolean DEMO_MODE = Settings.getInstance().isDemoNet();
    public static final boolean TEST_MODE = Settings.getInstance().isTestNet();

    /**
     * set uo all balances ERA to 10000 and COMPU to 100
     */
    public static final boolean ERA_COMPU_ALL_UP = TEST_MODE || TEST_DB > 0;

    public static final int TESTNET_PORT = TEST_DB > 0 ? 9006 : DEMO_MODE ? 9066 : 9065; // TESTNET - 95
    public static final int MAINNET_PORT = TEST_DB > 0 ? 9006 : 9046;

    public static final int DEFAULT_WEB_PORT = TEST_DB > 0 ? 9007 : TEST_MODE ? 9067 : 9047;
    public static final int DEFAULT_RPC_PORT = TEST_DB > 0 ? 9008 : TEST_MODE ? 9068 : 9048;

    public static final String DEFAULT_EXPLORER = "explorer.erachain.org";

    //public static final String TIME_ZONE = "GMT+3";
    //
    public static final boolean ROBINHOOD_USE = false;
    public static final boolean ANONIM_SERT_USE = TEST_MODE || BlockChain.ERA_COMPU_ALL_UP ? true : false;

    public static final int MAX_ORPHAN = 10000; // max orphan blocks in chain for 30 sec
    public static final int SYNCHRONIZE_PACKET = 300; // when synchronize - get blocks packet by transactions
    public static final int TARGET_COUNT_SHIFT = 10;
    public static final int TARGET_COUNT = 1 << TARGET_COUNT_SHIFT;
    public static final int BASE_TARGET = 100000;///1 << 15;
    public static final int REPEAT_WIN = DEMO_MODE ? 10 : TEST_MODE ? 5 : ERA_COMPU_ALL_UP ? 15 : 40; // GENESIS START TOP ACCOUNTS

    // RIGHTs
    public static final int GENESIS_ERA_TOTAL = 10000000;
    public static final int GENERAL_ERA_BALANCE = GENESIS_ERA_TOTAL / 100;
    public static final int MAJOR_ERA_BALANCE = 33000;
    public static final int MINOR_ERA_BALANCE = 1000;
    public static final int MIN_GENERATING_BALANCE = 100;
    public static final int MIN_REGISTRATING_BALANCE = 10;
    public static final BigDecimal MIN_GENERATING_BALANCE_BD = new BigDecimal(MIN_GENERATING_BALANCE);
    public static final BigDecimal MIN_REGISTRATING_BALANCE_BD = new BigDecimal(MIN_REGISTRATING_BALANCE);
    //public static final int GENERATING_RETARGET = 10;
    //public static final int GENERATING_MIN_BLOCK_TIME = DEVELOP_USE ? 120 : 288; // 300 PER DAY
    //public static final int GENERATING_MIN_BLOCK_TIME_MS = GENERATING_MIN_BLOCK_TIME * 1000;
    public static final int WIN_BLOCK_BROADCAST_WAIT_MS = 10000; //
    // задержка на включение в блок для хорошей сортировки

    /**
     * проверить цепочку по силе у соседей. Если поставить меньше 2 то будет проверять каждый блок, что иногда плохо
     * Наверно оптимально 2-4 блока. Так же было замечено что если 2 узла всего тоони войдя в режим проверки
     * начинали поочереди откатывать свои цепочки до бесконечности - то есть нельзя чтобы каждый блок это смотрелось
     */
    public static final int CHECK_PEERS_WEIGHT_AFTER_BLOCKS = 3;
    // хранить неподтвержденные долше чем то время когда мы делаем обзор цепочки по силе
    public static final int ON_CONNECT_SEND_UNCONFIRMED_NEED_COUNT = 10;

    //public static final int GENERATING_MAX_BLOCK_TIME = 1000;
    public static final int MAX_BLOCK_SIZE_BYTES = 1 << 25; //4 * 1048576;
    public static final int MAX_BLOCK_SIZE = MAX_BLOCK_SIZE_BYTES >> 8;
    public static final int MAX_REC_DATA_BYTES = MAX_BLOCK_SIZE_BYTES >> 2;

    // переопределим размеры по HARD
    static private final int MAX_BLOCK_SIZE_GEN_TEMP = MAX_BLOCK_SIZE_BYTES / 100 * (10 * Controller.HARD_WORK + 10) ;
    public static final int MAX_BLOCK_SIZE_BYTES_GEN = TEST_DB > 0 ? TEST_DB << 9
            : MAX_BLOCK_SIZE_GEN_TEMP > MAX_BLOCK_SIZE_BYTES ? MAX_BLOCK_SIZE_BYTES : MAX_BLOCK_SIZE_GEN_TEMP;
    public static final int MAX_BLOCK_SIZE_GEN = TEST_DB > 0 ? TEST_DB << 1 : MAX_BLOCK_SIZE_BYTES_GEN >> 8;

    public static final int MAX_UNCONFIGMED_MAP_SIZE = MAX_BLOCK_SIZE_GEN << 2;
    public static final int ON_CONNECT_SEND_UNCONFIRMED_UNTIL = MAX_UNCONFIGMED_MAP_SIZE;

    public static final int GENESIS_WIN_VALUE = TEST_MODE ? 3000 : ERA_COMPU_ALL_UP ? 10000 : 22000;

    public static final String[] GENESIS_ADMINS = new String[]{"78JFPWVVAVP3WW7S8HPgSkt24QF2vsGiS5",
            "7B3gTXXKB226bxTxEHi8cJNfnjSbuuDoMC"};

    public static final int VERS_4_11 = TEST_DB > 0 ? 0 : TEST_MODE ? 0 : 194400;

    //public static final int ORDER_FEE_DOWN = VERS_4_11;
    public static final int HOLD_VALID_START = TESTS_VERS > 0? 0 : VERS_4_11;

    public static final int ALL_BALANCES_OK_TO = TESTS_VERS > 0 ? 0 : TEST_MODE ? 0 : 623904;
    public static final int CANCEL_ORDERS_ALL_VALID = TEST_DB > 0 ? 0 : ALL_BALANCES_OK_TO; //260120;
    /**
     * Включает обработку заявок на бирже по цене рассчитанной по остаткам
     */
    public static final int LEFT_PRICE_HEIGHT = TEST_DB > 0 ? 0 : CANCEL_ORDERS_ALL_VALID;
    /**
     * {@link LEFT_PRICE_HEIGHT} as SeqNo
     */
    public static final long LEFT_PRICE_HEIGHT_SEQ = TEST_DB > 0 ? 0 : Transaction.makeDBRef(LEFT_PRICE_HEIGHT, 0);

    public static final int SKIP_VALID_SIGN_BEFORE = TEST_DB > 0 ? 0 : TEST_MODE ? 0 : 44666;

    public static final int VERS_4_12 = TEST_DB > 0 ? 0 : VERS_4_11;

    public static final int VERS_30SEC = TEST_DB > 0 ? 0 : TEST_MODE ? 0 : 280785; //	2019-09-17 12:01:13

    //public static final long VERS_30SEC_TIME = Settings.DEFAULT_MAINNET_STAMP + (long) VERS_30SEC
    //        * (DEVELOP_USE ? 120L : TEST_MODE? 30L : 288L);

    // TODO поидее отрицательное тоже работать будет как надо
    public static final long VERS_30SEC_TIME =
            TEST_MODE ? 0 : Settings.DEFAULT_MAINNET_STAMP + (long) VERS_30SEC * 288L;

    public static final int VERS_4_21_02 = 684000;

    /**
     * Включает реферальную систему
     */
    public static final int REFERAL_BONUS_FOR_PERSON_4_21 = TEST_MODE ? 0 : Integer.MAX_VALUE;

    /**
     * Включает новые права на выпуск персон и на удостоверение публичных ключей и увеличение Бонуса персоне
     */
    public static final int START_ISSUE_RIGHTS = TEST_DB > 0 ? 0 : TEST_MODE ? 0 : Integer.MAX_VALUE; ///VERS_4_21_02;
    public static final int DEFAULT_DURATION = 365 * 5; // 5 years

    public static final int DEVELOP_FORGING_START = 100;

    public HashSet<String> trustedPeers = new HashSet<>();

    public static final HashSet<Integer> validBlocks = new HashSet<>();

    /**
     * Записи которые удалены
     */
    public static final HashSet<Long> WIPED_RECORDS =  new HashSet<>();

    /*
     *  SEE in concrete TRANSACTIONS
     * public static final byte[][] VALID_RECORDS = new byte[][]{
     * };
     */

    public static final byte[][] VALID_ADDRESSES = TEST_DB > 0? new byte[][]{} : new byte[][]{
            Base58.decode("1A3P7u56G4NgYfsWMms1BuctZfnCeqrYk3")
    };

    public static final byte[][] DISCREDIR_ADDRESSES = TEST_DB > 0? new byte[][]{} : new byte[][]{
            Base58.decode("HPftF6gmSH3mn9dKSAwSEoaxW2Lb6SVoguhKyHXbyjr7"),
            Base58.decode("AoPMZ3Q8u5q2g9aK8JZSQRnb6iS53FjUjrtT8hCfHg9F") // 7DedW8f87pSDiRnDArq381DNn1FsTBa68Y")
    };
    public static final byte[][] VALID_SIGN = TEST_DB > 0? new byte[][]{} : new byte[][]{
            Base58.decode("5DnTfBxw2y8fDshzkdqppB24y5P98vnc873z4cofQZ31JskfJbnpRPjU5uZMQwfSYJYkJZzMxMYq6EeNCys18sEq"),
            Base58.decode("4CqzJSD9j4GNGcYVtNvMic98Zq9aQALLdkFkuXMLGnGqUTgdHqHcoSU7wJ24wvaAAukg2g1Kw1SA6UFQo7h3VasN"),
            Base58.decode("E4pUUdCqQt6HWCJ1pUeEtCDngow7pEJjyRtLZTLEDWFEFwicvxVXAgJbUPyASueZVUobZ28xtX6ZgDLb5cxeXy2"),
            Base58.decode("4UPo6sAF63fkqhgkAXh94tcF43XYz8d7f6PqBGSX13eo3UWCENptrk72qkLxtXYEEsHs1wS2eH6VnZEVctnPdUkb"),
            Base58.decode("3aYMNRUVYxVaozihhGkxU8JTeAFT73Ua7JDqUfvrDUpDNPs7mS4pxHUaaDiZsGYi91fK5c2yVLWVQW9tqqDGCK2a"),
            Base58.decode("KhVG9kHf4nttWSEvM6Rd99wuTtRtFAQvSwwo9ae4WFJ2fWaidY4jF33WTRDvYEtaWWu2cmh6x4tEX7ded6QDSGt"),
            Base58.decode("2KW1mywfP99GcEo5hV8mdHxgPDkFJj5ABcVjNa7vQd1GPC13HRqBERUPKfLZ5HrQ3Dyp42u8PWrzBKUP3cUHG3N4"),
            Base58.decode("2SsSCv8EuMnZrGYq4jFhvJ3gRdbdEU92Unp6u4JNwrw4D7SHHaRpH2b9VuLtTA3zuUVx1EqTB5wJQWxeuJbwxYvs"),
            Base58.decode("4iM1HvHgSV3WTXJ3M4XVMZ4AcfrDaA3bdyFmZcX5BJJkacNTjVURWuhp2gLyhxCJok7eAHkd94nM4q3VcDAc2zCJ"),
            Base58.decode("3THvTzHcyEDPGisprZAN955RMEhye84ygnBMPxrFRT6bCocQ84xt1jaSaNyD9px9dxq3zCNbebXnmL251JZhfCHm"),
            Base58.decode("M7jNQ8w2fCjD3Mvo8cH2G5JYFTnGfLYSQv7xCCso7BsmMKJ7Ruc3pnr1rbpFwVrBkQG3auB5SGCmoWbCq9pw8hU"),
            Base58.decode("m1ryu4QMHLaoALYwx35ugNtQec1QAS1KZe8kkx8bQ8UKcesGGbCbqRYhJrtrPDy3gsxVp4hTQGr7qY3NsndBebr"),
            Base58.decode("3Lzamim6R4khdsVfpdsCzyuhqbguCM6yQTyJPJmvPC7agsaBk7UhYuRxZ8tduLpRhZEMpJwAVd5ucRAiXY8cX6ZE"),
            Base58.decode("44chQvtt3NKgRjphBwKTgRfz4rD7YvpHs4k17w1Xvt6drmjBwJWXsFXBfHV97LbMx4kMkzpHCXgN7mNjDUZeTL6M"),
            Base58.decode("xckfcdNWJN1uoGGTe5nXg5JmGUEyzoJQYkt3bUB6vGUGs8p9j8uhVKeYsY5g2sj67w4pz6CcxdhrVFPzGZnkba2"),
            Base58.decode("2x8QSztNRFDKmMjotzfTvbAkDo7s7Uqh9HpyFVQTiDqYpfweV4z1wzcMjn6GtVHszqBZp6ynuUr4JP9PAEBPLtiy"),
            Base58.decode("9UBPJ4XJzRkw7kQAdFvXbEZuroUszFPomH25UAmMkYyTFPfnbyo9qKKTMZffoSjoMHzMssszaTPiFVhxaxEwBrY"),
            Base58.decode("4Vo6hmojFGgAJhfjyiN8PNYktpgrdHGF8Bqe12Pk3PvcvcH8tuJTcTnnCqyGChriHTuZX1u5Qwho8BuBPT4FJ53W")
    };

    public static final byte[][] VALID_BAL = TEST_DB > 0 ? new byte[][]{} : TEST_MODE ? new byte[][]{} :
            new byte[][]{
                    //Base58.decode("5sAJS3HeLQARZJia6Yzh7n18XfDp6msuaw8J5FPA8xZoinW4FtijNru1pcjqGjDqA3aP8HY2MQUxfdvk8GPC5kjh"),
                    //Base58.decode("3K3QXeohM3V8beSBVKSZauSiREGtDoEqNYWLYHxdCREV7bxqE4v2VfBqSh9492dNG7ZiEcwuhhk6Y5EEt16b6sVe"),
                    //Base58.decode("5JP71DmsBQAVTQFUHJ1LJXw4qAHHcoBCzXswN9Ez3H5KDzagtqjpWUU2UNofY2JaSC4qAzaC12ER11kbAFWPpukc"),
                    //Base58.decode("33okYP8EdKkitutgat1PiAnyqJGnnWQHBfV7NyYndk7ZRy6NGogEoQMiuzfwumBTBwZyxchxXj82JaQiQXpFhRcs"),
                    //Base58.decode("23bci9zcrPunGppKCm6hKvfRoAStWv4JV2xe16tBEVZSmkCrhw7bXAFzPvv2jqZJXcbA8cmr8oMUfdmS1HJGab7s"),

                    //Base58.decode("54xdM25ommdxTbAVvP7C9cFYPmwaAexkWHfkhgb8yhfCVvvRNrs166q8maYuXWpk4w9ft2HvctaFaafnKNfjyoKR"),
                    //Base58.decode("61Fzu3PhsQ74EoMKrwwxKHMQi3z9fYAU5UeUfxtGdXPRfKbWdgpBQWgAojEnmDHK2LWUKtsmyqWb4WpCEatthdgK"),
            };

    // DEX precision
    ///public static final int TRADE_PRECISION = 4;
    /**
     * Если после исполнения торговой сделки оостатется статок у ордера-инициатора и
     * цена для остатка отклонится больше чем на эту величину то ему возвращаем остаток
     * see org.erachain.core.item.assets.OrderTestsMy#testOrderProcessingNonDivisible() - 0.0000432
     * Тут точность можно сделать меньше так он либо полностью исполнится либо встанет уже с новой ценой по остатку в стакане
     */
    final public static BigDecimal INITIATOR_PRICE_DIFF_LIMIT = new BigDecimal("0.0005");
    final public static BigDecimal INITIATOR_PRICE_DIFF_LIMIT_NEG = INITIATOR_PRICE_DIFF_LIMIT.multiply(new BigDecimal(5));
    /**
     * Если после исполнения торговой сделки оостатется статок у ордера-цели и
     * цена для остатка отклонится больше чем на эту величину то либо скидываем остаток в эту сделку либо ему возвращаем остаток
     * Тут нужно точность выше чем у Инициатора - так как он может перекрыть цену других встречных ордеров в стакане
     * И по хорошему его нужно пересчитать как Активный если цена полезла не в его сторону
     */
    final public static BigDecimal TARGET_PRICE_DIFF_LIMIT = new BigDecimal("0.00005");
    final public static BigDecimal TARGET_PRICE_DIFF_LIMIT_NEG = TARGET_PRICE_DIFF_LIMIT.multiply(new BigDecimal(5));
    /**
     * Если сыграло INITIATOR_PRICE_DIFF_LIMIT и цена сделки после скидывания в нее остатка ордера-цели не выйдет за это ограничени то скидываем в сделку.
     * Инача отдаем обратно
     */
    ///final public static BigDecimal TRADE_PRICE_DIFF_LIMIT = new BigDecimal("2.0").scaleByPowerOfTen(-(BlockChain.TRADE_PRECISION - 1));
    final public static BigDecimal TRADE_PRICE_DIFF_LIMIT = new BigDecimal("0.001");


    public static final int ITEM_POLL_FROM = TEST_DB > 0 ? 0 : TEST_MODE ? 0 : VERS_4_11;

    public static final int AMOUNT_SCALE_FROM = TEST_DB > 0 ? 0 : TEST_MODE ? 0 : 1033;
    public static final int AMOUNT_DEDAULT_SCALE = 8;
    public static final int FREEZE_FROM = TEST_DB > 0 ? 0 : TEST_MODE ? 0 : 249222;
    // только на них можно замороженные средства вернуть из списка FOUNDATION_ADDRESSES (там же и замароженные из-за утраты)
    public static final String[] TRUE_ADDRESSES = TEST_DB > 0 ? new String[]{} : new String[]{
            "7R2WUFaS7DF2As6NKz13Pgn9ij4sFw6ymZ"
            //"78JFPWVVAVP3WW7S8HPgSkt24QF2vsGiS5",
            // "7S8qgSTdzDiBmyw7j3xgvXbVWdKSJVFyZv",
    };
    // CHAIN
    public static final int CONFIRMS_HARD = 3; // for reference by signature
    // MAX orphan CHAIN
    public static final int CONFIRMS_TRUE = MAX_ORPHAN; // for reference by ITEM_KEY
    //public static final int FEE_MIN_BYTES = 200;
    public static final int FEE_PER_BYTE_4_10 = 64;
    public static final int FEE_PER_BYTE = 100;
    public static final int FEE_SCALE = 8;
    public static final BigDecimal FEE_RATE = BigDecimal.valueOf(1, FEE_SCALE);
    //public static final BigDecimal MIN_FEE_IN_BLOCK_4_10 = BigDecimal.valueOf(FEE_PER_BYTE_4_10 * 8 * 128, FEE_SCALE);
    public static final BigDecimal MIN_FEE_IN_BLOCK_4_10 = BigDecimal.valueOf(50000, FEE_SCALE);
    public static final BigDecimal MIN_FEE_IN_BLOCK = BigDecimal.valueOf(FEE_PER_BYTE * 6 * 128, FEE_SCALE);
    public static final float FEE_POW_BASE = (float) 1.5;
    public static final int FEE_POW_MAX = 6;
    public static final int FINANCIAL_FEE_LEVEL = 100;
    public static final int ISSUE_MULT_FEE = 1 << 10;
    public static final int ISSUE_ASSET_MULT_FEE = 1 << 8;
    public static final int TEST_FEE_ORPHAN = 0; //157000;

    public static final int FEE_FOR_ANONIMOUSE = 33;
    //
    public static final boolean VERS_4_11_USE_OLD_FEE = false;


    /**
     * Multi-level Referal Sysytem. Levels for deep
     */
    public static final int FEE_INVITED_DEEP = TEST_DB > 0 ? 0 : 3;
    /**
     * Stop referals system on this person Number. Причем рефералка которая должна упать этим персонам
     * (с номером ниже заданного) по сути просто сжигается - то есть идет дефляция.
     */
    public static final long BONUS_STOP_PERSON_KEY = TEST_MODE ? 0 : 13L;

    public static final int FEE_INVITED_SHIFT = 1;
    /**
     * Постаянная награда за байт трнзакции
     */
    public static final int BONUS_REFERAL = 200 * FEE_PER_BYTE;
    /**
     * Какую долю отдавать на уровень ниже - как степерь двойки. 1 - половину, 2 - четверть, 3 - восьмую часть
     */
    public static final int FEE_INVITED_SHIFT_IN_LEVEL = 1;

    // SERTIFY
    // need RIGHTS for non PERSON account
    public static final BigDecimal MAJOR_ERA_BALANCE_BD = BigDecimal.valueOf(MAJOR_ERA_BALANCE);
    // need RIGHTS for PERSON account
    public static final BigDecimal MINOR_ERA_BALANCE_BD = BigDecimal.valueOf(MINOR_ERA_BALANCE);

    // GIFTS for RSertifyPubKeys
    public static final int GIFTED_COMPU_AMOUNT_4_10 = FEE_PER_BYTE_4_10 << 8;
    public static final BigDecimal GIFTED_COMPU_AMOUNT_BD_4_10 = BigDecimal.valueOf(GIFTED_COMPU_AMOUNT_4_10, FEE_SCALE);
    public static final int GIFTED_COMPU_AMOUNT_FOR_PERSON_4_10 = GIFTED_COMPU_AMOUNT_4_10 << 3;
    public static final BigDecimal GIFTED_COMPU_AMOUNT_FOR_PERSON_BD_4_10 = BigDecimal.valueOf(GIFTED_COMPU_AMOUNT_FOR_PERSON_4_10, FEE_SCALE);

    public static final int GIFTED_COMPU_AMOUNT = 50000; // FEE_PER_BYTE << 8;
    public static final BigDecimal GIFTED_COMPU_AMOUNT_BD = BigDecimal.valueOf(GIFTED_COMPU_AMOUNT, FEE_SCALE);
    public static final int GIFTED_COMPU_AMOUNT_FOR_PERSON = 250000; //GIFTED_COMPU_AMOUNT << 7;
    public static final BigDecimal GIFTED_COMPU_AMOUNT_FOR_PERSON_BD = BigDecimal.valueOf(GIFTED_COMPU_AMOUNT_FOR_PERSON, FEE_SCALE);

    public static final Tuple2<Integer, byte[]> CHECKPOINT = new Tuple2<Integer, byte[]>(
            TEST_MODE ? 0 : 235267,
            Base58.decode(
                    TEST_MODE ? ""
                            : "2VTp79BBpK5E4aZYV5Tk3dYRS887W1devsrnyJeN6WTBQYQzoe2cTg819DdRs5o9Wh6tsGLsetYTbDu9okgriJce"));

    // issue PERSON
    //public static final BigDecimal PERSON_MIN_ERA_BALANCE = BigDecimal.valueOf(10000000);
    public static HashSet<String> TRUSTED_ANONYMOUS = new HashSet<String>();
    public static HashSet<String> ANONYMASERS = new HashSet<String>();
    public static HashSet<String> FOUNDATION_ADDRESSES = new HashSet<String>();
    public static HashMap<String, int[][]> FREEZED_BALANCES = new HashMap<String, int[][]>();
    public static HashMap<String, Pair<Integer, byte[]>> NOVA_ASSETS = new HashMap<String, Pair<Integer, byte[]>>();
    public static HashMap<String, String> LOCKED__ADDRESSES = new HashMap<String, String>();
    public static HashMap<String, Tuple3<String, Integer, Integer>> LOCKED__ADDRESSES_PERIOD = new HashMap<String, Tuple3<String, Integer, Integer>>();
    public static HashMap<Long, PublicKeyAccount> ASSET_OWNERS = new HashMap<Long, PublicKeyAccount>();
    static Logger LOGGER = LoggerFactory.getLogger(BlockChain.class.getSimpleName());
    private GenesisBlock genesisBlock;
    private long genesisTimestamp;
    private Block waitWinBuffer;

    //private int target = 0;
    //private byte[] lastBlockSignature;
    //private Tuple2<Integer, Long> HWeight;

    public long transactionWinnedTimingAverage;
    public long transactionWinnedTimingCounter;

    public long transactionValidateTimingAverage;
    public long transactionValidateTimingCounter;

    /**
     * Учитывает время очистки очереди неподтвержденных трнзакций и сброса на жесткий диск их памяти
     * И поэтому это число хуже чем в Логе по подстчету обработки транзакций в блоке
     */
    public long transactionProcessTimingAverage;
    public long transactionProcessTimingCounter;

    //private DLSet dcSet;

    // dcSet_in = db() - for test
    public BlockChain(DCSet dcSet_in) throws Exception {

        //CREATE GENESIS BLOCK
        genesisBlock = new GenesisBlock();
        genesisTimestamp = genesisBlock.getTimestamp();

        trustedPeers.addAll(Settings.getInstance().getTrustedPeers());


        if (TEST_DB > 0 || TEST_MODE && !DEMO_MODE) {
            ;
        } else if (DEMO_MODE) {

            // GENERAL TRUST
            TRUSTED_ANONYMOUS.add("7BAXHMTuk1vh6AiZU65oc7kFVJGqNxLEpt");
            TRUSTED_ANONYMOUS.add("7PvUGfFTYPjYi5tcoKHL4UWcf417C8B3oh");
            //TRUSTED_ANONYMOUS.add("79ZVGgCFrQPoVTsFm6qCNTZNkRbYNsTY4u");

            // права для Кибальникова
            ASSET_OWNERS.put(7L, new PublicKeyAccount("FgdfKGEQkP1RobtbGqVSQN61AZYGy6W1WSAJvE9weYMe"));
            ASSET_OWNERS.put(8L, new PublicKeyAccount("FgdfKGEQkP1RobtbGqVSQN61AZYGy6W1WSAJvE9weYMe"));

            // из p130 счета для прорверки
            NOVA_ASSETS.put("BTC",
                    new Pair<Integer, byte[]>(12, new Account("7EPhDbpjsaRDFwB2nY8Cvn7XukF58kGdkz").getShortAddressBytes()));
            NOVA_ASSETS.put("USD",
                    new Pair<Integer, byte[]>(95, new Account("7EPhDbpjsaRDFwB2nY8Cvn7XukF58kGdkz").getShortAddressBytes()));

            LOCKED__ADDRESSES.put("7EPhDbpjsaRDFwB2nY8Cvn7XukF58kGdkz", "7A94JWgdnNPZtbmbphhpMQdseHpKCxbrZ1");
            TRUSTED_ANONYMOUS.add("762eatKnsB3xbyy2t9fwjjqUG1GoxQ8Rhx");
            ANONYMASERS.add("7CzxxwH7u9aQtx5iNHskLQjyJvybyKg8rF");


            ANONYMASERS.add("7KC2LXsD6h29XQqqEa7EpwRhfv89i8imGK"); // face2face
        } else {

            ////////// WPIPED
            // WRONG Issue Person #125
            WIPED_RECORDS.add(Longs.fromByteArray(Base58.decode("zDLLXWRmL8qhrU9DaxTTG4xrLHgb7xLx5fVrC2NXjRaw2vhzB1PArtgqNe2kxp655saohUcWcsSZ8Bo218ByUzH")));
            // WRONG orders by Person 90 Yakovlev
            WIPED_RECORDS.add(Longs.fromByteArray(Base58.decode("585CPBAusjDWpx9jyx2S2hsHByTd52wofYB3vVd9SvgZqd3igYHSqpS2gWu2THxNevv4LNkk4RRiJDULvHahPRGr")));
            WIPED_RECORDS.add(Longs.fromByteArray(Base58.decode("4xDHswuk5GsmHAeu82qysfdq9GyTxZ798ZQQGquprirrNBr7ACUeLZxBv7c73ADpkEvfBbhocGMhouM9y13sP8dK")));
            WIPED_RECORDS.add(Longs.fromByteArray(Base58.decode("3kFoA1giAr8irjA2iSC49ef8gJo9pftMg4Uif72Jcby6qvre9XAdFntpeLVZu2PAdWNi6DWiaeZRZQ8SHNT5GoZz")));
            WIPED_RECORDS.add(Longs.fromByteArray(Base58.decode("4x2NkCc9JysnCmyMcYib7NjKaNf2kPoLZ3ywifmTzjc9S8JeiJRfNEGsovCTFrTR6RA1Tazn9emASZ3mK5WBBniV")));
            WIPED_RECORDS.add(Longs.fromByteArray(Base58.decode("2Y81A7YjBji7NDKxYWMeNapSqFWFr8D4PSxBc4dCxSrCCVia6HPy2ZsezYKgeqZugNibAMra6DYT7NKCk6cSVUWX")));
            WIPED_RECORDS.add(Longs.fromByteArray(Base58.decode("4drnqT2e8uYdhqz2TqscPYLNa94LWHhMZk4UD2dgjT5fLGMuSRiKmHyyghfMUMKreDLMZ5nCK2EMzUGz3Ggbc6W9")));
            WIPED_RECORDS.add(Longs.fromByteArray(Base58.decode("L3cVbRemVECiPnLW3qdJixVkyc4QyUmjcbLmkAkz4SMMgmwHNq5KhBxNywmvfvAGGLcE3vjYFm4VT65rJktdALD")));
            WIPED_RECORDS.add(Longs.fromByteArray(Base58.decode("2hmDQkDU4zdBGdAkmpvjPhTQCHhjGQcvHGwCnyxMfDVSJPiKPiLWeW5CuBW6ZVhHq9N4H5tRFPdkKQimcykqnpv3")));

            // WRONG Vouch - from FUTURE
            WIPED_RECORDS.add(Longs.fromByteArray(Base58.decode("x9mDarBVKNuGLXWNNLxkD9hkFcRYDNo19PfSk6TpCWPGxeQ5PJqfmzKkLLp2QcF8fcukYnNQZsmqwdnATZN4Hm6")));

            // CANCEL ORDERS - wrongs after fix exchange

            // DELETED base TOKEN (LIA, ERG, etc...
            WIPED_RECORDS.add(Longs.fromByteArray(Base58.decode("Fb2TRdSLPUY7GvYnLXxhhM27qhJUQTGCRB3sRnZV5uKK3fDa4cuyGLK21xcgJ34GAaWqyh7tx6DMorqgR9s2t5g")));
            WIPED_RECORDS.add(Longs.fromByteArray(Base58.decode("sagBYvqLUVTbm6tjJ4rmkCxF1AY9SvC4jJEnfSnrc4F3T9JmqhNgGMZLzotXHxTwwQgGFprhWV4WQWYjv41Niq4")));
            WIPED_RECORDS.add(Longs.fromByteArray(Base58.decode("3LppekMMVMMuRcNrJdo14FxWRiwUnVs3KNULpjPAL7wThgqSAcDYy6369pZzSENZEenamWmUVaRUDASr3D9XrfrK")));
            WIPED_RECORDS.add(Longs.fromByteArray(Base58.decode("53gxbfbnp61Yjppf2aN33mbzEs5xWYGULsJTDBiUZ8zhdmsibZr7JFP3ZkEfiEXvsUApKmTiWvX1JVamD8YYgowo")));
            WIPED_RECORDS.add(Longs.fromByteArray(Base58.decode("3q8y3fFAqpv8rc6vzQPtXpxHt1RCbSewJ8To4JVmB1D9JzoV37XMgmk3uk9vHzdVfTzTagjNRK1Hm6edXsawsGab")));
            WIPED_RECORDS.add(Longs.fromByteArray(Base58.decode("4rEHZd1n5efcdKbbnodYrcURdyWhSLSQLUjPCwmDwjQ8m9BCzn8whZXrirxN8f94otiit2RSxJcUNggPHwhgK2r8")));
            WIPED_RECORDS.add(Longs.fromByteArray(Base58.decode("2i1jHNAEFDvdaC93d2RjYy22ymiJLRnDMV2NedXdRGZfxpavZL3QnwgWNNATcwUMSAbwG2RtZxQ6TqVx2PkoyDuD")));
            WIPED_RECORDS.add(Longs.fromByteArray(Base58.decode("1ArCghAasj2Jae6ynNEphHjQa1DsTskXqkHXCPLeTzChwzLw631d23FZjFHvnphnUJ6fw4mL2iu6AXBZQTFQkaA")));

            // VOTE 2
            // TODO добавить потом
            WIPED_RECORDS.add(Longs.fromByteArray(Base58.decode("Xq48dimwhwkXRkFun6pSQFHDSmrDnNqpUbFMkvQHC26nAyoQ3Srip3gE42axNWi5cXSPfTX5yrFkK6R4Hinuq6V")));


            // GENERAL TRUST
            TRUSTED_ANONYMOUS.add("7BAXHMTuk1vh6AiZU65oc7kFVJGqNxLEpt");
            TRUSTED_ANONYMOUS.add("7PvUGfFTYPjYi5tcoKHL4UWcf417C8B3oh");
            //TRUSTED_ANONYMOUS.add("79ZVGgCFrQPoVTsFm6qCNTZNkRbYNsTY4u");

            // ANOMIMASER for incomes from PERSONALIZED
            ANONYMASERS.add("7BAXHMTuk1vh6AiZU65oc7kFVJGqNxLEpt");
            ANONYMASERS.add("79ZVGgCFrQPoVTsFm6qCNTZNkRbYNsTY4u");
            ANONYMASERS.add("7KC2LXsD6h29XQqqEa7EpwRhfv89i8imGK"); // face2face
            ANONYMASERS.add("7PvUGfFTYPjYi5tcoKHL4UWcf417C8B3oh"); // GATE-issuer


            // TICKER = KEY + CREATOR
            NOVA_ASSETS.put("BTC",
                    new Pair<Integer, byte[]>(12, new Account("7PvUGfFTYPjYi5tcoKHL4UWcf417C8B3oh").getShortAddressBytes()));
            NOVA_ASSETS.put("ETH",
                    new Pair<Integer, byte[]>(14, new Account("7PvUGfFTYPjYi5tcoKHL4UWcf417C8B3oh").getShortAddressBytes()));

            NOVA_ASSETS.put("USD",
                    new Pair<Integer, byte[]>(95, new Account("7PvUGfFTYPjYi5tcoKHL4UWcf417C8B3oh").getShortAddressBytes()));
            NOVA_ASSETS.put("EUR",
                    new Pair<Integer, byte[]>(94, new Account("7PvUGfFTYPjYi5tcoKHL4UWcf417C8B3oh").getShortAddressBytes()));
            NOVA_ASSETS.put("CNY",
                    new Pair<Integer, byte[]>(93, new Account("7PvUGfFTYPjYi5tcoKHL4UWcf417C8B3oh").getShortAddressBytes()));
            NOVA_ASSETS.put("RUB",
                    new Pair<Integer, byte[]>(92, new Account("7PvUGfFTYPjYi5tcoKHL4UWcf417C8B3oh").getShortAddressBytes()));
            NOVA_ASSETS.put("JPY",
                    new Pair<Integer, byte[]>(91, new Account("7PvUGfFTYPjYi5tcoKHL4UWcf417C8B3oh").getShortAddressBytes()));
            NOVA_ASSETS.put("GBP",
                    new Pair<Integer, byte[]>(90, new Account("7PvUGfFTYPjYi5tcoKHL4UWcf417C8B3oh").getShortAddressBytes()));
            NOVA_ASSETS.put("CHF",
                    new Pair<Integer, byte[]>(89, new Account("7PvUGfFTYPjYi5tcoKHL4UWcf417C8B3oh").getShortAddressBytes()));
            NOVA_ASSETS.put("AUD",
                    new Pair<Integer, byte[]>(88, new Account("7PvUGfFTYPjYi5tcoKHL4UWcf417C8B3oh").getShortAddressBytes()));
            NOVA_ASSETS.put("SGD",
                    new Pair<Integer, byte[]>(87, new Account("7PvUGfFTYPjYi5tcoKHL4UWcf417C8B3oh").getShortAddressBytes()));
            NOVA_ASSETS.put("TRY",
                    new Pair<Integer, byte[]>(86, new Account("7PvUGfFTYPjYi5tcoKHL4UWcf417C8B3oh").getShortAddressBytes()));


            // COMMODITY
            NOVA_ASSETS.put("GOLD",
                    new Pair<Integer, byte[]>(21, new Account("7PvUGfFTYPjYi5tcoKHL4UWcf417C8B3oh").getShortAddressBytes()));
            NOVA_ASSETS.put("OIL",
                    new Pair<Integer, byte[]>(22, new Account("7PvUGfFTYPjYi5tcoKHL4UWcf417C8B3oh").getShortAddressBytes()));
            NOVA_ASSETS.put("GAS",
                    new Pair<Integer, byte[]>(23, new Account("7PvUGfFTYPjYi5tcoKHL4UWcf417C8B3oh").getShortAddressBytes()));
            NOVA_ASSETS.put("BREND",
                    new Pair<Integer, byte[]>(24, new Account("7PvUGfFTYPjYi5tcoKHL4UWcf417C8B3oh").getShortAddressBytes()));


            /// Права для Кибальникова в Боевой Версии
            NOVA_ASSETS.put("ERG",
                    new Pair<Integer, byte[]>(20, new Account("7GiE2pKyrULF2iQhAXvdUusXYqiKRQx68m").getShortAddressBytes()));

            //NOVA_ASSETS.put("@@USD",
            //		new Pair<Integer, byte[]>(95, new Account("7JS4ywtcqrcVpRyBxfqyToS2XBDeVrdqZL").getShortBytes()));
            //NOVA_ASSETS.put("¤¤RUB",
            //		new Pair<Integer, byte[]>(93, new Account("7JS4ywtcqrcVpRyBxfqyToS2XBDeVrdqZL").getShortBytes()));
            //NOVA_ASSETS.put("ERARUB",
            //		new Pair<Integer, byte[]>(91, new Account("7JS4ywtcqrcVpRyBxfqyToS2XBDeVrdqZL").getShortBytes()));
            //NOVA_ASSETS.put("ERAUSD",
            //		new Pair<Integer, byte[]>(85, new Account("7JS4ywtcqrcVpRyBxfqyToS2XBDeVrdqZL").getShortBytes()));

            // LOCKED -> to TRUSTED for it address
            LOCKED__ADDRESSES.put("7PvUGfFTYPjYi5tcoKHL4UWcf417C8B3oh", "79ZVGgCFrQPoVTsFm6qCNTZNkRbYNsTY4u");
            LOCKED__ADDRESSES.put("7Rt6gdkrFzayyqNec3nLhEGjuK9UsxycZ6", "79ZVGgCFrQPoVTsFm6qCNTZNkRbYNsTY4u");

            // TEAM 0 LOCKS
/// end            LOCKED__ADDRESSES_PERIOD.put("79kXsWXHRYEb7ESMohm9DXYjXBzPfi1seE", new Tuple3("78JFPWVVAVP3WW7S8HPgSkt24QF2vsGiS5", 137000, 240000)); // Vasya
/// end            LOCKED__ADDRESSES_PERIOD.put("787H1wwYPwu33BEm2KbNeksAgVaRf41b2H", new Tuple3("78JFPWVVAVP3WW7S8HPgSkt24QF2vsGiS5", 137000, 240000)); // Natasha
/// end            LOCKED__ADDRESSES_PERIOD.put("7CT5k4Qqhb53ciHfrxXaR3bGyribLgSoyZ", new Tuple3("78JFPWVVAVP3WW7S8HPgSkt24QF2vsGiS5", 137000, 240000)); // Lena
/// end            LOCKED__ADDRESSES_PERIOD.put("74g61DcTa8qdfvWxzcbTjTf6PhMfAB77HK", new Tuple3("78JFPWVVAVP3WW7S8HPgSkt24QF2vsGiS5", 137000, 240000)); // Ivan
/// end            LOCKED__ADDRESSES_PERIOD.put("7BfB66DpkEx7KJaMN9bzphTJcZR29wprMU", new Tuple3("78JFPWVVAVP3WW7S8HPgSkt24QF2vsGiS5", 137000, 240000)); // Ruslan
/// end            LOCKED__ADDRESSES_PERIOD.put("1", new Tuple3("78JFPWVVAVP3WW7S8HPgSkt24QF2vsGiS5", 137000, 240000)); // Sergey
/// end            LOCKED__ADDRESSES_PERIOD.put("1", new Tuple3("78JFPWVVAVP3WW7S8HPgSkt24QF2vsGiS5", 137000, 240000)); // Vladimir
/// end            LOCKED__ADDRESSES_PERIOD.put("1", new Tuple3("78JFPWVVAVP3WW7S8HPgSkt24QF2vsGiS5", 137000, 240000)); // Roman

            // TEST
            //FOUNDATION_ADDRESSES.add("7F9cZPE1hbzMT21g96U8E1EfMimovJyyJ7");

            // ERACHAIN FUNDATION
            FOUNDATION_ADDRESSES.add("74a73pykkNwmuwkZdh5Lt2xTbK7anG5B6i");
            FOUNDATION_ADDRESSES.add("7QTDHp15vcHN3F4zP2BTcDXJkeotzQZkG4");
            FOUNDATION_ADDRESSES.add("7FiXN8VTgjMsLrZUQY9ZBFNfek7SsDP6Uc");
            FOUNDATION_ADDRESSES.add("74QcLxHgPkuMSPsKTh7zGpJsd5aAxpWpFA");
            FOUNDATION_ADDRESSES.add("7BAXHMTuk1vh6AiZU65oc7kFVJGqNxLEpt");
            FOUNDATION_ADDRESSES.add("7P3HR8kdj4ojXPvpTnEtVnpEwenipvrcH1");
            FOUNDATION_ADDRESSES.add("75Mb8cGchcG4DF31wavhNrnoycWsoLQqP4");
            FOUNDATION_ADDRESSES.add("75LzKAoxx4TgAAkpMRStve26YEY625TCRE");

            FOUNDATION_ADDRESSES.add("73QYndpFQeFvyMvwBcMUwJRDTp7XaxkSmZ"); // STOLEN
            FOUNDATION_ADDRESSES.add("7FJUV5GLMuVdopUHSwTLsjmKF4wkPwFEcG"); // LOSED
            FOUNDATION_ADDRESSES.add("75LK84g7JHoLG2jRUmbJA6srLrFkaXEU5A"); // FREEZED


            // TEST
            //FREEZED_BALANCES.put("7F9cZPE1hbzMT21g96U8E1EfMimovJyyJ7",
            //		new int[][]{{9000, 110000}, {3200, 90000}, {138000, 7000}, {547500, 5000}});

            // TEAM 2
/// end             FREEZED_BALANCES.put("77QMFKSdY4ZsG8bFHynYdFNCmis9fNw5yP",
/// end                     new int[][]{{225655, 90000}, {333655, 60000}});
/// end             FREEZED_BALANCES.put("7N7d8juuSSeEd92rkcEsfXhdi9WXE8zYXs",
/// end                     new int[][]{{225655, 80000}, {333655, 53000}});
/// end             FREEZED_BALANCES.put("7LETj4cW4rLWBCN52CaXmzQDnhwkEcrv9G",
/// end                     new int[][]{{225655, 97000}, {333655, 65000}});

            // TEAM 3
/// end             FREEZED_BALANCES.put("7GMENsugxjV8PToyUyHNUQF7yr9Gy6tJou",
/// end                    new int[][]{{225655, 197000}, {333655, 131000}});
/// end            FREEZED_BALANCES.put("7DMJcs8kw7EXUSeEFfNwznRKRLHLrcXJFm",
/// end                    new int[][]{{225655, 150000}, {333655, 100000}});
/// end            FREEZED_BALANCES.put("7QUeuMiWQjoQ3MZiriwhKfEG558RJWUUis",
/// end                    new int[][]{{225655, 150000}, {333655, 100000}});
/// end            FREEZED_BALANCES.put("7MxscS3mS6VWim8B9K3wEzFAUWYbsMkVon",
/// end                    new int[][]{{225655, 140000}, {333655, 90000}});
/// end            FREEZED_BALANCES.put("79NMuuW7thad2JodQ5mKxbMoyf1DjNT9Ap",
/// end                    new int[][]{{225655, 130000}, {333655, 90000}});
/// end            FREEZED_BALANCES.put("7MhifBHaZsUcjgckwFN57bAE9fPJVDLDQq",
/// end                    new int[][]{{225655, 110000}, {333655, 80000}});
/// end            FREEZED_BALANCES.put("7FRWJ4ww3VstdyAyKFwYfZnucJBK7Y4zmT",
/// end                    new int[][]{{225655, 100000}, {333655, 70000}});
/// end            FREEZED_BALANCES.put("7FNAphtSYXtP5ycn88B2KEywuHXzM3XNLK",
/// end                    new int[][]{{225655, 90000}, {333655, 60000}});
/// end            FREEZED_BALANCES.put("79ZVGgCFrQPoVTsFm6qCNTZNkRbYNsTY4u",
/// end                    new int[][]{{225655, 80000}, {333655, 60000}});

            // TEAM 1
/// end            FREEZED_BALANCES.put("74rRXsxoKtVKJqN8z6t1zHfufBXsELF94y",
/// end                    new int[][]{{225655, 20000}, {333655, 10000}});
/// end            FREEZED_BALANCES.put("7PChKkoASF1eLtCnAMx8ynU2sMYdSPwkGV",
/// end                    new int[][]{{225655, 60000}, {333655, 40000}});

/// end            FREEZED_BALANCES.put("7Jhh3TPmfoLag8FxnJRBRYYfqnUduvFDbv",
/// end                    new int[][]{{225655, 150000}, {333655, 100000}});
/// end            FREEZED_BALANCES.put("7Rt6gdkrFzayyqNec3nLhEGjuK9UsxycZ6",
/// end                    new int[][]{{115000, 656000}, {225655, 441000}});

            validBlocks.add(214085);
            validBlocks.add(330685);

        }

        DCSet dcSet = dcSet_in;
        if (dcSet == null) {
            dcSet = DCSet.getInstance();
        }

        if (TEST_MODE) {
            LOGGER.info(genesisBlock.getTestNetInfo());
        }

        int height = dcSet.getBlockSignsMap().size();
        if (height <= 0)
        // process genesis block
        {
            if (dcSet_in == null && dcSet.getBlockMap().getLastBlockSignature() != null) {
                LOGGER.info("reCreateDB Database...");

                try {
                    dcSet.close();
                    dcSet = Controller.getInstance().reCreateDC(Controller.getInstance().inMemoryDC);
                } catch (Exception e) {
                    LOGGER.error(e.getMessage(), e);
                    Controller.getInstance().stopAll(6);
                }
            }

            //PROCESS
            genesisBlock.process(dcSet);

        } else {

            // TRY compare GENESIS BLOCK SIGNATURE
            if (!Arrays.equals(dcSet.getBlockMap().getAndProcess(1).getSignature(),
                    genesisBlock.getSignature())) {

                throw new Exception("wrong DB for GENESIS BLOCK");
            }

        }

        //lastBlockSignature = dcSet.getBlocksHeadMap().getLastBlockSignature();
        //HWeight = dcSet.getBlockSignsMap().get(lastBlockSignature);

    }

    //
    public static int getHeight(DCSet dcSet) {

        //GET LAST BLOCK
        ///byte[] lastBlockSignature = dcSet.getBlocksHeadMap().getLastBlockSignature();
        ///return dcSet.getBlockSignsMap().getHeight(lastBlockSignature);
        return dcSet.getBlockSignsMap().size();
    }

    public static int GENERATING_MIN_BLOCK_TIME(int height) {

        if (VERS_30SEC > 0 && height <= VERS_30SEC) {
            return 288; // old MainNet
        }

        return 30;
    }

    public static boolean isWiped(byte[] signature) {
        return WIPED_RECORDS.contains(Longs.fromByteArray(signature));
    }

    public static int GENERATING_MIN_BLOCK_TIME_MS(int height) {
        return GENERATING_MIN_BLOCK_TIME(height) * 1000;
    }

    public static int GENERATING_MIN_BLOCK_TIME_MS(long timestamp) {
        int height = timestamp < VERS_30SEC_TIME? 1 : VERS_30SEC + 1;
        return GENERATING_MIN_BLOCK_TIME(height) * 1000;
    }

    public static int FLUSH_TIMEPOINT(int height) {
        return GENERATING_MIN_BLOCK_TIME_MS(height) - (GENERATING_MIN_BLOCK_TIME_MS(height) >> 3);
    }

    public static int UNCONFIRMED_SORT_WAIT_MS(int height) {
        if (VERS_30SEC > 0 && height <= VERS_30SEC) {
            return -GENERATING_MIN_BLOCK_TIME_MS(height);
        }
        return 0;
    }

    public static int BLOCKS_PER_DAY(int height) {
        return 24 * 60 * 60 / GENERATING_MIN_BLOCK_TIME(height); // 300 PER DAY
    }

    public static int WIN_TIMEPOINT(int height) {
        return GENERATING_MIN_BLOCK_TIME_MS(height) >> 2;
    }

    public static int UNCONFIRMED_DEADTIME_MS(long timestamp) {
        int height = timestamp < VERS_30SEC_TIME ? 1 : VERS_30SEC + 1;
        if (TEST_DB > 0) {
            return GENERATING_MIN_BLOCK_TIME_MS(height);
        } else {
            return TEST_MODE ? GENERATING_MIN_BLOCK_TIME_MS(height) << 4 : GENERATING_MIN_BLOCK_TIME_MS(height) << 3;
        }
    }

    public static int VALID_PERSON_REG_ERA(int height, BigDecimal totalERA, BigDecimal totalLIA) {

        if (START_ISSUE_RIGHTS > 0 && height < START_ISSUE_RIGHTS) {
            return 0;
        }

        if (totalLIA.compareTo(BigDecimal.TEN) < 0) {
            ;
        } else {
            if (totalERA.compareTo(BigDecimal.TEN) < 0) {
                return Transaction.NOT_ENOUGH_ERA_OWN_10;
            }
        }

        return 0;

    }

    public static int VALID_PERSON_CERT_ERA(int height, BigDecimal totalERA, BigDecimal totalLIA) {

        if (START_ISSUE_RIGHTS > 0 && height < START_ISSUE_RIGHTS) {
            if (totalERA.compareTo(new BigDecimal("100")) < 0) {
                return Transaction.NOT_ENOUGH_ERA_OWN_100;
            }
            return 0;
        }

        if (totalLIA.compareTo(BigDecimal.TEN) < 0) {
            ;
        } else if (totalERA.compareTo(new BigDecimal("20")) < 0) {
            if (totalERA.compareTo(BigDecimal.TEN) < 0) {
                return Transaction.NOT_ENOUGH_ERA_OWN_10;
            }
        } else {
            if (totalERA.compareTo(new BigDecimal("100")) < 0) {
                return Transaction.NOT_ENOUGH_ERA_OWN_100;
            }
        }

        return 0;

    }

    public static BigDecimal BONUS_FOR_PERSON(int height) {

        if (START_ISSUE_RIGHTS == 0 || height > START_ISSUE_RIGHTS || TEST_MODE) {
            return BigDecimal.valueOf(5000 * BlockChain.FEE_PER_BYTE, BlockChain.FEE_SCALE);
        } else {
            return BigDecimal.valueOf(2000 * BlockChain.FEE_PER_BYTE, BlockChain.FEE_SCALE);
        }
    }

    public static boolean REFERAL_BONUS_FOR_PERSON(int height) {
        return TEST_MODE || height > REFERAL_BONUS_FOR_PERSON_4_21;
    }

    public static int getCheckPoint(DCSet dcSet, boolean useDynamic) {

        int heightCheckPoint = 1;
        if (CHECKPOINT.a > 1) {
            Integer item = dcSet.getBlockSignsMap().get(CHECKPOINT.b);
            if (item == null || item < 1)
                return 1;

            heightCheckPoint = item;
        }

        if (!useDynamic)
            return heightCheckPoint;

        int dynamicCheckPoint = getHeight(dcSet) - BlockChain.MAX_ORPHAN;

        if (dynamicCheckPoint > heightCheckPoint)
            return dynamicCheckPoint;
        return heightCheckPoint;
    }

    public byte[] getMyHardCheckPointSign() {
        byte[] mySign;
        if (CHECKPOINT.a > 1) {
            return CHECKPOINT.b;
        } else {
            return genesisBlock.getSignature();
        }
    }

    public boolean validageHardCheckPointPeerSign(String peerSign) {
        return Arrays.equals(getMyHardCheckPointSign(), Base58.decode(peerSign));
    }

    public boolean isPeerTrusted(Peer peer) {
        return trustedPeers.contains(peer.getAddress().getHostAddress());
    }

    /**
     * Calculate Target (Average Win Value for 1024 last blocks) for this block
     * @param height - height of blockchain
     * @param targetPrevious - previous Target
     * @param winValue - current Win Value
     * @return
     */
    public static long calcTarget(int height, long targetPrevious, long winValue) {

        if (height < TARGET_COUNT) {
            return targetPrevious - (targetPrevious / height) + (winValue / height);
        }

        // CUT GROWTH
        long cut1 = targetPrevious + (targetPrevious >> 1);
        if (height > TARGET_COUNT && winValue > cut1) {
            winValue = cut1;
        }

        //return targetPrevios - (targetPrevios>>TARGET_COUNT_SHIFT) + (winValue>>TARGET_COUNT_SHIFT);
        // better accuracy
        long target = (((targetPrevious << TARGET_COUNT_SHIFT) - targetPrevious) + winValue) >> TARGET_COUNT_SHIFT;
        if (target < 1000 && (ERA_COMPU_ALL_UP))
            target = 1000;

        return target;
    }

    // GET MIN TARGET
    // TODO GENESIS_CHAIN
    // SEE core.block.Block.calcWinValue(DLSet, Account, int, int)
    public static int getTargetedMin(int height) {
        int base;
        if (height < BlockChain.REPEAT_WIN)
            // FOR not repeated WINS - not need check BASE_TARGET
            /////base = BlockChain.BASE_TARGET>>1;
            base = BlockChain.BASE_TARGET - (BlockChain.BASE_TARGET >> 2); // ONLY UP
        else if (ERA_COMPU_ALL_UP)
            base = 1; //BlockChain.BASE_TARGET >>5;
        else if (height < 110000)
            base = (BlockChain.BASE_TARGET >> 3); // + (BlockChain.BASE_TARGET>>4);
        else if (height < 115000)
            base = (BlockChain.BASE_TARGET >> 1) - (BlockChain.BASE_TARGET >> 4);
        else
            base = (BlockChain.BASE_TARGET >> 1) + (BlockChain.BASE_TARGET >> 4);

        return base;

    }

    public static int calcWinValueTargeted(long win_value, long target) {

        if (target == 0) {
            // in forked chain in may be = 0
            return -1;
        }

        int result = (int) (BlockChain.BASE_TARGET * win_value / target);
        if (result < 1 || result > BlockChain.BASE_TARGET * 10)
            // fix overload
            return BlockChain.BASE_TARGET * 10;
        return result;

    }

    /**
     * calc WIN_VALUE for ACCOUNT in HEIGHT
     *
     * @param dcSet
     * @param creator                 account of block creator
     * @param height                  current blockchain height
     * @param forgingBalance          current forging Balance on account
     * @param previousForgingPoint_in
     * @return (long) Win Value
     */
    public static long calcWinValue(DCSet dcSet, Account creator, int height, int forgingBalance,
                                    Tuple3<Integer, Integer, Integer> previousForgingPoint_in) {

        if (forgingBalance < MIN_GENERATING_BALANCE && height > ALL_BALANCES_OK_TO) {
            return 0L;
        }

        Tuple3<Integer, Integer, Integer> previousForgingPoint;
        if (previousForgingPoint_in == null) {
            previousForgingPoint = creator.getForgingData(dcSet, height);
            if (previousForgingPoint == null) {
                previousForgingPoint = creator.getLastForgingData(dcSet);
            }
        } else {
            previousForgingPoint = previousForgingPoint_in;
        }

        if (ERA_COMPU_ALL_UP) {
            if (previousForgingPoint == null) {
                // так как неизвестно когда блок первый со счета соберется - задаем постоянный отступ у ДЕВЕЛОП
                previousForgingPoint = new Tuple3<Integer, Integer, Integer>(height - DEVELOP_FORGING_START, forgingBalance, 0);
            }
        } else {
            if (previousForgingPoint == null)
                return 0l;
        }

        int previousForgingHeight = previousForgingPoint.a;

        // OWN + RENT balance - in USE
        if (forgingBalance > previousForgingPoint.b) {
            forgingBalance = previousForgingPoint.b;
        }

        if (forgingBalance < BlockChain.MIN_GENERATING_BALANCE) {
            if (height > ALL_BALANCES_OK_TO)
                return 0l;
            forgingBalance = BlockChain.MIN_GENERATING_BALANCE;
        }

        int difference = height - previousForgingHeight;

        if (CHECK_BUGS > 1 && difference < REPEAT_WIN) {
            boolean debug = true;
        }

        int repeatsMin;

        if (height < BlockChain.REPEAT_WIN) {
            repeatsMin = height - 2;
        } else {
            repeatsMin = BlockChain.GENESIS_ERA_TOTAL / forgingBalance;
            repeatsMin = (repeatsMin >> 2);

            if (ERA_COMPU_ALL_UP) {
                if (DEMO_MODE && height < 2100) {
                    repeatsMin = 1;
                } else {
                    repeatsMin = REPEAT_WIN;
                }
            } else {
                if (height < 40000) {
                    if (repeatsMin > 4)
                        repeatsMin = 4;
                } else if (height < 100000) {
                    if (repeatsMin > 6)
                        repeatsMin = 6;
                } else if (height < 110000) {
                    if (repeatsMin > 10) {
                        repeatsMin = 10;
                    }
                } else if (height < 120000) {
                    if (repeatsMin > 40)
                        repeatsMin = 40;
                } else if (height < VERS_4_21_02) {
                    if (repeatsMin > 200)
                        repeatsMin = 200;
                } else if (repeatsMin < 10) {
                    repeatsMin = 10;
                }
            }
        }

        if (difference < repeatsMin) {
            return difference - repeatsMin;
        }

        long win_value;

        if (difference > 1)
            win_value = (long) forgingBalance * (long) difference;
        else
            win_value = forgingBalance;

        if (ERA_COMPU_ALL_UP || BlockChain.TEST_MODE)
            return win_value;

        if (false) {
            if (height < BlockChain.REPEAT_WIN)
                win_value >>= 4;
            else if (TEST_MODE)
                win_value >>= 4;
            else if (height < BlockChain.TARGET_COUNT)
                win_value = (win_value >> 4) - (win_value >> 6);
            else if (height < BlockChain.TARGET_COUNT << 2)
                win_value >>= 5;
            else if (height < BlockChain.TARGET_COUNT << 6)
                win_value = (win_value >> 5) - (win_value >> 7);
            else if (height < BlockChain.TARGET_COUNT << 10)
                win_value >>= 6;
            else
                win_value = (win_value >> 7) - (win_value >> 9);
        } else {
            if (height < BlockChain.REPEAT_WIN)
                win_value >>= 2;
            else if (height < (BlockChain.REPEAT_WIN<<2))
                win_value >>= 5;
            else
                win_value >>= 7;
        }


        return win_value;

    }

    /**
     * Calculate targeted Win Value and cut by BASE
     * @param dcSet dataChainSet
     * @param height blockchain height
     * @param win_value win value
     * @param target average win value for blockchain by 1024 last blocks
     * @return targeted Win Value and cut by BASE
     */
    public static int calcWinValueTargetedBase(DCSet dcSet, int height, long win_value, long target) {

        if (win_value < 1)
            return (int) win_value;

        int base = BlockChain.getTargetedMin(height);
        int targetedWinValue = calcWinValueTargeted(win_value, target);
        if (!ERA_COMPU_ALL_UP && !BlockChain.TEST_MODE
                && height > VERS_4_11
                && base > targetedWinValue) {
            return -targetedWinValue;
        }

        return targetedWinValue;

    }

    public GenesisBlock getGenesisBlock() {
        return this.genesisBlock;
    }

    //public long getGenesisTimestamp() {
    //    return this.genesisTimestamp;
    //}

	/*
	//public synchronized Tuple2<Integer, Long> getHWeight(DCSet dcSet, boolean withWinBuffer) {
	public Tuple2<Integer, Long> getHWeight(DCSet dcSet, boolean withWinBuffer) {

		if (dcSet.isStoped())
			return null;

		//GET LAST BLOCK
		byte[] lastBlockSignature = dcSet.getBlocksHeadMap().getLastBlockSignature();
		// test String b58 = Base58.encode(lastBlockSignature);

		int height;
		long weight;
		if (withWinBuffer && this.waitWinBuffer != null) {
			// with WIN BUFFER BLOCK
			height = 1;
			weight = this.waitWinBuffer.calcWinValueTargeted(dcSet);
		} else {
			height = 0;
			weight = 0l;
		}

		if (lastBlockSignature == null) {
			height++;
		} else {
			height += dcSet.getBlockSignsMap().getHeight(lastBlockSignature);
			weight += dcSet.getBlockSignsMap().getFullWeight();
		}

		return  new Tuple2<Integer, Long>(height, weight);

	}
	 */

    public long getTimestamp(int height) {
        if (VERS_30SEC == 0 || height <= VERS_30SEC) {
            return this.genesisTimestamp + (long) height * GENERATING_MIN_BLOCK_TIME_MS(height);
        }

        return this.genesisTimestamp + (TEST_MODE ? 0L : 16667L)
                + (long) VERS_30SEC * GENERATING_MIN_BLOCK_TIME_MS(VERS_30SEC)
                + (long) (height - VERS_30SEC) * GENERATING_MIN_BLOCK_TIME_MS(height);

    }

    public long getTimestamp(DCSet dcSet) {
        int height = getHeight(dcSet);
        return getTimestamp(height);
    }

    public int getHeightOnTimestamp(long timestamp) {
        long diff = timestamp - genesisTimestamp;
        int height = (int) (diff / GENERATING_MIN_BLOCK_TIME_MS(1));
        if (height <= VERS_30SEC)
            return height;

        // новый шаг между блоками
        diff -= VERS_30SEC * GENERATING_MIN_BLOCK_TIME_MS(1);

        height = (int) (diff / GENERATING_MIN_BLOCK_TIME_MS(VERS_30SEC + 1));

        return VERS_30SEC + height;

    }

    // BUFFER of BLOCK for WIN solving
    public Block getWaitWinBuffer() {
        return this.waitWinBuffer;
    }

	/*
	public void setCheckPoint(int checkPoint) {

		if (checkPoint > 1)
			this.checkPoint = checkPoint;
	}
	 */

    public int compareNewWin(DCSet dcSet, Block block) {
        return this.waitWinBuffer == null ? -1 : this.waitWinBuffer.compareWin(block);
    }

    public void clearWaitWinBuffer() {
        if (this.waitWinBuffer != null) {
            waitWinBuffer.close();
        }
        this.waitWinBuffer = null;
    }

    public Block popWaitWinBuffer() {
        Block block = this.waitWinBuffer;
        this.waitWinBuffer = null;
        return block;
    }

    // SOLVE WON BLOCK
    // 0 - unchanged;
    // 1 - changed, need broadcasting;
    public synchronized boolean setWaitWinBuffer(DCSet dcSet, Block block, Peer peer) {

        LOGGER.info("try set new winBlock: " + block.toString());

        byte[] lastSignature = dcSet.getBlockMap().getLastBlockSignature();
        if (!Arrays.equals(lastSignature, block.getReference())) {
            LOGGER.info("new winBlock from FORK!");
            return false;
        }

        if (this.waitWinBuffer != null && block.compareWin(waitWinBuffer) <= 0) {
            LOGGER.info("new winBlock is POOR!");
            return false;
        }

        // создаем в памяти базу - так как она на 1 блок только нужна - а значит много памяти не возьмет
        DB database = DCSet.makeDBinMemory();
        boolean noValid = true;
        try {
            noValid = !block.isValid(dcSet.fork(database), true);
        } finally {
            // если невалидная то закроем Форк базы, иначе базу храним для последующего слива
            if (noValid)
                database.close();
        }

        // FULL VALIDATE because before was only HEAD validating
        if (noValid) {

            LOGGER.info("new winBlock is BAD!");
            if (peer != null)
                Controller.getInstance().banPeerOnError(peer, "invalid block", 10);
            else
                LOGGER.error("MY WinBlock is INVALID! ignore...");

            return false;
        }

        // set and close OLD
        setWaitWinBufferUnchecked(block);

        LOGGER.info("new winBlock setted!!!" + block.toString());
        return true;

    }

    /**
     * если идет синхронизация то записываем без проверки
     *
     * @param block
     */
    public void setWaitWinBufferUnchecked(Block block) {
        if (this.waitWinBuffer == null || block.compareWin(waitWinBuffer) > 0) {
            if (this.waitWinBuffer != null) {
                waitWinBuffer.close();
            }
            this.waitWinBuffer = block;
        }
    }

    public Tuple2<Integer, Long> getHWeightFull(DCSet dcSet) {
        return new Tuple2<Integer, Long>(dcSet.getBlocksHeadsMap().size(),
                dcSet.getBlocksHeadsMap().getFullWeight());
    }

    public long getFullWeight(DCSet dcSet) {

        return dcSet.getBlocksHeadsMap().getFullWeight();
    }

    public List<byte[]> getSignatures(DCSet dcSet, byte[] parentSignature) {

        //logger.debug("getSignatures for ->" + Base58.encode(parent));

        List<byte[]> headers = new ArrayList<byte[]>();

        //CHECK IF BLOCK EXISTS
        Integer height = dcSet.getBlockSignsMap().get(parentSignature);
        if (height != null && height > 0) {

            int packet;
            if (Arrays.equals(parentSignature, this.genesisBlock.getSignature())
                    || Arrays.equals(parentSignature, CHECKPOINT.b)) {
                packet = 3;
            } else {
                packet = SYNCHRONIZE_PACKET;
            }
            //BlocksHeads_2Map childsMap = dcSet.getBlockHeightsMap();
            //BlocksHeads_2Map map = dcSet.getBlockHeightsMap();
            BlocksHeadsMap map = dcSet.getBlocksHeadsMap();
            int counter = 0;
            do {
                headers.add(parentSignature);
                if (map.contains(++height))
                    parentSignature = map.get(height).signature;
                else
                    break;
            } while (parentSignature != null && counter++ < packet);
            //logger.debug("get size " + counter);
        } else if (Arrays.equals(parentSignature, this.CHECKPOINT.b)) {
            headers.add(parentSignature);
        } else {
            //logger.debug("*** getSignatures NOT FOUND !");
        }

        return headers;
    }

    public Block getBlock(DCSet dcSet, byte[] header) {

        return dcSet.getBlockSignsMap().getBlock(header);
    }

    public Block getBlock(DCSet dcSet, int height) {

        return dcSet.getBlockMap().getAndProcess(height);
    }

    /**
     * Среднее время обработки транзакции при прилете блока из сети. Блок считается как одна транзакция
     *
     * @return
     */
    public void updateTXWinnedTimingAverage(long processTiming, int counter) {
        // при переполнении может быть минус
        // в миеросекундах подсчет делаем
        processTiming = processTiming / 1000 / (Controller.BLOCK_AS_TX_COUNT + counter);
        if (transactionWinnedTimingCounter < 1 << 5) {
            transactionWinnedTimingCounter++;
            transactionWinnedTimingAverage = ((transactionWinnedTimingAverage * transactionWinnedTimingCounter)
                    + processTiming - transactionWinnedTimingAverage) / transactionWinnedTimingCounter;
        } else
            transactionWinnedTimingAverage = ((transactionWinnedTimingAverage << 5)
                    + processTiming - transactionWinnedTimingAverage) >> 5;
    }

    private long pointValidateAverage;
    public void updateTXValidateTimingAverage(long processTiming, int counter) {
        // тут всегда Количество больше 0 приходит
        processTiming = processTiming / 1000 / counter;
        if (transactionValidateTimingCounter < 1 << 3) {
            transactionValidateTimingCounter++;
            transactionValidateTimingAverage = ((transactionValidateTimingAverage * transactionValidateTimingCounter)
                    + processTiming - transactionValidateTimingAverage) / transactionValidateTimingCounter;
        } else
            if (System.currentTimeMillis() - pointValidateAverage > 10000) {
                pointValidateAverage = System.currentTimeMillis();
                transactionValidateTimingAverage = ((transactionValidateTimingAverage << 1)
                        + processTiming - transactionValidateTimingAverage) >> 1;
            } else {
                transactionValidateTimingAverage = ((transactionValidateTimingAverage << 5)
                        + processTiming - transactionValidateTimingAverage) >> 5;
            }
    }

    private long pointProcessAverage;
    public void updateTXProcessTimingAverage(long processTiming, int counter) {
        if (processTiming < 999999999999l) {
            // при переполнении может быть минус
            // в микросекундах подсчет делаем
            processTiming = processTiming / 1000 / (Controller.BLOCK_AS_TX_COUNT + counter);
            if (transactionProcessTimingCounter < 1 << 3) {
                transactionProcessTimingCounter++;
                transactionProcessTimingAverage = ((transactionProcessTimingAverage * transactionProcessTimingCounter)
                        + processTiming - transactionProcessTimingAverage) / transactionProcessTimingCounter;
            } else
                if (System.currentTimeMillis() - pointProcessAverage > 10000) {
                    pointProcessAverage = System.currentTimeMillis();
                    transactionProcessTimingAverage = ((transactionProcessTimingAverage << 1)
                            + processTiming - transactionProcessTimingAverage) >> 1;

                } else {
                    transactionProcessTimingAverage = ((transactionProcessTimingAverage << 5)
                            + processTiming - transactionProcessTimingAverage) >> 5;
                }
        }
    }

    public Pair<Block, List<Transaction>> scanTransactions(DCSet dcSet, Block block, int blockLimit, int transactionLimit, int type, int service, Account account) {
        //CREATE LIST
        List<Transaction> transactions = new ArrayList<Transaction>();
        int counter = 0;

        //IF NO BLOCK START FROM GENESIS
        if (block == null) {
            block = new GenesisBlock();
        }

        //START FROM BLOCK
        int scannedBlocks = 0;
        do {
            int seqNo = 0;
            //FOR ALL TRANSACTIONS IN BLOCK
            for (Transaction transaction : block.getTransactions()) {

                transaction.setDC(dcSet, Transaction.FOR_NETWORK, block.heightBlock, ++seqNo);

                //CHECK IF ACCOUNT INVOLVED
                if (account != null && !transaction.isInvolved(account)) {
                    continue;
                }

                //CHECK IF TYPE OKE
                if (type != -1 && transaction.getType() != type) {
                    continue;
                }

                //CHECK IF SERVICE OKE
                if (service != -1 && transaction.getType() == Transaction.ARBITRARY_TRANSACTION) {
                    ArbitraryTransaction arbitraryTransaction = (ArbitraryTransaction) transaction;

                    if (arbitraryTransaction.getService() != service) {
                        continue;
                    }
                }

                //ADD TO LIST
                transactions.add(transaction);
                counter++;
            }

            //SET BLOCK TO CHILD
            block = block.getChild(dcSet);
            scannedBlocks++;
        }
        //WHILE BLOCKS EXIST && NOT REACHED TRANSACTIONLIMIT && NOT REACHED BLOCK LIMIT
        while (block != null && (counter < transactionLimit || transactionLimit == -1) && (scannedBlocks < blockLimit || blockLimit == -1));

        //CHECK IF WE REACHED THE END
        if (block == null) {
            block = this.getLastBlock(dcSet);
        } else {
            block = block.getParent(dcSet);
        }

        //RETURN PARENT BLOCK AS WE GET CHILD RIGHT BEFORE END OF WHILE
        return new Pair<Block, List<Transaction>>(block, transactions);
    }

    public Block getLastBlock(DCSet dcSet) {
        return dcSet.getBlockMap().last();
    }


    public byte[] getLastBlockSignature(DCSet dcSet) {
        return dcSet.getBlockMap().getLastBlockSignature();
    }

    // get last blocks for target
    public List<Block> getLastBlocksForTarget_old(DCSet dcSet) {

        Block last = dcSet.getBlockMap().last();

		/*
		if (this.lastBlocksForTarget != null
				&& Arrays.equals(this.lastBlocksForTarget.get(0).getSignature(), last.getSignature())) {
			return this.lastBlocksForTarget;
		}
		 */

        List<Block> list = new ArrayList<Block>();

        if (last == null || last.getVersion() == 0) {
            return list;
        }

        for (int i = 0; i < TARGET_COUNT && last.getVersion() > 0; i++) {
            list.add(last);
            last = last.getParent(dcSet);
        }

        return list;
    }

    // get Target by last blocks in chain
    public long getTarget(DCSet dcSet) {
        Block block = this.getLastBlock(dcSet);
        return block.getTarget();
    }

    // CLEAR UNCONFIRMED TRANSACTION from Invalid and DEAD
    public void clearUnconfirmedRecords(DCSet dcSet, boolean cutDeadTime) {

        dcSet.getTransactionTab().clearByDeadTimeAndLimit(this.getTimestamp(dcSet), cutDeadTime);

    }

    public String blockFromFuture(int height) {
        long blockTimestamp = getTimestamp(height);
        if (blockTimestamp + (BlockChain.WIN_BLOCK_BROADCAST_WAIT_MS >> 2) > NTP.getTime()) {
            return "invalid Timestamp from FUTURE: "
                    + (blockTimestamp + (BlockChain.WIN_BLOCK_BROADCAST_WAIT_MS >> 2) - NTP.getTime());
        }

        return null;
    }
}
