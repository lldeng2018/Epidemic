// Error.java

/** Error reporting framework
 *  @author Douglas W. Jones
 *  @version Apr. 13, 2021 better Javadoc usage
 *  <p>All error and warning messages go to <tt>System.err</tt>
 *  (aka <tt>stderr</tt>, the standard error stream).
 */
public class Error {
    private static int warningCount = 0;

    /** Report a fatal error.
     *  <p>This never returns, the program terminates reporting failure.
     *  @param msg  error message to be output
     */
    public static void fatal( String msg ) {
	System.err.println( "Epidemic: " + msg );
	System.exit( 1 );  // abnormal termination
    }

    /** Give a non-fatal warning.
     *  <p>This keeps a running count of warnings.
     *  @see exitIfWarnings
     *  @param msg  the warning message
     */
    public static void warn( String msg ) {
	System.err.println( "Warning: " + msg );
	warningCount = warningCount + 1;
    }

    /** Terminate program if there were any warnings.
     *  @param msg  the message to use
     */
    public static void exitIfWarnings( String msg ) {
	if (warningCount > 0) fatal( msg );
    }
}
