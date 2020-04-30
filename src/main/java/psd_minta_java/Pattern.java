package psd_minta_java;

import java.util.ArrayList;

import com.google.api.client.util.DateTime;

import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventDateTime;

public class Pattern {
	
	
	/**
	 * Egy darab Event objektumot generál, "n" summaryvel, 12 órával későbbi endtime-mal.
	 * @param shiftStartTime  műszak kezdete Unix time szerint millisec-ben
	 * @return Event
	 */
	public Event eventGen(long shiftStartTime) {

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
     * Egy ArrayList-et ad vissza, amiben egy személy számára benne lesz a generált minta.
     * @param startTime unix time innentől
     * @param origEnd unix time idáig
     * @param numberOfPeople ennyi személy van összesen
     * @return
     * @throws Exception
     */
	public ArrayList<Event> PatternGen(long startTime, long origEnd, int numberOfPeople) throws Exception {
		
		
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
}
