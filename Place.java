// Place.java

import java.util.LinkedList;

/** Places that people are associate with and may occupy.
 *  <p>Every place is an instance of some <code>PlaceKind</code>.
 *  @author Douglas W. Jones
 *  @version Apr. 13, 2021 improved Javadoc comments
 *  @see PlaceKind for most of the attributes of places
 */
public class Place {

    // instance variables fixed at creation

    /** What kind of place is this? */
    public final PlaceKind kind;

    // how dangerous is it to stay here?
    private final double transmissivity;

    // instance variables that vary with circumstances

    // how many infectious people are here
    private int contageous = 0;

    // who is currently in this place?
    private final LinkedList<Person> occupants = new LinkedList<>();

    /** Construct a new place.
     *  @param k  the kind of place
     *  @param t  the transmissivity of the place
     */
    public Place( PlaceKind k, Double t ) {
	kind = k;
	transmissivity = t;
    }

    /** Make a person arrive at a place.
     *  <p>This is a schedulable event service routine.
     *  @param time when the arrival happens
     *  @param p the person involved
     */
    void arrive( double time, Person p ) {
	if (p.isContageous()) contageous( time, +1 );
	occupants.add( p );
    }

    /** Make a person depart from a place.
     *  <p>This is a schedulable event service routine.
     *  @param time when the departure happens
     *  @param p the person involved
     */
    void depart( double time, Person p ) {
	occupants.remove( p );
	if (p.isContageous()) contageous( time, -1 );
    }

    /** Signal that a person in this place has changed their contageon state.
     *  <p>This is a schedulable event service routine but
     *  It is more likely to be called directly from other
     *  event service routines.  It is called when a person arrives or
     *  departs from a place, and also when a person in some place
     *  becomes contageous, recovers or dies.
     *  @param time at which contageon change happens
     *  @param c, +1 means one more is contageous, -1 means one less.
     */
    void contageous( double time, int c ) {
	contageous = contageous + c;

	// when the number of contageous people in a place changes,
	for (Person p: occupants) {
	    p.scheduleInfect( time, 1 / (contageous * transmissivity) );
	}
    }
}
