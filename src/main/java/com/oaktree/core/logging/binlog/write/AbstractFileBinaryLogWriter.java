package com.oaktree.core.logging.binlog.write;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicLong;

import com.oaktree.core.logging.binlog.ByteBufferWrapper;
import com.oaktree.core.logging.binlog.ByteUtils;
import com.oaktree.core.logging.binlog.UnsafeMemory;
import com.oaktree.core.logging.pool.IObjectFactory;
import com.oaktree.core.logging.pool.IPool;
import com.oaktree.core.logging.pool.SimplePool;

/**
 * Mechanics
 *
 * Created with IntelliJ IDEA.
 * User: IJ
 * Date: 09/04/13
 * Time: 18:35
 * To change this template use File | Settings | File Templates.
 */
public class AbstractFileBinaryLogWriter extends AbstractBinaryLogWriter {
    private String fileName;
    FileOutputStream file;
    FileChannel fc ;
    protected final int bufferSize;
    protected final byte[] schema;
    private String mode = "rw";
    private int poolSize = Runtime.getRuntime().availableProcessors() * 2; //in case of hyper threading.
    
    /**
     * Possible pool of unsafe memory objects.
     */
    protected IPool<UnsafeMemory> pool;
    /**
     * pool of pre allocated byte buffers
     */
    protected IPool<ByteBufferWrapper> bufferManager;
    
    private boolean allocateDirect = true; //setter

    protected boolean useByteBuffer = false;
    
    public void setUseByteBuffer(boolean useByteBuffer) {
    	this.useByteBuffer = useByteBuffer;
    }
    public AbstractFileBinaryLogWriter(boolean useByteBuffer,byte[] schema, String name, String fileName) {
        super(name);
        this.fileName = fileName;
        this.useByteBuffer = useByteBuffer;
        this.schema = schema;
        bufferSize = ByteUtils.calcSchemaSize(schema,256);
        String strPoolSize = System.getProperty("binary.log.poolsize");
    	if (strPoolSize != null) {
    		poolSize = Integer.valueOf(strPoolSize);
    	}
		
        if (useByteBuffer) {
        	bufferManager = new SimplePool<ByteBufferWrapper>(poolSize ,
					new IObjectFactory<ByteBufferWrapper>() {

						@Override
						public ByteBufferWrapper make() {
							if (allocateDirect) {
	        	        		return new ByteBufferWrapper(ByteBuffer.allocateDirect(bufferSize));
	        	        	} else {
	        	        		return new ByteBufferWrapper(ByteBuffer.allocate(bufferSize));
	        	        	}
						}
					}); 
			
        } else {
        	pool = new SimplePool<UnsafeMemory>(poolSize,
					new IObjectFactory<UnsafeMemory>() {

						@Override
						public UnsafeMemory make() {
							byte[] buffer = new byte[bufferSize];
							return new UnsafeMemory(buffer);
						}
					}) {
			};
        }
    }
    MappedByteBuffer mbb;
	private RandomAccessFile f;
	private RandomAccessFile i;
	private MappedByteBuffer mbi;
	private FileChannel ifc;
	long position = 0;
	long size = 200000;
    public void start() {
        super.start();
        System.out.println("Opening file " + fileName);

        try {
        	this.f =new RandomAccessFile(fileName,mode);
        	this.i =new RandomAccessFile(fileName+".index",mode);
        	fc = f.getChannel();
        	ifc = i.getChannel();
            //this.file = new FileOutputStream(fileName);
            //fc = file.getChannel();
        	
        	mbb = fc.map(MapMode.READ_WRITE, 0, size);
        	position = size;
        	mbi = ifc.map(MapMode.READ_WRITE, 0, ByteUtils.calcSchemaSize(new byte[]{ByteUtils.Types.LONG}, 0));
            TimerTask tt = new TimerTask() {
				
				@Override
				public void run() {
					//System.out.println("Forcing");
					mbb.force();
				}
			};
			Timer t = new Timer();
			//t.schedule(tt, 500, 500);
            setCanLog(true);
            System.out.println(fileName+ " is open");
            writeHeader();
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }


    }
    

    
    /**
     * write num fields and byte for each field saying the type.
     * @throws IOException
     */
    private void writeHeader() throws IOException {
    	if (useByteBuffer) {
    		ByteBufferWrapper bbw = bufferManager.get();
			ByteBuffer buf = bbw.getByteBuffer();//ByteBuffer.allocate(4 + schema.length);
			buf.putInt(schema.length);
			for (byte o : schema) {
				buf.put(o);
			}
			writeBytes(buf);
			bufferManager.free(bbw);
    	} else {
	    	UnsafeMemory um = pool.get();
	    	um.putInt(schema.length);
	    	for (byte o:schema) {
	          um.putByte(o);
	    	}
	    	writeBytes(um.getBytes());
	    	pool.free(um);
    	}
    }


    
    public void stop() {
        System.out.println("Closing file " + fileName);
        setCanLog(false);
        try {
        	mbi.force();
        	
        	i.close();
        	ifc.close();
        	f.close();
        	mbb.force();
            fc.force(true);
            fc.close();
            file.close();
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }
        System.out.println(fileName+ " is closed");

    }
    private long pos = 0;
    
    protected void writeBytes(byte[] bytes) {
    	if (canLog()) {
    		try {
    			file.write(bytes);
    		} catch (Exception e) {
                e.printStackTrace();
            }	
    	}
    }
    
    protected void writeBytes(ByteBuffer buf) {
        if (canLog()) {
            try {
                buf.flip();

//                while(buf.hasRemaining()) {
//                    pos +=fc.write(buf);
//                }
                synchronized (this) {
                	if (!mbb.hasRemaining() || mbb.position()+buf.limit() >= mbb.capacity()) {                		
                		mbb.force();
                		long oldpos = mbb.position();
                		long position = oldpos+size;
                		unmap(fc,mbb);
                		this.f.setLength(f.length()+size);
                		fc.close();
                		fc = f.getChannel();
                		mbb = fc.map(MapMode.READ_WRITE, oldpos, size);                		
                		System.out.println("Allocated "+size+"bytes in new mbb: "+oldpos+"->"+position);
                		//mbb.position((int)oldpos);
                	}
                	System.out.println("Writing at pos "+mbb.position()+" "+buf.limit()+" bytes");
	                mbb.put(buf);
	                mbb.force();
	                
	                updateIndex();
                }
                
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    
    private static void unmap(FileChannel fc, MappedByteBuffer bb) throws Exception {
    	Class<?> fcClass = fc.getClass();
    	java.lang.reflect.Method unmapMethod = fcClass.getDeclaredMethod("unmap",new Class[]{java.nio.MappedByteBuffer.class});
    	unmapMethod.setAccessible(true);
    	unmapMethod.invoke(null,new Object[]{bb});    		
	}
    
	private AtomicLong index = new AtomicLong(0);
	private void updateIndex() {
		mbi.putLong(0,index.getAndIncrement());
		
	}
}
