package com.oaktree.core.logging;

import java.text.DateFormat;
import java.text.DecimalFormat;
import java.util.Calendar;
import java.util.Date;

/**
 * Text utilities
 * @author ij
 *
 */
public class Text {

	/**
	 * 2dp number formatter; thread local for thread safety.
	 */
    private static ThreadLocal<DecimalFormat> tf2DpLocal = new ThreadLocal<DecimalFormat>() {
		protected DecimalFormat initialValue() {
			return new DecimalFormat("#,###.##");
		}
	};
	
	/**
	 * 4dp number formatter; thread local for thread safety.
	 */
    private static ThreadLocal<DecimalFormat> tf4DpLocal = new ThreadLocal<DecimalFormat>() {
		protected DecimalFormat initialValue() {
			return new DecimalFormat("#,###.##");
		}
	};
	
	/**
	 * Render a number to 2 dp.
	 * @param number
	 * @return formatted number
	 */
	public final static String to2Dp(double number) {
		return tf2DpLocal.get().format(number);
	}
	
	/**
	 * Render a number to 4 dp.
	 * @param number
	 * @return formatted number
	 */
	public final static String to4Dp(double number) {
		return tf4DpLocal.get().format(number);
	}
	
	public static final char ZERO = '0';

	private static long today;
	
	static {
		Calendar cal = Calendar.getInstance();
		cal.set(Calendar.HOUR_OF_DAY, 0);
		cal.set(Calendar.MINUTE, 0);
		cal.set(Calendar.SECOND, 0);
		cal.set(Calendar.MILLISECOND, 0);
		today = cal.getTime().getTime();
		
	}
	
	/**
	 * Get the millis time for midnight today. Use for getting duration in millis
	 * @return today (midnight in millis)
	 */
	public static long getToday() {
		return today;
	}
	
	/**
	 * Given a long derive a structure that breaks this down
	 * into meaningful time units.
	 * @author ij
	 *
	 */
	public static class Time {
		public long hours;
		public long mins;
		public long secs;
		public long millis;
		public Time(long time) {
			hours = time / 3600000;
			time -= hours * 3600000;
			mins = time / 60000; 
			time -= mins * 60000;
			secs = time / 1000;
			time -= secs * 1000;
			millis = time;
		}
	};
	
	/**
	 * A datetime representation using thread local calendar.
	 * @author ij
	 *
	 */
	public static class DateTime {
		public Time time;
		public long day;
		public long month;
		public long year;
		private ThreadLocal<Calendar> tl = new ThreadLocal<Calendar>(){
			protected Calendar initialValue() {
				return Calendar.getInstance();
			}
		};
		private long millisSinceEpoch;
		public DateTime(long datetime) {
			Calendar c = tl.get();
			c.setTimeInMillis(datetime);
			time = new Time(datetime-today);
			day = c.get(Calendar.DATE);
			month = c.get(Calendar.MONTH)+1;
			year = c.get(Calendar.YEAR);
			millisSinceEpoch = datetime;
		}
		public long getMillisSinceEpoch() {
			return millisSinceEpoch;
		}
	}
	
	/**
	 * Take a duration in millis since midnight and make a nice string out of it.
	 * @param millis
	 * @return time
	 */
	public static String toTime(final long millis) {
		final StringBuilder buffer= new StringBuilder();
		final Time t = new Time(millis);
		if (t.hours / 10 == 0) {
			buffer.append(Text.ZERO);
		}
		buffer.append(t.hours);
		buffer.append(Text.COLON);
		if (t.mins / 10 == 0) {
			buffer.append(Text.ZERO);
		}
		buffer.append(t.mins);
		buffer.append(Text.COLON);
		if (t.secs / 10 == 0) {
			buffer.append(Text.ZERO);
		}
		buffer.append(t.secs);
		buffer.append(Text.PERIOD);
		if (t.millis / 100 == 0) {
			if (t.millis/10 == 0) {
				buffer.append(Text.ZERO);
			}			
			buffer.append(Text.ZERO);
		}
		buffer.append(t.millis);
		
		return buffer.toString();
	}

	
	public final static String SPACE = " ";
	public static final String	BRACES	= "()";
	public static final String NOTHING = "";
	public static final String SEPERATOR = "|";
	public static final String DASH = "-";
	public static final String INCOMING = "INCOMING ";
	public static final String OUTGOING = "OUTGOING ";
	public static final String NOS = "NOS";
	public static final String OCRR = "OCRR";
	public static final String OCR = "OCR";
	public final static String UNKNOWN = "UNKNOWN";
	public final static String BUY = "BUY";
	public final static String SELL = "SELL";	
	public final static String COLON = ":";


	/**
	 * A date formatter per thread.
	 */
	private static ThreadLocal<DateFormat> tfLocal = new ThreadLocal<DateFormat>(){
		protected DateFormat initialValue() {
			return DateFormat.getTimeInstance();
		}
	};

	/**
	 * Render a nice time.
	 * @param start
	 * @return time
	 */
	public static String renderTime(long start) {
		Date dt = new Date(start);
		return tfLocal.get().format(dt);
	}

	public static String renderTime(Date dt) {
		return Text.renderTime(dt.getTime());
	}

	/**
	 * Make a 2 digit string from a number. e.g. 9 is 09.
	 * @param number
	 * @return num2dp
	 */
	public static String twoDigits(long number) {
		if (number < 10) {
			return new StringBuilder("0").append(number).toString();
		}
		return new StringBuilder().append(number).toString();
	}
	/**
	 * Make a 3 digit string from a number e.g. 99 is 099
	 * @param number
	 * @return num3dp
	 */
	public static String threeDigits(long number) {
		StringBuffer buffer = new StringBuffer();
		if (number < 100) {
			buffer.append("0");
		}
		if (number < 10) {
			buffer.append("0");
		}
		buffer.append(number);
		return buffer.toString();
	}

	/**
	 * Constant for the message type for NOS
	 */
	public final static String NEW  = "D";

	/**
	 * Constant for the message type for an amend.
	 */
	public final static String AMEND = "G";

	/**
	 * Constant for the message type for a cancel.
	 */
	public final static String CANCEL = "F";

	public static final char TAB = '\t';
	public static final char NEW_LINE = '\n';

	public static final String EMPTY_BOOK_STRING = "Book Empty";

	public static final String AT = "@";

	public static final String COMMA = ",";

	public static final char UNKNOWN_CHAR = 'x';

	public static final String PERIOD = ".";

	public static final String REG_PERIOD = "[.]";

	public static final String START = "(";
	public static final String END = ")";

	public static final String UNDERSCORE = "_";
	
}
