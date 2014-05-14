package com.oaktree.core.logging.binlog.write;

import java.nio.ByteBuffer;

import com.oaktree.core.logging.ILogRecord;
import com.oaktree.core.logging.binlog.*;
/**
 * Write collections of random sized records to file.
 * each record is written with its own header relating to:
 * num fields (int)|fields
 * each field is written as
 * field type (byte)|field length (int)|field value
 *
 * Created with IntelliJ IDEA.
 * User: IJ
 * Date: 09/04/13
 * Time: 18:44
 * To change this template use File | Settings | File Templates.
 */
public class LogRecordFileBinaryLogWriter extends AbstractFileBinaryLogWriter implements IBinaryLogWriter {

	//timestamp,threadid,level,threadname,message
	final static byte[] schema = new byte[]{ByteUtils.Types.LONG,ByteUtils.Types.SHORT,ByteUtils.Types.SHORT,ByteUtils.Types.STRING_20,ByteUtils.Types.STRING_128}; 
   
	public LogRecordFileBinaryLogWriter(boolean useByteBuffer,String name, String fileName) {
		super(useByteBuffer,schema,name, fileName);
    }


    /**
     * log a log record.
     * @param time
     * @param a
     * @param b
     * @param c
     */
    public void log(long time, short level, short threadid, String tname, String msg) {
    	if (useByteBuffer) {
    		ByteBufferWrapper bbw = bufferManager.get();
    		ByteBuffer buffer = bbw.getByteBuffer();
	        
    		try {
		        buffer.clear();
		        buffer.putLong(time);
		        buffer.putShort(threadid);
		        buffer.putShort(level);
		        ByteUtils.putString(tname, buffer, 20);
		        ByteUtils.putString(msg, buffer, 128);	    	
		        writeBytes(buffer);
		        bufferManager.free(bbw);
    		} catch (Exception e) {
    			System.err.println("Failure to write to buffer " + System.identityHashCode(buffer) + " for msg "+msg + " on tid "+threadid);
    		}
    	} else {
    		UnsafeMemory um = pool.get();
    		um.reset();
    		um.putLong(time);
	        um.putShort(threadid);
	        um.putShort(level);
	        um.putString(tname,20);
	        um.putString(msg,128);
			writeBytes(um.getBytes());
			pool.free(um);
    	}
    }


	public void log(ILogRecord record) {
		log(record.getMillis(), (short)record.getLevel().intValue(), (short)record.getThreadId(), record.getThreadName(), record.getMessage());		
	}


//    protected ByteBuffer getByteBuffer() {
//        return bufferManager.get();
//    }


//    public static void main(String[] args) throws Exception {
//    	boolean useByteBuffer = false;
//        GenericFileBinaryLogWriter logger = new GenericFileBinaryLogWriter(useByteBuffer,new byte[]{ByteUtils.Types.LONG,ByteUtils.Types.LONG},"GFBL","test.bl");
//        logger.setUseByteBuffer(useByteBuffer);
//        logger.start();
//
//        long TESTS = 500000;
//        ResultTimer t = new ResultTimer(10000);
//        for (long l = 0; l < TESTS; l++) {
//            t.startSample();
//            logger.log(new Object[]{12l+l,32l});
//            t.endSample();
//        }
//        System.out.println("Object WriteDuration:" +t.toString(TimeUnit.MICROSECONDS));
//
//        t = new ResultTimer(10000);
//        for (long l = 0; l < TESTS; l++) {
//            t.startSample();
//            logger.log(new long[]{12l+l,32l});
//            t.endSample();
//        }
//        System.out.println("primative WriteDuration:" +t.toString(TimeUnit.MICROSECONDS));
//        logger.stop();
//
//
//        logger = new GenericFileBinaryLogWriter(useByteBuffer,new byte[]{ByteUtils.Types.LONG,ByteUtils.Types.SHORT,ByteUtils.Types.SHORT,ByteUtils.Types.SHORT},"GFBL","test.bl");
//        logger.setUseByteBuffer(useByteBuffer);
//        logger.start();
//
//        t = new ResultTimer(10000);
//        for (long l = 0; l < TESTS; l++) {
//            long time = System.currentTimeMillis();
//            t.startSample();
//            logger.log(time,(short)1,(short)1,(short)1);
//            t.endSample();
//        }
//        System.out.println("set WriteDuration:" +t.toString(TimeUnit.MICROSECONDS));
//
//        logger = new GenericFileBinaryLogWriter(useByteBuffer,new byte[]{ByteUtils.Types.LONG,ByteUtils.Types.LONG,ByteUtils.Types.INT,ByteUtils.Types.LONG,ByteUtils.Types.DOUBLE,ByteUtils.Types.CHAR,ByteUtils.Types.STRING},"GFBL","test.bl");
//        logger.setUseByteBuffer(useByteBuffer);
//        logger.start();
//
//        t = new ResultTimer(10000);
//        for (long l = 0; l < TESTS; l++) {
//            t.startSample();
//            logger.log(new Object[]{12l+l,32l,567,345l,67.8,'a',"Fuckoffbigstringgettingpushed"});
//            t.endSample();
//        }
//        System.out.println("BigObject WriteDuration:" +t.toString(TimeUnit.MICROSECONDS));
//
//    }
}
