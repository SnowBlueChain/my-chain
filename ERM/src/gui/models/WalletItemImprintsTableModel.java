package gui.models;
////////
import java.util.Observable;
import java.util.Observer;

import org.mapdb.Fun.Tuple2;

import utils.ObserverMessage;
import controller.Controller;
import core.item.imprints.ImprintCls;
import database.SortableList;
import lang.Lang;

@SuppressWarnings("serial")
public class WalletItemImprintsTableModel extends TableModelCls<Tuple2<String, String>, ImprintCls> implements Observer
{
	public static final int COLUMN_KEY = 0;
	public static final int COLUMN_NAME = 1;
	public static final int COLUMN_ADDRESS = 2;
	public static final int COLUMN_CONFIRMED = 3;
	
	private SortableList<Tuple2<String, String>, ImprintCls> imprints;
	
	private String[] columnNames = Lang.getInstance().translate(new String[]{"Key", "Name", "Owner", "Confirmed"});
	
	public WalletItemImprintsTableModel()
	{
		Controller.getInstance().addWalletListener(this);
	}
	
	@Override
	public SortableList<Tuple2<String, String>, ImprintCls> getSortableList() {
		return this.imprints;
	}
	
	public ImprintCls getItem(int row)
	{
		return this.imprints.get(row).getB();
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
		 return this.imprints.size();
	}

	@Override
	public Object getValueAt(int row, int column) 
	{
		if(this.imprints == null || row > this.imprints.size() - 1 )
		{
			return null;
		}
		
		ImprintCls imprint = this.imprints.get(row).getB();
		
		switch(column)
		{
		case COLUMN_KEY:
			
			return imprint.getKey();
		
		case COLUMN_NAME:
			
			return imprint.getName();
		
		case COLUMN_ADDRESS:
			
			return imprint.getCreator().getAddress();
						
		case COLUMN_CONFIRMED:
			
			return imprint.isConfirmed();
			
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
		if(message.getType() == ObserverMessage.LIST_IMPRINT_TYPE)
		{
			if(this.imprints == null)
			{
				this.imprints = (SortableList<Tuple2<String, String>, ImprintCls>) message.getValue();
				this.imprints.registerObserver();
				//this.imprints.sort(PollMap.NAME_INDEX);
			}
			
			this.fireTableDataChanged();
		}
		
		//CHECK IF LIST UPDATED
		if(message.getType() == ObserverMessage.ADD_IMPRINT_TYPE || message.getType() == ObserverMessage.REMOVE_IMPRINT_TYPE)
		{
			this.fireTableDataChanged();
		}	
	}
}
