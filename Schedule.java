// Schedule.java

/** Tuple of start and end times used for scheduling people's visits to places
 *  @author Douglas W. Jones
 *  @version Apr. 13, 2021 better Javadoc comments
 *  @see Person
 *  @see Place
 *  @see MyScanner for the tools used to read schedules
 *  @see Error for the tools used to report errors in schedules
 *  @see Check for the tools used to check sanity of numbers in schedules
 *  @see Simulator for the tools used to schedule activity under schedules
 *  @see MyRandom for the tools used to assure randomness
 */
public class Schedule {
    // instance variables
    private final double startTime; // times are in seconds anno midnight
    private final double duration;  // duration of visit
    private final double likelihood;// probability this visit will take place

    // source of randomness
    static final MyRandom rand = MyRandom.stream;

    /** Construct a new Schedule.
     *  <p>The Sysntax of a schedule is <code>(0.0-0.0 0.0)</code>
     *  <p>This means <code>(startTime-endTime probability)</code>
     *  <p>That is, a person following this schedule will go to some place
     *  at <code>startTime</code>, stay there until <code>endTime</code>
     *  but they will only ravel with the indicated <code>probability</code>d
     *  on any particular day.
     *  <p>The begin paren must just have been scanned from the input stream
     *  before the constructor call.
     *  @param in  the input stream
     *  @param context  the context for error messages
     */
    public Schedule( MyScanner in, MyScanner.Message context ) {

	// get start time of schedule
	final double st = in.getNextFloat(
	    23.98F, ()-> context.myString() + "(: not followed by start time"
	);
	in.getNextLiteral(
	    MyScanner.dash, ()-> context.myString() + "(" + st
						    + ": not followed by -"
	);
	// get end time of schedule
	final double et = in.getNextFloat(
	    23.99F, ()-> context.myString() + "(" + st
					    + "-: not followed by end time"
	);

	final double lh; // likelihood of move taking place
	if (!in.tryNextLiteral( MyScanner.endParen )) {
	    lh = in.getNextFloat(
		0.0, ()-> context.myString() + "(" + st + '-' + et
					     + "-: not followed by likelihood"
	    );
	    
	    in.getNextLiteral(
		MyScanner.endParen,
		()-> context.myString() + "(" + st + "-" + et + " " + lh
					+ ": not followed by )"
	    );
	} else {
	    lh = 1.0;
	}

	// check sanity constraints on schedule
	if (st >= 24.00F) {
	    Error.warn(
		context.myString() + "(" + st + "-" + et
				   + "): start time is tomorrow"
	    );
	}
	Check.nonNeg( st, 0.0F,
	    ()-> context.myString() + "(" + st + "-" + et
				    + "): start time is yesterday"
	);
	if (st >= et) {
	    Error.warn(
		context.myString() + "(" + st + "-" + et
				   + "): times out of order"
	    );
	}
	Check.nonNeg( lh, 0.0F,
	    ()-> context.myString() + "(" + st + "-" + et + " " + lh
				    + "): likelihood cannot be negative"
	);
	if (lh > 1.0) {
	    Error.warn(
		context.myString() + "(" + st + "-" + et + " " + lh
				   + "): likelihood cannot be over 1.0"
	    );
	}
	startTime = st * Time.hour;
	duration = (et * Time.hour) - startTime;
	likelihood = lh;
    }

    /** Compare two schedules to see if they overlap.
     *  @param s  the schedule to compare with
     *  @return true if they overlap, false otherwise
     */
    public boolean overlap( Schedule s ) {
	if (s == null) return false;
	double thisEnd = this.startTime + this.duration;
	if (this.startTime <= s.startTime) {
	    if (s.startTime <= (this.startTime + this.duration)) return true;
	}
	double sEnd = s.startTime + s.duration;
	if (s.startTime <= this.startTime) {
	    if (this.startTime <= (s.startTime + s.duration)) return true;
	}
	return false;
    }

    /** Commit a person to following a schedule regarding a place.
     *  <p>This starts the logical process of making a person follow
     *  this schedule.
     *  @param person  the person to commit
     *  @param place   the schedule that person will follow
     */
    public void apply( Person person, Place place ) {
	Simulator.schedule( startTime, (double t)-> go( t, person, place ) );
    }

    /** Keep a person on schedule.
     *  <p>This is a schedulable event service routine.
     *  <p>This continues the logical process of moving a person according
     *  to this schedule.
     *  @param person
     *  @param place
     */
    private void go( double time, Person person, Place place ) {
	double tomorrow = time + Time.day;

	// first, ensure that we keep following this schedule
	Simulator.schedule( tomorrow, (double t)-> go( t, person, place ) );

	if (rand.nextFloat() < likelihood) {
	    // second, make the person go there if they take the trip
	    person.travelTo( time, place );

	    // third, make sure we get home if we took the trip
	    Simulator.schedule(
		time + duration, (double t)-> person.goHome( t )
	    );
	}
    }

    /** Convert a Schedule back to textual form.
     *  <p>Useful largely during debugging when it is useful to
     *  reconstruct the simulator input to see if it was read correctly.
     *  @return the schedule as a string
     *  @see Schedule for syntax details
     */
    public String toString() {
	return "(" + startTime/Time.hour
	     + "-" + (startTime + duration) / Time.hour
	     + " " + likelihood + ")";
    }
}
