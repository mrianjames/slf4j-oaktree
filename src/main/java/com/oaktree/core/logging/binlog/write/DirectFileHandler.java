package com.oaktree.core.logging.binlog.write;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.List;

import com.oaktree.core.logging.ILogRecord;
import com.oaktree.core.logging.Level;
import com.oaktree.core.logging.Text;
import com.oaktree.core.logging.binlog.ByteUtils;
import com.oaktree.core.logging.formatters.IFormatter;
import com.oaktree.core.logging.handlers.ILoggingHandler;

/**
 * Normal handlers delegate formatting to a class that makes a string.
 * This class avoids this and uses a single bytebuffer to render the fields of the
 * logrecord into bytes for direct recording.
 * 
 * @author ianjames
 *
 */
public class DirectFileHandler implements ILoggingHandler {

	private ByteBuffer buffer;
	private int capacity;
	public void setBufferCapacity(String capacity) {
		this.capacity = Integer.valueOf(capacity);
	}
	private boolean direct;
	public void setDirect(String direct) {
		this.direct = Boolean.valueOf(direct);
	}
	private String filename;
	public void setFilename(String fn) {
		this.filename= fn;
	}
	private Level level;
	private String name;
	private File f;
	private FileOutputStream fos;
	private FileChannel fc;

	public void start() {
		if (direct) {
			buffer = ByteBuffer.allocateDirect(capacity);
			System.out.println("Creating a direct buffer of size "+capacity);
			
		} else {
			buffer = ByteBuffer.allocate(capacity);
			System.out.println("Creating a heap buffer of size "+capacity);
			
		}
		if (filename== null) {
			throw new IllegalStateException("No filename specified");
		}
		this.f = new File(filename);
		try {
			this.fos = new FileOutputStream(f);
			this.fc = fos.getChannel();
		} catch (Exception e) {
			throw new IllegalStateException("Invalid file",e);
		}
	}
	@Override
	public void publish(ILogRecord record) {
		try {
			if (record == null || this.level.intValue() > record.getLevel().intValue()) {
				return;
			}
			buffer.clear();
			recordToByteBuffer(record);
			buffer.flip();
			fc.write(buffer);			
			record.onProcessed(this);
		} catch (Throwable t) {
			t.printStackTrace();
		}
	}

	private ByteBuffer recordToByteBuffer(ILogRecord record) {
//		buffer.putLong(record.getMillis());
//        buffer.putShort((short)record.getThreadId());
//        buffer.putShort((short)record.getLevel().intValue());
		ByteUtils.putString(String.valueOf(record.getMillis()), buffer);
		buffer.putChar(' ');
		ByteUtils.putString(String.valueOf(record.getThreadId()), buffer);
		buffer.putChar(' ');
		ByteUtils.putString(record.getLevel().name(), buffer);
		buffer.putChar(' ');
        ByteUtils.putString(record.getThreadName(), buffer);
        buffer.putChar(' ');
        ByteUtils.putString(record.getMessage(), buffer);
        buffer.putChar(Text.NEW_LINE);
		return buffer;
	}
	@Override
	public void setFormatter(IFormatter f) {
		throw new IllegalStateException("Invalid formatter");
	}
	@Override
	public void setLevel(Level l) {
		this.level = l;
	}
	@Override
	public void publish(List<ILogRecord> batch) {
		buffer.clear();
	}
	@Override
	public void stop() {
		try {
			fc.close();
			fos.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	@Override
	public String getName() {
		return name;
	}
	@Override
	public void setName(String name) {
		this.name = name;
	}
}
