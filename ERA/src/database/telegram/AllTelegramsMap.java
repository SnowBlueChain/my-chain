package database.telegram;

import java.util.HashMap;
import java.util.Map;

import org.mapdb.BTreeKeySerializer;
import org.mapdb.DB;

import core.account.PublicKeyAccount;
import core.transaction.Transaction;
import database.DBMap;
import database.serializer.TransactionSerializer;
import datachain.DCMap;
import utils.ObserverMessage;

public class AllTelegramsMap extends  DCMap<String , Transaction> {
    
    private Map<Integer, Integer> observableData = new HashMap<Integer, Integer>();
      

    public AllTelegramsMap(TelegramSet dWSet, DB database) {
        super(dWSet, database);

        this.observableData.put(DBMap.NOTIFY_RESET, ObserverMessage.ALL_TELEGRAM_RESET_TYPE);
        this.observableData.put(DBMap.NOTIFY_ADD, ObserverMessage.ALL_TELEGRAMT_ADD_TYPE);
        this.observableData.put(DBMap.NOTIFY_REMOVE, ObserverMessage.ALL_TELEGRAMT_REMOVE_TYPE);
        this.observableData.put(DBMap.NOTIFY_LIST, ObserverMessage.ALL_TELEGRAMT_LIST_TYPE);
    }

   
    @SuppressWarnings("unchecked")
    @Override
    protected Map<String, Transaction> getMap(DB database) {
      //OPEN MAP
       return database.createTreeMap("telegrams")
              .keySerializer(BTreeKeySerializer.BASIC)
              .valueSerializer(new  TransactionSerializer())
              .counterEnable()
              .makeOrGet();
    }

    @Override
    protected Map<String, Transaction> getMemoryMap() {
        // TODO Auto-generated method stub
        return getMemoryMap();
    }

    @Override
    protected Transaction getDefaultValue() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    protected Map<Integer, Integer> getObservableData() {
        // TODO Auto-generated method stub
        return this.observableData;
    }

    @Override
    protected void createIndexes(DB database) {
        // TODO Auto-generated method stub
              
    }
    
    public boolean add(String signature, Transaction telegramMessage) {
        return this.set(signature, telegramMessage);
    }

    public void deleteFromAccount(PublicKeyAccount account) {
        // TODO Auto-generated method stub
        
    }
    
   
}
