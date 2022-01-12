// Role.java

import java.util.LinkedList;

/** People in the simulated community each have a role.
 *  <p>Roles create links from people to the categories of places they visit
 *  @author Douglas W. Jones
 *  @version Apr. 13, 2021 better Javadoc comments
 *  @see Person
 *  @see PlaceSchedule
 *  @see MyRandom
 *  @see MyScanner
 */
public class Role {

    // linkage from role to associated place involves a schedule
    private class PlaceSchedule {
	public PlaceKind placeKind;
	public Schedule schedule;
	public PlaceSchedule( PlaceKind p, Schedule s ) {
	    placeKind = p;
	    schedule = s;
	}
    }

    // instance variables

    /** The name of this role */
    public final String name;

    // the list of all of the kinds of places associated with this role
    private final LinkedList<PlaceSchedule> placeKinds = new LinkedList<>();

    private double fraction;  // fraction of the population in this role
    private int number;       // number of people in this role

    // static variables used for summary of all roles
    private static double sum = 0.0F; // sum of all the fractions
    private static LinkedList<Role> allRoles = new LinkedList<Role>();

    /** Scan the description of a new role from some input stream
     *  <p>The stream must contain the following items, in order:
     *  <br>* the role name
     *  <br>* the fraction of the population in that role
     *  <br>* a list of the places associated with that role
     *  <br>* a semicolon
     *  <p>Each place should either be
     *  <br>* the name of the home &ndash; each person has just one home
     *  <br>* some other place name, followed by a <code>schedule</code>
     *  @param in   the input stream
     *  @see Schedule
     */
    public Role( MyScanner in ) {
	PlaceKind homePlaceKind = null; // the home place for this role

	name = in.getNextName( "???", ()-> "role with no name" );
	fraction = in.getNextFloat(
	    9.9999F, ()-> "role " + name + ": not followed by population"
	);

	// get the list of places associated with this role
	boolean hasNext = in.hasNext(); // needed below for missing semicolon
	while (hasNext && !in.tryNextLiteral( MyScanner.semicolon )) {

	    String placeName = in.getNextName( "???",
		()->"role " + name + " " + fraction + ": place name expected"
	    );
	    PlaceKind pk = PlaceKind.findPlaceKind( placeName );
	    Schedule s = null;

	    // is placeName followed a schedule?
	    if (in.tryNextLiteral( MyScanner.beginParen )) {
		s = new Schedule( in, ()-> this.describe() + " " + placeName );
	    }

	    // was it a real place name?
	    if (pk == null) {
		Error.warn(
		    this.describe() + " " + placeName + ": undefined place?"
		);
	    }

	    // see if this role is already associated with PlaceKind pk
	    boolean duplicated = false;
	    boolean overlap = false;
	    if (pk != null) {
		if (pk == homePlaceKind) duplicated = true;
		for (PlaceSchedule ps: placeKinds) {
		    if (ps.placeKind == pk) duplicated = true;
		    if ((ps.schedule != null) && (ps.schedule.overlap(s))) {
			overlap = true;
		    }
		}
	    }
	    if (duplicated) {
		Error.warn(
		    this.describe() + " " + placeName + ": place name reused?"
		);
	    } else if (overlap) {
		Error.warn(
		    this.describe() + " " + placeName + ": schedule overlap?"
		);
	    } else { // only record non-duplicate entries
		placeKinds.add( new PlaceSchedule( pk, s ) );  // schedule all
		if (s == null) {
		    if (homePlaceKind != null) Error.warn(
			this.describe() + " " + placeName + ": a second home?"
		    );
		    homePlaceKind = pk;
		}
	    }
	    hasNext = in.hasNext();
	}
	if (!hasNext) Error.warn( this.describe() + ": missing semicolon?" );

	// complain if the name is not unique
	if (findRole( name ) != null) {
	    Error.warn( this.describe() + ": role name reused?" );
	}
	// force the fraction or population to be positive
	fraction = Check.positive( fraction, 0.0F,
	    ()-> this.describe() + ": negative population?"
	);
	sum = sum + fraction;

	// complain if no places for this role
	if (homePlaceKind == null) {
	    Error.warn( this.describe() + ": no home specified?" );
	}
	if (placeKinds.isEmpty()) {
	    Error.warn( this.describe() + ": has no places?" );
	}

	allRoles.add( this ); // include this role in the list of all roles
    }

    /** Produce a reasonably full textual description of this role.
     *  <p>This is common code used to produce context for error messages.
     *  @return the description
     */
    private String describe() {
	return "role " + name + " " + fraction;
    }

    /** Find a role, by name.
     *  <p>Used to prevent duplicate definition of roles.
     *  As it turns out, there is no case where roles need to be looked up
     *  by name except to prevent duplication.
     *  @param n  the name of the role
     *  @return the role with that name, or null if none has been defined
     */
    private static Role findRole( String n ) {
	for (Role r: allRoles) {
	    if (r.name.equals( n )) return r;
	}
	return null; // role not found
    }

    /** Create the total population, divided up by roles.
     *  <p>Only after all roles have been defined should this be called in
     *  order to actually populate the roles.
     *  <p>In addition to creating people to fill the roles, this code
     *  tells the associated places about the people that live there or
     *  will visit.  This is done in two steps, first sending people to
     *  categories of places, and then populating the specific places.
     *  The division is needed in order to randomize which specific place
     *  in each category gets visited by each person.
     *  @param population  the total population to be created
     *  @param infected  the total number of initially infected people
     *  @see Person
     *  @see PlaceKind
     */
    public static void populateRoles( int population, int infected ) {
	int pop = population; // working copy used only in infection decisions
	int inf = infected;   // working copy used only in infection decisions
	final MyRandom rand = MyRandom.stream;

	if (allRoles.isEmpty()) Error.fatal( "no roles specified" );
	for (Role r: allRoles) {
	    // how many people are in this role
	    r.number = (int)Math.round( (r.fraction / r.sum) * population );

	    // make that many people and infect the right number at random
	    for (int i = 0; i < r.number; i++) {
		Person p = new Person( r );

		// the ratio inf/pop is probability this person is infected
		if (rand.nextFloat() < ((float)inf / (float)pop)) {
		    p.infect( 0.0 );
		    inf = inf - 1;
		}
		pop = pop - 1;

		// each person is associated all their role's place kinds
		// note that this does not create places yet
		for (PlaceSchedule ps: r.placeKinds) {
		    ps.placeKind.populate( p, ps.schedule );
		}
	    }
	}

	// finish putting people in their places
	// this actually creates the places and puts people in them
	PlaceKind.distributePeople();
    }
}
