package org.erachain.datachain;

import com.google.common.collect.Iterators;
import org.erachain.controller.Controller;
import org.erachain.core.BlockChain;
import org.erachain.core.item.assets.Order;
import org.erachain.core.item.assets.OrderComparatorForTrade;
import org.erachain.core.item.assets.OrderComparatorForTradeReverse;
import org.erachain.core.transaction.Transaction;
import org.erachain.dbs.DBTab;
import org.erachain.dbs.DBTabImpl;
import org.erachain.dbs.mapDB.OrdersSuitMapDB;
import org.erachain.dbs.mapDB.OrdersSuitMapDBFork;
import org.erachain.dbs.rocksDB.OrdersSuitRocksDB;
import org.erachain.utils.ObserverMessage;
import org.mapdb.DB;

import java.math.BigDecimal;
import java.util.*;

import static org.erachain.database.IDB.DBS_ROCK_DB;

/**
 * Хранение ордеров на бирже
 * Ключ: ссылка на запись создавшую заказ
 * Значение: Ордер
 * <p>
 * ВНИМАНИЕ !!! ВТОричные ключи не хранят дубли если созданы вручную а не
 * в mapDB.DBMapSuit#createIndex() (тут первичный ключ добавится автоматически)
 * - тоесть запись во втричном ключе не будет учтена иперезапишется если такой же ключ прийдет
 * Поэтому нужно добавлять униальность
 *
 * @return
 */
public class OrderMapImpl extends DBTabImpl<Long, Order> implements OrderMap {

    public OrderMapImpl(int dbsUsed, DCSet databaseSet, DB database) {
        super(dbsUsed, databaseSet, database);

        if (databaseSet.isWithObserver()) {
            this.observableData.put(DBTab.NOTIFY_RESET, ObserverMessage.RESET_ORDER_TYPE);
            this.observableData.put(DBTab.NOTIFY_LIST, ObserverMessage.LIST_ORDER_TYPE);
            this.observableData.put(DBTab.NOTIFY_ADD, ObserverMessage.ADD_ORDER_TYPE);
            this.observableData.put(DBTab.NOTIFY_REMOVE, ObserverMessage.REMOVE_ORDER_TYPE);
        }
    }

    public OrderMapImpl(int dbsUsed, OrderMap parent, DCSet dcSet) {
        super(dbsUsed, parent, dcSet);
    }

    @Override
    public void openMap() {
        // OPEN MAP
        if (parent == null) {
            switch (dbsUsed) {
                case DBS_ROCK_DB:
                    map = new OrdersSuitRocksDB(databaseSet, database);
                    break;
                default:
                    map = new OrdersSuitMapDB(databaseSet, database);
            }
        } else {
            switch (dbsUsed) {
                case DBS_ROCK_DB:
                    //map = new NativeMapTreeMapFork(parent, databaseSet, null, null); - просто карту нельзя так как тут особые вызовы
                    //break;
                default:
                    ///map = new nativeMapTreeMapFork(parent, databaseSet, null, null); - просто карту нельзя так как тут особые вызовы
                    map = new OrdersSuitMapDBFork((OrderMap) parent, databaseSet);
            }
        }
    }

    @Override
    public long getCount(long have, long want) {
        if (Controller.getInstance().onlyProtocolIndexing) {
            return 0;
        }
        return Iterators.size(((OrderSuit) map).getHaveWantIterator(have, want));
    }

    @Override
    public long getCountHave(long have) {
        if (Controller.getInstance().onlyProtocolIndexing) {
            return 0;
        }
        return Iterators.size(((OrderSuit) map).getHaveWantIterator(have));
    }

    @Override
    public long getCountWant(long want) {
        return Iterators.size(((OrderSuit) map).getWantHaveIterator(want));
    }

    @Override
    public List<Order> getOrders(long haveWant) {

        if (Controller.getInstance().onlyProtocolIndexing) {
            return new ArrayList<>();
        }

        //GET ALL ORDERS FOR KEYS
        List<Order> orders = new ArrayList<Order>();

        Iterator<Long> iterator = ((OrderSuit) map).getHaveWantIterator(haveWant);

        while (iterator.hasNext()) {
            orders.add(map.get(iterator.next()));
        }

        iterator = ((OrderSuit) map).getWantHaveIterator(haveWant);

        while (iterator.hasNext()) {
            orders.add(map.get(iterator.next()));
        }

        return orders;
    }

    @Override
    public long getCountOrders(long haveWant) {
        if (Controller.getInstance().onlyProtocolIndexing) {
            return 0;
        }

        return this.getCountHave(haveWant) + this.getCountWant(haveWant);
    }


    @Override
    public HashSet<Long> getSubKeysWithParent(long have, long want, BigDecimal limit) {
        return ((OrderSuit) map).getSubKeysWithParent(have, want, limit);
    }

    @Override
    public List<Order> getOrdersForTradeWithFork(long have, long want, BigDecimal limit) {

        //FILTER ALL KEYS
        HashSet<Long> keys = ((OrderSuit) map).getSubKeysWithParent(have, want, limit);

        //GET ALL ORDERS FOR KEYS
        List<Order> orders = new ArrayList<Order>();

        for (Long key : keys) {
            Order order = this.get(key);
            if (order != null) {
                orders.add(order);
            } else {
                // возможно произошло удаление в момент запроса??
            }
        }

        Collections.sort(orders, new OrderComparatorForTrade());

        //RETURN
        return orders;
    }

    @Override
    public List<Order> getOrdersForTrade(long have, long want, boolean reverse) {
        //FILTER ALL KEYS
        Collection<Long> keys = ((OrderSuit) map).getSubKeysWithParent(have, want, null);

        //GET ALL ORDERS FOR KEYS
        List<Order> orders = new ArrayList<Order>();

        for (Long key : keys) {
            Order order = this.get(key);
            if (order != null) {
                orders.add(order);
            } else {
                // возможно произошло удаление в момент запроса??
            }
        }

        if (reverse) {
            Collections.sort(orders, new OrderComparatorForTradeReverse());
        } else {
            Collections.sort(orders, new OrderComparatorForTrade());
        }

        //RETURN
        return orders;
    }

    @Override
    public List<Order> getOrders(long have, long want, int limit) {

        if (Controller.getInstance().onlyProtocolIndexing) {
            return new ArrayList<>();
        }
        Iterator<Long> iterator = ((OrderSuit) map).getHaveWantIterator(have, want);

        iterator = Iterators.limit(iterator, limit);

        List<Order> orders = new ArrayList<>();
        while (iterator.hasNext()) {
            orders.add(get(iterator.next()));
        }

        return orders;
    }

    @Override
    public List<Order> getOrdersForAddress(
            String address, Long have, Long want) {

        if (Controller.getInstance().onlyProtocolIndexing) {
            return new ArrayList<>();
        }
        Iterator<Long> iterator = ((OrderSuit) map).getAddressHaveWantIterator(address, have, want);

        //GET ALL ORDERS FOR KEYS
        List<Order> orders = new ArrayList<Order>();

        while (iterator.hasNext()) {

            Long key = iterator.next();
            Order order = this.get(key);

            // MAY BE NULLS!!!
            if (order != null)
                orders.add(this.get(key));
        }

        return orders;

    }

    @Override
    public boolean set(Long id, Order order) {
        if (BlockChain.CHECK_BUGS > 3) {
            if (((DCSet) this.getDBSet()).getCompletedOrderMap().contains(id)) {
                // если он есть в уже завершенных
                LOGGER.error("already in Completed");
                Long err = null;
                ++err;
            }
        }

        return super.set(id, order);
    }

    @Override
    public void put(Long id, Order order) {
        if (BlockChain.CHECK_BUGS > 3) {
            if (((DCSet) this.getDBSet()).getCompletedOrderMap().contains(id)) {
                // если он есть в уже завершенных
                LOGGER.error("already in Completed");
                Long err = null;
                ++err;
            }
        }

        super.put(id, order);
    }

    @Override
    public Order remove(Long id) {
        if (BlockChain.CHECK_BUGS > 3) {
            if (((DCSet) this.getDBSet()).getCompletedOrderMap().contains(id)) {
                // если он есть в уже завершенных
                LOGGER.error("already in Completed");
                Long err = null;
                ++err;
            }
        }
        return super.remove(id);
    }

    @Override
    public void delete(Long id) {
        if (BlockChain.CHECK_BUGS > 3) {
            if (((DCSet) this.getDBSet()).getCompletedOrderMap().contains(id)) {
                // если он есть в уже завершенных
                LOGGER.error("Order [" + Transaction.viewDBRef(id) + "] already in Completed");
                Long err = null;
                ++err;
            }
        }
        super.delete(id);
    }

    @Override
    public void put(Order order) {
        if (BlockChain.CHECK_BUGS > 3 && Transaction.viewDBRef(order.getId()).equals("178617-18")) {
            boolean debug = true;
        }
        this.put(order.getId(), order);
    }

    @Override
    public void delete(Order order) {
        if (BlockChain.CHECK_BUGS > 3 && Transaction.viewDBRef(order.getId()).equals("178617-18")) {
            boolean debug = true;
        }
        this.delete(order.getId());
    }
}
