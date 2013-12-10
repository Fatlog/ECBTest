/*
 * Copyright (c) 2013
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only...
 */

package controllers;

import play.*;
import play.mvc.*;
import play.libs.*;

import java.util.*;

import models.*;
import utils.*;

/**
 * This class returns exchange rate data from 
 * "www.ecb.europa.eu" as an XML string.
 *
 * @author  Robert O'Regan
 */
public class ExchangeRates extends Controller {

	/** Variable that denotes if the XML from the third party
	 * has been lazy loaded. This is used to control requests to 
	 * the thrid party site if multiple clients connect simultaneously. */
	private static boolean dataRetrieved = false;
	
	/**
	 * Return the exchange rate data.
	 *
	 * This method returns exchange rate data. The exchange rate data is 
	 * normally stored in teh database. If it is not available, the data is
	 * requested from a third party site and stored in the database for
	 * subsequent requests.	 
	 */ 
    public static void list() {
		// check for the data in the database first		
		String xml = getExchangeRateFromDatabase();
		if(xml != null) {
			// xml is in database, return it to client			
			renderText(xml);
		} else {
			// xml is not in database, request it from third party
			// and store it in the database before returning it to client
			renderText(getExchangeRateFromThirdParty());
		}
    }
	
	/**
	 * Returns exchange rate data.
	 * 
	 * This method queries the database for the exchange rate data.
	 * 
	 * @return String The exchange rate data.
	 */
	private static String getExchangeRateFromDatabase() {
		System.out.println("Getting data from Database...");	
		CassandraWrapper cass = new CassandraWrapper();		
		return cass.getColumnValue("ecb", "xml");
	}
	
	/**
	 * Returns exchange rate data.
	 *
	 * Returns exchange rate data by requesting it from a third party
	 * website. This method is synchronized to prevent multiple calls
	 * to the third party site. 
	 *
	 * @return String The exchange rate data.
	 */
	private static synchronized String getExchangeRateFromThirdParty() {
	
		//** Could also possibly have used database flag, 
		//** semaphores or reentrant lock. I initially used a 
		//** reentrant lock but this didn't seem to play nicely
		//** with the Play framework and the await() call
			
		System.out.println("Getting data from Third Party...");	
		
		// if the data was retrieved by a previous request then just
		// lookup the database and return the data from there instead
		// of querying the third party site again
		if(dataRetrieved) {
			System.out.println("Data now in DB. Querying DB instead...");
			return getExchangeRateFromDatabase();
		}
		
		// endpoint for data
		final String url = 
			"http://www.ecb.europa.eu/stats/eurofxref/eurofxref-hist-90d.xml";
		
		// send request
		F.Promise<WS.HttpResponse> r1 = WS.url(url).getAsync();		
		
		// Suspend processing here, until all three remote calls are complete.
		WS.HttpResponse httpResponse = await(r1);	 
		
		// set flag for any subsequent requests that could have been 
		// suspended/queued...
		dataRetrieved = true;
		
		// get response
		String xml = httpResponse.getString();
		
		// store the xml in the database for subsequent user requests
		CassandraWrapper cass = new CassandraWrapper();
		cass.writeColumnValue("ecb", "xml", xml);
		
		// return response to user
		return xml;
	}
}