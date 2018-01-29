package datachain;



import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.mapdb.Atomic;
import org.mapdb.BTreeKeySerializer;
import org.mapdb.DB;
import org.mapdb.Fun.Tuple2;

import core.block.Block;
import core.item.ItemCls;
import database.DBMap;
import utils.Pair;

// Block Height -> creator
public abstract class AutoIntegerByte extends DCMap<Integer, byte[]> 
{
	protected Map<Integer, Integer> observableData = new HashMap<Integer, Integer>();
		
	// protected int type;

	protected Atomic.Integer atomicKey;
	protected int key;

	static Logger LOGGER = Logger.getLogger(AutoIntegerByte.class.getName());

	public AutoIntegerByte(DCSet databaseSet, DB database, String name) {
		super(databaseSet, database);

		this.atomicKey = database.getAtomicInteger(name + "_key");
		this.key = this.atomicKey.get();
	}

	public AutoIntegerByte(DCSet databaseSet, DB database,
			String name, int observeReset, int observeAdd, int observeRemove, int observeList) {

		this(databaseSet, database, name);

		if (databaseSet.isWithObserver()) {
			if (observeReset > 0)
				this.observableData.put(DBMap.NOTIFY_RESET, observeReset);
			if (databaseSet.isDynamicGUI()) {
				if (observeAdd > 0)
					this.observableData.put(DBMap.NOTIFY_ADD, observeAdd);
				if (observeRemove > 0)
					this.observableData.put(DBMap.NOTIFY_REMOVE, observeRemove);
			}
			if (observeList > 0)
				this.observableData.put(DBMap.NOTIFY_LIST, observeList);
		}
	}

	public AutoIntegerByte(AutoIntegerByte parent) {
		super(parent, null);

		this.key = parent.size();
	}

	@Override
	public int size() {
		return this.key;
	}

	public void setSize(int size) {
		// INCREMENT ATOMIC KEY IF EXISTS
		if (this.atomicKey != null) {
			this.atomicKey.set(size);
		}
		this.key = size;
	}

	protected void createIndexes(DB database) {
	}

	@Override
	protected Map<Integer, byte[]> getMemoryMap() {
		return new HashMap<Integer, byte[]>();
	}

	@Override
	protected byte[] getDefaultValue() {
		return null;
	}

	@Override
	protected Map<Integer, Integer> getObservableData() {
		return this.observableData;
	}

	public long add(byte[] item) {
		// INCREMENT ATOMIC KEY IF EXISTS
		if (this.atomicKey != null) {
			this.atomicKey.incrementAndGet();
		}

		// INCREMENT KEY
		this.key++;

		// INSERT WITH NEW KEY
		this.set(this.key, item);

		// RETURN KEY
		return this.key;
	}

	public byte[] last() {
		return this.get(this.key);		
	}
	
	public void remove() {
		super.delete(key);

		if (this.atomicKey != null) {
			this.atomicKey.decrementAndGet();
		}

		// DECREMENT KEY
		--this.key;

	}

}
