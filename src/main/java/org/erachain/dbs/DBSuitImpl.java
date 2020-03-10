package org.erachain.dbs;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class DBSuitImpl<T, U> implements DBSuit<T, U> {

    protected DBTab cover;

    protected boolean sizeEnable;
    //protected abstract void openMap();

    @Override
    public boolean isSizeEnable() {
        return sizeEnable;
    }

    @Override
    public int getDefaultIndex() {
        return 0;
    }

    //protected abstract void getMap();

    protected void createIndexes() {
    }

    @Override
    public U getDefaultValue() {
        if (cover != null)
            return (U) cover.getDefaultValue();

        return null;
    }

    @Override
    public void close() {
        cover = null;
    }

}
