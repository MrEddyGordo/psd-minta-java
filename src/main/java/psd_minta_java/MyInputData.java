package psd_minta_java;

import java.util.ArrayList;
import java.util.HashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.api.client.util.DateTime;

public class MyInputData {
	private static final Logger LOGGER = LogManager.getLogger(MyInputData.class);
	
	public MyInputData(ArrayList<String> nevSorrend, HashMap<String,String> naptarak, long startTime, long endTime) {
		
        System.out.println("Kezdőidőpont: "+new DateTime(startTime));
        System.out.println("Végidőpont: "+new DateTime(endTime));
		
        try {
        	for (String naptarNev : nevSorrend) {
        		//System.out.println(naptarNev);
        		if(!naptarak.containsKey(naptarNev)) throw new Exception("Nincsen ilyen nevű naptár: " + naptarNev);
			}
        } catch(Exception e) {
        	LOGGER.error(e);
        	System.exit(0);
        }
		
	}
	
}


//Timestamp in milliseconds: 1578290400000
//Date and time (GMT): 2020. January 6., Monday 6:00:00
//Date and time (your time zone): 2020. január 6., hétfő 7:00:00 GMT+01:00

//Timestamp in milliseconds: 1593561600000
//Date and time (GMT): 2020. July 1., Wednesday 0:00:00


//1593842400
//Is equivalent to:
//07/04/2020 @ 6:00am (UTC)
//1609459200
//Is equivalent to:
//01/01/2021 @ 12:00am (UTC)
//1593561600
//Is equivalent to:
//07/01/2020 @ 12:00am (UTC)