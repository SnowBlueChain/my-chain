package org.erachain.gui.models;

import org.erachain.controller.Controller;
import org.erachain.core.item.assets.AssetCls;
import org.erachain.core.item.assets.Order;
import org.erachain.core.transaction.Transaction;
import org.erachain.datachain.DCSet;
import org.erachain.lang.Lang;
import org.erachain.utils.DateTimeFormat;
import org.erachain.utils.Pair;
import org.mapdb.Fun.Tuple2;

import java.util.Observer;

@SuppressWarnings("serial")
public class WalletOrdersTableModel extends WalletAutoKeyTableModel<Tuple2<String, Long>, Tuple2<Long, Order>> implements Observer {
    public static final int COLUMN_TIMESTAMP = 0;
    public static final int COLUMN_BLOCK = 1;
    public static final int COLUMN_AMOUNT = 2;
    public static final int COLUMN_HAVE = 3;
    public static final int COLUMN_PRICE = 4;
    public static final int COLUMN_WANT = 5;
    public static final int COLUMN_AMOUNT_WANT = 6;
    public static final int COLUMN_LEFT = 7;
    public static final int COLUMN_CREATOR = 8;
    public static final int COLUMN_STATUS = 9;

    public WalletOrdersTableModel() {
        super(Controller.getInstance().wallet.database.getOrderMap(),
                new String[]{"Timestamp", "Block - transaction", "Amount", "Have", "Price",
                "Want", "Total", "Left", "Creator", "Status"}, new Boolean[]{true}, true);
    }

    @Override
    public Object getValueAt(int row, int column) {
        if (this.listSorted == null || row >= this.listSorted.size()) {
            return null;
        }
        Pair<Tuple2<String, Long>, Tuple2<Long, Order>> item = this.listSorted.get(row);
        if (item == null)
            return null;

        Order order = item.getB().b;

        Long blockDBrefLong = item.getA().b;

        Tuple2<Integer, Integer> blockDBref = Transaction.parseDBRef(blockDBrefLong);

        switch (column) {
            case COLUMN_TIMESTAMP:

                return DateTimeFormat.timestamptoString(Controller.getInstance().getBlockChain().getTimestamp(blockDBref.a));

            case COLUMN_AMOUNT:

                return order.getAmountHave().toPlainString();

            case COLUMN_HAVE:

                AssetCls asset = DCSet.getInstance().getItemAssetMap().get(order.getHaveAssetKey());
                return asset == null ? "[" + order.getHaveAssetKey() + "]" : asset.getShort();

            case COLUMN_PRICE:

                return order.getPrice();

            case COLUMN_WANT:

                asset = DCSet.getInstance().getItemAssetMap().get(order.getWantAssetKey());
                return asset == null ? "[" + order.getWantAssetKey() + "]" : asset.getShort();

            case COLUMN_AMOUNT_WANT:

                return order.getAmountWant().toPlainString();


            case COLUMN_LEFT:

                return order.getFulfilledWant().toPlainString();

            case COLUMN_CREATOR:

                return order.getCreator().getPersonAsString();

            case COLUMN_STATUS:

                if (order.getAmountHave().compareTo(order.getFulfilledHave()) == 0) {
                    return Lang.getInstance().translate("Done");
                } else {

                    if (DCSet.getInstance().getCompletedOrderMap().contains(order.getId()))
                        return Lang.getInstance().translate("Canceled");

                    if (DCSet.getInstance().getOrderMap().contains(order.getId())) {
                        if (order.getFulfilledHave().signum() == 0)
                            return Lang.getInstance().translate("Active");
                        else
                            return Lang.getInstance().translate("Fulfilled");
                    }

                    return "orphaned"; //"unconfirmed";
                }
                // TODO: return "orphaned !";

            case COLUMN_BLOCK:

                return blockDBref == null ? "?-?" : blockDBref.a + "-" + blockDBref.b;

        }

        return null;
    }

}