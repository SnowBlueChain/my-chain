package org.erachain.dbs;

import org.erachain.database.IDB;
import org.erachain.database.SortableList;

import java.util.Map;

/**
 * Описатель Таблиц (Tab), в которых есть Обернутые карты - (Suit)
 * @param <T>
 * @param <U>
 */
public interface DBTab<T, U> extends DBTabSuitCommon<T, U> {

    IDB getDBSet();

    Map<Integer, Integer> getObservableData();

    Integer deleteObservableData(int index);

    Integer setObservableData(int index, Integer data);

    boolean checkObserverMessageType(int messageType, int thisMessageType);

    //NavigableSet<Fun.Tuple2<?, T>> getIndex(int index, boolean descending);

    SortableList<T, U> getList();

}
