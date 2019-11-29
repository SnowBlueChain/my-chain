package org.erachain.datachain;

import org.erachain.core.item.assets.Order;
import org.erachain.core.item.assets.Trade;
import org.erachain.dbs.DBTab;
import org.mapdb.Fun;

import java.math.BigDecimal;
import java.util.Iterator;
import java.util.List;

public interface TradeMap extends DBTab<Fun.Tuple2<Long, Long>, Trade> {
    void put(Trade trade);

    Iterator<Fun.Tuple2<Long, Long>> getIterator(Order order);

    List<Trade> getInitiatedTrades(Order order);

    List<Trade> getTradesByOrderID(Long orderID);

    @SuppressWarnings("unchecked")
    List<Trade> getTrades(long haveWant)
    // get trades for order as HAVE and as WANT
    ;

    @SuppressWarnings("unchecked")
    List<Trade> getTrades(long have, long want, int offset, int limit);

    @SuppressWarnings("unchecked")
    Trade getLastTrade(long have, long want);

    List<Trade> getTradesByTimestamp(long have, long want, long timestamp, int limit);

    List<Trade> getTradesByTimestamp(long have, long want, int start, int stop, int limit);

    BigDecimal getVolume24(long have, long want);

    void delete(Trade trade);
}
