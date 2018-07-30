package core.item.assets;

import datachain.DCSet;
import datachain.OrderMap;
import org.mapdb.BTreeMap;
import org.mapdb.Fun;
import org.mapdb.Fun.Tuple2;
import org.mapdb.Fun.Tuple3;
import org.mapdb.Fun.Tuple5;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Comparator;
import java.util.Map;

/**
 * Sorts Orders by price and TIMESTAMP for resolve exchange
 *
 * @author icreator
 */
public class OrderKeysComparatorForTrade implements Comparator<Long> {

    @Override
    public int compare(Long orderKey1, Long orderKey2) {

        OrderMap map = DCSet.getInstance().getOrderMap();

        Order order1 = map.get(orderKey1);
        Order order2 = map.get(orderKey2);

        int compare = order1.getPrice().compareTo(order2.getPrice());
        if (compare != 0)
            return compare;

        return Long.signum(order1.getTimestamp() - order2.getTimestamp());

    }

}
