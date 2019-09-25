/*
 * Copyright (c) [2016] [ <ether.camp> ]
 * This file is part of the ethereumJ library.
 *
 * The ethereumJ library is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * The ethereumJ library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with the ethereumJ library. If not, see <http://www.gnu.org/licenses/>.
 */

package org.erachain.dbs.rocksDB.common;

import java.util.Map;


/**
 * TODO зачем выделен этот файл, какой функционал он несет, почему нельзя было его встрогить в супер
 * Этот интерфейс позаимствовани из проекта "tron". Скорее всего он использовался для разделения функционала.
 * Можно удалить.
 * Встроить можно все что угодно куда угодно
 * @param <K>
 * @param <V>
 */
public interface BatchSourceInter<K, V> extends SourceInter<K, V> {


  void updateByBatch(Map<K, V> rows);

  void updateByBatch(Map<K, V> rows, WriteOptionsWrapper writeOptions);
}
