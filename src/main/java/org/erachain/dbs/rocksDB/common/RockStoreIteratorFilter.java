package org.erachain.dbs.rocksDB.common;

import lombok.extern.slf4j.Slf4j;
import org.rocksdb.RocksIterator;

import java.util.NoSuchElementException;

import static org.erachain.utils.ByteArrayUtils.areEqualMask;

@Slf4j
public final class RockStoreIteratorFilter extends RockStoreIterator {

  byte[] filter;

  public RockStoreIteratorFilter(RocksIterator dbIterator, boolean descending, boolean isIndex, byte[] filter) {
    super(dbIterator, descending, isIndex);
    this.filter = filter;
  }

  @Override
  public boolean hasNext() {
    boolean hasNext = false;
    try {
      if (jumpToLast) {
        dbIterator.seekForPrev(filter);
        jumpToLast = false;
        first = false;
      }
      if (first) {
        dbIterator.seek(filter);
        first = false;
      }
      if (!(hasNext = dbIterator.isValid())) {
        dbIterator.close();
      }
    } catch (Exception e) {
      logger.error(e.getMessage(),e);
      try {
        dbIterator.close();
      } catch (Exception e1) {
        logger.error(e1.getMessage(),e1);
      }
    }

    // ERROR on some BYTES!!!
    // hasNext = hasNext && new String(dbIterator.key()).startsWith(new String(filter));
    return (hasNext = hasNext && areEqualMask(dbIterator.key(), filter));
  }

  @Override
  public byte[] next() {
    if (!dbIterator.isValid()) {
      throw new NoSuchElementException();
    }
    byte[] key;
    if (isIndex) {
      key = dbIterator.value();
    } else {
      key = dbIterator.key();
    }
    if (descending) {
      dbIterator.prev();
    } else {
      dbIterator.next();
    }
    return key;
  }
}