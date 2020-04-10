package psd_minta_java;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.DateTime;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.calendar.Calendar;
//import com.google.api.services.calendar.Calendar.CalendarList;

import com.google.api.services.calendar.model.CalendarList;
import com.google.api.services.calendar.model.CalendarListEntry;


import com.google.api.services.calendar.CalendarScopes;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventDateTime;
import com.google.api.services.calendar.model.Events;


import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.GeneralSecurityException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.TimeZone;

import org.apache.logging.log4j.*;
import java.util.concurrent.TimeUnit;

public class PSD_minta {
	private static final Logger LOGGER = LogManager.getLogger(PSD_minta.class);
    private static final String APPLICATION_NAME = "PSD mintagenerátor JAVA";
    private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();
    private static final String TOKENS_DIRECTORY_PATH = "tokens";

    /**
     * Global instance of the scopes required by this quickstart.
     * If modifying these scopes, delete your previously saved tokens/ folder.
     */
    private static final List<String> SCOPES = Collections.singletonList(CalendarScopes.CALENDAR);
    private static final String CREDENTIALS_FILE_PATH = "/credentials.json";

    /**
     * Creates an authorized Credential object.
     * @param HTTP_TRANSPORT The network HTTP Transport.
     * @return An authorized Credential object.
     * @throws IOException If the credentials.json file cannot be found.
     */
    private static Credential getCredentials(final NetHttpTransport HTTP_TRANSPORT) throws IOException {
        // Load client secrets.
        InputStream in = PSD_minta.class.getResourceAsStream(CREDENTIALS_FILE_PATH);
        if (in == null) {
            throw new FileNotFoundException("Resource not found: " + CREDENTIALS_FILE_PATH);
        }
        GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));

        // Build flow and trigger user authorization request.
        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                HTTP_TRANSPORT, JSON_FACTORY, clientSecrets, SCOPES)
                .setDataStoreFactory(new FileDataStoreFactory(new java.io.File(TOKENS_DIRECTORY_PATH)))
                .setAccessType("offline")
                .build();
        LocalServerReceiver receiver = new LocalServerReceiver.Builder().setPort(8888).build();
        return new AuthorizationCodeInstalledApp(flow, receiver).authorize("user");
    }
    
    
    
    //mintageneráló
    //Event ArrayListet ad vissza
    //Kezdő időponttólponttól a végpontig a minta szerint
    //GMT szerinti unix timeout eszik 
    //Eventeket hoz tesz a listába
	public static ArrayList<Event> PatternGen(long startTime, long origEnd) throws Exception {
		
		if(startTime < 1577746800000L) throw new Exception("2020 előtti a kezdődátum!");
		
		ArrayList<Event> ret = new ArrayList<Event>();
		
		//addig megyünk, míg a kezdőidőpont kisebb, mint a megadott vég
		while (startTime < origEnd) {
			
			// Kettő nappal
			for (int i = 0; i < 2; i++) {
				Event e = eventGen(startTime);
				ret.add(e);
				startTime += 24 * 60 * 60 * 1000;				
			}
						
			//12 óra hozzáadása
			startTime += 12 * 60 * 60 * 1000;
			
			// kettő éjjel
			for (int j = 0; j < 2; j++) {
			
				Event e = eventGen(startTime);
				ret.add(e);
				startTime += 24 * 60 * 60 * 1000;
			}
			
			//12 óra + 7 nap hozzáadása
			startTime += (12 + 7 * 24)  * 60 * 60 * 1000;


		}

		return ret;
	}
	
	//Linux Time-ot millisecben megadjuk, ebből generál egy Event-et.
	public static Event eventGen(long shiftStartTime) {
		
		//Az óraátállítást azzal küszöböljük ki, hogy megkérdezzük mennyi a TimeZoneShift és annyit mindig kivonunk a kezdőidőpontból.
		
		DateTime startDateTime = new DateTime(shiftStartTime);		
		int tzs = startDateTime.getTimeZoneShift();
		long milliSecShift = tzs * 60 * 1000;
		shiftStartTime -= milliSecShift;
		startDateTime = new DateTime(shiftStartTime);
		
		long endTime = shiftStartTime + 12 * 60 * 60 * 1000;
		DateTime endDateTime = new DateTime(endTime);
		
		Event ret = new Event().setSummary("n");
		EventDateTime start = new EventDateTime().setDateTime(startDateTime);
		ret.setStart(start);

		EventDateTime end = new EventDateTime().setDateTime(endDateTime);
		ret.setEnd(end);
		
		
		return ret;
	}
    
	public static void deleteEvents(String calendarId, Calendar service, long startTime, long origEnd) throws Exception {


        DateTime timeFrom = new DateTime(startTime);
        DateTime timeMax = new DateTime(origEnd);
        Events events = service.events().list(calendarId)
                //.setMaxResults(10)
                .setTimeMin(timeFrom)
                .setOrderBy("startTime")
                .setSingleEvents(true)
                .setTimeMax(timeMax)
                .execute();
        List<Event> items = events.getItems();
        if (items.isEmpty()) {
            LOGGER.info("No upcoming events found.");
        } else {
            LOGGER.info("Upcoming events marked for deletion: ");
            for (Event event : items) {
                DateTime start = event.getStart().getDateTime();
                if (start == null) {
                    start = event.getStart().getDate();
                }
                System.out.printf("%s %s (%s) EventiD: %s\n", event.getOrganizer().getDisplayName(),event.getSummary(), start, event.getId());
                LOGGER.info("Törlés: " + event.getOrganizer().getDisplayName() + " " + event.getSummary() + " Kezdet: " +  start + " EventId: " + event.getId());
                if(event.getSummary().equals("n")) service.events().delete(calendarId, event.getId()).execute();                 
            }
        }
	}

    public static void main(String... args) throws IOException, GeneralSecurityException {
    	LOGGER.trace("-------------");
    	LOGGER.trace("App indulása.");
    	
    	
        // Build a new authorized API client service.
        final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
        Calendar service = new Calendar.Builder(HTTP_TRANSPORT, JSON_FACTORY, getCredentials(HTTP_TRANSPORT))
                .setApplicationName(APPLICATION_NAME)
                .build();

        
//        String calendarId = "or0dcbqcn9ih7qd385m6s6n87k@group.calendar.google.com";
//        // List the next 10 events from the primary calendar.
//        DateTime now = new DateTime(System.currentTimeMillis());
//        Events events = service.events().list(calendarId)
//                //.setMaxResults(10)
//                //.setTimeMin(now)
//                .setOrderBy("startTime")
//                .setSingleEvents(true)
//                .execute();
//        List<Event> items = events.getItems();
//        if (items.isEmpty()) {
//            System.out.println("No upcoming events found.");
//        } else {
//            System.out.println("Upcoming events");
//            for (Event event : items) {
//                DateTime start = event.getStart().getDateTime();
//                if (start == null) {
//                    start = event.getStart().getDate();
//                }
//                System.out.printf("%s (%s) EventiD: %s\n", event.getSummary(), start, event.getId());
//            }
//        }
        
        HashMap<String,String> naptarak = new HashMap<String,String>();

     // Iterate through entries in calendar list
     // Megnézzük milyen nevű naptárak léteznek
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
        

        
//        Timestamp in milliseconds: 1578290400000
//        Date and time (GMT): 2020. January 6., Monday 6:00:00
//        Date and time (your time zone): 2020. január 6., hétfő 7:00:00 GMT+01:00
        
//        Timestamp in milliseconds: 1593561600000
//        Date and time (GMT): 2020. July 1., Wednesday 0:00:00
        

        

        
        //Kell legyen pontosan ilyen nevű naptár
        //Ebben már számít a sorrend.
        ArrayList<String> nevSorrend = new ArrayList<String>();
        
        nevSorrend.add("Tamás");
        nevSorrend.add("Imre");
        nevSorrend.add("András");
        nevSorrend.add("Gergő");
        nevSorrend.add("VTamás");
        nevSorrend.add("Sándor");
        
        try {
        	for (String naptarNev : nevSorrend) {
        		//System.out.println(naptarNev);
        		if(!naptarak.containsKey(naptarNev)) throw new Exception("Nincsen ilyen nevű naptár: " + naptarNev);
			}
        } catch(Exception e) {
        	LOGGER.error(e);
        	System.exit(0);
        }
        
        //feltoltjuk Event-ekkel a naptárakat az adott megfelelő sorrendben
        //Ahanyadik a naptár, annyiszor 48 óra eltolás viszünk be a kezdőidőpontba.
        
        //System.out.println(nevSorrend.size());
        
        long twoDayInMilliSec = 2 * 24 * 60 * 60 * 1000; 
        
//        for (int i = 0; i < nevSorrend.size(); i++) {
//					
//        try {
//			for(Event myEvent : PatternGen(1578290400000L + twoDayInMilliSec * i, 1593561600000L)){
//				
//				try {
//				
//					if(myEvent.getId() == null) myEvent = service.events().insert(naptarak.get(nevSorrend.get(i)), myEvent).execute();
//					TimeUnit.MILLISECONDS.sleep(50);        
//				} catch (Exception e) {
//					TimeUnit.SECONDS.sleep(10);
//					System.out.println(e);
//					LOGGER.error(e);
//					if(myEvent.getId() == null) myEvent = service.events().insert(naptarak.get(nevSorrend.get(i)), myEvent).execute();
//					
//				}
//					LOGGER.info("Event Created in: " +nevSorrend.get(i) + " , " + myEvent.getStart().getDateTime() + " EventId: " + myEvent.getId());
//			}
//		} catch (Exception e) {
//			System.out.println(e);
//			//e.printStackTrace();
//		}
        
        
        
//Ez a törlés: ***********************
        
 //       for (int j = 0; j < nevSorrend.size(); j++) {
			
		
        //public static void deleteEvents(String calendarId, Calendar service, long startTime, long origEnd)
//        try {
//        	deleteEvents(naptarak.get(nevSorrend.get(j)), service, 1578290400000L, 1593561600000L);
//        } catch(Exception e) {
//        	LOGGER.catching(e);
//        	
//        	
//        	try {
//        	TimeUnit.SECONDS.sleep(10);
//            deleteEvents(naptarak.get(nevSorrend.get(j)), service, 1578290400000L, 1593561600000L);
//        	} catch(Exception belso) {
//        		System.out.println(belso);
//        	}
//        
//        
//        
//        
//        }  
//        }
        
//***********************************

        LOGGER.trace("App futásának vége.");
}

    
}



