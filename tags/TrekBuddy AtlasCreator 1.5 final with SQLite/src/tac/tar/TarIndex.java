package tac.tar;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Hashtable;

import org.apache.log4j.Logger;

public class TarIndex {

	private static final Logger log = Logger.getLogger(TarIndex.class);
	private File tarFile;
	private RandomAccessFile tarRAFile;
	private Hashtable<String, Integer> tarIndex;

	public TarIndex(File tarFile, Hashtable<String, Integer> tarIndex) throws FileNotFoundException {
		super();
		this.tarFile = tarFile;
		this.tarIndex = tarIndex;
		tarRAFile = new RandomAccessFile(tarFile, "r");
	}

	public byte[] getEntryContent(String entryName) throws IOException {
		Integer off = tarIndex.get(entryName);
		if (off == null)
			return null;
		tarRAFile.seek(off);
		byte[] buf = new byte[512];
		tarRAFile.readFully(buf);
		TarHeader th = new TarHeader();
		th.read(buf);
		int fileSize = th.getFileSizeInt();
		log.trace("reading file " + entryName + " off=" + off + " size=" + fileSize);
		byte[] data = new byte[fileSize];
		tarRAFile.readFully(data);
		return data;
	}

	public int size() {
		return tarIndex.size();
	}

	public void close() {
		try {
			tarRAFile.close();
		} catch (IOException e) {
		}
	}

	public void closeAndDelete() {
		close();
		tarFile.deleteOnExit();
		tarFile.delete();
	}
}