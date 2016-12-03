package gui.items.documents;

import java.util.Date;
import java.util.Stack;

import javax.swing.table.DefaultTableModel;

import org.mapdb.Fun.Tuple3;

import core.crypto.Base58;
import core.item.imprints.ImprintCls;
import core.transaction.Transaction;
import database.DBSet;
import database.HashesSignsMap;
import database.SortableList;
import lang.Lang;

public class Model_Hashes_info extends DefaultTableModel {

	public static final int COLUMN_KEY = 0;
	public static final int COLUMN_NAME = 1;
	public static final int COLUMN_ADDRESS = 2;
	// public static final int COLUMN_AMOUNT = 3;
	// public static final int COLUMN_DIVISIBLE = 4;
	Stack<Tuple3<Long, Integer, Integer>> hashs;
	DBSet db;

	private String[] columnNames = Lang.getInstance().translate(new String[] { "Date", "Block", "Owner" });// ,
																											// "Quantity"});//,
																											// "Divisible"});

	public Model_Hashes_info(String aa) {

		db = DBSet.getInstance();
		HashesSignsMap map = db.getHashesSignsMap();
		byte[] a = Base58.decode(aa);
		hashs = map.get(a);

	}
	
	
	
	@Override
	public int getColumnCount() 
	{
		return 3;
	}
	
	@Override
	public String getColumnName(int index) 
	{
		return this.columnNames[index];
	}

	@Override
	public int getRowCount() 
	{
		return 10;
		
	}
	
	
	
	
	
	@Override
	public Object getValueAt(int row, int column) 
	{
		if(hashs == null || row > hashs.size() - 1 )
		{
			return null;
		}
		
		Tuple3<Long, Integer, Integer> ss = hashs.get(row);
		 Transaction tt = db.getTransactionFinalMap().getTransaction(ss.b,1);
		
		switch(column)
		{
		case COLUMN_KEY:
			
			return new Date(tt.getTimestamp());
		
		case COLUMN_NAME:
			
			return ss.b;
		
		case COLUMN_ADDRESS:
			
			return tt.getCreator();
			
		}
		
		return null;
	}

}
