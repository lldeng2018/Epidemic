// MyRandom.java

import java.util.Random;

/** Wrapper extending class Random, turning it into a singleton class.
 *  <p>Ideally, no user should ever create an instance of Random, all use this!
 *  @author Douglas W. Jones
 *  @version Apr. 6, 2021 lifted from Epidemic.java of that date
 *  @see Random
 */
public class MyRandom extends Random {
    /** the only random number stream
     */
    public static final MyRandom stream = new MyRandom(); // the only stream;

    // nobody can construct a MyRandom except the above line of code
    private MyRandom() {
	super();
    }

    /* alternative access to the only random number stream
     * @return the only stream
     */
    public static MyRandom stream() {
	return stream;
    }

    // add distributions that weren't built in

    /** Exponential distribution.
     *  @param mean  the mean value of the distribution
     *  @return  a positive exponentially distributed random value
     */
    public double nextExponential( double mean ) {
	return mean * -Math.log( this.nextDouble() );
    }

    /** Log-normal distribution.
     *  @param median    the median value of the distribution
     *  @param sigma     the sigma of the underlying normal distribution
     *  @return a log-normally distributed random value
     */
    public double nextLogNormal( double median, double sigma ) {
	return Math.exp( sigma * this.nextGaussian() ) * median;
    }
}
