package org.erachain.datachain;

import org.erachain.core.account.Account;
import org.erachain.dbs.IteratorCloseable;

import java.util.Collection;

public interface ItemAssetBalanceSuit {

    IteratorCloseable<byte[]> assetIterator(long assetKey);
    Collection<byte[]> assetKeys(long assetKey);

    IteratorCloseable<byte[]> accountIterator(Account account);
    Collection<byte[]> accountKeys(Account account);

}
