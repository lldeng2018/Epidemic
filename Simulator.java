//Simulator.java

import java.util.PriorityQueue;

/** Framework for discrete event simulation
 *  @author  Douglas W. Jones
 *  @version Apr. 19, 2021 Better information hiding for reschedule, cancel.
 */
class Simulator {
    private Simulator() {} // prevent construction of instances!  Don't call!

    /** Functional interface for scheduling actions to be done later
     *  <p> Users will generally never mention Action or trigger because,
     *  when a value implementing this interface is needed, it will usually
     *  take the form of a lambda expression like this:
     *  <pre>
     *  (double t)-&gt; someMethod( t, otherParameters )
     *  </pre>
     */
    public static interface Action {
	void trigger( double time );
    }

    /** Event is the parent of real events scheduled in the simulator
     *  <p>Because class <code>RealEvent</code> is private to class
     *  <code>simulator</code>, users cannot access fields or methods of
     *  class <code>event</code>.  This protects users from dangerous
     *  errors involving access to fields that should not be touched
     */
    public static class Event {}

    /** RealEvents scheduled in the simulation framework
     */
    private static class RealEvent extends Event {
	public double time;       // when will this event occur
	public final Action act;  // what to do then
	public RealEvent( double t, Action a ) {
	    time = t;
	    act = a;
	}
    }

    // the pending event set, holding all scheduled but not triggered events
    private static final PriorityQueue<RealEvent> eventSet
	= new PriorityQueue<>(
	    ( RealEvent e1, RealEvent e2 )-> Double.compare( e1.time, e2.time )
	);

    /** Schedule an event to occur at a future time
     *  <p>Typically, users schedule events using a lambda expression for
     *  the action to be take at the scheduled time, for example:
     *  <pre>
     *    Simulator.schedule( now+later, (double t)-> whatToDo( t, stuff ) );
     *  </pre>
     *  <p>It is important that the time of the event be passed as a lambda
     *  parameter to the action.
     *  <p>In most cases, the caller will ignore the return value because
     *  this is only needed for events that will be cancelled or rescheduled.
     *  @param t, the time of the event
     *  @param a, what to do for that event
     *  @returns a handle on the scheduled event
     */
    public static Event schedule( double t, Action a ) {
	RealEvent e = new RealEvent( t, a );
	eventSet.add( e );
	return e; // the RealEvent is returned as an Event, minus all detail
    }

    /** Cancel a previously scheduled event.
     *  <p>Note that nothing happens if the event being cancelled has
     *  already been simulated or has not been scheduled.
     *  @param e  the event to cancel
     */
    public static void cancel( Event e ) {
	RealEvent re = (RealEvent)e; // This is not free, but it's cheap
				     // only pay this price if we cancel
	eventSet.remove( re );
    }

    /** Re-schedule a previously scheduled event.
     *  <p>Note that nothing happens if the event being rescheduled has
     *  already been simulated or has not been scheduled.
     */
    public static void reschedule( Event e, double t ) {
	RealEvent re = (RealEvent)e; // This is not free, but it's cheap
				     // only pay this price if we reschedule
	if (eventSet.remove( re )) {
	    re.time = t;
	    eventSet.add( re );
	}
    }

    /** Run the simulation
     *  Before running the simulation, schedule the initial events
     *  all of the simulation occurs as side effects of scheduled events
     */
    public static void run() {
	while (!eventSet.isEmpty()) {
	    RealEvent e = eventSet.remove();
	    e.act.trigger( e.time );
	}
    }
}



