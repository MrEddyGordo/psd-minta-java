package psd_minta_java;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import com.google.api.services.calendar.Calendar;

public class PSD_minta {
	private static final Logger LOGGER = LogManager.getLogger(PSD_minta.class);

    public static void main(String... args) throws IOException {
    	LOGGER.trace("-------------");
    	LOGGER.trace("App indulása.");
    	
    	ConnectToCalendarAPI newConn = new ConnectToCalendarAPI();
    	Calendar service = newConn.getCalendarService();

    	CalendarOperations calOp = new CalendarOperations(service);
    	String accName = calOp.getAccount();
        
        HashMap<String,String> naptarak = calOp.getCalendarNamesandIDs();
        
        
        ArrayList<String> nevSorrend = new ArrayList<String>();
        nevSorrend.add("Tamás");
        nevSorrend.add("Imre");
        nevSorrend.add("András");
        nevSorrend.add("Gergő");
        nevSorrend.add("VTamás");
        nevSorrend.add("Sándor");
        
        long startTime = 1593842400000L;
        long endTime = 1609459200000L;
        
        MyInputData mid = new MyInputData(nevSorrend, naptarak, startTime, endTime);
        
        
        try {
        	//calOp.insertPattern(accName, service, naptarak, nevSorrend, startTime, endTime);
			calOp.askToDelete(accName, service, naptarak, nevSorrend, startTime, endTime);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

        LOGGER.trace("App futásának vége.");
    }
}




