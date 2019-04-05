package org.erachain.database;

import com.google.common.collect.Iterables;
import org.erachain.utils.ObserverMessage;
import org.mapdb.BTreeMap;
import org.mapdb.Bind;
import org.mapdb.DB;
import org.mapdb.Fun;
import org.mapdb.Fun.Function2;
import org.mapdb.Fun.Tuple2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public abstract class AutoKeyDBMap<T, U> extends DBMap<T, U> {

    protected BTreeMap AUTOKEY_INDEX;

    public AutoKeyDBMap(IDB databaseSet, DB database) {
        super(databaseSet, database);

    }

    protected void makeAutoKey(DB database, BTreeMap map, String name) {

        this.AUTOKEY_INDEX = database.createTreeMap(name + "_AUTOKEY_INDEX")
                .comparator(Fun.COMPARATOR)
                .makeOrGet();

        //BIND
        Bind.secondaryKey(map, this.AUTOKEY_INDEX, new Fun.Function2<Long, T, Tuple2>() {
            @Override
            public Long run(T key, Tuple2 value) {
                return (Long) value.a;
            }
        });
    }

    @Override
    protected U getDefaultValue() {
        return null;
    }

    public Collection<T> getFromToKeys(long fromKey, long toKey) {
        // РАБОТАЕТ намного БЫСТРЕЕ
        return AUTOKEY_INDEX.subMap(fromKey, toKey).values();

    }

    /**
     * добавляем только в конец по AUTOKEY_INDEX, иначе обновляем
     * @param key
     * @param value
     * @return
     */
    @Override
    public boolean set(T key, U value) {

        if (this.contains(key)) {
            // если запись тут уже есть то при перезаписи не меняем AUTO_KEY
            Tuple2 item = (Tuple2) super.get(key);
            return super.set(key, (U) new Tuple2(item.a, ((Tuple2)value).b));

        } else {
            // новый элемент - добавим в конец карты
            return super.set(key, (U) new Tuple2(-((long)size() + 1), ((Tuple2)value).b));
        }
    }

    /**
     * удаляет только если это верхний элемент. Инача ничего не делаем, так как иначе размер собъется
     *
     * @param key
     * @return
     */
    public U delete(T key) {

        U item = super.get(key);
        if (item == null) {
            return item;
        }

        if (((Long)((Tuple2)item).a).equals(size()))
            return super.delete(key);

        return item;
    }


}
