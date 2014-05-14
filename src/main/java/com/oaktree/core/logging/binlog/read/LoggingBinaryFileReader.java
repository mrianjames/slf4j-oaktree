package com.oaktree.core.logging.binlog.read;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.oaktree.core.logging.Level;
import com.oaktree.core.logging.Text;
import com.oaktree.core.logging.binlog.ByteUtils;

/**
 * Reader of binary log file.
 * A file has a schema that we derive by reading the file header.
 * The schema is considered as the header constructed as
 * int - num fields
 * and a byte per field describing the format of the value.
 * @see ByteUtils.Types
 *
 * This class allows a caller to randomly access records within the file and present them as a
 * bytebuffer or as a converted array of objects.
 *
 * Created with IntelliJ IDEA.
 * User: IJ
 * Date: 10/04/13
 * Time: 07:40
 * To change this template use File | Settings | File Templates.
 */
public class LoggingBinaryFileReader implements IBinaryLogReader {
    private static final int MAX_STRING_SIZE = 256;
    private final String fileName;
    private RandomAccessFile file;
    private FileChannel fc;
    private final static Logger log = LoggerFactory.getLogger(LoggingBinaryFileReader.class);
    private byte[] schema;
    private long headerEnd; //header end position.
	private long records;

    public LoggingBinaryFileReader(String fileName) {
        this.fileName = fileName;
    }
    @Override
    public void open() {
        try {
            this.file = new RandomAccessFile(fileName,"r");
            this.fc = file.getChannel();
            
            RandomAccessFile indexfile = new RandomAccessFile(fileName+".index","r");
            FileChannel indexChannel = indexfile.getChannel();
            MappedByteBuffer mbb = indexChannel.map(MapMode.READ_ONLY, 0, indexChannel.size());
            this.schema = readHeader();
            this.records = mbb.getLong();
            indexChannel.close();
            indexfile.close();
            if (log.isTraceEnabled()) {
                log.trace("Schema:" + classesToString(schema));
            }
        } catch (Exception e) {
            log.error("Error opening file " + fileName + ": " + e.getMessage());
        }
   }

    private String classesToString(byte[] schema) {
        final StringBuilder b = new StringBuilder(256);
        for (byte c:schema) {
            b.append(ByteUtils.toDescription(c) +",");
        }
        return b.toString();
    }

    @Override
    public long getNumRecords() {
//        long records = 0;
//        try {
//            long bytes = fc.size();
//            records = bytes/ByteUtils.calcSchemaSize(schema,MAX_STRING_SIZE);
//        } catch (Exception e) {
//            log.error("Error getting filesize: " + e.getMessage());
//        }
        return records;
    }

    /**
     * Gets a record and prints it to logger
     * @param record
     */
    public void printRecord(long record) {
        StringBuilder l = new StringBuilder(256);
        ByteBuffer buffer = getRecord(record);
        if (buffer != null) {
            buffer.flip();
            for (byte b:schema) {
                ByteUtils.writeObjectToStringBuilder(buffer,b,l);
                l.append(",");
            }
        }   else {
            log.warn("Cannot retreive record " + record);
        }
        //log.info("Record " + record +": " + l.toString());
        System.out.println("Record " + record +": " + l.toString());
    }
    
    /**
     * Gets a record and prints it to logger
     * @param record
     */
    public void printLogRecord(long record) {
    	try {
	        StringBuilder l = new StringBuilder(256);
	        ByteBuffer buffer = getRecord(record);
	        if (buffer != null) {
	            buffer.flip();
	            long time = (Long) ByteUtils.getObject(buffer, ByteUtils.Types.LONG);
	            short tid = (Short)ByteUtils.getObject(buffer, ByteUtils.Types.SHORT);
	            short level = (Short)ByteUtils.getObject(buffer, ByteUtils.Types.SHORT);
	            String tname = (String)ByteUtils.getObject(buffer, ByteUtils.Types.STRING_20);
	            String msg = (String)ByteUtils.getObject(buffer, ByteUtils.Types.STRING_128);
	            l.append(Text.toTime(time-Text.getToday()));
	            l.append(Text.SPACE);
	            l.append(tid);
	            l.append(Text.SPACE);
	            l.append(tname);
	            l.append(Text.SPACE);
	            l.append(Level.fromInt(level));
	            l.append(Text.SPACE);
	            l.append(msg);
	            System.out.println(l.toString());
	        }   else {
	            log.warn("Cannot retreive record " + record);
	        }
    	} catch (Exception e) {
    		System.err.println("Cannot parse record "+record);
    		e.printStackTrace();
    	}
        //log.info("Record " + record +": " + l.toString());
        
    }

    /**
     * Gets a record as a bytebuffer
     * @param record
     * @return record as a byte buffer.
     */
    public ByteBuffer getRecord(long record) {
        try {
            int recordSize = ByteUtils.calcSchemaSize(schema,MAX_STRING_SIZE);
            long position = headerEnd + (record * recordSize);
            fc.position(position);
            ByteBuffer buffer = ByteBuffer.allocate(recordSize);
            fc.read(buffer,position);
            //buffer.flip();
            return buffer;
        } catch (Exception e) {
            log.error(e.getMessage());
        }
        return null;
    }

    /**
     * Convenience static method to avoid creating the object etc.
     * @param fileName
     * @param index
     * @return record
     */
    public static Object[] getRecordByIndex(String fileName, long index) {
        LoggingBinaryFileReader r = new LoggingBinaryFileReader(fileName);
        r.open();
        Object[] objects = r.getRecordAsObjects(index);
        r.close();
        return objects;
    }

    @Override
    public Object[] getRecordAsObjects(long index) {
        ByteBuffer buffer = getRecord(index);
        List<Object> o = new ArrayList<Object>();
        if (buffer != null) {
            buffer.flip();
            for (byte b : getSchema()) {
                o.add(ByteUtils.getObject(buffer, b));
            }
        }
        return o.toArray(new Object[o.size()]);
    }



    public byte[] readHeader() throws IOException {
       ByteBuffer buf = ByteBuffer.allocate(4);
       fc.read(buf);
       buf.flip();
       int fields = buf.getInt();

       buf = ByteBuffer.allocate(fields);
       fc.read(buf,4);
       buf.flip();

       byte[] classes = new byte[fields];
       for (int i=0;i<fields;i++) {
           classes[i] = buf.get();
       }
       this.headerEnd = 4+fields;

       return classes;
   }

    @Override
    public void close() {
        try {
            this.fc.close();
            log.info("File "+fileName+" closed");
        } catch (Exception e) {
            log.error("Error opening file " + fileName + ": " + e.getMessage());
        }
    }

    public final static void printAll(String fileName) {
        LoggingBinaryFileReader r = new LoggingBinaryFileReader(fileName);
        r.open();
        long numRecords = r.getNumRecords();
        for (long l = 0;l < numRecords;l++) {
            r.printRecord(l);
        }
        r.close();
    }
    public static long getNumberRecords(String fileName) {
        LoggingBinaryFileReader r = new LoggingBinaryFileReader(fileName);
        r.open();
        long numRecords = r.getNumRecords();
        r.close();
        return numRecords;
    }


    public byte[] getSchema() {
        return schema;
    }

    @Override
    public boolean validate() {
        //validate the schema holds for every record
        long numRecords = getNumRecords();
        Object[] values = null;
        for (long l = 0;l < numRecords;l++) {
            try {
                values = getRecordAsObjects(l);
                if (((Short)values[2]).intValue() == 0) {
                	throw new IllegalStateException("Dodgy at " + l);
                }
            } catch (Exception e) {
                log.warn("Record " + l + " failed validation: " + e.getMessage());
                return false;
            }
        }
        log.info("File " + fileName + " is validated");
        return true;
    }


    /**
     * SAMPLE TEST RUNS.
     * @param args
     */
    public static void main(String[] args) {
        //GenericFileBinaryLogReader.printAll("test.bl");

        LoggingBinaryFileReader reader = new LoggingBinaryFileReader("binlog.bin");
        reader.open();
        if (!reader.validate()) {
        	throw new IllegalStateException("Cannot validate file");
        }
        long numRecords = reader.getNumRecords();
        log.info("Records: " + numRecords);
        //ResultTimer timer = new ResultTimer(10000);
        long start = System.nanoTime();
        //List<Long> durations = new ArrayList<Long>();
        long dt = 0;
        for (long l =0; l < numRecords; l++) {
            long s = System.nanoTime();
            //Object[] record = reader.getRecordAsObjects(l);
            reader.printLogRecord(l);
            long e = System.nanoTime();
            dt += (e-s);
            //log.info("Record #"+l+": " + record);
        }
        log.info("Avg: " + (dt/numRecords)/1000+" us per read");
        log.info("Record 500:");
        reader.printLogRecord(500);
        //avg 15us to read random.
        reader.close();
    }

}

