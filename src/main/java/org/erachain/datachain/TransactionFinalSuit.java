package org.erachain.datachain;

import org.erachain.dbs.IteratorCloseable;

/**
 * Iterators for this TAB
 */
public interface TransactionFinalSuit {

    void deleteForBlock(Integer height);

    IteratorCloseable<Long> getBlockIterator(Integer height);

    IteratorCloseable<Long> getIteratorByCreator(byte[] addressShort, boolean descending);

    IteratorCloseable<Long> getIteratorByCreator(byte[] addressShort, Long fromSeqNo, boolean descending);

    IteratorCloseable<Long> getIteratorByCreator(byte[] addressShort, Long fromSeqNo, Long toSeqNo, boolean descending);


    IteratorCloseable<Long> getIteratorByRecipient(byte[] addressShort, boolean descending);

    IteratorCloseable<Long> getIteratorByRecipient(byte[] addressShort, Long fromSeqNo, boolean descending);

    IteratorCloseable<Long> getIteratorByRecipient(byte[] addressShort, Long fromSeqNo, Long toSeqNo, boolean descending);

    /**
     * @param addressShort
     * @param type         - TRANSACTION type
     * @param isCreator    True - only CREATORS, False - only RECIPIENTS
     * @param descending
     * @return
     */
    IteratorCloseable<Long> getIteratorByAddressAndType(byte[] addressShort, Integer type, Boolean isCreator, boolean descending);

    /**
     * Здесь обязательно нужно задавать тип транзакции и получатель или создатель - иначе по FROM_ID работать не будет в RocksDB.
     * Если надо делать поиск с заданного fromID - то надо передать сюда полный индекс для начального поиска если какие-то
     * из параметров не заданные - чтобы его поставить как поисковый для начала а дальше уже индекс огрничения - хотя тоже не сработает
     * ограничение так как там по fromID перед ним значения будут выше и отсанов только по LIMIT выше делать надо.
     * Таким образом для постарничного перебора и в getIteratorByAddressAndType - туда надо передавать текущий индекс
     * для начального поиска и с него уже итератор брать - тогда страницами можно организовать. <br>
     * Получается что если задан fromID но не задан какой-либо уточняющий параметр то надо наверху взять транзакцию
     * и из нее взять эти недостающие параметры чтобы точно найти первую запись.
     * <b>Значит сюда нужно передавать как начальные параметры так и ограничивающие сверху - тогда все четко будет работать</b>
     *
     * @param addressShort
     * @param type         - TRANSACTION type
     * @param isCreator    True - only CREATORS, False - only RECIPIENTS
     * @param fromID
     * @param descending
     * @return
     */
    IteratorCloseable<Long> getIteratorByAddressAndType(byte[] addressShort, Integer type, Boolean isCreator, Long fromID, boolean descending);

    IteratorCloseable<Long> getIteratorByAddressAndType(byte[] addressShort, Integer type, Boolean isCreator, Long fromID, Long toID, boolean descending);

    IteratorCloseable<Long> getIteratorByTitle(String filter, boolean asFilter, String fromWord, Long fromSeqNo, boolean descending);

    IteratorCloseable<Long> getIteratorByAddress(byte[] addressShort, boolean descending);

    IteratorCloseable<Long> getBiDirectionIterator(Long fromSeqNo, boolean descending);

    IteratorCloseable<Long> getBiDirectionAddressIterator(byte[] addressShort, Long fromSeqNo, boolean descending);

}
