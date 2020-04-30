package psd_minta_java;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.util.DateTime;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.model.CalendarList;
import com.google.api.services.calendar.model.CalendarListEntry;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.Events;

public class CalendarOperations {
		private static final Logger LOGGER = LogManager.getLogger(CalendarOperations.class);
		
		Calendar service;
		HashMap<String,String> naptarak = new HashMap<String,String>();
		
		public CalendarOperations(Calendar service) {
			this.service = service;
		}
		
		public String getAccount() throws IOException {
	        CalendarListEntry MycalendarListEntry = service.calendarList().get("primary").execute();
	        String accName = MycalendarListEntry.getSummary();
	        return accName;
		}
		
        
        /**
         * Megnézzük milyen nevű naptárak léteznek, eltároljuk a naptárnév és ID párosokat.
         * @return
         * @throws IOException
         */
        public HashMap<String,String> getCalendarNamesandIDs() throws IOException {
        String pageToken = null;
        do {
          CalendarList calendarList = service.calendarList().list().setPageToken(pageToken).execute();
          List<CalendarListEntry> items1 = calendarList.getItems();

          for (CalendarListEntry calendarListEntry : items1) {
            //System.out.println("Summary: " + calendarListEntry.getSummary() + " Id: " + calendarListEntry.getId());
            naptarak.put(calendarListEntry.getSummary(), calendarListEntry.getId());
            LOGGER.info("Érvényes naptárnevek: " + calendarListEntry.getSummary());
            
          }
          pageToken = calendarList.getNextPageToken();
        } while (pageToken != null);
		return naptarak;
        }
        
        
    	/**
    	 * "n" summaryjű eseményeket töröl egy adott ID-jű naptárból a kezdő- és végidőpont között.
    	 * 
    	 * @param calendarId  
    	 * @param service  authorized API client service
    	 * @param startTime
    	 * @param origEnd
    	 * @throws Exception
    	 */
    	public void deleteEvents(String calendarId, String owner, Calendar service, long startTime, long origEnd)
    			throws Exception {

    		DateTime timeFrom = new DateTime(startTime);
    		DateTime timeMax = new DateTime(origEnd);
    		Events events = service.events().list(calendarId).setTimeMin(timeFrom).setOrderBy("startTime")
    				.setSingleEvents(true).setTimeMax(timeMax).execute();
    		List<Event> items = events.getItems();
    		if (items.isEmpty()) {
    			LOGGER.info(owner + " naptárában nincsen törlendő esemény.");
    		} else {
    			LOGGER.info(owner + " naptárából a következőket töröljük.");
    			for (Event event : items) {
    				DateTime start = event.getStart().getDateTime();
    				if (start == null) {
    					start = event.getStart().getDate();
    				}
    				// System.out.printf("%s %s (%s) EventiD: %s\n",
    				// event.getOrganizer().getDisplayName(),event.getSummary(), start,
    				// event.getId());
    				
    				/*
    				 * Ha a try block túl gyorsan kommunikál és 403 "Rate Limit Exceeded" Exceptiont dob a Google,
    				 * akkor a catch elkapja és vár tempD * 1 sec-nyi időt, majd újrapróbálja a try-t.
    				 */
    				int tempD = 0;

    				do {
    					try {
    						tempD++;
    						if (event.getSummary().equals("n"))
    							service.events().delete(calendarId, event.getId()).execute();
    						tempD = 0;
    						LOGGER.info("Törlés: " + event.getOrganizer().getDisplayName() + " " + event.getSummary()
    								+ " Kezdet: " + start + " EventId: " + event.getId());
    					} catch (GoogleJsonResponseException e) {

    						if (e.getStatusCode() == 403) {
    							int waitFor = 1000 * tempD;
    							TimeUnit.MILLISECONDS.sleep(waitFor);
    							LOGGER.info(e);
    							LOGGER.info("Várakozás " + waitFor + "msec ideig.");
    						}
    					}

    				} while (tempD > 0);
    			}
    		}
    	}
    	
        
        
    	/**
    	 * A "naptarak"-ban lévő hasmmapből a nevSorrend sorrendben minden eseményt töröl az adott időszakban.
    	 * @param service
    	 * @param naptarak
    	 * @param nevSorrend
    	 * @param startTime
    	 * @param endTime
    	 * @throws Exception
    	 */
    	public void deleteBetweenForEveryone(Calendar service, HashMap<String, String> naptarak,
    			ArrayList<String> nevSorrend, long startTime, long endTime) throws Exception {
    		String calendarId;
    		String owner;
    		/*
    		 * Végigmegyünk mindenkin, aki a naptárakban benne van.
    		 */
    		for (int j = 0; j < nevSorrend.size(); j++) {
    			owner = nevSorrend.get(j);
    			calendarId = (String) naptarak.get(owner);
    			deleteEvents(calendarId, owner, service, startTime, endTime);
    		}

    	}
    	
    	public  void askToDelete(String accName, Calendar service, HashMap<String, String> naptarak,
    			ArrayList<String> nevSorrend, long startTime, long endTime) throws Exception {
            Scanner kbd = new Scanner (System.in);
            
            try {
            	System.out.println("Fiók: " + accName);
            	System.out.printf("Ténylegesen töröljünk a(z) " + accName + " fiókból minden 'n' eseményt a fenti időszakból az ÖSSZES naptárból: ");
            	for (String nevOut : nevSorrend) {
            		System.out.printf("%s ",nevOut);
            	}
            	System.out.printf("? (i / n) ");
            	String answer = kbd.nextLine();
            	if(answer.equals("i")) deleteBetweenForEveryone(service, naptarak, nevSorrend,startTime,endTime);
            	else {
            		System.out.println("Megszakítás.");
            		System.exit(0);
            	}
    		} catch (Exception e) {
    			System.out.println(e);
    			LOGGER.error(e);
    		}
            
            kbd.close();
    	}
        
        
    	public void insertPattern(String accName, Calendar service, HashMap<String, String> naptarak,
    			ArrayList<String> nevSorrend, long startTime, long endTime) {
        //feltoltjuk Event-ekkel a naptárakat az adott megfelelő sorrendben
    		
    	Pattern pattern = new Pattern();
    	Scanner kbd = new Scanner (System.in);
    	System.out.printf("Ténylegesen illeszük be a " + accName + " fiókba a mintát? ");
    	for (String nevOut : nevSorrend) {
    		System.out.printf("%s ",nevOut);
    	}
    	System.out.printf("? (i / n) ");
    	String answer = kbd.nextLine();
    	if(!answer.equals("i")) System.exit(0);
        long twoDaysInMilliSec = 2 * 24 * 60 * 60 * 1000; 
        
        int tempN = 0;
        for (int i = 0; i < nevSorrend.size(); i++) {
    				
        try {
        	//Ahanyadik a naptár, annyiszor 48 óra eltolás viszünk be a kezdőidőpontba.
    		for(Event myEvent : pattern.PatternGen(startTime + twoDaysInMilliSec * i, endTime, nevSorrend.size())){
    			
    		do {
    			
    			try {
    				tempN++;
    				if(myEvent.getId() == null) myEvent = service.events().insert(naptarak.get(nevSorrend.get(i)), myEvent).execute();
    				tempN = 0;
    				LOGGER.info("Event Created in: " +nevSorrend.get(i) + " , " + myEvent.getStart().getDateTime() + " EventId: " + myEvent.getId());
    				//TimeUnit.MILLISECONDS.sleep(50); 
    			} catch (GoogleJsonResponseException e) {
    				System.out.println(e.getStatusCode());
    				System.out.println(e.getMessage());
    				if(e.getStatusCode() == 403) {
    					int waitFor = 1000 * tempN;
    					TimeUnit.MILLISECONDS.sleep(waitFor);
    					LOGGER.info(e);
    					LOGGER.info("Várakozás " + waitFor + "msec ideig.");
    				}
    			}
    			catch (Exception e) {
    				System.out.println(e);
    				LOGGER.error(e);					
    			}
    				
    		} while(tempN > 0);
    				
    		}
    	} catch (Exception e) {
    		System.out.println(e);
    		LOGGER.error(e);
    	}
        }
        kbd.close();
    	}
		
}
