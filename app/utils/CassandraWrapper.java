/*
 * Copyright (c) 2013
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only...
 */
package utils;

import com.netflix.astyanax.AstyanaxContext;
import com.netflix.astyanax.Keyspace;
import com.netflix.astyanax.MutationBatch;
import com.netflix.astyanax.connectionpool.NodeDiscoveryType;
import com.netflix.astyanax.connectionpool.OperationResult;
import com.netflix.astyanax.connectionpool.exceptions.ConnectionException;
import com.netflix.astyanax.connectionpool.impl.ConnectionPoolConfigurationImpl;
import com.netflix.astyanax.connectionpool.impl.CountingConnectionPoolMonitor;
import com.netflix.astyanax.impl.AstyanaxConfigurationImpl;
import com.netflix.astyanax.model.Column;
import com.netflix.astyanax.model.ColumnFamily;
import com.netflix.astyanax.model.ColumnList;
import com.netflix.astyanax.serializers.StringSerializer;
import com.netflix.astyanax.thrift.ThriftFamilyFactory;

/**
 * A wrapper calss for interacting with the Cassandra database.
 *
 * @author  Robert O'Regan
 */
public class CassandraWrapper {

	/** Cassandra Connection Parameters */
	private final String ConnectionPool = "ConnectionPool";
	private final int ConnectionPort = 9160;
	private final String ConnectionString = "127.0.0.1:9160";
	private final String KeyspaceName = "boxever";
	private final String ClusterName = "BoxeverCluster";
	private final String ColumnFamilyName = "currencies";

	/** Cassandra Connection Variables */
	private AstyanaxContext<Keyspace> context = null;
	Keyspace keyspace = null;
	ColumnFamily<String, String> CF_CURRENCIES = null;
	
	/**
	 * Connect to Cassandra.
	 *
	 * This method is used to connect to Cassandra.
	 */
	private void connect() {
		// connect to the boxever keyspace on cassandra
		context = new AstyanaxContext.Builder()
			.forCluster(ClusterName)
			.forKeyspace(KeyspaceName)
			.withAstyanaxConfiguration(new AstyanaxConfigurationImpl()      
				.setDiscoveryType(NodeDiscoveryType.NONE)
			)
			.withConnectionPoolConfiguration(
				new ConnectionPoolConfigurationImpl(ConnectionPool)
				.setPort(ConnectionPort)
				.setMaxConnsPerHost(10)
				.setSeeds(ConnectionString)
			)
			.withConnectionPoolMonitor(new CountingConnectionPoolMonitor())
			.buildKeyspace(ThriftFamilyFactory.getInstance());

		context.start();
		keyspace = context.getEntity();
		
		// set the column family to use
		setColumnFamily();
	}
	
	/**
	 * Set the Column Family.
	 *
	 * This method sets the Column Family for interatcing 
	 * with the Cassandra database.
	 */
	private void setColumnFamily() {
		// assuming use of one column family...
		CF_CURRENCIES = new ColumnFamily<String, String>(
			ColumnFamilyName,         // Column Family Name
			StringSerializer.get(),   // Key Serializer
			StringSerializer.get());  // Column Serializer
	}
	
	/**
	 * Query Cassandra for a specific columns value.
	 *
	 * This method enables the user to query for a spceific column
	 * value.
	 *
	 * @param rowKey 		String The row key the column belongs to
	 * @param columnName	String The column name to query for
	 * @return String 		The columns value
	 */
	public String getColumnValue(String rowKey, String columnName) {
		// connect to cassandra
		connect();
		
		// variable to hold return value
		String xml = null;
		
		// run query for the requested data
		ColumnList<String> result = null;
		try {
			result = keyspace.prepareQuery(CF_CURRENCIES)
				.getKey(rowKey)
				.execute()
				.getResult();
			
			// if there are no columns in the resopnse then return null
			if(result.isEmpty()) {
				return null;
			}
		} catch (ConnectionException e) {
			System.out.println("Error!!!");
		}
			
		// extract the xml from the response using the column name
		xml = result.getColumnByName(columnName).getStringValue();			
		
		return xml;
	}
	
	/**
	 * Write a value to Cassandra.
	 *
	 * This method enables the addition of a value to Cassandra.
	 * The value is stored under the row key and column name specified.
	 *
	 * @param rowKey		String The row key to store teh value under
	 * @param columnName	String The column name to store the value under
	 */
	public void writeColumnValue(String rowKey, String column, String value) {
		// connect to cassandra
		connect();
		
		try {
			MutationBatch m = keyspace.prepareMutationBatch();
			m.withRow(CF_CURRENCIES, rowKey)
			  .putColumn(column, value, null);
			OperationResult<Void> result = m.execute();
		} catch (ConnectionException e) {
			System.out.println("Error");
		}
	}
}