package org.erachain.dbs.mapDB;

// 30/03

import lombok.extern.slf4j.Slf4j;
import org.erachain.controller.Controller;
import org.erachain.core.BlockChain;
import org.erachain.core.item.assets.Order;
import org.erachain.core.transaction.Transaction;
import org.erachain.database.DBASet;
import org.erachain.database.serializer.OrderSerializer;
import org.erachain.datachain.OrderSuit;
import org.erachain.dbs.IteratorCloseable;
import org.erachain.dbs.IteratorCloseableImpl;
import org.mapdb.BTreeMap;
import org.mapdb.Bind;
import org.mapdb.DB;
import org.mapdb.Fun;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

;

//import com.sun.media.jfxmedia.logging.Logger;

/**
 * Хранит блоки полностью - с транзакциями
 * <p>
 * ключ: номер блока (высота, height)<br>
 * занчение: Блок<br>
 * <p>
 * Есть вторичный индекс, для отчетов (blockexplorer) - generatorMap
 * TODO - убрать длинный индек и вставить INT
 *
 * @return
 */

@Slf4j
public class OrdersSuitMapDB extends DBMapSuit<Long, Order> implements OrderSuit {

    @SuppressWarnings("rawtypes")
    private BTreeMap haveWantKeyMap;
    @SuppressWarnings("rawtypes")
    // TODO: cut index to WANT only
    private BTreeMap wantHaveKeyMap;
    private BTreeMap addressHaveWantKeyMap;

    public OrdersSuitMapDB(DBASet databaseSet, DB database) {
        super(databaseSet, database, logger);
    }

    @Override
    public void openMap() {
        // OPEN MAP
        map = database.createTreeMap("orders")
                .valueSerializer(new OrderSerializer())
                //.comparator(Fun.BYTE_ARRAY_COMPARATOR) // for byte[]
                .comparator(Fun.COMPARATOR)
                .makeOrGet();

        //HAVE/WANT KEY
        this.haveWantKeyMap = database.createTreeMap("orders_key_have_want")
                .comparator(Fun.COMPARATOR)
                .makeOrGet();

        ///////////////////// HERE PROTOCOL INDEX

        //BIND HAVE/WANT KEY
        Bind.secondaryKey((Bind.MapWithModificationListener) map, this.haveWantKeyMap,
                new Fun.Function2<Fun.Tuple4<Long, Long, BigDecimal, Long>, Long,
                        Order>() {
                    @Override
                    public Fun.Tuple4<Long, Long, BigDecimal, Long> run(
                            Long key, Order value) {
                        return new Fun.Tuple4<>(value.getHaveAssetKey(), value.getWantAssetKey(),

                                // по остаткам цены НЕЛЬЗЯ! так как при изменении цены после покусывания стрый ключ не находится!
                                // и потом при поиске по итераторы находятся эти неудалившиеся ключи!
                                value.calcLeftPrice(),
                                //// теперь можно - в Обработке ордера сделал решение этой проблемы value.getPrice(),

                                value.getId());
                    }
                });

        if (Controller.getInstance().onlyProtocolIndexing)
            // NOT USE SECONDARY INDEXES
            return;

        ///////////////////// HERE NOT PROTOCOL INDEXES

        // ADDRESS HAVE/WANT KEY
        this.addressHaveWantKeyMap = database.createTreeMap("orders_key_address_have_want")
                .comparator(Fun.COMPARATOR)
                .makeOrGet();

        //BIND HAVE/WANT KEY
        Bind.secondaryKey((Bind.MapWithModificationListener) map, this.addressHaveWantKeyMap,
                new Fun.Function2<Fun.Tuple5<String, Long, Long, BigDecimal, Long>, Long, Order>() {
                    @Override
                    public Fun.Tuple5<String, Long, Long, BigDecimal, Long> run(
                            Long key, Order value) {
                        return new Fun.Tuple5<String, Long, Long, BigDecimal, Long>
                                (value.getCreator().getAddress(), value.getHaveAssetKey(), value.getWantAssetKey(), value.getPrice(),
                                        key);
                    }
                });

        // WANT/HAVE KEY
        this.wantHaveKeyMap = database.createTreeMap("orders_key_want_have")
                .comparator(Fun.COMPARATOR)
                .makeOrGet();

        //BIND HAVE/WANT KEY
        Bind.secondaryKey((Bind.MapWithModificationListener) map, this.wantHaveKeyMap,
                new Fun.Function2<Fun.Tuple4<Long, Long, BigDecimal, Long>, Long,
                        Order>() {
                    @Override
                    public Fun.Tuple4<Long, Long, BigDecimal, Long> run(
                            Long key, Order value) {
                        return new Fun.Tuple4<>(value.getWantAssetKey(), value.getHaveAssetKey(),
                                value.getPrice(),
                                value.getId());
                    }
                });
    }

    //@Override
    protected void getMemoryMap() {
        openMap();
    }

    @Override
    public IteratorCloseable<Long> getHaveWantIterator(long have, long want) {

        return new IteratorCloseableImpl(((BTreeMap<Fun.Tuple4, Long>) this.haveWantKeyMap).subMap(
                Fun.t4(have, want, null, null),
                Fun.t4(have, want, Fun.HI(), Fun.HI())).values().iterator());

    }

    @Override
    public IteratorCloseable<Long> getHaveWantIterator(long have) {
        return new IteratorCloseableImpl(((BTreeMap<Fun.Tuple4, Long>) this.haveWantKeyMap).subMap(
                Fun.t4(have, null, null, null),
                Fun.t4(have, Fun.HI(), Fun.HI(), Fun.HI())).values().iterator());
    }

    @Override
    public IteratorCloseable<Long> getWantHaveIterator(long want, long have) {

        return new IteratorCloseableImpl(((BTreeMap<Fun.Tuple4, Long>) this.wantHaveKeyMap).subMap(
                Fun.t4(want, have, null, null),
                Fun.t4(want, have, Fun.HI(), Fun.HI())).values().iterator());

    }

    @Override
    public IteratorCloseable<Long> getWantHaveIterator(long want) {
        return new IteratorCloseableImpl(((BTreeMap<Fun.Tuple4, Long>) this.wantHaveKeyMap).subMap(
                Fun.t4(want, null, null, null),
                Fun.t4(want, Fun.HI(), Fun.HI(), Fun.HI())).values().iterator());
    }

    @Override
    public IteratorCloseable<Long> getAddressHaveWantIterator(String address, long have, long want) {

        return new IteratorCloseableImpl(((BTreeMap<Fun.Tuple5, Long>) this.addressHaveWantKeyMap).subMap(
                Fun.t5(address, have, want, null, null),
                Fun.t5(address, have, want, Fun.HI(), Fun.HI())).values().iterator());
    }

    @Override
    public IteratorCloseable<Long> getAddressIterator(String address) {

        return new IteratorCloseableImpl(((BTreeMap<Fun.Tuple5, Long>) this.addressHaveWantKeyMap).subMap(
                Fun.t5(address, null, null, null, null),
                Fun.t5(address, Fun.HI(), Fun.HI(), Fun.HI(), Fun.HI())).values().iterator());
    }

    @Override
    public HashMap<Long, Order> getUnsortedEntries(long have, long want, BigDecimal stopPrice, Map deleted) {

        // берем все сейчас! так как тут просто перебьор будет и нам надо вщять + одну выше цены
        // Object limitOrHI = stopPrice == null ? Fun.HI() : stopPrice; // надо тут делать выбор иначе ошибка преобразования в subMap
        Collection<Long> keys = ((BTreeMap<Fun.Tuple4, Long>) this.haveWantKeyMap).subMap(
                Fun.t4(have, want, null, null),
                Fun.t4(have, want, Fun.HI(), Fun.HI()))
                .values();

        HashMap<Long, Order> result = new HashMap<>();
        for (Long key : keys) {
            if (deleted != null && deleted.containsKey(key)) {
                // SKIP deleted in FORK
                continue;
            }

            Order order = get(key);
            if (order == null) {
                String refDB = Transaction.viewDBRef(key); // 176395-2
                Order getOrder = this.map.get(key);
                order = get(key);
                Long err = null;
                continue;
                //err++;
            }
            result.put(key, order);
            // сдесь ходябы одну заявку с неподходящей вроде бы ценой нужно взять
            // причем берем по Остаткам Цену теперь
            if (stopPrice != null && order.calcLeftPrice().compareTo(stopPrice) > 0) {
                break;
            }
        }

        return result;
    }

    @Override
    public void delete(Long key) {
        if (BlockChain.CHECK_BUGS > 3 && Transaction.viewDBRef(key).equals("176395-2")) {
            boolean debug = true;
        }
        super.delete(key);
    }

    @Override
    public Order remove(Long key) {
        if (BlockChain.CHECK_BUGS > 3 && Transaction.viewDBRef(key).equals("176395-2")) {
            boolean debug = true;
        }
        return super.remove(key);
    }

}
