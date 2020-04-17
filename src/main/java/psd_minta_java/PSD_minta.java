package psd_minta_java;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
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
import java.util.Scanner;


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
    
    
    

    /**
     * Egy ArrayList-et ad vissza, amiben egy személy számára benne lesz a generált minta.
     * @param startTime unix time innentől
     * @param origEnd unix time idáig
     * @param numberOfPeople ennyi személy van összesen
     * @return
     * @throws Exception
     */
	public static ArrayList<Event> PatternGen(long startTime, long origEnd, int numberOfPeople) throws Exception {
		
		if(startTime < 1577746800000L) throw new Exception("2020 előtti a kezdődátum!");
		if(numberOfPeople < 2) throw new Exception("Kettőnél több ember kell.");
		
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
			
			//12 óra + 7 nap hozzáadása 6 ember esetén, +8 nap lenne, de fent már hozzáad 1 napot a 2 éjjel végén.
			//startTime += (12 + 7 * 24)  * 60 * 60 * 1000;
			//3-nál 12 óra + 2 nap, 4-nél 12 óra + 4 nap
			//5: 12 6, 6: 12 8,
			// Tehát: -12 óra + (emberszám - 2) * 2 nap

			startTime += (-12 + ((numberOfPeople - 2) * 48)) * 60 * 60 * 1000;

		}

		return ret;
	}
	

	/**
	 * Egy darab Event objektumot generál, "n" summaryvel, 12 órával későbbi endtime-mal.
	 * @param shiftStartTime  műszak kezdete Unix time szerint millisec-ben
	 * @return Event
	 */
	public static Event eventGen(long shiftStartTime) {

		// Az óraátállítást azzal küszöböljük ki, hogy megkérdezzük mennyi a
		// TimeZoneShift és annyit mindig kivonunk a kezdőidőpontból.

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
    
	/**
	 * "n" summaryjű eseményeket töröl egy adott ID-jű naptárból a kezdő- és végidőpont között.
	 * 
	 * @param calendarId  
	 * @param service  authorized API client service
	 * @param startTime
	 * @param origEnd
	 * @throws Exception
	 */
	public static void deleteEvents(String calendarId, String owner, Calendar service, long startTime, long origEnd)
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
	public static void deleteBetweenForEveryone(Calendar service, HashMap<String, String> naptarak,
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
	
	public static void askToDelete(String accName, Calendar service, HashMap<String, String> naptarak,
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
	
	
	public static void insertPattern(String accName, Calendar service, HashMap<String, String> naptarak,
			ArrayList<String> nevSorrend, long startTime, long endTime) {
    //feltoltjuk Event-ekkel a naptárakat az adott megfelelő sorrendben
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
		for(Event myEvent : PatternGen(startTime + twoDaysInMilliSec * i, endTime, nevSorrend.size())){
			
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


    public static void main(String... args) throws IOException, GeneralSecurityException {
    	LOGGER.trace("-------------");
    	LOGGER.trace("App indulása.");
    	
    	
        // Build a new authorized API client service.
        final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
        Calendar service = new Calendar.Builder(HTTP_TRANSPORT, JSON_FACTORY, getCredentials(HTTP_TRANSPORT))
                .setApplicationName(APPLICATION_NAME)
                .build();

        
        CalendarListEntry MycalendarListEntry = service.calendarList().get("primary").execute();
        String accName = MycalendarListEntry.getSummary();
        
        HashMap<String,String> naptarak = new HashMap<String,String>();
        

     // Iterate through entries in calendar list
     // Megnézzük milyen nevű naptárak léteznek, eltároljuk a naptárnév és ID párosokat.
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
        

//        1593842400
//        Is equivalent to:
//        07/04/2020 @ 6:00am (UTC)
//        1609459200
//        Is equivalent to:
//        01/01/2021 @ 12:00am (UTC)
//        1593561600
//        Is equivalent to:
//        07/01/2020 @ 12:00am (UTC)

        long startTime = 1593842400000L;
        //long startTime = 1593561600000L;
        long endTime = 1609459200000L;
        
        System.out.println("Kezdőidőpont: "+new DateTime(startTime));
        System.out.println("Végidőpont: "+new DateTime(endTime));
        
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
        
        insertPattern(accName, service, naptarak, nevSorrend, startTime, endTime);
        



        try {
			//askToDelete(accName, service, naptarak, nevSorrend, startTime, endTime);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        

        

        LOGGER.trace("App futásának vége.");
    }
}




