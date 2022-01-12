// Person.java

import java.util.LinkedList;
import java.lang.Double;

/** People are the central actors in the simulation.
 *  @author Douglas W. Jones -- from distributed solution to MP11
 *  @author Jackson Kopesky -- see comments on methods
 *  @author Leon Deng
 *  @version Apr. 26, 2021 cleaner code for population statistics
 *  @see Role for the roles people play
 *  @see Place for the places people visit
 *  @see MyRandom for the source of randomness
 */
public class Person {

    private static enum DiseaseStates {
	uninfected,
	latent,
	asymptomatic,  //
	symptomatic,   //  These 3 states are defined as contageous
	bedridden,     //
	recovered,
	dead; // this must be the last state
	// note that the order of the above enumeration defines the order
	// of the fields of the CSV file output by the simulator.

	int pop = 0; // each disease state has a population
    }

    // timing characteristics of disease state
    private static InfectionRule latent;
    private static InfectionRule asymptomatic;
    private static InfectionRule symptomatic;
    private static InfectionRule bedridden;

    /** Set the disease parameters for the disease states.
     *  <p>This must be called once before simulation starts.
     *  @param l   the infection rule for disease latency
     *  @param a   the infection rule for the asymptomatic phase
     *  @param s   the infection rule for the symptomatic phase
     *  @param b   the infection rule for the bedridden phase
     */
    public static void setDiseaseParameters(
	InfectionRule l, InfectionRule a, InfectionRule s, InfectionRule b
    ) {
	latent = l;
	asymptomatic = a;
	symptomatic = s;
	bedridden = b;
    }

    // linkage from person to place involves a schedule
    private class PlaceSchedule {
	public Place place;
	public Schedule schedule;
	public PlaceSchedule( Place p, Schedule s ) {
	    place = p;
	    schedule = s;
	}
    }

    // instance variables created from model description
    private final Role role;      // role of this person
    private Place home;           // this person's home place, set by emplace
    private final LinkedList<PlaceSchedule> places = new LinkedList<>();

    // instance variables that change as simulation progressses
    private DiseaseStates diseaseState = DiseaseStates.uninfected;
    private Place location;	       // initialized by emplace
    private Simulator.Event happening = null; //current infection
	// for the above, the default 0.0 allows for infection at startup

    // static variables used for all people
    private static LinkedList<Person> allPeople = new LinkedList<Person>();
    private static MyRandom rand = MyRandom.stream;

    /** Construct a new person to perform some role
     *  <p>This constructor deliberately defers putting people in any places.
     *  For each constructed person <code>p</code>, a call must be made to
     *  <code>p.emplace(place,schedule)</code> before simulation begins.
     *  The separation between constructing people and emplacing them allows
     *  for shuffling the set of people in order to randomize the places into
     *  which they fall.
     *  @param r  the role of this person
     */
    public Person( Role r ) {
	role = r;

	allPeople.add( this ); // include this person in the list of all

	diseaseState.pop ++;   // keep the population statistics up to date
    };

    // methods used during model construction, at time 0.0

    /** Associate this person with a particular place and schedule.
     *  <p>Each person must be emplaced before simulation begins.
     *  Emplacing a pereson commits that person to visiting the place
     *  according to the given schedule, and it also places the person
     *  in a home place identified by a null schedule.
     *  @param p  the place
     *  @param s  the associated schedule
     */
    public void emplace( Place p, Schedule s ) {
	if (s != null) {
	    places.add( new PlaceSchedule( p, s ) );
	    s.apply( this, p ); // commit to following schedule s for place p
	} else {
	    assert home == null: "Role guarantees only one home place";
	    home = p;
	    location = home;

	    location.arrive( 0.0, this ); // tell location about new occupant
	}
    }

    // state query

    /** Is this person contageous?
     *  <p>A person is defined as being contageous if they are in any disease
     *  state between <code>asymptomatic</code> and <code>bedridden</code>
     *  inclusive.
     *  @return true if they are
     */
    public boolean isContageous() {
	return
	    (diseaseState.compareTo( DiseaseStates.asymptomatic ) >= 0)
	 && (diseaseState.compareTo( DiseaseStates.bedridden ) <= 0);
    }

    // simulation of behavior

    /** Schedule the time at which a person will be infected.
     *  <p>This may be used to reschedule infection for someone who was
     *  previously scheduled to be infected at a different time.
     *  The actual delay until infection is randomized based on the mean
     *  delay provided.
     *  @param time  the current time
     *  @param meanDelay  the mean delay until infection
     *  @author Jackson Kopesky -- modified to enable
     *  event rescheduling/cancellation
     */
    public void scheduleInfect( double time, double meanDelay ) {
	if (diseaseState == DiseaseStates.uninfected) { // irrelevant if not
	    double delay = rand.nextExponential( meanDelay );
	    double goTime = time + delay;
	    if (happening == null){ //new event
	    	happening = Simulator.schedule( goTime, (double t)-> infect( t ) ); //schedule it
	    } else if (Double.isInfinite(delay) || Double.isNaN(delay)) { //invalid event
	        Simulator.cancel(happening); //cancel it
	        happening = null;
	    } else {
	        Simulator.reschedule( happening, goTime);
	    }
	}
    }

    /** Infect this person.
     *  <p>This is a schedulable event service routine.
     *  <p>This may be called on a person in any infection state but it only
     *  moves the person to <code>latent</code> if they are currently.
     *  <code>uninfected</code>.  
     *  @param time the time of infection
     *  @author Jackson Kopesky -- removed chekc for infectMeTime
     */
    public void infect( double time ) {
	if (diseaseState == DiseaseStates.uninfected) { // no reinfection
	    final double duration = latent.duration();

	    // update population statistics
	    diseaseState.pop--;
	    diseaseState = DiseaseStates.latent;
	    diseaseState.pop++;

	    if (latent.recover()) {
		Simulator.schedule( time + duration, (double t)->recover( t ) );
	    } else {
		Simulator.schedule(
		    time + duration, (double t)-> beContageous( t )
		);
	    }
	}
    }

    /** This person becomes contageous and asymptomatic.
     *  <p>This is a schedulable event service routine.
     *  <p>This may only be called on a person in with a <code>latent</code>
     *  infection and makes the person <code>asymptomatic</code>.
     *  @param time   the time of this state change
     */
    public void beContageous( double time ) {
	assert diseaseState == DiseaseStates.latent : "not latent";
	final double duration = asymptomatic.duration();

	// update population statistics
	diseaseState.pop--;
	diseaseState = DiseaseStates.asymptomatic;
	diseaseState.pop++;

	// tell place that I'm sick
	if (location != null) location.contageous( time, +1 );

	if (asymptomatic.recover()) {
	    Simulator.schedule( time + duration, (double t)-> recover( t ) );
	} else {
	    Simulator.schedule( time + duration, (double t)-> feelSick( t ) );
	}
    }

    /** This person is contageous and starts feeling sick.
     *  <p>This is a schedulable event service routine.
     *  <p>This may only be called on a person in with an
     *  <code>asymptomatic</code> infection and makes them
     *  <code>symptomatic</code>.
     *  makes the person symptomatic.
     *  @param time  the time of this state change
     */
    public void feelSick( double time ) {
	assert diseaseState == DiseaseStates.asymptomatic: "not asymptomatic";
	final double duration = symptomatic.duration();

	// update population statistics
	diseaseState.pop--;
	diseaseState = DiseaseStates.symptomatic;
	diseaseState.pop++;

	if (symptomatic.recover()) {
	    Simulator.schedule( time + duration, (double t)-> recover( t ) );
	} else {
	    Simulator.schedule( time + duration, (double t)-> goToBed( t ) );
	}
    }

    /** This person is contageous and feels so bad they go to bed.
     *  <p>This is a schedulable event service routine
     *  <p>This may only be called on a person in with a
     *  <code>symptomatic</code> infection and makes the person
     *  <code>bedridden</code>.
     *  @param time  the time of this state change
     */
    public void goToBed( double time ) {
	assert diseaseState == DiseaseStates.symptomatic: "not symptomatic";
	final double duration = bedridden.duration();

	// update population statistics
	diseaseState.pop--;
	diseaseState = DiseaseStates.bedridden;
	diseaseState.pop++;

	if (symptomatic.recover()) {
	    Simulator.schedule( time + duration, (double t)-> recover( t ) );
	} else {
	    Simulator.schedule( time + duration, (double t)-> die( t ) );
	}
    }

    /** This person gets better.
     *  <p>This is a schedulable event service routine.
     *  <p>This may be called on a person in any disease state
     *  and leaves the person <code>recovered</code>
     *  and immune from further infection.
     *  @param time   the time of this state change
     */
    public void recover( double time ) {
	// update population statistics
	diseaseState.pop--;
	diseaseState = DiseaseStates.recovered;
	diseaseState.pop++;

	if (location != null) location.contageous( time, -1 );
    }

    /** This person dies
     *  <p>This is a schedulable event service routine.
     *  <p>This may only be called only on a person who is already
     *  <code>bedridden</code>, and it makes that person <code>dead</code>.
     *  @param time  the time of this state change
     */
    public void die( double time ) {
	assert diseaseState == DiseaseStates.bedridden: "not bedridden";
	// update population statistics
	diseaseState.pop--;
	diseaseState = DiseaseStates.dead;
	diseaseState.pop++;

	if (location != null) {
	    location.depart( time, this );
	}

	// no new event is scheduled.
    }

    /** Tell this person to go home at this time
     *  <p>This is a schedulable event service routine.
     *  @param time of the move
     */
    public void goHome( double time ) {
	travelTo( time, home );
    }

    /** Tell this person to go somewhere
     *  <p>This is a schedulable event service routine.
     *  <p>Note that this enforces the rule that <code>bedridden</code>
     *  people never leave home.
     *  @param time  when the person goes there
     *  @param place  where the person goes
     */
    public void travelTo( double time, Place place ) {
	if ((diseaseState != DiseaseStates.bedridden) || (place == home)) {
	    location.depart( time, this );
	    location = place;
	    location.arrive( time, this );
	}
    }

    // reporting tools

    /** Start the logical process of reporting results.
     *  <p>The report is in CSV format sent to <code>system.out</code>
     *  (aka <code>stdout</code>).  If a headline is requested, the first
     *  line gives the names of each column.  All following lines are
     *  numeric, giving the time and the number of people in each disease
     *  state.  The order of the disease states is set by a private
     *  in class <code>Person</code> and disclosed in the headline.
     *  printed here.
     *  @param headline is a headline to be included
     */
    public static void startReporting( boolean headline ) {
	if (headline) {
	    System.out.print( "time" );
	    for (DiseaseStates s: DiseaseStates.values()) {
	        System.out.print( "," );
	        System.out.print( s.name() );
	    }
	    System.out.println();
	}
	// schedule the first report
	Simulator.schedule( 0.0, (double t)-> Person.report( t ) );
    }

    /** Report population statistics at the given time.
     *  <p>This is a schedulable event service routine.
     *  <p>Each report is a CSV line sent to <code>system.out</code>
     *  (aka <code>stdout</code>) giving the time and the
     *  population statistics for each disease state.
     *  @param time  the simulated time of the report
     */
    private static void report( double time ) {
	System.out.print( Double.toString( time/Time.day ) );
	for (DiseaseStates s: DiseaseStates.values()) {
	    System.out.print( "," );
	    System.out.print( Integer.toString( s.pop ) );
	}
	System.out.println();

	// schedule the next report
	Simulator.schedule( time + 24*Time.hour,
	    (double t)-> Person.report( t )
	);
    }

    /** Print out the entire population.
     *  This is needed only in the early stages of debugging
     *  and obviously useless for large populations.
     */
    public static void printAll() {
	for (Person p: allPeople) {
	    // line 1: person id and role
	    System.out.print( p.toString() );
	    System.out.print( " " );
	    System.out.println( p.role.name );

	    // line 2 the home
	    System.out.print( " " ); // indent following lines
	    System.out.print( p.home.kind.name );
	    System.out.print( " " );
	    System.out.print( p.home.toString() );
	    System.out.println();
	    // lines 3 and up: each place and its schedule
	    for (PlaceSchedule ps: p.places ) {
		System.out.print( " " ); // indent following lines
		System.out.print( ps.place.kind.name );
		System.out.print( " " );
		System.out.print( ps.place.toString() );
		assert ps.schedule != null: "guaranteed by PlaceKind";
		System.out.print( ps.schedule.toString() );
		System.out.println();
	    }
	}
    }
}
