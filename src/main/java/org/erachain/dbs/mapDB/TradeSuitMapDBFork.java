package org.erachain.dbs.mapDB;

import lombok.extern.slf4j.Slf4j;
import org.erachain.core.item.assets.Order;
import org.erachain.core.item.assets.Trade;
import org.erachain.database.DBASet;
import org.erachain.database.serializer.TradeSerializer;
import org.erachain.datachain.TradeMap;
import org.erachain.datachain.TradeSuit;
import org.erachain.dbs.IteratorCloseable;
import org.erachain.dbs.IteratorCloseableImpl;
import org.mapdb.BTreeMap;
import org.mapdb.Fun;
import org.mapdb.Fun.Tuple2;

import java.util.Map;

/**
 * Хранит сделки на бирже
 * Ключ: ссылка на иницатора + ссылка на цель
 * Значение - Сделка
 * Initiator DBRef (Long) + Target DBRef (Long) -> Trade
 */
@Slf4j
public class TradeSuitMapDBFork extends DBMapSuitFork<Tuple2<Long, Long>, Trade> implements TradeSuit {

    public TradeSuitMapDBFork(TradeMap parent, DBASet databaseSet) {
        super(parent, databaseSet, logger);
    }

    @Override
    public void openMap() {
        //OPEN MAP
        map = database.createTreeMap("trades")
                .valueSerializer(new TradeSerializer())
                .comparator(Fun.TUPLE2_COMPARATOR)
                //.comparator(Fun.COMPARATOR)
                .makeOrGet();
    }

    /**
     * поиск ключей для протокольных вторичных индексов с учетом Родительской таблицы (если база форкнута)
     * - нужно для отката Заказа - просмотр по всем его покусанным сделкам
     * @param order
     * @return
     */
    @Override
    public IteratorCloseable<Tuple2<Long, Long>> getIterator(Order order) {
        //FILTER ALL KEYS
        Map uncastedMap = map;
        return new IteratorCloseableImpl(((BTreeMap<Tuple2<Long, Long>, Order>) uncastedMap).subMap(
                Fun.t2(order.getId(), null),
                Fun.t2(order.getId(), Fun.HI())).keySet().iterator());
    }

    @Override
    public IteratorCloseable<Tuple2<Long, Long>> getIteratorByKeys(Long orderID) {
        //FILTER ALL KEYS
        Map uncastedMap = map;
        return new IteratorCloseableImpl(((BTreeMap<Tuple2<Long, Long>, Order>) uncastedMap).subMap(
                Fun.t2(orderID, null),
                Fun.t2(orderID, Fun.HI())).keySet().iterator());
    }

    @Override
    public IteratorCloseable<Tuple2<Long, Long>> getTargetsIterator(Long orderID) {
        return null;
    }

    @Override
    public IteratorCloseable<Tuple2<Long, Long>> getHaveIterator(long have) {
        return null;
    }

    @Override
    public IteratorCloseable<Tuple2<Long, Long>> getWantIterator(long want) {
        return null;
    }

    @Override
    public IteratorCloseable<Tuple2<Long, Long>> getPairIteratorDesc(long have, long want) {
        return null;
    }

    @Override
    public IteratorCloseable<Tuple2<Long, Long>> getPairHeightIterator(int startHeight, int stopHeight) {
        return null;
    }

    @Override
    public IteratorCloseable<Tuple2<Long, Long>> getPairHeightIterator(long have, long want, int startHeight, int stopHeight) {
        return null;
    }

    @Override
    public IteratorCloseable<Tuple2<Long, Long>> getIteratorFromID(long[] startTradeID) {
        return null;
    }

    @Override
    public IteratorCloseable<Tuple2<Long, Long>> getPairOrderIDIterator(long startOrderID, long stopOrderID) {
        return null;
    }

    @Override
    public IteratorCloseable<Tuple2<Long, Long>> getPairOrderIDIterator(long have, long want, long startOrderID, long stopOrderID) {
        return null;
    }

}
