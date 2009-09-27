package tac.program;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Date;
import java.util.Iterator;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;

/**
 * @author Tsai-Yeh Tung (tytung@gmail.com)
 */
public class MapsToSQLiteDBCreator {
	
	private String TABLE_DDL = "CREATE TABLE IF NOT EXISTS tiles (x int, y int, z int, s int, image blob, PRIMARY KEY (x,y,z,s))";
	private String INDEX_DDL = "CREATE INDEX IF NOT EXISTS IND on tiles (x,y,z,s)";
	private String DATA_FILE = "BigPlanet_maps.sqlitedb";
	// IGNORE: When a constraint violation occurs, the one row that contains the constraint violation is not inserted or changed.
	//         But the command continues executing normally.
	private String INSERT_SQL = "INSERT or IGNORE INTO tiles (x,y,z,s,image) VALUES (?,?,?,?,?)";
	private String GET_SQL = "SELECT * FROM tiles WHERE x=? AND y=? AND z=? AND s=?";

	private String extFilename = null;
	private Set<File> fileSet = new TreeSet<File>();
	private static Connection conn = null;
	
	// magic number (need -Xmx512M to prevent java.lang.OutOfMemoryError: Java heap space)
	private final int NUMBER_OF_TILES = 1000; // 2000 will throw OutOfMemoryError
	
	public MapsToSQLiteDBCreator() throws Exception {
		initializeDB();
	}

	public MapsToSQLiteDBCreator(String dbFileName) throws Exception {
		this.DATA_FILE = dbFileName;
		initializeDB();
	}
	
	private void initializeDB() throws ClassNotFoundException, SQLException {
		conn = getConnection();
		Statement stat = conn.createStatement();
		stat.executeUpdate(TABLE_DDL);
		stat.executeUpdate(INDEX_DDL);
		// create table for Android
		stat.executeUpdate("CREATE TABLE IF NOT EXISTS android_metadata (locale TEXT)");
		if (!stat.executeQuery("SELECT * FROM android_metadata").first()) {
			String locale = Locale.getDefault().toString();
			stat.executeUpdate("INSERT INTO android_metadata VALUES ('"+locale+"')");
		}
	}
	
	private Connection getConnection() throws ClassNotFoundException, SQLException {
		String driver = "SQLite.JDBCDriver";
		String url = "jdbc:sqlite:/"+DATA_FILE;
		Class.forName(driver);
		Connection conn = DriverManager.getConnection(url);
		return conn;
	}
	
	private void findImage(File file) throws Exception {
		if (file.isDirectory()) {
			if (fileSet.size() > NUMBER_OF_TILES*2) { // magic number
				addBatchImagesIntoDB(fileSet);
				fileSet.clear();
			}
			String[] files = file.list();
			for (int i = 0; i < files.length; i++)
				findImage(new File(file, files[i])); // recursively
		} else {
			if (extFilename == null) {
				extFilename = file.getName().substring(file.getName().indexOf("."));
			}
			fileSet.add(file); // prepare for batch inserting all files (very fast)
			//addImageIntoDB(file); // insert file (too slow)
		}
	}
	
	/**
	 * 16015 milliseconds while inserting 5625 map tile files (Taiwan Google Maps of zoom 0-13) on Intel Pentium 4 650 3.40GHz
	 */
	private void addBatchImagesIntoDB(Set<File> fileSet) {
		try {
			int s = 0; // Google Maps
			conn.setAutoCommit(false);
			PreparedStatement prepStmt = conn.prepareStatement(INSERT_SQL);
			Iterator<File> iterator = fileSet.iterator();
			int i = 0;
			while(iterator.hasNext()) {
				File imageFile = (File) iterator.next(); // imageFile=y.png.andnav (z/x/y.png.andnav)
				File dir = imageFile.getParentFile();
				int z = Integer.parseInt(dir.getParentFile().getName());
				int x = Integer.parseInt(dir.getName());
				int y = Integer.parseInt(imageFile.getName().split("\\.")[0]);
				InputStream is = new FileInputStream(imageFile);
				// SQL
				prepStmt.setInt(1, x);
				prepStmt.setInt(2, y);
				prepStmt.setInt(3, 17-z); // z=17-zoom (Big Planet format)
				prepStmt.setInt(4, s);
				prepStmt.setBinaryStream(5, is, is.available());
				prepStmt.addBatch();
				is.close();
				if (++i % NUMBER_OF_TILES == 0) { // magic number
					prepStmt.executeBatch();
					conn.commit();
					prepStmt.clearBatch();
				}
				System.out.println("adding "+z+"/"+x+"/"+y+extFilename);
			}
			prepStmt.executeBatch();
			conn.commit();
		} catch (SQLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * 776516 milliseconds while inserting 5625 map tile files (Taiwan Google Maps of zoom 0-13) on Intel Pentium 4 650 3.40GHz
	 */
	private void addImageIntoDB(File file) {
		File dir = file.getParentFile();
		int z = Integer.parseInt(dir.getParentFile().getName());
		int x = Integer.parseInt(dir.getName());
		int y = Integer.parseInt(file.getName().split("\\.")[0]);
		try {
			InputStream is = new FileInputStream(file);
			insert(x, y, z, 0, is);
			is.close();
		} catch (NumberFormatException e) {
			e.printStackTrace();
		} catch (SQLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private boolean isDuplicated(int x, int y, int z, int s) throws SQLException {		
		PreparedStatement prepStmt = conn.prepareStatement(GET_SQL);
		prepStmt.setInt(1, x);
		prepStmt.setInt(2, y);
		prepStmt.setInt(3, 17-z); // z=17-zoom (Big Planet format)
		prepStmt.setInt(4, s);
		ResultSet rs = prepStmt.executeQuery();
		while (rs.next()) {
			rs.close();
			return true;
		}
		rs.close();
		return false;
	}
	
	private void insert(int x, int y, int z, int s, InputStream is) throws SQLException, IOException {
		if (!isDuplicated(x,y,z,s)) {
			PreparedStatement prepStmt = conn.prepareStatement(INSERT_SQL);
			prepStmt.setInt(1, x);
			prepStmt.setInt(2, y);
			prepStmt.setInt(3, 17-z); // z=17-zoom (Big Planet format)
			prepStmt.setInt(4, s);
			prepStmt.setBinaryStream(5, is, is.available());
			prepStmt.executeQuery();
			System.out.println("adding "+z+"/"+x+"/"+y+extFilename);
		} else {
			System.out.println("skipping "+z+"/"+x+"/"+y+extFilename);
		}
	}

	public static void createSQLiteDB(File atlasDir) {
		try {
			MapsToSQLiteDBCreator creator = new MapsToSQLiteDBCreator();
			creator.createActualSQLiteDB(atlasDir, creator);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void createSQLiteDB(File atlasDir, String dbFileName) {
		try {
			MapsToSQLiteDBCreator creator = new MapsToSQLiteDBCreator(dbFileName);
			creator.createActualSQLiteDB(atlasDir, creator);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private void createActualSQLiteDB(File atlasDir, MapsToSQLiteDBCreator creator) {
		try {
			creator.findImage(atlasDir);
			creator.addBatchImagesIntoDB(creator.fileSet);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				conn.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
			creator = null;
		}
	}

	public static void main(String[] args) {
		File imageDir = null;
		String dbFileName = null;
		
		String usage = "Usage: "+MapsToSQLiteDBCreator.class.getSimpleName()+" -in <ImageDir> [-out <SQLiteDB_Name>]";
		
		if (args.length == 0) {
			System.out.println(usage);
			return;
		}
		
		for (int i = 0; i < args.length; i++) {
			if (args[i].equals("-in")) {
				try {
					imageDir = new File(args[++i]);
				} catch (ArrayIndexOutOfBoundsException e) {
					System.out.println(usage);
					return;
				}
				if (!imageDir.isDirectory()) {
					System.out.println(usage);
					System.out.println("\t'"+imageDir.getPath()+"' must be a folder.");
					return;
				}
			} else if (args[i].equals("-out")) {
				try {
					dbFileName =  args[++i];
				} catch (ArrayIndexOutOfBoundsException e) {
					System.out.println(usage);
					return;
				}
			} else {
				System.out.println(usage);
				return;
			}
		}
		
		Date start = new Date();
		if (dbFileName == null) {
			createSQLiteDB(imageDir);
		} else {
			createSQLiteDB(imageDir, dbFileName);
		}
		Date end = new Date();

		System.out.print(end.getTime() - start.getTime());
		System.out.println(" total milliseconds to save all map images into SQLite database.");
	}
	
	// Deletes all files and sub directories under a directory or just deletes a file.
	public static boolean deleteDir(File dir) {
		if (dir.isDirectory()) {
			String[] children = dir.list();
			for (int i = 0; i < children.length; i++) {
				boolean success = deleteDir(new File(dir, children[i]));
				if (!success) {
					return false;
				}
			}
		}
		return dir.delete();
	}

}
