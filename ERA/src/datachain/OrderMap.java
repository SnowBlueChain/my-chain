package datachain;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.mapdb.BTreeMap;
import org.mapdb.Bind;
import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.mapdb.Fun;
import org.mapdb.Fun.Tuple4;

import core.item.assets.Order;
import core.item.assets.OrderReverse;
import utils.ObserverMessage;
import database.DBMap;
import database.serializer.OrderSerializer;
import datachain.DCSet;

public class OrderMap extends DCMap<BigInteger, Order> 
{
	private Map<Integer, Integer> observableData = new HashMap<Integer, Integer>();
	
	@SuppressWarnings("rawtypes")
	private BTreeMap haveWantKeyMap;
	@SuppressWarnings("rawtypes")
	private BTreeMap wantHaveKeyMap;
	
	public OrderMap(DCSet databaseSet, DB database)
	{
		super(databaseSet, database);
		
		if (databaseSet.isWithObserver()) {
			this.observableData.put(DBMap.NOTIFY_RESET, ObserverMessage.RESET_ORDER_TYPE);
			if (databaseSet.isDynamicGUI()) {
				this.observableData.put(DBMap.NOTIFY_ADD, ObserverMessage.ADD_ORDER_TYPE);
				this.observableData.put(DBMap.NOTIFY_REMOVE, ObserverMessage.REMOVE_ORDER_TYPE);
			}
			this.observableData.put(DBMap.NOTIFY_LIST, ObserverMessage.LIST_ORDER_TYPE);
		}
	}

	public OrderMap(OrderMap parent, DCSet dcSet) 
	{
		super(parent, dcSet);

	}

	protected void createIndexes(DB database){}

	@Override
	protected Map<BigInteger, Order> getMap(DB database) 
	{
		//OPEN MAP
		return this.openMap(database);
	}

	@Override
	protected Map<BigInteger, Order> getMemoryMap() 
	{
		DB database = DBMaker.newMemoryDB().make();
		
		//OPEN MAP
		return this.openMap(database);
	}
	
	@SuppressWarnings("unchecked")
	private Map<BigInteger, Order> openMap(DB database)
	{
		//OPEN MAP
		BTreeMap<BigInteger, Order> map = database.createTreeMap("orders")
				.valueSerializer(new OrderSerializer())
				.makeOrGet();
		
		//HAVE/WANT KEY
		this.haveWantKeyMap = database.createTreeMap("orders_key_have_want")
				.comparator(Fun.COMPARATOR)
				.makeOrGet();
		
		//BIND HAVE/WANT KEY
		Bind.secondaryKey(map, this.haveWantKeyMap,
				new Fun.Function2<Tuple4<Long, Long, BigDecimal, BigInteger>, BigInteger, Order>() {
			@Override
			public Tuple4<Long, Long, BigDecimal, BigInteger> run(BigInteger key, Order value) {
				return new Tuple4<Long, Long, BigDecimal, BigInteger>(value.getHave(), value.getWant(), value.getPriceCalc(), key);
			}	
		});
		
		//HAVE/WANT KEY
		this.wantHaveKeyMap = database.createTreeMap("orders_key_want_have")
				.comparator(Fun.COMPARATOR)
				.makeOrGet();
		
		//BIND HAVE/WANT KEY
		Bind.secondaryKey(map, this.wantHaveKeyMap, new Fun.Function2<Tuple4<Long, Long, BigDecimal, BigInteger>, BigInteger, Order>() {
			@Override
			public Tuple4<Long, Long, BigDecimal, BigInteger> run(BigInteger key, Order value) {
				return new Tuple4<Long, Long, BigDecimal, BigInteger>(value.getWant(), value.getHave(), value.getPriceCalc(), key);
			}	
		});
		
				
		//RETURN
		return map;
	}

	@Override
	protected Order getDefaultValue() 
	{
		return null;
	}
	
	@Override
	protected Map<Integer, Integer> getObservableData() 
	{
		return this.observableData;
	}

	public void add(Order order) {
		
		// this order is NOT executable
		order.setExecutable(true);
		
		this.set(order.getId(), order);
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private Collection<BigInteger> getKeys(long have, long want) {
		
		//FILTER ALL KEYS
		Collection<BigInteger> keys = ((BTreeMap<Tuple4, BigInteger>) this.haveWantKeyMap).subMap(
				Fun.t4(have, want, null, null),
				Fun.t4(have, want, Fun.HI(), Fun.HI())).values();
		
		//IF THIS IS A FORK
		if(this.parent != null)
		{
			//GET ALL KEYS FOR FORK
			Collection<BigInteger> forkKeys = ((OrderMap) this.parent).getKeys(have, want);
			
			//COMBINE LISTS
			Set<BigInteger> combinedKeys = new TreeSet<BigInteger>(keys);
			combinedKeys.addAll(forkKeys);
			
			//DELETE DELETED
			for(BigInteger deleted: this.deleted)
			{
				combinedKeys.remove(deleted);
			}
			
			//CONVERT SET BACK TO COLLECTION
			keys = combinedKeys;
		}
		
		return keys;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private Collection<BigInteger> getKeysHave(long have) {
		
		//FILTER ALL KEYS
		Collection<BigInteger> keys = ((BTreeMap<Tuple4, BigInteger>) this.haveWantKeyMap).subMap(
				Fun.t4(have, null, null, null),
				Fun.t4(have, Fun.HI(), Fun.HI(), Fun.HI())).values();

		return keys;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private Collection<BigInteger> getKeysWant(long want) {
		
		//FILTER ALL KEYS
		Collection<BigInteger> keys = ((BTreeMap<Tuple4, BigInteger>) this.wantHaveKeyMap).subMap(
				Fun.t4(want, null, null, null),
				Fun.t4(want, Fun.HI(), Fun.HI(), Fun.HI())).values();
		
		return keys;
	}
	
	public List<Order> getOrders(long haveWant) 
	{
		return getOrders(haveWant, false);
	}
	
	public List<Order> getOrders(long haveWant, boolean filter) 
	{
		Map<BigInteger, Boolean> orderKeys = new TreeMap<BigInteger, Boolean>();
		
		//FILTER ALL KEYS
		Collection<BigInteger> keys = this.getKeysHave(haveWant);
		
		for (BigInteger key : keys) {
			orderKeys.put(key, true);
		}
		
		keys = this.getKeysWant(haveWant);
		
		for (BigInteger key : keys) {
			orderKeys.put(key, true);
		}
		
		//GET ALL ORDERS FOR KEYS
		List<Order> orders = new ArrayList<Order>();

		for(Map.Entry<BigInteger, Boolean> orderKey : orderKeys.entrySet())
		{
			//Filters orders with unacceptably small amount. These orders have not worked
			if(filter){
				if(isExecutable(getDCSet(), orderKey.getKey()))
					orders.add(this.get(orderKey.getKey()));
			}
			else
			{
				orders.add(this.get(orderKey.getKey()));
			}
		}

		//IF THIS IS A FORK
		if(this.parent != null)
		{
			//RESORT ORDERS
			Collections.sort(orders);
		}

		//RETURN
		return orders;
	}

	public boolean isExecutable(DCSet db, BigInteger key) 
	{

		/* OLD
		Order order = this.get(key);
		
		BigDecimal increment = order.calculateBuyIncrement(order, db);
		BigDecimal amount = order.getAmountHaveLeft();
		amount = amount.subtract(amount.remainder(increment));
		return  (amount.compareTo(BigDecimal.ZERO) > 0);
		} else {
			
		}
		*/


		Order order = this.get(key);
		if (order.getAmountHaveLeft().compareTo(BigDecimal.ZERO) == 0)
			return false;
		BigDecimal price = order.getPriceCalcReverse();
		if ( !order.isWantDivisible(db)
				&&
				order.getAmountHaveLeft().compareTo(price) < 0)
			return false;
		
		/*
		BigDecimal thisPrice = order.getPriceCalc();
		boolean isReversePrice = thisPrice.compareTo(BigDecimal.ONE) < 0;
		//if (isReversePrice)
			//return order.getAmountHaveLeft().compareTo(order.getPriceCalc()) >= 0;
		
			
		//return BigDecimal.ONE.divide(order.getAmountHaveLeft(), 12,  RoundingMode.HALF_UP )
		//		.compareTo(order.getPriceCalcReverse()) >= 0;
		 */
		
		return order.isExecutable();
		
	}
	
	public List<Order> getOrders(long have, long want, boolean orderReverse) 
	{
		//FILTER ALL KEYS
		Collection<BigInteger> keys = this.getKeys(have, want);

		//GET ALL ORDERS FOR KEYS
		List<Order> orders = new ArrayList<Order>();

		if (false && orderReverse) {
			for(BigInteger key: keys)
			{
				orders.add((OrderReverse)this.get(key));
			}			
		} else {
			for(BigInteger key: keys)
			{
				orders.add(this.get(key));
			}
		}
		
		//IF THIS IS A FORK
		if(this.parent != null)
		{
			//RESORT ORDERS
			Collections.sort(orders);
		}
		
		//RETURN
		return orders;
	}
	
	public SortableList<BigInteger, Order> getOrdersSortableList(long have, long want)
	{
		//RETURN
		return getOrdersSortableList(have, want, false);
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public SortableList<BigInteger, Order> getOrdersSortableList(long have, long want, boolean filter)
	{
		//FILTER ALL KEYS
		Collection<BigInteger> keys = ((BTreeMap<Tuple4, BigInteger>) this.haveWantKeyMap).subMap(
				Fun.t4(have, want, null, null),
				Fun.t4(have, want, Fun.HI(), Fun.HI())).values();
				
		//Filters orders with unacceptably small amount. These orders have not worked
		if(filter){
			List<BigInteger> keys2 = new ArrayList<BigInteger>();
			
			DCSet db = getDCSet();
			Iterator<BigInteger> iter = keys.iterator();
			while (iter.hasNext()) {
				BigInteger key = iter.next();
				if(isExecutable(db, key))
					keys2.add(key);
			}
			keys = keys2;
		}
		
		//RETURN
		return new SortableList<BigInteger, Order>(this, keys);
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public SortableList<BigInteger, Order> getOrdersHaveSortableList(long have)
	{
		//FILTER ALL KEYS
		Collection<BigInteger> keys = ((BTreeMap<Tuple4, BigInteger>) this.haveWantKeyMap).subMap(
				Fun.t4(have, null, null, null),
				Fun.t4(have, Fun.HI(), Fun.HI(), Fun.HI())).values();
		
		//RETURN
		return new SortableList<BigInteger, Order>(this, keys);
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public SortableList<BigInteger, Order> getOrdersWantSortableList(long want)
	{
		//FILTER ALL KEYS
		Collection<BigInteger> keys = ((BTreeMap<Tuple4, BigInteger>) this.haveWantKeyMap).subMap(
				Fun.t4(null, want, null, null),
				Fun.t4(Fun.HI(), want, Fun.HI(), Fun.HI())).values();
		
		//RETURN
		return new SortableList<BigInteger, Order>(this, keys);
	}
	public Order get(BigInteger key)
	{
		Order order = super.get(key);
		if (order != null )
			order.setExecutable(true);
		else
			LOGGER.error("*** database.OrderMap.get(BigInteger) - key[" + key + "] not found!");

		
		return order;
	}


	public void delete(Order order) 
	{
		this.delete(order.getId());
	}
}
