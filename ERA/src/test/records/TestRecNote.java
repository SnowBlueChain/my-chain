package test.records;

import static org.junit.Assert.*;
import org.junit.Test;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
//import java.math.BigInteger;
//import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

//import java.util.List;
 import org.apache.log4j.Logger;

import ntp.NTP;


import core.account.PrivateKeyAccount;
import core.account.PublicKeyAccount;
import core.block.GenesisBlock;
import core.crypto.Crypto;
import core.item.assets.AssetCls;
import core.item.templates.Template;
import core.item.templates.TemplateCls;
import core.transaction.IssueTemplateRecord;
import core.transaction.R_SignNote;
import core.transaction.Transaction;
import core.transaction.TransactionFactory;
import datachain.DCSet;
import datachain.ItemTemplateMap;
import utils.Corekeys;
import webserver.WebResource;

public class TestRecNote {

	static Logger LOGGER = Logger.getLogger(TestRecNote.class.getName());

	Long releaserReference = null;

	boolean asPack = false;
	long FEE_KEY = AssetCls.FEE_KEY;
	long VOTE_KEY = AssetCls.ERA_KEY;
	byte FEE_POWER = (byte)1;
	byte[] noteReference = new byte[64];
	long timestamp = NTP.getTime();
	
	private byte[] icon = new byte[]{1,3,4,5,6,9}; // default value
	private byte[] image = new byte[]{4,11,32,23,45,122,11,-45}; // default value

	byte[] data = "test123!".getBytes();
	byte[] isText = new byte[] { 1 };
	byte[] encrypted = new byte[] { 0 };

	//CREATE EMPTY MEMORY DATABASE
	private DCSet db;
	private GenesisBlock gb;
	
	//CREATE KNOWN ACCOUNT
	byte[] seed = Crypto.getInstance().digest("test".getBytes());
	byte[] privateKey = Crypto.getInstance().createKeyPair(seed).getA();
	PrivateKeyAccount maker = new PrivateKeyAccount(privateKey);
	TemplateCls note;
	long noteKey = -1;
	IssueTemplateRecord issueTemplateRecord;
	R_SignNote signNoteRecord;

	
	ItemTemplateMap noteMap;

	// INIT NOTES
	private void init() {
		
		db = DCSet.createEmptyDatabaseSet();
		noteMap = db.getItemTemplateMap();
		
		gb = new GenesisBlock();
		try {
			gb.process(db);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		// FEE FUND
		maker.setLastTimestamp(gb.getTimestamp(db), db);
		maker.changeBalance(db, false, FEE_KEY, BigDecimal.valueOf(1).setScale(8), false);

	}
	private void initNote(boolean process) {
		
		note = new Template(maker, "test132", icon, image, "12345678910strontje");
				
		//CREATE ISSUE PLATE TRANSACTION
		issueTemplateRecord = new IssueTemplateRecord(maker, note, FEE_POWER, timestamp, maker.getLastTimestamp(db));
		issueTemplateRecord.sign(maker, false);
		if (process) {
			issueTemplateRecord.process(db, gb, false);
			noteKey = note.getKey(db);
		}
	}
	
	@Test
	public void testAddreessVersion() 
	{
		int vers = Corekeys.findAddressVersion("E");
		assertEquals(-1111, vers);
	}
	
	//ISSUE PLATE TRANSACTION
	
	@Test
	public void validateSignatureIssueNoteTransaction() 
	{
		
		init();
		
		initNote(false);
		
		//CHECK IF ISSUE PLATE TRANSACTION IS VALID
		assertEquals(true, issueTemplateRecord.isSignatureValid());
		
		//INVALID SIGNATURE
		issueTemplateRecord = new IssueTemplateRecord(maker, note, FEE_POWER, timestamp, maker.getLastTimestamp(db), new byte[64]);
		
		//CHECK IF ISSUE PLATE IS INVALID
		assertEquals(false, issueTemplateRecord.isSignatureValid());
	}
		

	
	@Test
	public void parseIssueNoteTransaction() 
	{
		
		init();
		
		TemplateCls note = new Template(maker, "test132", icon, image, "12345678910strontje");
		byte[] raw = note.toBytes(false, false);
		assertEquals(raw.length, note.getDataLength(false));
				
		//CREATE ISSUE PLATE TRANSACTION
		IssueTemplateRecord issueTemplateRecord = new IssueTemplateRecord(maker, note, FEE_POWER, timestamp, maker.getLastTimestamp(db));
		issueTemplateRecord.sign(maker, false);
		issueTemplateRecord.process(db, gb, false);
		
		//CONVERT TO BYTES
		byte[] rawIssueNoteTransaction = issueTemplateRecord.toBytes(true, null);
		
		//CHECK DATA LENGTH
		assertEquals(rawIssueNoteTransaction.length, issueTemplateRecord.getDataLength(false));
		
		try 
		{	
			//PARSE FROM BYTES
			IssueTemplateRecord parsedIssueNoteTransaction = (IssueTemplateRecord) TransactionFactory.getInstance().parse(rawIssueNoteTransaction, releaserReference);
			LOGGER.info("parsedIssueNoteTransaction: " + parsedIssueNoteTransaction);

			//CHECK INSTANCE
			assertEquals(true, parsedIssueNoteTransaction instanceof IssueTemplateRecord);
			
			//CHECK SIGNATURE
			assertEquals(true, Arrays.equals(issueTemplateRecord.getSignature(), parsedIssueNoteTransaction.getSignature()));
			
			//CHECK ISSUER
			assertEquals(issueTemplateRecord.getCreator().getAddress(), parsedIssueNoteTransaction.getCreator().getAddress());
			
			//CHECK OWNER
			assertEquals(issueTemplateRecord.getItem().getOwner().getAddress(), parsedIssueNoteTransaction.getItem().getOwner().getAddress());
			
			//CHECK NAME
			assertEquals(issueTemplateRecord.getItem().getName(), parsedIssueNoteTransaction.getItem().getName());
				
			//CHECK DESCRIPTION
			assertEquals(issueTemplateRecord.getItem().getDescription(), parsedIssueNoteTransaction.getItem().getDescription());
							
			//CHECK FEE
			assertEquals(issueTemplateRecord.getFee(), parsedIssueNoteTransaction.getFee());	
			
			//CHECK REFERENCE
			//assertEquals(issueTemplateRecord.getReference(), parsedIssueNoteTransaction.getReference());	
			
			//CHECK TIMESTAMP
			assertEquals(issueTemplateRecord.getTimestamp(), parsedIssueNoteTransaction.getTimestamp());				
		}
		catch (Exception e) 
		{
			fail("Exception while parsing transaction. " + e);
		}
		
	}

	
	@Test
	public void processIssueNoteTransaction()
	{
		
		init();				
		
		Template template = new Template(maker, "test", icon, image, "strontje");
				
		//CREATE ISSUE PLATE TRANSACTION
		IssueTemplateRecord issueTemplateRecord = new IssueTemplateRecord(maker, template, FEE_POWER, timestamp, maker.getLastTimestamp(db));
		
		assertEquals(Transaction.VALIDATE_OK, issueTemplateRecord.isValid(db, releaserReference));
		
		issueTemplateRecord.sign(maker, false);
		issueTemplateRecord.process(db, gb, false);
		int mapSize = noteMap.size();
		
		LOGGER.info("template KEY: " + template.getKey(db));
				
		//CHECK PLATE EXISTS SENDER
		long key = db.getIssueTemplateMap().get(issueTemplateRecord);
		assertEquals(true, noteMap.contains(key));
		
		TemplateCls note_2 = new Template(maker, "test132_2", icon, image, "2_12345678910strontje");				
		IssueTemplateRecord issueNoteTransaction_2 = new IssueTemplateRecord(maker, note_2, FEE_POWER, timestamp+10, maker.getLastTimestamp(db));
		issueNoteTransaction_2.sign(maker, false);
		issueNoteTransaction_2.process(db, gb, false);
		LOGGER.info("note_2 KEY: " + note_2.getKey(db));
		issueNoteTransaction_2.orphan(db, false);
		assertEquals(mapSize, noteMap.size());
		
		//CHECK PLATE IS CORRECT
		assertEquals(true, Arrays.equals(noteMap.get(key).toBytes(true, false), template.toBytes(true, false)));
					
		//CHECK REFERENCE SENDER
		assertEquals(issueTemplateRecord.getTimestamp(), maker.getLastTimestamp(db));
	}
	
	
	@Test
	public void orphanIssueNoteTransaction()
	{
		
		init();
				
		Template template = new Template(maker, "test", icon, image, "strontje");
				
		//CREATE ISSUE PLATE TRANSACTION
		IssueTemplateRecord issueTemplateRecord = new IssueTemplateRecord(maker, template, FEE_POWER, timestamp, maker.getLastTimestamp(db));
		issueTemplateRecord.sign(maker, false);
		issueTemplateRecord.process(db, gb, false);
		long key = db.getIssueTemplateMap().get(issueTemplateRecord);
		assertEquals(issueTemplateRecord.getTimestamp(), maker.getLastTimestamp(db));
		
		issueTemplateRecord.orphan(db, false);
				
		//CHECK PLATE EXISTS SENDER
		assertEquals(false, noteMap.contains(key));
						
		//CHECK REFERENCE SENDER
		//assertEquals(issueTemplateRecord.getReference(), maker.getLastReference(db));
	}
	
	// TODO - in statement - valid on key = 999

	//SIGN PLATE TRANSACTION
	
	@Test
	public void validateSignatureSignNoteTransaction() 
	{
		
		init();
		
		initNote(true);
		
		signNoteRecord = new R_SignNote(maker, FEE_POWER, noteKey, data, isText, encrypted, timestamp+10, maker.getLastTimestamp(db));
		signNoteRecord.sign(maker, asPack);
		
		//CHECK IF ISSUE PLATE TRANSACTION IS VALID
		assertEquals(true, signNoteRecord.isSignatureValid());
		
		//INVALID SIGNATURE
		signNoteRecord = new R_SignNote(maker, FEE_POWER, noteKey, data, isText, encrypted, timestamp+10, maker.getLastTimestamp(db), new byte[64]);
		
		//CHECK IF ISSUE PLATE IS INVALID
		assertEquals(false, signNoteRecord.isSignatureValid());
	}
		

	
	@Test
	public void parseSignNoteTransaction() 
	{
		
		init();
		
		initNote(true);
		
		signNoteRecord = new R_SignNote(maker, FEE_POWER, noteKey, data, isText, encrypted, timestamp+10, maker.getLastTimestamp(db));
		signNoteRecord.sign(maker, asPack);
		
		//CONVERT TO BYTES
		byte[] rawSignNoteRecord = signNoteRecord.toBytes(true, null);
		
		//CHECK DATA LENGTH
		assertEquals(rawSignNoteRecord.length, signNoteRecord.getDataLength(false));
		
		try 
		{	
			//PARSE FROM BYTES
			R_SignNote parsedSignNoteRecord = (R_SignNote) TransactionFactory.getInstance().parse(rawSignNoteRecord, releaserReference);
			LOGGER.info("parsedSignNote: " + parsedSignNoteRecord);

			//CHECK INSTANCE
			assertEquals(true, parsedSignNoteRecord instanceof R_SignNote);
			
			//CHECK SIGNATURE
			assertEquals(true, Arrays.equals(signNoteRecord.getSignature(), parsedSignNoteRecord.getSignature()));
			
			//CHECK ISSUER
			assertEquals(signNoteRecord.getCreator().getAddress(), parsedSignNoteRecord.getCreator().getAddress());
			
			//CHECK OWNER
			assertEquals(signNoteRecord.getKey(), parsedSignNoteRecord.getKey());
			
			//CHECK NAME
			assertEquals(true, Arrays.equals(signNoteRecord.getData(), parsedSignNoteRecord.getData()));
				
			//CHECK DESCRIPTION
			assertEquals(signNoteRecord.isText(), parsedSignNoteRecord.isText());
							
			//CHECK FEE
			assertEquals(signNoteRecord.getFee(), parsedSignNoteRecord.getFee());	
			
			//CHECK REFERENCE
			//assertEquals(signNoteRecord.getReference(), parsedSignNoteRecord.getReference());	
			
			//CHECK TIMESTAMP
			assertEquals(signNoteRecord.getTimestamp(), parsedSignNoteRecord.getTimestamp());				
		}
		catch (Exception e) 
		{
			fail("Exception while parsing transaction. " + e);
		}

		
		// NOT DATA
		data = null;
		signNoteRecord = new R_SignNote(maker, FEE_POWER, noteKey, data, isText, encrypted, timestamp+20, maker.getLastTimestamp(db));
		signNoteRecord.sign(maker, asPack);
		
		//CONVERT TO BYTES
		rawSignNoteRecord = signNoteRecord.toBytes(true, null);
		
		//CHECK DATA LENGTH
		assertEquals(rawSignNoteRecord.length, signNoteRecord.getDataLength(false));
		
		try 
		{	
			//PARSE FROM BYTES
			R_SignNote parsedSignNoteRecord = (R_SignNote) TransactionFactory.getInstance().parse(rawSignNoteRecord, releaserReference);
			LOGGER.info("parsedSignNote: " + parsedSignNoteRecord);

			//CHECK INSTANCE
			assertEquals(true, parsedSignNoteRecord instanceof R_SignNote);
			
			//CHECK SIGNATURE
			assertEquals(true, Arrays.equals(signNoteRecord.getSignature(), parsedSignNoteRecord.getSignature()));
			
			//CHECK ISSUER
			assertEquals(signNoteRecord.getCreator().getAddress(), parsedSignNoteRecord.getCreator().getAddress());
			
			//CHECK OWNER
			assertEquals(signNoteRecord.getKey(), parsedSignNoteRecord.getKey());
			
			//CHECK NAME
			assertEquals(null, parsedSignNoteRecord.getData());
				
			//CHECK DESCRIPTION
			assertEquals(signNoteRecord.isText(), parsedSignNoteRecord.isText());
							
			//CHECK FEE
			assertEquals(signNoteRecord.getFee(), parsedSignNoteRecord.getFee());	
			
			//CHECK REFERENCE
			//assertEquals(signNoteRecord.getReference(), parsedSignNoteRecord.getReference());	
			
			//CHECK TIMESTAMP
			assertEquals(signNoteRecord.getTimestamp(), parsedSignNoteRecord.getTimestamp());				
		}
		catch (Exception e) 
		{
			fail("Exception while parsing transaction. " + e);
		}

		// NOT KEY
		//data = null;
		noteKey = 0;
		signNoteRecord = new R_SignNote(maker, FEE_POWER, noteKey, data, isText, encrypted, timestamp+20, maker.getLastTimestamp(db));
		signNoteRecord.sign(maker, asPack);
		
		//CONVERT TO BYTES
		rawSignNoteRecord = signNoteRecord.toBytes(true, null);
		
		//CHECK DATA LENGTH
		assertEquals(rawSignNoteRecord.length, signNoteRecord.getDataLength(false));
		
		try 
		{	
			//PARSE FROM BYTES
			R_SignNote parsedSignNoteRecord = (R_SignNote) TransactionFactory.getInstance().parse(rawSignNoteRecord, releaserReference);
			LOGGER.info("parsedSignNote: " + parsedSignNoteRecord);

			//CHECK INSTANCE
			assertEquals(true, parsedSignNoteRecord instanceof R_SignNote);
			
			//CHECK SIGNATURE
			assertEquals(true, Arrays.equals(signNoteRecord.getSignature(), parsedSignNoteRecord.getSignature()));
			
			//CHECK ISSUER
			assertEquals(signNoteRecord.getCreator().getAddress(), parsedSignNoteRecord.getCreator().getAddress());
			
			//CHECK OWNER
			assertEquals(signNoteRecord.getKey(), parsedSignNoteRecord.getKey());
			
			//CHECK NAME
			assertEquals(null, parsedSignNoteRecord.getData());
				
			//CHECK DESCRIPTION
			assertEquals(signNoteRecord.isText(), parsedSignNoteRecord.isText());
							
			//CHECK FEE
			assertEquals(signNoteRecord.getFee(), parsedSignNoteRecord.getFee());	
			
			//CHECK REFERENCE
			//assertEquals(signNoteRecord.getReference(), parsedSignNoteRecord.getReference());	
			
			//CHECK TIMESTAMP
			assertEquals(signNoteRecord.getTimestamp(), parsedSignNoteRecord.getTimestamp());				
		}
		catch (Exception e) 
		{
			fail("Exception while parsing transaction. " + e);
		}

		// NOT KEY
		data = null;
		noteKey = 0;
		signNoteRecord = new R_SignNote(maker, FEE_POWER, noteKey, data, isText, encrypted, timestamp+20, maker.getLastTimestamp(db));
		signNoteRecord.sign(maker, asPack);
		
		//CONVERT TO BYTES
		rawSignNoteRecord = signNoteRecord.toBytes(true, null);
		
		//CHECK DATA LENGTH
		assertEquals(rawSignNoteRecord.length, signNoteRecord.getDataLength(false));
		
		try 
		{	
			//PARSE FROM BYTES
			R_SignNote parsedSignNoteRecord = (R_SignNote) TransactionFactory.getInstance().parse(rawSignNoteRecord, releaserReference);
			LOGGER.info("parsedSignNote: " + parsedSignNoteRecord);

			//CHECK INSTANCE
			assertEquals(true, parsedSignNoteRecord instanceof R_SignNote);
			
			//CHECK SIGNATURE
			assertEquals(true, Arrays.equals(signNoteRecord.getSignature(), parsedSignNoteRecord.getSignature()));
			
			//CHECK ISSUER
			assertEquals(signNoteRecord.getCreator().getAddress(), parsedSignNoteRecord.getCreator().getAddress());
			
			//CHECK OWNER
			assertEquals(signNoteRecord.getKey(), parsedSignNoteRecord.getKey());
			
			//CHECK NAME
			assertEquals(null, parsedSignNoteRecord.getData());
				
			//CHECK DESCRIPTION
			assertEquals(signNoteRecord.isText(), parsedSignNoteRecord.isText());
							
			//CHECK FEE
			assertEquals(signNoteRecord.getFee(), parsedSignNoteRecord.getFee());	
			
			//CHECK REFERENCE
			//assertEquals(signNoteRecord.getReference(), parsedSignNoteRecord.getReference());	
			
			//CHECK TIMESTAMP
			assertEquals(signNoteRecord.getTimestamp(), parsedSignNoteRecord.getTimestamp());				
		}
		catch (Exception e) 
		{
			fail("Exception while parsing transaction. " + e);
		}

	}

	
	@Test
	public void processSignNoteTransaction()
	{
		
		init();
		
		initNote(true);
		
		signNoteRecord = new R_SignNote(maker, FEE_POWER, noteKey, data, isText, encrypted, timestamp+10, maker.getLastTimestamp(db));
		
		assertEquals(Transaction.VALIDATE_OK, signNoteRecord.isValid(db, releaserReference));
		
		signNoteRecord.sign(maker, false);
		signNoteRecord.process(db, gb, false);
							
		//CHECK REFERENCE SENDER
		assertEquals(signNoteRecord.getTimestamp(), maker.getLastTimestamp(db));	
			
		///// ORPHAN
		signNoteRecord.orphan(db, false);
										
		//CHECK REFERENCE SENDER
		//assertEquals(signNoteRecord.getReference(), maker.getLastReference(db));
	}

	private List<String> imagelinks = new ArrayList<String>();

	private void handleVars(String description) {
		Pattern pattern = Pattern.compile(Pattern.quote("{{") + "(.+?)" + Pattern.quote("}}"));
		//Pattern pattern = Pattern.compile("{{(.+)}}");
		Matcher matcher = pattern.matcher(description);
		while (matcher.find()) {
			String url = matcher.group(1);
			imagelinks.add(url);
			//description = description.replace(matcher.group(), getImgHtml(url));
		}
	}
	@Test
	public void regExTest()
	{
		String descr = "AJH {{wer}}, asdj {{we431!12}}";
		handleVars(descr);
		assertEquals(imagelinks, "");
	}


}
