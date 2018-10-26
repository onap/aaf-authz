/**
 * ============LICENSE_START====================================================
 * org.onap.aaf
 * ===========================================================================
 * Copyright (c) 2018 AT&T Intellectual Property. All rights reserved.
 * ===========================================================================
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ============LICENSE_END====================================================
 *
 */
package org.onap.aaf.auth.helpers.creators;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.datastax.driver.core.ColumnDefinitions;
import com.datastax.driver.core.LocalDate;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Token;
import com.datastax.driver.core.TupleValue;
import com.datastax.driver.core.TypeCodec;
import com.datastax.driver.core.UDTValue;
import com.google.common.reflect.TypeToken;

public class RowCreator {

	public static Row getRow() {
		Row row = new Row() {

			@Override
			public boolean isNull(String name) {
				// TODO Auto-generated method stub
				return false;
			}

			@Override
			public BigInteger getVarint(String name) {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public UUID getUUID(String name) {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public UDTValue getUDTValue(String name) {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public TupleValue getTupleValue(String name) {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public Date getTimestamp(String name) {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public long getTime(String name) {
				// TODO Auto-generated method stub
				return 0;
			}

			@Override
			public String getString(String name) {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public short getShort(String name) {
				// TODO Auto-generated method stub
				return 0;
			}

			@Override
			public <T> Set<T> getSet(String name, TypeToken<T> elementsType) {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public <T> Set<T> getSet(String name, Class<T> elementsClass) {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public Object getObject(String name) {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public <K, V> Map<K, V> getMap(String name, TypeToken<K> keysType, TypeToken<V> valuesType) {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public <K, V> Map<K, V> getMap(String name, Class<K> keysClass, Class<V> valuesClass) {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public long getLong(String name) {
				// TODO Auto-generated method stub
				return 0;
			}

			@Override
			public <T> List<T> getList(String name, TypeToken<T> elementsType) {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public <T> List<T> getList(String name, Class<T> elementsClass) {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public int getInt(String name) {
				// TODO Auto-generated method stub
				return 0;
			}

			@Override
			public InetAddress getInet(String name) {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public float getFloat(String name) {
				// TODO Auto-generated method stub
				return 0;
			}

			@Override
			public double getDouble(String name) {
				// TODO Auto-generated method stub
				return 0;
			}

			@Override
			public BigDecimal getDecimal(String name) {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public LocalDate getDate(String name) {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public ByteBuffer getBytesUnsafe(String name) {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public ByteBuffer getBytes(String name) {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public byte getByte(String name) {
				// TODO Auto-generated method stub
				return 0;
			}

			@Override
			public boolean getBool(String name) {
				// TODO Auto-generated method stub
				return false;
			}

			@Override
			public <T> T get(String name, TypeCodec<T> codec) {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public <T> T get(String name, TypeToken<T> targetType) {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public <T> T get(String name, Class<T> targetClass) {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public boolean isNull(int i) {
				// TODO Auto-generated method stub
				return false;
			}

			@Override
			public BigInteger getVarint(int i) {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public UUID getUUID(int i) {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public UDTValue getUDTValue(int i) {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public TupleValue getTupleValue(int i) {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public Date getTimestamp(int i) {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public long getTime(int i) {
				// TODO Auto-generated method stub
				return 0;
			}

			@Override
			public String getString(int i) {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public short getShort(int i) {
				// TODO Auto-generated method stub
				return 0;
			}

			@Override
			public <T> Set<T> getSet(int i, TypeToken<T> elementsType) {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public <T> Set<T> getSet(int i, Class<T> elementsClass) {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public Object getObject(int i) {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public <K, V> Map<K, V> getMap(int i, TypeToken<K> keysType, TypeToken<V> valuesType) {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public <K, V> Map<K, V> getMap(int i, Class<K> keysClass, Class<V> valuesClass) {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public long getLong(int i) {
				// TODO Auto-generated method stub
				return 0;
			}

			@Override
			public <T> List<T> getList(int i, TypeToken<T> elementsType) {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public <T> List<T> getList(int i, Class<T> elementsClass) {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public int getInt(int i) {
				// TODO Auto-generated method stub
				return 0;
			}

			@Override
			public InetAddress getInet(int i) {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public float getFloat(int i) {
				// TODO Auto-generated method stub
				return 0;
			}

			@Override
			public double getDouble(int i) {
				// TODO Auto-generated method stub
				return 0;
			}

			@Override
			public BigDecimal getDecimal(int i) {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public LocalDate getDate(int i) {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public ByteBuffer getBytesUnsafe(int i) {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public ByteBuffer getBytes(int i) {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public byte getByte(int i) {
				// TODO Auto-generated method stub
				return 0;
			}

			@Override
			public boolean getBool(int i) {
				// TODO Auto-generated method stub
				return false;
			}

			@Override
			public <T> T get(int i, TypeCodec<T> codec) {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public <T> T get(int i, TypeToken<T> targetType) {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public <T> T get(int i, Class<T> targetClass) {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public Token getToken(String name) {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public Token getToken(int i) {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public Token getPartitionKeyToken() {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public ColumnDefinitions getColumnDefinitions() {
				// TODO Auto-generated method stub
				return null;
			}
		};
		return row;
	}

}
