// Epidemic.java

import java.io.File;
import java.io.FileNotFoundException;

/** The main class of an epidemic simulator.
 *  <p>This class should never be instantiated.
 *  It is just a container for the main method and its (private) support code.
 *  <p>Input items in the model description
 *  begin with one of the following keywords:
 *  <br><tt>population</tt>, an integer and a semicolon
 *  <br><tt>infected</tt>, an integer and a semicolon
 *  <br><tt>latent</tt> followed by an <code>InfectionRule</code>
 *  <br><tt>asymptomatic</tt> followed by an <code>InfectionRule</code>
 *  <br><tt>symptomatic</tt> followed by an <code>InfectionRule</code>
 *  <br><tt>bedridden</tt> followed by an <code>InfectionRule</code>
 *  <br><tt>end</tt> followed by a time (in days) and a semicolon
 *  <br><tt>role</tt> followed by a <code>role</code>
 *  <br><tt>place</tt> followed by a <code>placeKind</code>
 *  <p>Most of the input parsing is done by the constructor for the
 *  indicated object, where the class is listed above.  Each item ends with
 *  a semicolon, so multiple items may be listed on a line and items may
 *  be broken over multiple lines.  A model may include any number of
 *  role and place specifications.
 *  @author Douglas W. Jones
 *  @version Apr. 13, 2021 improved Javadoc comments
 *  @see MyScanner
 *  @see InfectionRule
 *  @see Role
 *  @see PlaceKind
 *  @see Person
 *  @see Simulator
 */
public class Epidemic {

    /** Read and process the details of the model from an input stream.
     *  @param in    the stream to read from
     */
    private static void buildModel( MyScanner in ) {
	int pop = 0;      // the population of the model, 0 = uninitialized
	int infected = 0; // number initially infected, 0 = uninitialized
	double endOfTime = 0.0;  // 0.0 = uninitialized

	// rules describing the progress of the infection
	InfectionRule latent = null;
	InfectionRule asymptomatic = null;
	InfectionRule symptomatic = null;
	InfectionRule bedridden = null;

	while ( in.hasNext() ) { // scan the input file

	    // each item begins with a keyword
	    String keyword = in.getNextName( "???", ()-> "keyword expected" );
	    if ("population".equals( keyword )) {
		int p = Check.posIntSemicolon( in, ()-> "population" );
		if (pop != 0) {
		    Error.warn( "population specified more than once" );
		} else {
		    pop = p;
		}
	    } else if ("infected".equals( keyword )) {
		int i = Check.posIntSemicolon( in, ()-> "infected" );
		if (infected != 0) {
		    Error.warn( "infected specified more than once" );
		} else {
		    infected = i;
		}
	    } else if ("latent".equals( keyword )) {
		if (latent != null) {
		    Error.warn( "latency time specified more than once" );
		}
		latent = new InfectionRule( in, ()-> "latent" );
	    } else if ("asymptomatic".equals( keyword )) {
		if (asymptomatic != null) {
		    Error.warn( "asymptomatic time specified more than once" );
		}
		asymptomatic = new InfectionRule( in, ()-> "asymptomatic" );
	    } else if ("symptomatic".equals( keyword )) {
		if (symptomatic != null) {
		    Error.warn( "symptomatic time specified more than once" );
		}
		symptomatic = new InfectionRule( in, ()-> "symptomatic" );
	    } else if ("bedridden".equals( keyword )) {
		if (bedridden != null) {
		    Error.warn( "bedridden time specified more than once" );
		}
		bedridden = new InfectionRule( in, ()-> "bedridden" );
	    } else if ("end".equals( keyword )) {
		final double et = in.getNextFloat( 1.0F,
		    ()-> "time: end time missing"
		);
		in.getNextLiteral(
		    MyScanner.semicolon, ()-> "end " + et + ": missing ;"
		);
		Check.positive( et, 0.0F,
		    ()-> "end " + et + ": negative end time?"
		);
		if (endOfTime > 0.0) {
		    Error.warn( "end " + et + ": duplicate end time" );
		} else {
		    endOfTime = et;
		}
	    } else if ("role".equals( keyword )) {
		new Role( in );
	    } else if ("place".equals( keyword )) {
		new PlaceKind( in );
	    } else if (keyword == "???") { // there was no keyword
		// == is allowed here 'cause we're detecting the default value
		// we need to advance the scanner here or we'd stick in a loop
		if (in.hasNext()) in.next();
	    } else { // none of the above
		Error.warn( "not a keyword: " + keyword );
	    }
	}

	// check that all required fields are filled in

	if (pop == 0)             Error.warn( "population not given" );
	if (latent == null)       Error.warn( "latency time not given" );
	if (asymptomatic == null) Error.warn( "asymptomatic time not given" );
	if (symptomatic == null)  Error.warn( "symptomatic time not given" );
	if (bedridden == null)    Error.warn( "bedridden time not given" );
	if (endOfTime == 0.0)     Error.warn( "end of time not given" );

	Error.exitIfWarnings( "Aborted due to errors in input" );

	Person.setDiseaseParameters(
	    latent, asymptomatic, symptomatic, bedridden
	);

	Simulator.schedule( // schedule the end of time
	    endOfTime * Time.day, (double t)-> System.exit( 0 )
	);

	// Role is responsible for figuring out how many people per role
	Role.populateRoles( pop, infected );
    }

    /** The main method.
     *  <p>Most of this code is entirely about command line argument processing.
     *  It builds the model and then starts the simulation.
     *  <p>One command line argument is mandatory, name of the file
     *  holding the model description.
     *  @param args  the command line arguments
     */
    public static void main( String[] args ) {
	if (args.length < 1) Error.fatal( "missing file name" );
	if (args.length > 1) Error.warn( "too many arguments: " + args[1] );
	try {
	    buildModel( new MyScanner( new File( args[0] ) ) );
	    // Person.printAll();    // BUG:  potentially useful for debugging
	    Person.startReporting( true ); // start the results report
	    Simulator.run();               // and simulate
	} catch ( FileNotFoundException e ) {
	    Error.fatal( "could not open file: " + args[0] );
	}
    }
}
