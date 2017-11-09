/**
 * 
 */
package com.java.concurrent.utils.streams.files.writers;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.java.concurrent.utils.streams.common.StreamWriter;
import com.java.concurrent.utils.streams.common.exceptions.StreamIOException;
import com.java.concurrent.utils.streams.common.exceptions.StreamNullableAssignementException;

/**
 * @author Fabrizio Torelli &lt;hellgate75@gmail.com&gt;
 *
 */
public class FlatFileWriter implements StreamWriter<String> {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(FlatFileWriter.class);
	
	private FileWriter writer;
	
	private File file;
	
	private final ConcurrentLinkedQueue<String> queue = new ConcurrentLinkedQueue<>();
	
	private boolean saving = false;

	/**
	 * Constructor
	 * @throws FileNotFoundException 
	 */
	public FlatFileWriter(Path filePath) throws IOException {
		super();
		file = filePath.toFile();
		writer = new FileWriter(file);
	}

	/**
	 * Constructor
	 * @throws FileNotFoundException 
	 */
	public FlatFileWriter(File file) throws IOException {
		super();
		this.file = file;
		writer = new FileWriter(file);
	}

	/**
	 * Constructor
	 * @throws FileNotFoundException 
	 */
	public FlatFileWriter(String filePath) throws IOException {
		super();
		file = new File(filePath);
		writer = new FileWriter(file);
	}

	@Override
	public boolean write(String t) throws StreamNullableAssignementException, StreamIOException {
		if (t==null) {
			throw new StreamNullableAssignementException("Line cannot be null");
		}
		try {
			queue.add(t);
			if (! saving ) {
				saveToFile();
			}
			return true;
		} catch (Exception e) {
			LOGGER.error("Errors adding line <"+t+">", e);
			throw new StreamIOException("Errors adding line <"+t+">", e);
		}
	}

	@Override
	public long write(String... t) throws StreamNullableAssignementException, StreamIOException {
		if (t==null) {
			throw new StreamNullableAssignementException("Array cannot be null");
		}
		try {
			Arrays.asList(t).parallelStream().forEach(queue::add);
			if (! saving ) {
				saveToFile();
			}
			return t.length;
		} catch (Exception e) {
			LOGGER.error("Errors adding lines", e);
			throw new StreamIOException("Errors adding lines", e);
		}
	}

	@Override
	public long write(Collection<String> t) throws StreamNullableAssignementException, StreamIOException {
		if (t==null) {
			throw new StreamNullableAssignementException("Collection cannot be null");
		}
		try {
			t.parallelStream().forEach(queue::add);
			if (! saving ) {
				saveToFile();
			}
			return t.size();
		} catch (Exception e) {
			LOGGER.error("Errors adding lines", e);
			throw new StreamIOException("Errors adding lines", e);
		}
	}
	
	private synchronized void saveToFile() {
		String line = null;
		while (saving = ! queue.isEmpty()) {
			try {
				line = queue.poll();
				if (line==null)
					line="";
				writer.write(line);
				writer.flush();
				line=null;
			} catch (Exception e) {
				LOGGER.error("Errors saving line to file", e);
				if (line!=null) {
					queue.add(line);
				}
				saving = false;
				break;
			}
		}
	}

	@Override
	public void open() throws StreamIOException {
		if (writer==null) {
			try {
				writer = new FileWriter(file);
			} catch (Exception e) {
				LOGGER.error("Errors opening file", e);
				throw new StreamIOException("Errors opening file", e);
			}
		} else {
			LOGGER.warn("Stream already open!!");
		}
	}

	@Override
	public void close() throws StreamIOException {
		synchronized (this) {
			try {
				writer.close();
				writer = null;
			} catch (Exception e) {
				LOGGER.error("Errors closing file", e);
			} finally {
				this.notifyAll();
			}
		}
	}

	@Override
	public boolean isOpen() {
		return writer!=null;
	}

}
