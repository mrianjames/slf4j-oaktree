package com.oaktree.core.logging.scenarios;


import java.io.File;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.locks.LockSupport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.oaktree.core.logging.Text;

public class LongRunningLogger {
	private final static Logger logger = LoggerFactory.getLogger(LongRunningLogger.class);
	
	public static void main(String[] args) throws Exception {
		final int MAX_WRITES_PER_THREAD = 1000000;
		//final int MAX_WRITES_PER_THREAD = 0;
//		File f = new File("binlog.bin");
//		if (f.exists()) {
//			f.delete();
//		}
		int THREADS = 5;
		if (args.length > 0) {
			THREADS = Integer.valueOf(args[0]);
		}
		System.out.println("Threads: "+THREADS);
		Thread[] threads = new Thread[THREADS];
		final CountDownLatch latch = new CountDownLatch(THREADS);
		for (int x = 0; x < THREADS; x++) {
			final int threadid = x;
			Runnable r = new Runnable(){
				public void run() {
					String T = "T"+threadid;
					Thread.currentThread().setName(T);
					long i = 0;
					while (MAX_WRITES_PER_THREAD == 0 || i < MAX_WRITES_PER_THREAD) {
						logger.info(T+" This is a test message "+i);
						if (MAX_WRITES_PER_THREAD == 0) {
							LockSupport.parkNanos(1000); //was 1 us.
						}
						i++;
					}
					System.out.println("Thread "+threadid + " is complete. I wrote " + i + " messages.");
					latch.countDown();
				}
			};
			threads[x] = new Thread(r);
		}
		long start = System.currentTimeMillis();
		//made threads. now run em.
		for (Thread t:threads) {
			t.start();
		}
		latch.await();
		long end = System.currentTimeMillis();
		Thread.sleep(5000);
		long d = end -start;
		double avg = (double)d/(double)(MAX_WRITES_PER_THREAD*THREADS)*1000d;
		System.out.println("Duration: " + d + " ms. avg: "+Text.to4Dp(avg) + "us per write");
		System.out.println("Exiting");
	}

}
