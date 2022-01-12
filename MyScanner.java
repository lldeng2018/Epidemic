// MyScanner.java

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;
import java.util.regex.Pattern;

/** Support for scanning input files with error reporting
 *  <p>This is a wrapper class around class Scanner.
 *  Ideally, this should extend class <code>java.util.Scanner</code> but
 *  that class is final, precluding extension.
 *  @author Douglas W. Jones
 *  @version Apr. 13, 2021 improved Javadoc usage
 *  @see Error
 *  @see "java.util.Scanner"
 */
public class MyScanner {
    private Scanner sc; // the scanner we are wrapping

    /** Construct a scanner to scan a text file
     *  @param  f  the file to scan from
     *  @throws FileNotFoundException if f cannot be opened for reading
     */
    public MyScanner( File f ) throws FileNotFoundException {
	sc = new Scanner( f );
    }

    // the following methods are ones we wish we could inherit, so we fake it!

    /** a method from class <code>java.util.Scanner</code>.
     *  @return true if there is more text to scan
     */
    public boolean hasNext() { return sc.hasNext(); }

    /** a method from class <code>java.util.Scanner</code>.
     *  @param s  the string to look for
     *  @return true if the next token is a particular literal string
     */
    public boolean hasNext( String s ) { return sc.hasNext( s ); }

    /** a method from class <code>java.util.Scanner</code>.
     *  @return the next token
     */
    public String next() { return sc.next(); }

    // patterns that matter in what follows

    // delimiters are spaces, tabs, newlines and carriage returns
    private static final Pattern delimPat = Pattern.compile( "[ \t\n\r]*" );

    // note that all of the following patterns allow an empty string to match
    // this is used in error detection below

    // if it's not a name, it begins with a non-letter
    private static final Pattern NotNamePat
	= Pattern.compile( "([^A-Za-z]*)|" );

    // names consist of a letter followed optionally by letters or digits
    private static final Pattern namePat
	= Pattern.compile( "([A-Za-z][0-9A-Za-z]*)|" );

    // if it's not an int, it begins with a non-digit, non-negative-sign
    private static final Pattern NotIntPat
	= Pattern.compile( "([^-0-9]*)|" );

    // ints consist of an optional sign followed by at least one digit
    private static final Pattern intPat = Pattern.compile(
	"((-[0-9]|)[0-9]*)"
    );

    // floats consist of an optional sign followed by
    // at least one digit, with an optional point before between or after them
    private static final Pattern floatPat = Pattern.compile(
     "-?(([0-9]+\\.[0-9]*)|(\\.[0-9]+)|([0-9]*))"
    );

    /** Tool to defer computation of messages output by methods of MyScanner.
     *  <p>To pass a specific message, create a subclass of Message to do it
     *  In general, users will not need to use this interface by name;
     *  instead, they will pass parameters using lambda expressions such as:
     *  <pre>
     *  ()-&gt; "Some message" + composed + "as needed"
     *  </pre>
     *  <p>The reason for passing messages this way is because most input is
     *  mostly correct, so there is no reason to construct a complete error
     *  message unless there is an actual error in the input.  Therefore,
     *  instead of passing a string, we pass a lambda expression that allows
     *  the string to be computed in the rare case that it is actually needed.
     */
    public interface Message {
	String myString();
    }

    // new methods added to class Scanner

    /** Scan the next name or complain if it is missing.
     *  <p>Names begin with a letter followed by letters or digits.
     *  If there is no name, the scanner skips until if finds one.
     *  This differs from <code>next()</code> in that the name need not
     *  end with a delimiter but may abut trailing punctuation.
     *  @param defalt  return value if there is no name
     *  @param errorMessage  the message to complain with (lambda expression)
     *  @return the name that was found or defalt if there wasn't one
     */
    public String getNextName( String defalt, Message errorMessage ) {
	// first skip the delimiter, accumulate anything that's not a name
	String notName = sc.skip( delimPat ).skip( NotNamePat ).match().group();

	// second accumulate the name
	String name = sc.skip( namePat ).match().group();

	if (!notName.isEmpty()) { // there's something else a name belonged
	    Error.warn(
		errorMessage.myString() + ": name expected, skipping " + notName
	    );
	}

	if (name.isEmpty()) { // missing name
	    Error.warn( errorMessage.myString() );
	    return defalt;
	} else { // there was a name
	    return name;
	}
    }

    /** get the next integer from the scanner or complain if there is none.
     *  <p>Integers consist of at least one decimal digit in sequence.
     *  If there is no integer, the scanner skips until if finds one.
     *  This number need not
     *  end with a delimiter but may abut trailing punctuation.
     *  @param defalt  return value if there is no next integer
     *  @param errorMessage the message to complain with (lambda expression)
     *  @return the integer that was found or defalt if there wasn't one
     */
    public int getNextInt( int defalt, Message errorMessage ) {
	// first skip the delimiter, accumulate anything that's not an int
	String notInt = sc.skip( delimPat ).skip( NotIntPat ).match().group();

	// second accumulate the int, if any
	String text = sc.skip( delimPat ).skip( intPat ).match().group();

	if (!notInt.isEmpty()) { // there's something else where an int belonged
	    Error.warn(
		errorMessage.myString() + ": int expected, skipping " + notInt
	    );
	}

	if (text.isEmpty()) { // missing name
	    Error.warn( errorMessage.myString() );
	    return defalt;
	} else { // the name was present and it matches intPat
	    return Integer.parseInt( text );
	}
    }

    /** Scan the next floating point number or complain if there is none.
     *  <p>Numbers may be simple integers (no point) or an optional
     *  integer part, a point and an optional fractional part, but a point
     *  standing alone is not a number.  Exponential notation is not currently
     *  supported.  If there is no number, the scanner skips until it finds one.
     *  This number need not
     *  end with a delimiter but may abut trailing punctuation.
     *  @param defalt  return value if there is no next float
     *  @param errorMessage  the message to complain with (lambda expression)
     *  @return the number that was found or defalt if there wasn't one
     */
    public double getNextFloat( double defalt, Message errorMessage ) {
	// skip the delimiter, if any, then the float, if any; get the latter
	String text = sc.skip( delimPat ).skip( floatPat ).match().group();

	if (text.isEmpty()) { // missing name
	    Error.warn( errorMessage.myString() );
	    return defalt;
	} else { // the name was present and it matches intPat
	    return Float.parseFloat( text );
	}
    }

    /** Parameter for <code>tryNextLiteral</code> to recognize begin paren */
    public static final Pattern beginParen = Pattern.compile( "\\(|" );
    /** Parameter for <code>tryNextLiteral</code> to recognize end paren */
    public static final Pattern endParen = Pattern.compile( "\\)|" );
    /** Parameter for <code>tryNextLiteral</code> to recognize dash */
    public static final Pattern dash = Pattern.compile( "-|" );
    /** Parameter for <code>tryNextLiteral</code> to recognize semicolon */
    public static final Pattern semicolon = Pattern.compile( ";|" );

    /** Try to scan the next literal.
     *  <p>If the next input to the scanner is a literal, scan over it.
     *  Otherwise, report that the literal was not present.
     *  This differs from <code>hasNext(String)</code> in two ways:
     *  the literal need not end with a delimiter but may abut what follows,
     *  and if the literal is present, it is skipped.
     *  <p>For technical reasons, the pattern used to specify the literal must
     *  match the empty string as well.  To simplify things, patterns for
     *  popular literals are provided, so most users will write code like this:
     *  <pre>
     *  MyScanner.tryNextLiteral( MyScanner.dash )
     *  </pre>
     *  @param literal  the literal to get
     *  @return true if the literal was present and skipped, false otherwise
     */
    public boolean tryNextLiteral( Pattern literal ) {
	sc.skip( delimPat ); // allow delimiter before literal!
	String s = sc.skip( literal ).match().group();
	return !s.isEmpty();
    }

    /** Scan the next literal or complain if missing.
     *  <p>This uses <code>tryNextLiteral</code> to identify the literal.
     *  @param literal  the literal to get
     *  @param errorMessage  the message to complain with (lambda expression)
     *  @see tryNextLiteral
     */
    public void getNextLiteral( Pattern literal, Message errorMessage ) {
	if ( !tryNextLiteral( literal ) ) {
	    Error.warn( errorMessage.myString() );
	}
    }
}
