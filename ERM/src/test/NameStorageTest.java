package test;

import static org.junit.Assert.*;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;

import ntp.NTP;

import org.json.simple.JSONObject;
import org.junit.Before;
import org.junit.Test;

import qora.account.PrivateKeyAccount;
import qora.crypto.Crypto;
import qora.naming.Name;
import qora.transaction.ArbitraryTransaction;
import qora.transaction.ArbitraryTransactionV3;
import qora.transaction.GenesisTransaction;
import qora.transaction.RegisterNameTransaction;
import qora.transaction.Transaction;
import utils.Pair;
import utils.Qorakeys;
import utils.StorageUtils;
import database.DBSet;

@SuppressWarnings("unchecked")
public class NameStorageTest {

	private DBSet databaseSet;
	private PrivateKeyAccount sender;

	@Before
	public void setup() {
		//Ed25519.load();

		databaseSet = DBSet.createEmptyDatabaseSet();

		// CREATE KNOWN ACCOUNT
		byte[] seed = Crypto.getInstance().digest("test".getBytes());
		byte[] privateKey = Crypto.getInstance().createKeyPair(seed).getA();
		sender = new PrivateKeyAccount(privateKey);

		// CREATE KNOWN ACCOUNT
		seed = Crypto.getInstance().digest("buyer".getBytes());
		privateKey = Crypto.getInstance().createKeyPair(seed).getA();
		PrivateKeyAccount buyer = new PrivateKeyAccount(privateKey);

		// PROCESS GENESIS TRANSACTION TO MAKE SURE SENDER HAS FUNDS
		Transaction transaction = new GenesisTransaction(sender, BigDecimal
				.valueOf(1000).setScale(8), NTP.getTime());
		transaction.process(databaseSet);

		// PROCESS GENESIS TRANSACTION TO MAKE SURE BUYER HAS FUNDS
		transaction = new GenesisTransaction(buyer, BigDecimal.valueOf(1000)
				.setScale(8), NTP.getTime());
		transaction.process(databaseSet);

		// CREATE SIGNATURE
		long timestamp = NTP.getTime();
		Name name = new Name(sender, "drizzt", "this is the value");

		// CREATE NAME REGISTRATION
		Transaction nameRegistration = new RegisterNameTransaction(sender,
				name, (byte)0, timestamp,
				sender.getLastReference(databaseSet));
		//nameRegistration.sign(sender);
		

		// CHECK IF NAME REGISTRATION IS VALID
		assertEquals(Transaction.VALIDATE_OK,
				nameRegistration.isValid(databaseSet));
		nameRegistration.process(databaseSet);
	}

	@Test
	public void testNameStorageNotChangedIfNotOwner() throws Exception {
		long timestamp = NTP.getTime();

		// We have nothing in name storage for drizzt here.
		assertNull(databaseSet.getNameStorageMap().getOpt("drizzt",
				Qorakeys.PROFILEENABLE.toString()));
		assertNull(databaseSet.getNameStorageMap().get("drizzt"));

		JSONObject storageJsonObject = StorageUtils.getStorageJsonObject(
				Collections.singletonList(new Pair<String, String>(
						Qorakeys.PROFILEENABLE.toString(), "yes")), null, null,
				null, null, null);
		storageJsonObject.put("name", "drizzt");
		byte[] data = storageJsonObject.toString().getBytes();

		// ADDING KEY COMPLETE WITH YES
		ArbitraryTransactionV3 arbitraryTransaction = new ArbitraryTransactionV3(
				sender, null, 10, data, (byte)0, timestamp,
				sender.getLastReference(databaseSet));
		arbitraryTransaction.sign(sender);
		
		arbitraryTransaction.process(databaseSet);

		// KEY IS THERE!
		assertEquals(
				"yes",
				databaseSet.getNameStorageMap().getOpt("drizzt",
						Qorakeys.PROFILEENABLE.toString()));
		
		
		byte[] seed = Crypto.getInstance().digest("test2".getBytes());
		byte[] privateKey = Crypto.getInstance().createKeyPair(seed).getA();
		 PrivateKeyAccount badSender = new PrivateKeyAccount(privateKey);
		 
		 
		  storageJsonObject = StorageUtils.getStorageJsonObject(
					null, Arrays.asList(
							Qorakeys.PROFILEENABLE.toString()),null,
					null, null, null);
			storageJsonObject.put("name", "drizzt");
			 data = storageJsonObject.toString().getBytes();

			// ADDING KEY COMPLETE WITH YES
			arbitraryTransaction = new ArbitraryTransactionV3(
					badSender, null, 10, data, (byte)0, timestamp,
					badSender.getLastReference(databaseSet));
			
			
			arbitraryTransaction.process(databaseSet);

			// KEY IS STILL THERE!
			assertEquals(
					"yes",
					databaseSet.getNameStorageMap().getOpt("drizzt",
							Qorakeys.PROFILEENABLE.toString()));
		
		
		
	}

	@Test
	public void testAddRemoveComplete() throws Exception {

		long timestamp = NTP.getTime();

		// We have nothing in name storage for drizzt here.
		assertNull(databaseSet.getNameStorageMap().getOpt("drizzt",
				Qorakeys.PROFILEENABLE.toString()));
		assertNull(databaseSet.getNameStorageMap().get("drizzt"));

		JSONObject storageJsonObject = StorageUtils.getStorageJsonObject(
				Collections.singletonList(new Pair<String, String>(
						Qorakeys.PROFILEENABLE.toString(), "yes")), null, null,
				null, null, null);
		storageJsonObject.put("name", "drizzt");
		byte[] data = storageJsonObject.toString().getBytes();

		// ADDING KEY COMPLETE WITH YES
		ArbitraryTransaction arbitraryTransaction = new ArbitraryTransactionV3(
				sender, null, 10, data, (byte)0, timestamp,
				sender.getLastReference(databaseSet));
		arbitraryTransaction.sign(sender);
		arbitraryTransaction.process(databaseSet);

		// KEY IS THERE!
		assertEquals(
				"yes",
				databaseSet.getNameStorageMap().getOpt("drizzt",
						Qorakeys.PROFILEENABLE.toString()));

		// CHANGING KEY

		storageJsonObject = StorageUtils.getStorageJsonObject(Collections
				.singletonList(new Pair<String, String>(Qorakeys.PROFILEENABLE
						.toString(), "anothervalue")), null, null, null, null, null);
		storageJsonObject.put("name", "drizzt");
		data = storageJsonObject.toString().getBytes();

		arbitraryTransaction = new ArbitraryTransactionV3(sender, null, 10, data,
				(byte)0, timestamp,
				sender.getLastReference(databaseSet));
		arbitraryTransaction.sign(sender);
		arbitraryTransaction.process(databaseSet);

		// NEW KEY IS THERE!
		assertEquals(
				"anothervalue",
				databaseSet.getNameStorageMap().getOpt("drizzt",
						Qorakeys.PROFILEENABLE.toString()));

		// REMOVING KEY COMPLETE

		storageJsonObject = StorageUtils.getStorageJsonObject(null,
				Arrays.asList(Qorakeys.PROFILEENABLE.toString()), null, null,
				null, null);
		storageJsonObject.put("name", "drizzt");
		data = storageJsonObject.toString().getBytes();

		arbitraryTransaction = new ArbitraryTransactionV3(sender, null, 10, data,
				(byte)0, timestamp,
				sender.getLastReference(databaseSet));
		
		arbitraryTransaction.process(databaseSet);
		arbitraryTransaction.sign(sender);

		assertNull(databaseSet.getNameStorageMap().getOpt("drizzt",
				Qorakeys.PROFILEENABLE.toString()));

	}

	@Test
	public void testAddRemoveListKeys() throws Exception {

		long timestamp = NTP.getTime();

		// We have nothing in name storage for drizzt here.
		assertNull(databaseSet.getNameStorageMap().getOpt("drizzt",
				Qorakeys.PROFILELIKEPOSTS.toString()));
		assertNull(databaseSet.getNameStorageMap().get("drizzt"));

		JSONObject storageJsonObject = StorageUtils.getStorageJsonObject(null,
				null, Collections.singletonList(new Pair<String, String>(
						Qorakeys.PROFILELIKEPOSTS.toString(), "skerberus")),
				null, null, null);
		storageJsonObject.put("name", "drizzt");
		byte[] data = storageJsonObject.toString().getBytes();

		// ADDING Skerberus as List key
		ArbitraryTransaction arbitraryTransaction = new ArbitraryTransactionV3(
				sender, null, 10, data, (byte)0, timestamp,
				sender.getLastReference(databaseSet));
		arbitraryTransaction.sign(sender);
		arbitraryTransaction.process(databaseSet);

		assertEquals(
				"skerberus",
				databaseSet.getNameStorageMap().getOpt("drizzt",
						Qorakeys.PROFILELIKEPOSTS.toString()));

		storageJsonObject = StorageUtils.getStorageJsonObject(null, null,
				Collections.singletonList(new Pair<String, String>(
						Qorakeys.PROFILELIKEPOSTS.toString(), "vrontis")),
				null, null, null);
		storageJsonObject.put("name", "drizzt");
		data = storageJsonObject.toString().getBytes();

		// ADDING vrontis as List key
		arbitraryTransaction = new ArbitraryTransactionV3(sender, null, 10, data,
				(byte)0, timestamp,
				sender.getLastReference(databaseSet));
		arbitraryTransaction.sign(sender);
		
		arbitraryTransaction.process(databaseSet);

		// KEY IS THERE!
		assertEquals("skerberus;vrontis", databaseSet.getNameStorageMap()
				.getOpt("drizzt", Qorakeys.PROFILELIKEPOSTS.toString()));

		// removing step by step!

		storageJsonObject = StorageUtils.getStorageJsonObject(null, null, null,
				Collections.singletonList(new Pair<String, String>(
						Qorakeys.PROFILELIKEPOSTS.toString(), "skerberus")),
				null, null);
		storageJsonObject.put("name", "drizzt");
		data = storageJsonObject.toString().getBytes();

		// removing skerberus as List key
		arbitraryTransaction = new ArbitraryTransactionV3(sender, null, 10, data,
				(byte)0, timestamp,
				sender.getLastReference(databaseSet));
		arbitraryTransaction.sign(sender);

		arbitraryTransaction.process(databaseSet);

		// KEY IS THERE!
		assertEquals(
				"vrontis",
				databaseSet.getNameStorageMap().getOpt("drizzt",
						Qorakeys.PROFILELIKEPOSTS.toString()));

		// nothing happens cause not part of the list
		storageJsonObject = StorageUtils
				.getStorageJsonObject(
						null,
						null,
						null,
						Collections.singletonList(new Pair<String, String>(
								Qorakeys.PROFILELIKEPOSTS.toString(), "haloman")),
						null, null);
		storageJsonObject.put("name", "drizzt");
		data = storageJsonObject.toString().getBytes();

		// removing skerberus as List key
		arbitraryTransaction = new ArbitraryTransactionV3(sender, null, 10, data,
				(byte)0, timestamp,
				sender.getLastReference(databaseSet));
		arbitraryTransaction.sign(sender);

		arbitraryTransaction.process(databaseSet);

		// KEY IS THERE!
		assertEquals(
				"vrontis",
				databaseSet.getNameStorageMap().getOpt("drizzt",
						Qorakeys.PROFILELIKEPOSTS.toString()));

		// removing last person
		storageJsonObject = StorageUtils
				.getStorageJsonObject(
						null,
						null,
						null,
						Collections.singletonList(new Pair<String, String>(
								Qorakeys.PROFILELIKEPOSTS.toString(), "vrontis")),
						null, null);
		storageJsonObject.put("name", "drizzt");
		data = storageJsonObject.toString().getBytes();

		// removing skerberus as List key
		arbitraryTransaction = new ArbitraryTransactionV3(sender, null, 10, data,
				(byte)0, timestamp,
				sender.getLastReference(databaseSet));
		arbitraryTransaction.sign(sender);
		arbitraryTransaction.process(databaseSet);

		// KEY IS THERE!
		assertNull(databaseSet.getNameStorageMap().getOpt("drizzt",
				Qorakeys.PROFILELIKEPOSTS.toString()));

		// adding more than one element!

		storageJsonObject = StorageUtils.getStorageJsonObject(null, null,
				Collections.singletonList(new Pair<String, String>(
						Qorakeys.PROFILELIKEPOSTS.toString(), "a;b;c")), null,
				null, null);
		storageJsonObject.put("name", "drizzt");
		data = storageJsonObject.toString().getBytes();

		arbitraryTransaction = new ArbitraryTransactionV3(sender, null, 10, data,
				(byte)0, timestamp,
				sender.getLastReference(databaseSet));
		arbitraryTransaction.sign(sender);

		arbitraryTransaction.process(databaseSet);

		assertEquals(
				"a;b;c",
				databaseSet.getNameStorageMap().getOpt("drizzt",
						Qorakeys.PROFILELIKEPOSTS.toString()));

		// removing more than one

		storageJsonObject = StorageUtils.getStorageJsonObject(null, null, null,
				Collections.singletonList(new Pair<String, String>(
						Qorakeys.PROFILELIKEPOSTS.toString(), "a;c;nothing")),
				null, null);
		storageJsonObject.put("name", "drizzt");
		data = storageJsonObject.toString().getBytes();

		arbitraryTransaction = new ArbitraryTransactionV3(sender, null, 10, data,
				(byte)0, timestamp,
				sender.getLastReference(databaseSet));
		arbitraryTransaction.sign(sender);

		arbitraryTransaction.process(databaseSet);

		// KEY IS THERE!
		assertEquals(
				"b",
				databaseSet.getNameStorageMap().getOpt("drizzt",
						Qorakeys.PROFILELIKEPOSTS.toString()));

	}

	@Test
	public void testAddWithoutSeperatorAndCheckBasicOrphaning()
			throws Exception {
		long timestamp = NTP.getTime();

		JSONObject storageJsonObject = StorageUtils.getStorageJsonObject(null,
				null, null, null, Collections
						.singletonList(new Pair<String, String>(
								Qorakeys.WEBSITE.toString(), "first")), null);
		storageJsonObject.put("name", "drizzt");
		byte[] data = storageJsonObject.toString().getBytes();

		ArbitraryTransaction arbitraryTransaction = new ArbitraryTransactionV3(
				sender, null, 10, data, (byte)0, timestamp,
				sender.getLastReference(databaseSet));
		arbitraryTransaction.sign(sender);

		arbitraryTransaction.process(databaseSet);

		databaseSet.getTransactionMap().add(arbitraryTransaction);

		assertEquals(
				"first",
				databaseSet.getNameStorageMap().getOpt("drizzt",
						Qorakeys.WEBSITE.toString()));

		storageJsonObject = StorageUtils.getStorageJsonObject(null, null, null,
				null, Collections.singletonList(new Pair<String, String>(
						Qorakeys.WEBSITE.toString(), " second")), null);
		storageJsonObject.put("name", "drizzt");
		data = storageJsonObject.toString().getBytes();

		ArbitraryTransaction arbitraryTransaction2 = new ArbitraryTransactionV3(
				sender, null, 10, data, (byte)0, timestamp,
				sender.getLastReference(databaseSet));
		arbitraryTransaction2.sign(sender);

		arbitraryTransaction2.process(databaseSet);

		databaseSet.getTransactionMap().add(arbitraryTransaction2);

		assertEquals(
				"first second",
				databaseSet.getNameStorageMap().getOpt("drizzt",
						Qorakeys.WEBSITE.toString()));

		// ORPHANING FIRST TX!
		arbitraryTransaction.orphan(databaseSet);

		assertEquals(
				" second",
				databaseSet.getNameStorageMap().getOpt("drizzt",
						Qorakeys.WEBSITE.toString()));

		// ORPHANING second TX!
		arbitraryTransaction2.orphan(databaseSet);

		assertNull(databaseSet.getNameStorageMap().getOpt("drizzt",
				Qorakeys.WEBSITE.toString()));
	}

	@Test
	public void testComplexOrphanNameStorageTest() throws Exception {
		long timestamp = NTP.getTime();

		String random_linking_example = "randomlinkingExample";
		JSONObject storageJsonObject = StorageUtils.getStorageJsonObject(
				Collections.singletonList(new Pair<String, String>(
						Qorakeys.PROFILEENABLE.toString(), "yes")), null,
				Collections.singletonList(new Pair<String, String>(
						random_linking_example, "skerberus")), null,
				Collections.singletonList(new Pair<String, String>(
						Qorakeys.WEBSITE.toString(), "first")), null);
		storageJsonObject.put("name", "drizzt");
		byte[] data = storageJsonObject.toString().getBytes();

		ArbitraryTransaction arbitraryTransaction = new ArbitraryTransactionV3(
				sender, null, 10, data, (byte)0, timestamp,
				sender.getLastReference(databaseSet));
		arbitraryTransaction.sign(sender);
		arbitraryTransaction.process(databaseSet);

		databaseSet.getTransactionMap().add(arbitraryTransaction);

		// After first tx
		// Profenable:yes
		// Website: first
		// random : skerberus

		storageJsonObject = StorageUtils.getStorageJsonObject(null, null,
				Collections.singletonList(new Pair<String, String>(
						random_linking_example, "vrontis")), null, Collections
						.singletonList(new Pair<String, String>(
								Qorakeys.WEBSITE.toString(), "second")), null);
		storageJsonObject.put("name", "drizzt");
		data = storageJsonObject.toString().getBytes();

		ArbitraryTransaction arbitraryTransaction2 = new ArbitraryTransactionV3(
				sender, null, 10, data, (byte)0, timestamp,
				sender.getLastReference(databaseSet));
		arbitraryTransaction2.sign(sender);
		arbitraryTransaction2.process(databaseSet);

		databaseSet.getTransactionMap().add(arbitraryTransaction2);

		// After second tx
		// Profenable:yes
		// Website: firstsecond
		// random : skerberus;vrontis

		storageJsonObject = StorageUtils.getStorageJsonObject(null, null,
				Collections.singletonList(new Pair<String, String>("asdf",
						"asdf")), null, Collections
						.singletonList(new Pair<String, String>(
								Qorakeys.WEBSITE.toString(), "third")), null);
		storageJsonObject.put("name", "drizzt");
		data = storageJsonObject.toString().getBytes();

		ArbitraryTransaction arbitraryTransaction3 = new ArbitraryTransactionV3(
				sender, null, 10, data, (byte)0, timestamp,
				sender.getLastReference(databaseSet));
		arbitraryTransaction3.sign(sender);
		arbitraryTransaction3.process(databaseSet);

		databaseSet.getTransactionMap().add(arbitraryTransaction3);

		// After second tx
		// Profenable:yes
		// Website: firstsecondthird
		// random : skerberus;vrontis
		// asdf : asdf

		assertEquals("firstsecondthird", databaseSet.getNameStorageMap()
				.getOpt("drizzt", Qorakeys.WEBSITE.toString()));
		assertEquals(
				"yes",
				databaseSet.getNameStorageMap().getOpt("drizzt",
						Qorakeys.PROFILEENABLE.toString()));
		assertEquals("skerberus;vrontis", databaseSet.getNameStorageMap()
				.getOpt("drizzt", random_linking_example));
		assertEquals("asdf",
				databaseSet.getNameStorageMap().getOpt("drizzt", "asdf"));

		// removing second one -->

		// Profenable:yes
		// Website: firstthird
		// random : skerberus
		// asdf : asdf
		arbitraryTransaction2.orphan(databaseSet);

		assertEquals(
				"firstthird",
				databaseSet.getNameStorageMap().getOpt("drizzt",
						Qorakeys.WEBSITE.toString()));
		assertEquals(
				"yes",
				databaseSet.getNameStorageMap().getOpt("drizzt",
						Qorakeys.PROFILEENABLE.toString()));
		assertEquals(
				"skerberus",
				databaseSet.getNameStorageMap().getOpt("drizzt",
						random_linking_example));
		assertEquals("asdf",
				databaseSet.getNameStorageMap().getOpt("drizzt", "asdf"));

	}

	@Test
	public void testComplexOrphaning2() throws Exception {
		long timestamp = NTP.getTime();
		String random_linking_example = "randomlinkingExample";
		JSONObject storageJsonObject = StorageUtils.getStorageJsonObject(
				Collections.singletonList(new Pair<String, String>(
						Qorakeys.PROFILEENABLE.toString(), "yes")), null,
				Collections.singletonList(new Pair<String, String>(
						random_linking_example, "skerberus")), null,
				Collections.singletonList(new Pair<String, String>(
						Qorakeys.WEBSITE.toString(), "first")), null);
		storageJsonObject.put("name", "drizzt");
		byte[] data = storageJsonObject.toString().getBytes();

		ArbitraryTransaction arbitraryTransaction = new ArbitraryTransactionV3(
				sender, null, 10, data, (byte)0, timestamp,
				sender.getLastReference(databaseSet));
		arbitraryTransaction.sign(sender);
		arbitraryTransaction.process(databaseSet);

		databaseSet.getTransactionMap().add(arbitraryTransaction);

		// After first tx
		// Profenable:yes
		// Website: first
		// random : skerberus

		storageJsonObject = StorageUtils.getStorageJsonObject(null, null,
				Collections.singletonList(new Pair<String, String>(
						random_linking_example, "vrontis")), null, Collections
						.singletonList(new Pair<String, String>(
								Qorakeys.WEBSITE.toString(), "second")), null);
		storageJsonObject.put("name", "drizzt");
		data = storageJsonObject.toString().getBytes();

		ArbitraryTransaction arbitraryTransaction2 = new ArbitraryTransactionV3(
				sender, null, 10, data, (byte)0, timestamp,
				sender.getLastReference(databaseSet));
		arbitraryTransaction2.sign(sender);
		arbitraryTransaction2.process(databaseSet);

		databaseSet.getTransactionMap().add(arbitraryTransaction2);

		// After second tx
		// Profenable:yes
		// Website: firstsecond
		// random : skerberus;vrontis

		storageJsonObject = StorageUtils.getStorageJsonObject(null, null,
				Collections.singletonList(new Pair<String, String>("asdf",
						"asdf")), null, Collections
						.singletonList(new Pair<String, String>(
								Qorakeys.WEBSITE.toString(), "third")), null);
		storageJsonObject.put("name", "drizzt");
		data = storageJsonObject.toString().getBytes();

		ArbitraryTransaction arbitraryTransaction3 = new ArbitraryTransactionV3(
				sender, null, 10, data, (byte)0, timestamp,
				sender.getLastReference(databaseSet));
		arbitraryTransaction3.sign(sender);
		arbitraryTransaction3.process(databaseSet);

		databaseSet.getTransactionMap().add(arbitraryTransaction3);

		// After third tx
		// Profenable:yes
		// Website: firstsecondthird
		// random : skerberus;vrontis
		// asdf : asdf

		assertEquals("firstsecondthird", databaseSet.getNameStorageMap()
				.getOpt("drizzt", Qorakeys.WEBSITE.toString()));
		assertEquals(
				"yes",
				databaseSet.getNameStorageMap().getOpt("drizzt",
						Qorakeys.PROFILEENABLE.toString()));
		assertEquals("skerberus;vrontis", databaseSet.getNameStorageMap()
				.getOpt("drizzt", random_linking_example));
		assertEquals("asdf",
				databaseSet.getNameStorageMap().getOpt("drizzt", "asdf"));

		// removing first one -->

		// Website: secondthird
		// random : vrontis
		// asdf : asdf
		arbitraryTransaction.orphan(databaseSet);

		assertEquals(
				"secondthird",
				databaseSet.getNameStorageMap().getOpt("drizzt",
						Qorakeys.WEBSITE.toString()));
		assertNull(

		databaseSet.getNameStorageMap().getOpt("drizzt",
				Qorakeys.PROFILEENABLE.toString()));
		assertEquals(
				"vrontis",
				databaseSet.getNameStorageMap().getOpt("drizzt",
						random_linking_example));
		assertEquals("asdf",
				databaseSet.getNameStorageMap().getOpt("drizzt", "asdf"));

		// removing new first
		// Website: third
		// asdf : asdf
		arbitraryTransaction2.orphan(databaseSet);

		assertEquals(
				"third",
				databaseSet.getNameStorageMap().getOpt("drizzt",
						Qorakeys.WEBSITE.toString()));
		assertNull(

		databaseSet.getNameStorageMap().getOpt("drizzt",
				Qorakeys.PROFILEENABLE.toString()));
		assertNull(databaseSet.getNameStorageMap().getOpt("drizzt",
				random_linking_example));
		assertEquals("asdf",
				databaseSet.getNameStorageMap().getOpt("drizzt", "asdf"));

	}

	@Test
	public void testOrphanComplex3() throws Exception {
		long timestamp = NTP.getTime();
		String random_linking_example = "randomlinkingExample";
		JSONObject storageJsonObject = StorageUtils.getStorageJsonObject(
				Collections.singletonList(new Pair<String, String>(
						Qorakeys.PROFILEENABLE.toString(), "yes")), null,
				Collections.singletonList(new Pair<String, String>(
						random_linking_example, "skerberus")), null,
				Collections.singletonList(new Pair<String, String>(
						Qorakeys.WEBSITE.toString(), "first")), null);
		storageJsonObject.put("name", "drizzt");
		byte[] data = storageJsonObject.toString().getBytes();

		ArbitraryTransaction arbitraryTransaction = new ArbitraryTransactionV3(
				sender, null, 10, data, (byte)0, timestamp,
				sender.getLastReference(databaseSet));
		arbitraryTransaction.sign(sender);
		arbitraryTransaction.process(databaseSet);

		databaseSet.getTransactionMap().add(arbitraryTransaction);

		// After first tx
		// Profenable:yes
		// Website: first
		// random : skerberus

		storageJsonObject = StorageUtils.getStorageJsonObject(null, null,
				Collections.singletonList(new Pair<String, String>(
						random_linking_example, "vrontis")), null, Collections
						.singletonList(new Pair<String, String>(
								Qorakeys.WEBSITE.toString(), "second")), null);
		storageJsonObject.put("name", "drizzt");
		data = storageJsonObject.toString().getBytes();

		ArbitraryTransaction arbitraryTransaction2 = new ArbitraryTransactionV3(
				sender, null, 10, data, (byte)0, timestamp,
				sender.getLastReference(databaseSet));
		arbitraryTransaction2.sign(sender);
		arbitraryTransaction2.process(databaseSet);

		databaseSet.getTransactionMap().add(arbitraryTransaction2);

		// After second tx
		// Profenable:yes
		// Website: firstsecond
		// random : skerberus;vrontis

		storageJsonObject = StorageUtils.getStorageJsonObject(null, null,
				Collections.singletonList(new Pair<String, String>("asdf",
						"asdf")), null, Collections
						.singletonList(new Pair<String, String>(
								Qorakeys.WEBSITE.toString(), "third")), null);
		storageJsonObject.put("name", "drizzt");
		data = storageJsonObject.toString().getBytes();

		ArbitraryTransaction arbitraryTransaction3 = new ArbitraryTransactionV3(
				sender, null, 10, data, (byte)0, timestamp,
				sender.getLastReference(databaseSet));
		arbitraryTransaction3.sign(sender);
		arbitraryTransaction3.process(databaseSet);

		databaseSet.getTransactionMap().add(arbitraryTransaction3);

		// After third tx
		// Profenable:yes
		// Website: firstsecondthird
		// random : skerberus;vrontis
		// asdf : asdf

		assertEquals("firstsecondthird", databaseSet.getNameStorageMap()
				.getOpt("drizzt", Qorakeys.WEBSITE.toString()));
		assertEquals(
				"yes",
				databaseSet.getNameStorageMap().getOpt("drizzt",
						Qorakeys.PROFILEENABLE.toString()));
		assertEquals("skerberus;vrontis", databaseSet.getNameStorageMap()
				.getOpt("drizzt", random_linking_example));
		assertEquals("asdf",
				databaseSet.getNameStorageMap().getOpt("drizzt", "asdf"));

		// --> removing last one
		// Profenable:yes
		// Website: firstsecond
		// random : skerberus;vrontis
		arbitraryTransaction3.orphan(databaseSet);

		assertEquals(
				"firstsecond",
				databaseSet.getNameStorageMap().getOpt("drizzt",
						Qorakeys.WEBSITE.toString()));
		assertEquals(
				"yes",
				databaseSet.getNameStorageMap().getOpt("drizzt",
						Qorakeys.PROFILEENABLE.toString()));
		assertEquals("skerberus;vrontis", databaseSet.getNameStorageMap()
				.getOpt("drizzt", random_linking_example));
		assertNull(databaseSet.getNameStorageMap().getOpt("drizzt", "asdf"));
	}

}
