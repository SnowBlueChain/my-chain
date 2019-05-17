package org.erachain.datachain;

import org.erachain.database.DBMap;
import org.mapdb.BTreeKeySerializer;
import org.mapdb.BTreeMap;
import org.mapdb.DB;
import org.mapdb.Fun.Tuple5;

import java.util.HashMap;
import java.util.Map;
import java.util.Stack;
import java.util.TreeMap;

/**
 * Супер Класс для хранения по НомерСущности
 * key to key_Stack for End_Date Map
 *  in days
 *
 *  Ключ: НомерСущности
 *  Значение: карта к ключем по Номер Сущности и Значение:
 *          СТЭК(Дата Начала, Дата Конца,
 *          Данные запакованные
 *          ссвлка на запись)
 *
 * TODO: переделать ссылку на запись на Лонг
 *
 */
public class KKMap extends DCMap<
        Long, // item1 Key
        TreeMap<Long, // item2 Key
                Stack<Tuple5<
                        Long, // beg_date
                        Long, // end_date

                        byte[], // any additional data

                        Integer, // block.getHeight() -> db.getBlocksHeadMap(db.getHeightMap().getBlockByHeight(index))
                        Integer // block.get(transaction.getSignature()) -> block.get(index)
                        >>>> {

    private String name;

    public KKMap(DCSet databaseSet, DB database,
                 String name,
                 int observerMessage_reset, int observerMessage_add, int observerMessage_remove, int observerMessage_list
    ) {
        super(databaseSet, database);

        this.name = name;

        if (databaseSet.isWithObserver()) {
            this.observableData.put(DBMap.NOTIFY_RESET, observerMessage_reset);
            this.observableData.put(DBMap.NOTIFY_LIST, observerMessage_list);
            this.observableData.put(DBMap.NOTIFY_ADD, observerMessage_add);
            this.observableData.put(DBMap.NOTIFY_REMOVE, observerMessage_remove);
        }

    }

    public KKMap(KKMap parent) {
        super(parent, null);
    }


    protected void createIndexes(DB database) {
    }

    @Override
    protected Map<Long, TreeMap<Long, Stack<Tuple5<Long, Long, byte[], Integer, Integer>>>> getMap(DB database) {
        //OPEN MAP
        BTreeMap<Long, TreeMap<Long, Stack<Tuple5<Long, Long, byte[], Integer, Integer>>>> map = database.createTreeMap(name)
                .keySerializer(BTreeKeySerializer.BASIC)
                .counterEnable()
                .makeOrGet();

        //RETURN
        return map;
    }

    @Override
    protected Map<Long, TreeMap<Long, Stack<Tuple5<Long, Long, byte[], Integer, Integer>>>> getMemoryMap() {
        // HashMap ?
        return new TreeMap<Long, TreeMap<Long, Stack<Tuple5<Long, Long, byte[], Integer, Integer>>>>();
    }

    @Override
    protected TreeMap<Long, Stack<Tuple5<Long, Long, byte[], Integer, Integer>>> getDefaultValue() {
        return new TreeMap<Long, Stack<Tuple5<Long, Long, byte[], Integer, Integer>>>();
    }

    @SuppressWarnings("unchecked")
    public void putItem(Long key, Long itemKey, Tuple5<Long, Long, byte[], Integer, Integer> item) {

        TreeMap<Long, Stack<Tuple5<Long, Long, byte[], Integer, Integer>>> value = this.get(key);

        TreeMap<Long, Stack<Tuple5<Long, Long, byte[], Integer, Integer>>> value_new;
        if (this.parent == null)
            value_new = value;
        else {
            // !!!! NEEED .clone() !!!
            // need for updates only in fork - not in parent DB
            value_new = (TreeMap<Long, Stack<Tuple5<Long, Long, byte[], Integer, Integer>>>) value.clone();
        }

        Stack<Tuple5<Long, Long, byte[], Integer, Integer>> stack = value_new.get(itemKey);
        // если пустой то тоже пересоздаим и все - иначе ниже на .peek может вылететь ошибка
        if (stack == null || stack.isEmpty()) {
            stack = new Stack<Tuple5<Long, Long, byte[], Integer, Integer>>();
            stack.push(item);
            value_new.put(itemKey, stack);
        } else {
            if (this.parent == null) {
                stack.push(item);
                value_new.put(itemKey, stack);
            } else {
                Stack<Tuple5<Long, Long, byte[], Integer, Integer>> stack_new;
                stack_new = (Stack<Tuple5<Long, Long, byte[], Integer, Integer>>) stack.clone();
                if (item.a == null || item.b == null) {
                    // item has NULL values id dates - reset it by last values
                    Long valA;
                    Long valB;
                    Tuple5<Long, Long, byte[], Integer, Integer> lastItem = stack_new.peek();
                    if (item.a == null) {
                        // if input item Begin Date = null - take date from stack (last value)
                        valA = lastItem.a;
                    } else {
                        valA = item.a;
                    }
                    if (item.b == null) {
                        // if input item End Date = null - take date from stack (last value)
                        valB = lastItem.b;
                    } else {
                        valB = item.b;
                    }
                    stack_new.push(new Tuple5<Long, Long, byte[], Integer, Integer>(valA, valB, item.c, item.d, item.e));
                } else {
                    stack_new.push(item);
                }
                value_new.put(itemKey, stack_new);
            }
        }


        this.set(key, value_new);
    }

    // NOT UPDATE UNIQUE STATUS FOR ITEM - ADD NEW STATUS FOR ITEM + DATA
    @SuppressWarnings("unchecked")
    public void addItem(Long key, Long itemKey, Tuple5<Long, Long, byte[], Integer, Integer> item) {

        TreeMap<Long, Stack<Tuple5<Long, Long, byte[], Integer, Integer>>> value = this.get(key);

        TreeMap<Long, Stack<Tuple5<Long, Long, byte[], Integer, Integer>>> value_new;
        if (this.parent == null)
            value_new = value;
        else {
            // !!!! NEEED .clone() !!!
            // need for updates only in fork - not in parent DB
            value_new = (TreeMap<Long, Stack<Tuple5<Long, Long, byte[], Integer, Integer>>>) value.clone();
        }

        Stack<Tuple5<Long, Long, byte[], Integer, Integer>> stack = value_new.get(itemKey);
        if (stack == null) {
            stack = new Stack<Tuple5<Long, Long, byte[], Integer, Integer>>();
            stack.push(item);
            value_new.put(itemKey, stack);
        } else {
            if (this.parent == null) {
                stack.push(item);
                value_new.put(itemKey, stack);
            } else {
                Stack<Tuple5<Long, Long, byte[], Integer, Integer>> stack_new;
                stack_new = (Stack<Tuple5<Long, Long, byte[], Integer, Integer>>) stack.clone();
                stack_new.push(item);
                value_new.put(itemKey, stack_new);
            }
        }

        this.set(key, value_new);
    }

    public Tuple5<Long, Long, byte[], Integer, Integer> getItem(Long key, Long itemKey) {
        TreeMap<Long, Stack<Tuple5<Long, Long, byte[], Integer, Integer>>> value = this.get(key);
        Stack<Tuple5<Long, Long, byte[], Integer, Integer>> stack = value.get(itemKey);
        return stack != null ? !stack.isEmpty() ? stack.peek() : null : null;
    }

    public Stack<Tuple5<Long, Long, byte[], Integer, Integer>> getStack(Long key, Long itemKey) {
        TreeMap<Long, Stack<Tuple5<Long, Long, byte[], Integer, Integer>>> value = this.get(key);
        return value.get(itemKey);
    }

    // remove only last item from stack for this key of itemKey
    @SuppressWarnings("unchecked")
    public void removeItem(Long key, Long itemKey) {
        TreeMap<Long, Stack<Tuple5<Long, Long, byte[], Integer, Integer>>> value = this.get(key);

        TreeMap<Long, Stack<Tuple5<Long, Long, byte[], Integer, Integer>>> value_new;
        if (this.parent == null)
            value_new = value;
        else {
            value_new = (TreeMap<Long, Stack<Tuple5<Long, Long, byte[], Integer, Integer>>>) value.clone();
        }

        Stack<Tuple5<Long, Long, byte[], Integer, Integer>> stack = value_new.get(itemKey);
        if (stack == null || stack.isEmpty())
            return;

        if (this.parent == null) {
            stack.pop();
            value_new.put(itemKey, stack);
        } else {
            Stack<Tuple5<Long, Long, byte[], Integer, Integer>> stack_new;
            stack_new = (Stack<Tuple5<Long, Long, byte[], Integer, Integer>>) stack.clone();
            stack_new.pop();
            value_new.put(itemKey, stack_new);
        }

        this.set(key, value_new);
    }
}
