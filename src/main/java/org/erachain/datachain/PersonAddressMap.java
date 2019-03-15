package org.erachain.datachain;

import org.erachain.database.DBMap;
import org.mapdb.BTreeKeySerializer;
import org.mapdb.DB;
import org.mapdb.Fun.Tuple3;
import org.erachain.utils.ObserverMessage;

import java.util.HashMap;
import java.util.Map;
import java.util.Stack;
import java.util.TreeMap;

//import java.util.HashMap;
//import org.erachain.database.DBSet;

/**
 * Хранит Удостоверенные публичные ключи для персон.
 * Тут block.getHeight + transaction index  - это ссылка на транзакцию создавшую данную заметку<br>
 *
 * <b>Ключ:</b> person key<br>

 * <b>Значение:</b><br>
 TreeMap(<br>
 (String)address - публичный счет,<br>
 Stack((Integer)end_date - дата окончания действия удостоврения,<br>
 (Integer)block.getHeight - номер блока,<br>
 (Integer)transaction index - номер транзакции в блоке<br>
 ))
 */
// TODO: ссылку на ЛОНГ
public class PersonAddressMap extends DCMap<
        Long, // personKey
        TreeMap<
                String, // address
                Stack<Tuple3<Integer, // end_date
                        Integer, // block.getHeight
                        Integer // transaction index
                        >>>> {

    public PersonAddressMap(DCSet databaseSet, DB database) {
        super(databaseSet, database);

        if (databaseSet.isWithObserver()) {
            this.observableData.put(DBMap.NOTIFY_RESET, ObserverMessage.RESET_ALL_ACCOUNT_TYPE);
            if (databaseSet.isDynamicGUI()) {
                this.observableData.put(DBMap.NOTIFY_ADD, ObserverMessage.ADD_ALL_ACCOUNT_TYPE);
                this.observableData.put(DBMap.NOTIFY_REMOVE, ObserverMessage.REMOVE_ALL_ACCOUNT_TYPE);
            }
            this.observableData.put(DBMap.NOTIFY_LIST, ObserverMessage.LIST_ALL_ACCOUNT_TYPE);
        }
    }

    public PersonAddressMap(PersonAddressMap parent) {
        super(parent, null);
    }

    protected void createIndexes(DB database) {
    }

    @Override
    protected Map<Long, TreeMap<String, Stack<Tuple3<Integer, Integer, Integer>>>> getMap(DB database) {
        //OPEN MAP
        return database.createTreeMap("person_address")
                .keySerializer(BTreeKeySerializer.BASIC)
                .counterEnable()
                .makeOrGet();
    }

    @Override
    protected Map<Long, TreeMap<String, Stack<Tuple3<Integer, Integer, Integer>>>> getMemoryMap() {
        return new TreeMap<Long, TreeMap<String, Stack<Tuple3<Integer, Integer, Integer>>>>();
    }

    @Override
    protected TreeMap<String, Stack<Tuple3<Integer, Integer, Integer>>> getDefaultValue() {
        return new TreeMap<String, Stack<Tuple3<Integer, Integer, Integer>>>();
    }

    @Override
    protected Map<Integer, Integer> getObservableData() {
        return this.observableData;
    }

    ///////////////////////////////
    @SuppressWarnings("unchecked")
    public void addItem(Long person, String address, Tuple3<Integer, Integer, Integer> item) {
        TreeMap<String, Stack<Tuple3<Integer, Integer, Integer>>> value = this.get(person);

        TreeMap<String, Stack<Tuple3<Integer, Integer, Integer>>> value_new;
        if (this.parent == null)
            value_new = value;
        else {
            // !!!! NEEED .clone() !!!
            // need for updates only in fork - not in parent DB
            value_new = (TreeMap<String, Stack<Tuple3<Integer, Integer, Integer>>>) value.clone();
        }

        Stack<Tuple3<Integer, Integer, Integer>> stack = value_new.get(address);
        if (stack == null) {
            stack = new Stack<Tuple3<Integer, Integer, Integer>>();
            stack.push(item);
            value_new.put(address, stack);
        } else {
            if (this.parent == null) {
                stack.push(item);
                value_new.put(address, stack);
            } else {
                Stack<Tuple3<Integer, Integer, Integer>> stack_new;
                stack_new = (Stack<Tuple3<Integer, Integer, Integer>>) stack.clone();
                stack_new.push(item);
                value_new.put(address, stack_new);
            }
        }

        this.set(person, value_new);

    }

    // GET ALL ITEMS
    public TreeMap<String, Stack<Tuple3<Integer, Integer, Integer>>> getItems(Long person) {
        TreeMap<String, Stack<Tuple3<Integer, Integer, Integer>>> tree = this.get(person);
        return tree;
    }

    public Tuple3<Integer, Integer, Integer> getItem(Long person, String address) {
        TreeMap<String, Stack<Tuple3<Integer, Integer, Integer>>> tree = this.get(person);
        Stack<Tuple3<Integer, Integer, Integer>> stack = tree.get(address);
        if (stack == null) return null;
        return !stack.isEmpty() ? stack.peek() : null;
    }

    @SuppressWarnings("unchecked")
    public void removeItem(Long person, String address) {
        TreeMap<String, Stack<Tuple3<Integer, Integer, Integer>>> value = this.get(person);

        TreeMap<String, Stack<Tuple3<Integer, Integer, Integer>>> value_new;
        if (this.parent == null)
            value_new = value;
        else {
            // !!!! NEEED .clone() !!!
            // need for updates only in fork - not in parent DB
            value_new = (TreeMap<String, Stack<Tuple3<Integer, Integer, Integer>>>) value.clone();
        }

        Stack<Tuple3<Integer, Integer, Integer>> stack = value_new.get(address);
        if (stack == null || stack.isEmpty()) return;

        if (this.parent == null) {
            stack.pop();
            value_new.put(address, stack);
        } else {
            Stack<Tuple3<Integer, Integer, Integer>> stack_new;
            stack_new = (Stack<Tuple3<Integer, Integer, Integer>>) stack.clone();
            stack_new.pop();
            value_new.put(address, stack_new);
        }

        this.set(person, value_new);

    }

}
