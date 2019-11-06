package org.erachain.dbs;

import java.util.Collection;
import java.util.Iterator;
import java.util.Set;

/**
 * Общий Описатель Таблиц (Tab) и Обернутых карт - (Suit)
 * @param <T>
 * @param <U>
 */
public interface IMap<T, U> {

    Boolean EXIST = true;

    void openMap();

    IMap getSource();

    int size();

    boolean isSizeEnable();

    U get(T key);

    Set<T> keySet();

    Collection<U> values();

    /**
     *
     * @param key
     * @param value
     * @return If has old value = true
     */
    boolean set(T key, U value);

    /**
     * not check old value
     * @param key
     * @param value
     */
    void put(T key, U value);

    /**
     *
     * @param key
     * @return old value
     */
    U remove(T key);

    /**
     * not check old value
     * @param key
     * @return
     */
    void delete(T key);

    /**
     * Remove only Value - not Key.
     * @param key
     * @return old value
     */
    U removeValue(T key);

    /**
     * Delete only Value - not Key.
     * not check old value
     * @param key
     * @return
     */
    void deleteValue(T key);

    boolean contains(T key);

    /**
     *
     * @param index <b>primary Index = 0</b>, secondary index = 1...10000
     * @param descending true if need descending sort
     * @return
     */
    Iterator<T> getIterator(int index, boolean descending);

    Iterator<T> getIterator();

    int getDefaultIndex();

    void clear();

    void commit();

    void rollback();

    void clearCache();

    void close();

    boolean isClosed();

}
