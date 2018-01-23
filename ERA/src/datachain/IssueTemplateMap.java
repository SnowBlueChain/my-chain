package datachain;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import org.mapdb.BTreeKeySerializer;
import org.mapdb.DB;

import com.google.common.primitives.UnsignedBytes;

import core.transaction.Transaction;
import datachain.DCSet;
import datachain.Issue_ItemMap;

public class IssueTemplateMap extends Issue_ItemMap 
{
	
	public IssueTemplateMap(DCSet databaseSet, DB database)
	{
		super(databaseSet, database, "template");
	}

	public IssueTemplateMap(IssueTemplateMap parent) 
	{
		super(parent);
	}
}