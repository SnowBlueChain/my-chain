package org.erachain.gui.models;
////////

import org.erachain.controller.Controller;
import org.erachain.core.item.persons.PersonCls;
import org.erachain.datachain.DCSet;
import org.erachain.utils.ObserverMessage;
import org.erachain.utils.Pair;
import org.mapdb.Fun.Tuple2;
import org.slf4j.LoggerFactory;

@SuppressWarnings("serial")
public class WalletItemPersonsTableModel extends WalletAutoKeyTableModel<Tuple2<Long, Long>, Tuple2<Long, PersonCls>> {
    public static final int COLUMN_KEY = 0;
    public static final int COLUMN_NAME = 1;
    public static final int COLUMN_ADDRESS = 2;
    public static final int COLUMN_CONFIRMED = 3;
    public static final int COLUMN_FAVORITE = 4;


    public WalletItemPersonsTableModel() {
        super(Controller.getInstance().getWallet().database.getPersonMap(),
                new String[]{"Key", "Name", "Publisher", "Confirmed", "Favorite"},
                new Boolean[]{false, true, true, false, false}, true,
                ObserverMessage.WALLET_RESET_PERSON_TYPE, ObserverMessage.WALLET_LIST_PERSON_TYPE,
                ObserverMessage.WALLET_ADD_PERSON_TYPE,  ObserverMessage.WALLET_REMOVE_PERSON_TYPE);

        logger = LoggerFactory.getLogger(WalletItemPersonsTableModel.class.getName());

        addObservers();

    }


    @Override
    public Object getValueAt(int row, int column) {
        if (this.listSorted == null || row > this.listSorted.size() - 1) {
            return null;
        }
        Pair<Tuple2<Long , Long>, Tuple2<Long, PersonCls>> personRes = this.listSorted.get(row);
        if (personRes == null) {
            return null;
        }
        PersonCls person = personRes.getB().b;

        switch (column) {
            case COLUMN_KEY:
                return person.getKey(DCSet.getInstance());
            case COLUMN_NAME:
                return person.viewName();
            case COLUMN_ADDRESS:
                return person.getOwner().getPersonAsString();
            case COLUMN_CONFIRMED:
                return person.isConfirmed();
            case COLUMN_FAVORITE:
                return person.isFavorite();
        }
        return null;
    }

}
