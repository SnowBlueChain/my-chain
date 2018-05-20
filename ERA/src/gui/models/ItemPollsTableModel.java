package gui.models;

import java.math.BigDecimal;
import java.util.Observable;
import java.util.Observer;

import javax.validation.constraints.Null;

import controller.Controller;
import core.item.ItemCls;
import core.item.assets.AssetCls;
import core.item.polls.PollCls;
import datachain.DCSet;
import datachain.ItemPollMap;
import datachain.SortableList;
import lang.Lang;
import utils.ObserverMessage;

@SuppressWarnings("serial")
public class ItemPollsTableModel extends TableModelCls<Long, ItemCls> implements Observer
{
	public static final int COLUMN_NAME = 0;
	private static final int COLUMN_CREATOR = 1;
	public static final int COLUMN_VOTES = 2;
	private AssetCls asset;
	
	private String[] columnNames = Lang.getInstance().translate(new String[]{"Name", "Creator", "Total Votes"});
	private SortableList<Long, ItemCls> polls;
	private ItemPollMap map;
	
	public ItemPollsTableModel()
	{
		this.asset = Controller.getInstance().getAsset(AssetCls.FEE_KEY);
		//Controller.getInstance().addObserver(this);
		map = DCSet.getInstance().getItemPollMap();
		polls = map.getList();
	}
	
	public void setAsset(AssetCls asset) 
	{
		this.asset = asset;
		this.fireTableDataChanged();
	}
	
	public Class<? extends Object> getColumnClass(int c) {     // set column type
		Object o = getValueAt(0, c);
		return o==null?Null.class:o.getClass();
    }
	
	@Override
	public SortableList<Long, ItemCls> getSortableList() {
		return this.polls;
	}
	
	public PollCls getPoll(int row)
	{
		return (PollCls)this.polls.get(row).getB();
	}
	
	@Override
	public int getColumnCount() 
	{
		return this.columnNames.length;
	}
	
	@Override
	public String getColumnName(int index) 
	{
		return this.columnNames[index];
	}

	@Override
	public int getRowCount() 
	{
		 return (this.polls == null)? 0 : this.polls.size();
	}

	@Override
	public Object getValueAt(int row, int column) 
	{
		if(this.polls == null || row > this.polls.size() - 1 )
		{
			return null;
		}
		
		PollCls poll = (PollCls)this.polls.get(row).getB();
		
		switch(column)
		{
		case COLUMN_NAME:
			
			String key = poll.getName();
			
			//CHECK IF ENDING ON A SPACE
			if(key.endsWith(" "))
			{
				key = key.substring(0, key.length()-1);
				key += ".";
			}
			
			return key;
		
		case COLUMN_CREATOR:
			
			return poll.getOwner().getPersonAsString();
			
		case COLUMN_VOTES:
			
			BigDecimal amo = poll.getTotalVotes(DCSet.getInstance(), this.asset.getKey(DCSet.getInstance()));
			if (amo == null)
				return BigDecimal.ZERO;
			return amo;
			
			
		}
		
		return null;
	}

	@Override
	public void update(Observable o, Object arg) 
	{	
		try
		{
			this.syncUpdate(o, arg);
		}
		catch(Exception e)
		{
			//GUI ERROR
		}
	}
	
	@SuppressWarnings("unchecked")
	public synchronized void syncUpdate(Observable o, Object arg)
	{
		ObserverMessage message = (ObserverMessage) arg;
		
		//CHECK IF NEW LIST
		if(message.getType() == ObserverMessage.LIST_POLL_TYPE)
		{			
			if(this.polls == null)
			{
				this.polls = (SortableList<Long, ItemCls>) message.getValue();
				this.polls.registerObserver();
			}	
				
			this.fireTableDataChanged();
		}
				
		//CHECK IF LIST UPDATED
		if(message.getType() == ObserverMessage.ADD_POLL_TYPE || message.getType() == ObserverMessage.REMOVE_POLL_TYPE)
		{
			this.fireTableDataChanged();
		}
	}
	
	public void removeObservers() 
	{
		//if(this.polls!=null)this.polls.removeObserver();
		//DCSet.getInstance().getPollMap().deleteObserver(this);
	}

	@Override
	public Object getItem(int k) {
		// TODO Auto-generated method stub
		return this.polls.get(k).getB();
	}
}
