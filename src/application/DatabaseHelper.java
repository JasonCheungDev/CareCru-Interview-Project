package application;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.sql.*;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.ArrayList;
import java.util.List;

public class DatabaseHelper {

	public void createDatabase(Connection c){
		try {

			Class.forName("org.sqlite.JDBC");
			c = DriverManager.getConnection("jdbc:sqlite::memory:");

			//Check if a database exists
			File file = new File("libraries.db");
			if(file.exists()){
				//if it does load it into memory
				c.createStatement().executeUpdate("restore from libraries.db");

			} else{
				/*Creates the three tables that make up the database
				*LIBRARIES: Holds information about Libraries
				*LIB_BOOKS_BRIDGE: Bridge table connecting Libraries and Books tables
				*BOOKS: Holds information about Books
				*/
				Statement stmt = null;
				stmt = c.createStatement();

				String sql = "CREATE TABLE LIBRARIES " +
				"(LIB_ID INT PRIMARY KEY	NOT NULL," +
				" NAME 			 TEXT 	NOT NULL)";
				stmt. executeUpdate(sql);
				System.out.println("Created LIBRARIES table successfully");

				sql = "CREATE TABLE LIB_BOOKS_BRIDGE " +
				"(LIB_ID INT NOT NULL," +
				" BOOK_ID INT NOT NULL)";
				stmt.executeUpdate(sql);
				System.out.println("Created LIB_BOOKS_BRIDGE table successfully");

				sql = "CREATE TABLE BOOKS " +
				"(BOOK_ID INT PRIMARY KEY	NOT NULL," +
				" TITLE				TEXT	NOT NULL, " +
				" AUTHOR			TEXT 	NOT NULL)";
				stmt.executeUpdate(sql);
				System.out.println("Created BOOKS table successfully");
				stmt.close();

			}
		} catch ( Exception e ) {
			System.err.println( e.getClass().getName() + ": " + e.getMessage() );
			System.exit(0);

		}
	}

	public void insertLibrary(Connection c, Library inputLib, int libID){
		//Three separate prepared statements to insert into the three tables
		PreparedStatement ps = null;
		PreparedStatement ps2 = null;
		PreparedStatement ps3 = null;

	    List<Book> listOfBooks = inputLib.getBooks();

	    try {
	     	ps = c.prepareStatement("INSERT INTO LIBRARIES(LIB_ID, Name) VALUES(?, ?)");
	     	ps.setInt(1, libID);
	     	ps.setString(2,inputLib.getName());
	     	ps.executeUpdate();

	     	System.out.println("Inserted " + inputLib.getName() + " successfully");

	     	for(int i = 0; i <listOfBooks.size(); i++) {
	     		Book book = new Book();
	     		book = listOfBooks.get(i);

	     		ps2 = c.prepareStatement("INSERT INTO BOOKS (BOOK_ID, TITLE, AUTHOR) VALUES(?, ?, ?)");
	      		ps2.setInt(1, book.getId());
	      		ps2.setString(2, book.getTitle());
	      		ps2.setString(3, book.getAuthor());
	      		ps2.executeUpdate();
	      		System.out.println("Inserted " + book.getTitle() + " sucessfully");

	      		ps3 = c.prepareStatement("INSERT INTO LIB_BOOKS_BRIDGE (LIB_ID, BOOK_ID) VALUES(?, ?)"); 
	      		ps3.setInt(1, 1);
	      		ps3.setInt(2, book.getId());
	      		ps3.executeUpdate();

	      		ps.close();
	     		ps2.close();
	     		ps3.close();

	     	}
	     } catch (SQLException e) {
	     	e.printStackTrace();

	     }
	}

	public void deleteLibrary(Connection c, int libID){
		try {


        	Statement stmt = c.createStatement();
	        String sql = "DELETE from LIBRARIES where LIB_ID = " + Integer.toString(libID) + ";";
	        stmt.executeUpdate(sql);

	        sql = "DELETE from LIB_BOOKS_BRIDGE where LIB_ID = " + Integer.toString(libID) + ";";
	        stmt.executeUpdate(sql);

	        sql = "DELETE from BOOKS where BOOK_ID" +
	        " IN (SELECT BOOKS.BOOK_ID FROM BOOKS" +
	        " LEFT JOIN LIB_BOOKS_BRIDGE" +
	        " ON BOOKS.BOOK_ID = LIB_BOOKS_BRIDGE.BOOK_ID" +
	        " WHERE LIB_BOOKS_BRIDGE.BOOK_ID IS NULL);";

	        System.out.println("Success");
	        stmt.executeUpdate(sql);
	        System.out.println("sql success");
	        stmt.close();

      	} catch(SQLException e){
        	e.printStackTrace();
      	}
	}

	public void listLibraries(Connection c) throws IOException{
		try {

			ObjectMapper objectMapper = new ObjectMapper();

			Statement stmt = c.createStatement();
			ResultSet rs = stmt.executeQuery("SELECT * FROM LIBRARIES WHERE LIBRARIES.LIB_ID = 1;");

			Statement stmt2 = c.createStatement();
			ResultSet rs2 = stmt2.executeQuery("SELECT BOOKS.BOOK_ID, TITLE, AUTHOR from LIB_BOOKS_BRIDGE" +
			                                  " JOIN BOOKS ON LIB_BOOKS_BRIDGE.BOOK_ID = BOOKS.BOOK_ID" +
			                                  "  where LIB_BOOKS_BRIDGE.LIB_ID = 1;");

			Map<Integer, Library> database = new HashMap<>();

			while (rs.next()){
			  Library lib = new Library();
			  String name = rs.getString("name");
			  lib.setName(rs.getString("name"));

			  while (rs2.next()){
			    Book bk = new Book();
			    bk.setId(rs2.getInt("book_id"));
			    bk.setTitle(rs2.getString("title"));
			    bk.setAuthor(rs2.getString("author"));

			    lib.addBook(bk);
			  }
			  database.put(rs.getInt("lib_id"),lib);
			}
			System.out.println(objectMapper.writeValueAsString(database));
			stmt.close();
			stmt2.close();

			} catch (SQLException e){
				e.printStackTrace();
			}
	}

	public void closeDB(Connection c){
		try{
		c.createStatement().executeUpdate("backup to libraries.db");
		c.close();
		} catch (Exception e){
		System.err.println( e.getClass().getName() + ": " + e.getMessage() );
        System.exit(0);
		}

	}
}