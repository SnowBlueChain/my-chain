package org.erachain.datachain;

import com.google.common.primitives.UnsignedBytes;
import org.erachain.controller.Controller;
import org.erachain.core.account.Account;
import org.erachain.core.crypto.Crypto;
import org.erachain.core.transaction.Transaction;
import org.erachain.database.DBMap;
import org.erachain.database.SortableList;
import org.erachain.utils.ObserverMessage;
import org.mapdb.*;
import org.mapdb.Fun.Tuple2;
import org.mapdb.Fun.Tuple3;
import org.mapdb.Fun.Tuple5;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;

/**
 * Hasher работает неверно! и вообще там 32 битное число 0 INTEGER - чего нифига не хватает!
 *
 * (пока не используется - по идее для бухгалтерских единиц отдельная таблица)
 * Балансы для заданного адреса на данный актив. balances for all account in blockchain<br>
 * <b>Список балансов:</b> имущество, займы, хранение, производство, резерв<br>
 * Каждый баланс: Всего Пришло и Остаток<br><br>
 *
 * <b>Ключ:</b> account.address + asset key<br>
 *
 * <b>Значение:</b> Балансы. in_OWN, in_RENT, on_HOLD = in_USE (TOTAL on HAND)
 *
 */
// TODO SOFT HARD TRUE

public class ItemAssetBalanceMap extends DCMap<Tuple2<byte[], Long>, Tuple5<
        Tuple2<BigDecimal, BigDecimal>, // in OWN - total INCOMED + BALANCE
        Tuple2<BigDecimal, BigDecimal>, // in DEBT
        Tuple2<BigDecimal, BigDecimal>, // in STOCK
        Tuple2<BigDecimal, BigDecimal>, // it DO
        Tuple2<BigDecimal, BigDecimal>  // on HOLD
        >> {

    @SuppressWarnings("rawtypes")
    private BTreeMap assetKeyMap;

    public ItemAssetBalanceMap(DCSet databaseSet, DB database) {
        super(databaseSet, database);

        if (databaseSet.isWithObserver()) {
            this.observableData.put(DBMap.NOTIFY_RESET, ObserverMessage.RESET_BALANCE_TYPE);
            this.observableData.put(DBMap.NOTIFY_LIST, ObserverMessage.LIST_BALANCE_TYPE);
            this.observableData.put(DBMap.NOTIFY_ADD, ObserverMessage.ADD_BALANCE_TYPE);
            this.observableData.put(DBMap.NOTIFY_REMOVE, ObserverMessage.REMOVE_BALANCE_TYPE);
        }
    }

    public ItemAssetBalanceMap(ItemAssetBalanceMap parent, DCSet dcSet) {
        super(parent, dcSet);
    }

    @Override
    protected void createIndexes(DB database) {
    }

    @SuppressWarnings({"unchecked"})
    @Override
    protected Map<Tuple2<byte[], Long>, Tuple5<
            Tuple2<BigDecimal, BigDecimal>, Tuple2<BigDecimal, BigDecimal>, Tuple2<BigDecimal, BigDecimal>,
            Tuple2<BigDecimal, BigDecimal>, Tuple2<BigDecimal, BigDecimal>>> getMap(DB database) {
        //OPEN MAP
        BTreeMap<Tuple2<byte[], Long>, Tuple5<
                Tuple2<BigDecimal, BigDecimal>, Tuple2<BigDecimal, BigDecimal>, Tuple2<BigDecimal, BigDecimal>,
                Tuple2<BigDecimal, BigDecimal>, Tuple2<BigDecimal, BigDecimal>>> treeMap;
        HTreeMap<Tuple2<byte[], Long>, Tuple5<
                Tuple2<BigDecimal, BigDecimal>, Tuple2<BigDecimal, BigDecimal>, Tuple2<BigDecimal, BigDecimal>,
                Tuple2<BigDecimal, BigDecimal>, Tuple2<BigDecimal, BigDecimal>>> hashMap;

        Map<Tuple2<byte[], Long>, Tuple5<
                Tuple2<BigDecimal, BigDecimal>, Tuple2<BigDecimal, BigDecimal>, Tuple2<BigDecimal, BigDecimal>,
                Tuple2<BigDecimal, BigDecimal>, Tuple2<BigDecimal, BigDecimal>>> map;

        if (false) {
            hashMap = database.createHashMap("balances")
                    .keySerializer(SerializerBase.BASIC)
                    .hasher(Hasher.BASIC) // неверно хеширует массивы внутри - видимо по Ссылке в памяти а не по значениям
                    .counterEnable()
                    .makeOrGet();
            map = hashMap;
        } else {

            treeMap = database.createTreeMap("balances")
                    //.keySerializer(BTreeKeySerializer.TUPLE2)
                    //.keySerializer(BTreeKeySerializer.BASIC)
                    .keySerializer(new BTreeKeySerializer.Tuple2KeySerializer(
                            UnsignedBytes.lexicographicalComparator(), // Fun.BYTE_ARRAY_COMPARATOR,
                            Serializer.BYTE_ARRAY,
                            Serializer.LONG))
                    //.comparator(Fun.TUPLE2_COMPARATOR)
                    //.comparator(UnsignedBytes.lexicographicalComparator())
                    .counterEnable()
                    .makeOrGet();
            map = treeMap;
        }

        if (Controller.getInstance().onlyProtocolIndexing)
            // NOT USE SECONDARY INDEXES
            return map;


        //HAVE/WANT KEY
        /// так как основной Индекс не сравниваемы - byte[] то во Вторичном индексе делаем Строку
        // - иначе она не сработает так как тут дерево с поиском
        this.assetKeyMap = database.createTreeMap("balances_key_asset")
                .comparator(Fun.COMPARATOR)
                //.valuesOutsideNodesEnable()
                .makeOrGet();

        //BIND ASSET KEY
		/*
		Bind.secondaryKey(map, this.assetKeyMap, new Fun.Function2<Tuple3<Long, BigDecimal, byte[]>, Tuple2<byte[], Long>, BigDecimal>() {
			@Override
			public Tuple3<Long, BigDecimal, byte[]> run(Tuple2<byte[], Long> key, BigDecimal value) {
				return new Tuple3<Long, BigDecimal, byte[]>(key.b, value.negate(), key.a);
			}
		});*/
        Bind.secondaryKey(treeMap, this.assetKeyMap, new Fun.Function2<Tuple3<Long,
                Tuple5<
                        Tuple2<BigDecimal, BigDecimal>, Tuple2<BigDecimal, BigDecimal>, Tuple2<BigDecimal, BigDecimal>,
                        Tuple2<BigDecimal, BigDecimal>, Tuple2<BigDecimal, BigDecimal>>,
                String>,
                Tuple2<byte[], Long>,
                Tuple5<
                        Tuple2<BigDecimal, BigDecimal>, Tuple2<BigDecimal, BigDecimal>, Tuple2<BigDecimal, BigDecimal>,
                        Tuple2<BigDecimal, BigDecimal>, Tuple2<BigDecimal, BigDecimal>>>
                () {
            @Override
            public Tuple3<Long, Tuple5<
                    Tuple2<BigDecimal, BigDecimal>, Tuple2<BigDecimal, BigDecimal>, Tuple2<BigDecimal, BigDecimal>,
                    Tuple2<BigDecimal, BigDecimal>, Tuple2<BigDecimal, BigDecimal>>, String>
            run(Tuple2<byte[], Long> key, Tuple5<
                    Tuple2<BigDecimal, BigDecimal>, Tuple2<BigDecimal, BigDecimal>, Tuple2<BigDecimal, BigDecimal>,
                    Tuple2<BigDecimal, BigDecimal>, Tuple2<BigDecimal, BigDecimal>> value) {
                return new Tuple3<Long, Tuple5<
                        Tuple2<BigDecimal, BigDecimal>, Tuple2<BigDecimal, BigDecimal>, Tuple2<BigDecimal, BigDecimal>,
                        Tuple2<BigDecimal, BigDecimal>, Tuple2<BigDecimal, BigDecimal>>, String>(
                        key.b, new Tuple5<
                        Tuple2<BigDecimal, BigDecimal>, Tuple2<BigDecimal, BigDecimal>, Tuple2<BigDecimal, BigDecimal>,
                        Tuple2<BigDecimal, BigDecimal>, Tuple2<BigDecimal, BigDecimal>>
                        (new Tuple2<BigDecimal, BigDecimal>(value.a.a.negate(), value.a.b.negate()),
                                new Tuple2<BigDecimal, BigDecimal>(value.b.a.negate(), value.b.b.negate()),
                                new Tuple2<BigDecimal, BigDecimal>(value.c.a.negate(), value.c.b.negate()),
                                new Tuple2<BigDecimal, BigDecimal>(value.d.a.negate(), value.d.b.negate()),
                                new Tuple2<BigDecimal, BigDecimal>(value.e.a.negate(), value.e.b.negate())),
                        Crypto.getInstance().getAddressFromShort(key.a)
                    );
            }
        });

        //RETURN
        return map;
    }

    @Override
    protected Map<Tuple2<byte[], Long>, Tuple5<
            Tuple2<BigDecimal, BigDecimal>, Tuple2<BigDecimal, BigDecimal>, Tuple2<BigDecimal, BigDecimal>,
            Tuple2<BigDecimal, BigDecimal>, Tuple2<BigDecimal, BigDecimal>>> getMemoryMap() {
        return new TreeMap<Tuple2<byte[], Long>, Tuple5<
                Tuple2<BigDecimal, BigDecimal>, Tuple2<BigDecimal, BigDecimal>, Tuple2<BigDecimal, BigDecimal>,
                Tuple2<BigDecimal, BigDecimal>, Tuple2<BigDecimal, BigDecimal>>>(Fun.TUPLE2_COMPARATOR);
    }

    @Override
    protected Tuple5<
            Tuple2<BigDecimal, BigDecimal>, Tuple2<BigDecimal, BigDecimal>, Tuple2<BigDecimal, BigDecimal>,
            Tuple2<BigDecimal, BigDecimal>, Tuple2<BigDecimal, BigDecimal>> getDefaultValue() {
        return new Tuple5<
                Tuple2<BigDecimal, BigDecimal>, Tuple2<BigDecimal, BigDecimal>, Tuple2<BigDecimal, BigDecimal>,
                Tuple2<BigDecimal, BigDecimal>, Tuple2<BigDecimal, BigDecimal>>
                (new Tuple2<BigDecimal, BigDecimal>(BigDecimal.ZERO, BigDecimal.ZERO),
                        new Tuple2<BigDecimal, BigDecimal>(BigDecimal.ZERO, BigDecimal.ZERO),
                        new Tuple2<BigDecimal, BigDecimal>(BigDecimal.ZERO, BigDecimal.ZERO),
                        new Tuple2<BigDecimal, BigDecimal>(BigDecimal.ZERO, BigDecimal.ZERO),
                        new Tuple2<BigDecimal, BigDecimal>(BigDecimal.ZERO, BigDecimal.ZERO));
    }

	/*
	public void set(byte[] address, BigDecimal value)
	{
		this.set(address, FEE_KEY, value);
	}
	 */

    public void set(byte[] address, long key, Tuple5<
            Tuple2<BigDecimal, BigDecimal>, Tuple2<BigDecimal, BigDecimal>, Tuple2<BigDecimal, BigDecimal>,
            Tuple2<BigDecimal, BigDecimal>, Tuple2<BigDecimal, BigDecimal>> value) {
        if (key < 0)
            key = -key;

        this.set(new Tuple2<byte[], Long>(address, key), value);
    }

    private Account testAcc = new Account("76ACGgH8c63VrrgEw1wQA4Dno1JuPLTsWe");
    public boolean set(Tuple2<byte[], Long> key, Tuple5<
            Tuple2<BigDecimal, BigDecimal>, Tuple2<BigDecimal, BigDecimal>, Tuple2<BigDecimal, BigDecimal>,
            Tuple2<BigDecimal, BigDecimal>, Tuple2<BigDecimal, BigDecimal>> value) {

        boolean test = false;
        if (testAcc.equals(key.a)) {
            test = true;
        }

        boolean result = super.set(key, value);

        if (test) {
            Fun.Tuple5 balance5 = get(key);
        }

        return result;

    }

	/*
	public BigDecimal get(byte[] address)
	{
		return this.get(address, FEE_KEY);
	}
	 */

    public Tuple5<
            Tuple2<BigDecimal, BigDecimal>, Tuple2<BigDecimal, BigDecimal>, Tuple2<BigDecimal, BigDecimal>,
            Tuple2<BigDecimal, BigDecimal>, Tuple2<BigDecimal, BigDecimal>> get(byte[] address, long key) {
        if (key < 0)
            key = -key;


        Tuple5<
                Tuple2<BigDecimal, BigDecimal>, Tuple2<BigDecimal, BigDecimal>, Tuple2<BigDecimal, BigDecimal>,
                Tuple2<BigDecimal, BigDecimal>, Tuple2<BigDecimal, BigDecimal>> value = this.get(new Tuple2<byte[], Long>(address, key));

		/*
		// TODO for TEST
		// FOR TEST NET
		if (key == Transaction.FEE_KEY &&
				value.a.compareTo(BigDecimal.ONE) < 0) {

			return new Tuple3<BigDecimal, BigDecimal, BigDecimal>(
					BigDecimal.ONE, BigDecimal.ZERO, BigDecimal.ZERO);

		}
		 */

        return value;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public SortableList<Tuple2<byte[], Long>, Tuple5<
            Tuple2<BigDecimal, BigDecimal>, Tuple2<BigDecimal, BigDecimal>, Tuple2<BigDecimal, BigDecimal>,
            Tuple2<BigDecimal, BigDecimal>, Tuple2<BigDecimal, BigDecimal>>> getBalancesSortableList(long key) {
        if (key < 0)
            key = -key;

        //FILTER ALL KEYS
        Collection<Tuple2<byte[], Long>> keys = ((BTreeMap<Tuple3, Tuple2<byte[], Long>>) this.assetKeyMap).subMap(
                Fun.t3(key, null, null),
                Fun.t3(key, Fun.HI(), Fun.HI())).values();

        //RETURN
        return new SortableList<Tuple2<byte[], Long>, Tuple5<
                Tuple2<BigDecimal, BigDecimal>, Tuple2<BigDecimal, BigDecimal>, Tuple2<BigDecimal, BigDecimal>,
                Tuple2<BigDecimal, BigDecimal>, Tuple2<BigDecimal, BigDecimal>>>(this, keys);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public SortableList<Tuple2<byte[], Long>, Tuple5<
            Tuple2<BigDecimal, BigDecimal>, Tuple2<BigDecimal, BigDecimal>, Tuple2<BigDecimal, BigDecimal>,
            Tuple2<BigDecimal, BigDecimal>, Tuple2<BigDecimal, BigDecimal>>> getBalancesSortableList(Account account) {

        if (Controller.getInstance().onlyProtocolIndexing)
            return null;

        BTreeMap map = (BTreeMap) this.map;

        //FILTER ALL KEYS
        Collection keys = ((BTreeMap<Tuple2, BigDecimal>) map).subMap(
                Fun.t2(account.getShortAddressBytes(), null),
                Fun.t2(account.getShortAddressBytes(), Fun.HI())).keySet();

        // TODO - ERROR PARENT not userd!

        //RETURN
        return new SortableList<Tuple2<byte[], Long>, Tuple5<Tuple2<BigDecimal, BigDecimal>, Tuple2<BigDecimal, BigDecimal>, Tuple2<BigDecimal, BigDecimal>, Tuple2<BigDecimal, BigDecimal>, Tuple2<BigDecimal, BigDecimal>>>(this, keys);
    }

}
