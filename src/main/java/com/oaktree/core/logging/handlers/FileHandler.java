package com.oaktree.core.logging.handlers;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.Timer;
import java.util.TimerTask;

import com.oaktree.core.logging.formatters.ConsoleFormatter;


/**
 * For writing directly to the console. Flushes immediately to the writer.
 * This handler has no explicit locking though the PrintStream it uses will use some
 * to ensure entries enter the buffer as contiguous entries.
 * 
 * The handler has a level that can be set; messages with a level that is greater than the set
 * level will be ignored.
 */

public class FileHandler extends AbstractOutputStreamHandler implements IFileLoggingHandler, FileHandlerMBean {
	
	/**
	 * Name of the file we will open an output stream on.
	 */
	private String filename;
	
	/**
	 * How rollers should roll. By time interval (hours, mins etc) or by filesize.
	 */
	private FileRollingType rollingType = FileRollingType.NONE;

	/**
	 * Identifier for file rolling suffix
	 */
	private int rollId = 0;

	
	/**
	 * Depending on the rollingType this will be the number of units to wait until rolling
	 * e.g num of minutes to wait before rolling.
	 */
	private long rollDetail = 0;

	public FileHandler() {
		super();
	}
	public FileHandler(ConsoleFormatter formatter) {
		super(formatter);
	}

	@Override
	public String getFilename() {
		return filename;
	}
	
	
	@Override
	public void setFilename(String filename) {
		this.filename = filename;
	}
	
	/**
	 * Create a new stream for this handler e.g. at start or when rolling to a new file.
	 * @param fname
	 * @return
	 */
	private synchronized FileOutputStream makeStream(String fname) {
		try {
			return new FileOutputStream(new File(fname));
		} catch (Exception e) {
			throw new IllegalArgumentException("Cannot instantiate stream to file " + filename + ": " + e.getMessage());
		}
	}

	/**
	 * Open up new file/stream. Then swap for existing and close the old one.
	 */
	public void roll() {
		OutputStream n = this.makeStream(this.filename + ++rollId);
		/*
		 * TODO as this isnt atomic is there could be attempt to write when
		 * partially through this "flip" operation. Consider synchronization.
		 */
		OutputStream o = this.stream;
		this.stream = n;
		/*
		 * shutdown the old one.
		 */
		try {
			o.flush();
			o.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public FileRollingType getRollingType() {
		return rollingType;
	}
	public void setRollingType(String rollingType) {
		this.rollingType = FileRollingType.valueOf(rollingType);
	}
	public long getRollDetail() {
		return rollDetail;
	}
	public void setRollDetail(String rollDetail) {
		this.rollDetail = Integer.valueOf(rollDetail);
	}

	/**
	 * private timer task used to invoke roll when requested.
	 * @author Oak Tree Designs Ltd
	 *
	 */
	private static class RollingTask extends TimerTask {
		private FileHandler handler;
		public RollingTask(FileHandler handler) {
			this.handler = handler;
		}
		public void run() {
			if (handler != null) {
				handler.roll();
			}
		}
	}
	
	@Override
	public void start() {
		/*
		 * create the file and set the output stream...
		 */
		this.stream = this.makeStream(this.rollingType.equals(FileRollingType.NONE) ? this.filename : this.filename + (++rollId));
		if (!this.rollingType.equals(FileRollingType.NONE)) {
			if (this.rollDetail == 0) {
				return;
			}
			timer = new Timer(); 
			RollingTask rollingTask = new RollingTask(this);
			/*
			 * time (hours/minutes) will just invoke the timer.
			 */
			if (this.rollingType.equals(FileRollingType.MINUTES)) {
				timer.schedule(rollingTask, (long)this.rollDetail * 60l * 1000l, this.rollDetail * 60 * 1000);
			}
			if (this.rollingType.equals(FileRollingType.HOURS)) {
				timer.schedule(rollingTask, (long)this.rollDetail * 60l * 60l * 1000l, this.rollDetail * 60 * 60 * 1000);
			}
		}
	}
	
	@Override
	public void stop() {
		if (this.timer != null) {
			this.timer.cancel();
		}
		super.stop();
		
	}
	
	/**
	 * possibly null timer used for timed rollings.
	 */
	private Timer timer;
	
}
