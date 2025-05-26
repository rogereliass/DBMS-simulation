package DBMS;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.Test;

//working till test 7, 13May25 5:45 Roger
public class DBApp
{
	static int dataPageSize = 2;
	
	public static void createTable(String tableName, String[] columnsNames){
		Table table = new Table(tableName, columnsNames, dataPageSize);
		table.trace.add("Table created name:" + tableName + ", columnsNames:" + Arrays.toString(columnsNames));
        boolean stored = FileManager.storeTable(tableName, table);
        
//        if (stored) {
//            System.out.println("Table '" + tableName + "' created successfully.");
//        } else {
//            System.out.println("Error: Failed to store table '" + tableName + "'.");
//        }
        

	}
	
	public static void insert(String tableName, String[] record){
		long start = System.currentTimeMillis();
		Table table = FileManager.loadTable(tableName);
        if (table == null) {
            System.out.println("Error: Table '" + tableName + "' does not exist.");
            return;
        }
        
        table.addRecord(record);
        
        int pageIndex = table.getPages().size() - 1;
        Page updatedPage = table.getPages().get(pageIndex);
        
        long end = System.currentTimeMillis();
        table.trace.add("Inserted:" + Arrays.toString(record) + ", at page number:" + pageIndex + ", execution time (mil):" + (end - start));
		
        boolean tableStored = FileManager.storeTable(tableName, table);
        boolean pageStored = FileManager.storeTablePage(tableName, pageIndex, updatedPage);
        
     // === Update bitmap indexes ===
        for (String indexedCol : table.getIndexedColumns()) {
            // Manually find the column index
            String[] colNames = table.getColumnNames();
            int colIndex = -1;
            for (int i = 0; i < colNames.length; i++) {
                if (colNames[i].equals(indexedCol)) {
                    colIndex = i;
                    break;
                }
            }
            if (colIndex == -1) {
                System.out.println("Warning: Could not find column '" + indexedCol + "' in table '" + tableName + "'.");
                continue;
            }

            String colValue = record[colIndex];
            BitmapIndex index = FileManager.loadTableIndex(tableName, indexedCol);
            if (index == null) {
                System.out.println("Warning: Bitmap index for column '" + indexedCol + "' not found.");
                continue;
            }

            index.addValue(colValue);
            FileManager.storeTableIndex(tableName, indexedCol, index);
        }


//        if (tableStored && pageStored) {
//            System.out.println("Record inserted successfully into table '" + tableName + "'.");
//        } else {
//            System.out.println("Error: Failed to update the table or page on disk.");
//        }
        
	}
	
	
	
	public static ArrayList<String []> select(String tableName){
		long start = System.currentTimeMillis();
		ArrayList<String[]> result = new ArrayList<>();
        
        Table table = FileManager.loadTable(tableName);
        
        if (table == null) {
        	System.out.println("Error: Table- " + tableName + " does not exist.");
        	return result;
        }
        result = table.getAllRecords();
        
        long end = System.currentTimeMillis();
        table.trace.add("Select all pages:" + table.pages.size() + ", records:" + result.size() + ", execution time (mil):" + (end - start));
        
        
        FileManager.storeTable(tableName, table);
        return result;
	}
	
	public static ArrayList<String []> select(String tableName, int pageNumber, int recordNumber){
		long start = System.currentTimeMillis();
		ArrayList<String[]> result = new ArrayList<>();
		Table table = FileManager.loadTable(tableName);
        if (table == null) {
            System.out.println("Error: Table '" + tableName + "' does not exist.");
            return result;
        }
        if (pageNumber < 0 || pageNumber >= table.getPages().size()) {
            System.out.println("Error: Page number out of bounds.");
            return result;
        }
        Page page = table.getPages().get(pageNumber);
        if (recordNumber < 0 || recordNumber >= page.getRecordCount()) {
            System.out.println("Error: Record number out of bounds.");
            return result;
        }
        
        result.add(page.getRecord(recordNumber));
        long end = System.currentTimeMillis();
        table.trace.add("Select pointer page:" + pageNumber + ", record:" + recordNumber + ", total output count:" + result.size() + ", execution time (mil):" + (end - start));
        FileManager.storeTable(tableName, table);
        return result;
	}
	
	public static ArrayList<String []> select(String tableName, String[] cols, String[] vals){
		 long start = System.currentTimeMillis();
		    Table table = FileManager.loadTable(tableName);
		    ArrayList<String[]> result = new ArrayList<>();

		    HashMap<String, Integer> colIndex = new HashMap<>();
		    for (int i = 0; i < table.columnNames.length; i++) {
		        colIndex.put(table.columnNames[i], i);
		    }

		    ArrayList<String> matchPages = new ArrayList<>();
		    int totalMatches = 0;

		    for (int i = 0; i < table.pages.size(); i++) {
		        Page p = FileManager.loadTablePage(tableName, i);
		        int matchCount = 0;

		        for (String[] rec : p.getRecords()) {
		            boolean match = true;
		            for (int j = 0; j < cols.length; j++) {
		                int idx = colIndex.get(cols[j]);
		                if (!rec[idx].equals(vals[j])) {
		                    match = false;
		                    break;
		                }
		            }
		            if (match) {
		                result.add(rec);
		                matchCount++;
		            }
		        }

		        if (matchCount > 0)
		            matchPages.add("[" + i + ", " + matchCount + "]");
		        totalMatches += matchCount;
		    }

		    long end = System.currentTimeMillis();
		    table.trace.add("Select condition:" + Arrays.toString(cols) + "->" + Arrays.toString(vals)
		            + ", Records per page:" + matchPages + ", records:" + totalMatches
		            + ", execution time (mil):" + (end - start));
		    FileManager.storeTable(tableName, table);

		    return result;
	}
	
	public static String getFullTrace(String tableName){ 
	    Table t = FileManager.loadTable(tableName);

	    if (t == null || t.trace == null || t.trace.isEmpty()) {
	        return "No trace found for table: " + tableName;
	    }

	    StringBuilder sb = new StringBuilder();

	    for (String line : t.trace) {
	        sb.append(line).append("\n");
	    }

	    // Calculate page count and record count
	    int pageCount = t.pages.size();
	    int recordCount = 0;

	    for (int i = 0; i < pageCount; i++) {
	        Page p = FileManager.loadTablePage(tableName, i);
	        if (p != null) {
	            recordCount += p.getRecords().size();
	        }
	    }

	    sb.append("Pages Count: ").append(pageCount).append(", Records Count: ").append(recordCount).append(", ");

	    // This line must be last and exactly match test expectation
	    sb.append("Indexed Columns: ").append(new ArrayList<>(t.getIndexedColumns()));

	    return sb.toString();
	}

	
	public static String getLastTrace(String tableName){
		Table t = FileManager.loadTable(tableName);

	    if (t == null || t.trace == null || t.trace.isEmpty()) {
	        return "No trace found for table: " + tableName;
	    }

	    String lastTrace = t.trace.get(t.trace.size() - 1);
	    return lastTrace;
	}
	
	//-----------------------------------------------------------------------------
	//MILESTONE 2
	//-----------------------------------------------------------------------------
	
	//-----------------------------------------------------------------------------
	/**
     * Validates records by checking for missing pages. Returns missing records.
     * Also appends a validation trace entry.
     */
	public static ArrayList<String[]> validateRecords(String tableName) {
	    ArrayList<String[]> missing = new ArrayList<>();

	    try {
	        Table table = FileManager.loadTable(tableName);
	        if (table == null) return missing;

	        int expectedPages = table.getPages().size();
	        HashSet<Integer> existingPages = new HashSet<>();

	        // Scan the actual files on disk to find present pages
	        File tableDir = new File(FileManager.directory.getAbsolutePath() + File.separator + tableName);
	        for (File file : tableDir.listFiles()) {
	            String name = file.getName();
	            if (name.endsWith(".db") && !name.equals(tableName + ".db")) {
	                try {
	                    int pageIndex = Integer.parseInt(name.replace(".db", ""));
	                    existingPages.add(pageIndex);
	                } catch (NumberFormatException ignored) {}
	            }
	        }

	        // Detect missing pages and collect their records
	        for (int i = 0; i < expectedPages; i++) {
	            if (!existingPages.contains(i)) {
	                Page missingPage = table.getPages().get(i);
	                if (missingPage == null) {
	                    // Fallback to disk in case the page is not cached in memory
	                    missingPage = FileManager.loadTablePage(tableName, i);
	                }
	                if (missingPage != null) {
	                    for (String[] rec : missingPage.getRecords()) {
	                        String[] annotated = Arrays.copyOf(rec, rec.length + 1);
	                        annotated[rec.length] = String.valueOf(i);  // Add page index
	                        missing.add(annotated);
	                    }
	                }
	            }
	        }


	        table.trace.add(String.format("Validating records : %d records missing.", missing.size()));
	        FileManager.storeTable(tableName, table);

	    } catch (Exception e) {
	        System.out.println("Error during record validation: " + e.getMessage());
	    }

	    return missing;
	}



    /**
     * Recovers missing records back into their original pages based on the insertion trace.
     *
     * @param tableName the name of the table to recover
     * @param missing   list of missing record arrays returned by validateRecords()
     */
//	public static void recoverRecords(String tableName, ArrayList<String[]> missingRecords) { 
//	    try {
//	        Table table = FileManager.loadTable(tableName);
//	        if (table == null) return;
//
//	        HashMap<Integer, Page> recoveredPages = new HashMap<>();
//
//	        // Iterate through missing records and extract page index correctly
//	        for (String[] fullRecord : missingRecords) {
//	            int lastIndex = fullRecord.length - 1;
//
//	            // The last element should be the page index, parse it
//	            int pageIndex = Integer.parseInt(fullRecord[lastIndex]);
//
//	            // Remove the extra page index from the record (exclude the last element)
//	            String[] record = Arrays.copyOf(fullRecord, fullRecord.length - 1);
//
//	            // Ensure the page index is valid and map the record to the correct page
//	            recoveredPages.putIfAbsent(pageIndex, new Page(table.getPages().get(pageIndex).getRecordCount()));
//	            recoveredPages.get(pageIndex).addRecord(record);
//	        }
//
//	        // Store the recovered pages back to the table
//	        for (Map.Entry<Integer, Page> entry : recoveredPages.entrySet()) {
//	            int pageIndex = entry.getKey();
//	            Page page = entry.getValue();
//	            FileManager.storeTablePage(tableName, pageIndex, page);
//	        }
//
//	        List<Integer> recoveredPageIndices = new ArrayList<>(recoveredPages.keySet());
//	        Collections.sort(recoveredPageIndices);
//
//	        // Update trace with recovery information
//	        table.trace.add("Recovering " + missingRecords.size() + " records in pages: " + recoveredPageIndices + ".");
//
//	        // Store the updated table with the trace
//	        FileManager.storeTable(tableName, table);
//
//	    } catch (NumberFormatException e) {
//	        System.out.println("Error during record recovery: For input string: " + e.getMessage());
//	    } catch (Exception e) {
//	        System.out.println("Error during record recovery: " + e.getMessage());
//	    }
//	}

	public static void recoverRecords(String tableName, ArrayList<String[]> missingRecords) {
	    try {
	        Table table = FileManager.loadTable(tableName);
	        if (table == null || missingRecords == null || missingRecords.isEmpty()) {
	            table.trace.add("Recovering 0 records in pages: [].");
	            FileManager.storeTable(tableName, table);
	            return;
	        }

	        int pageCount = table.getPageNumber(); // Total pages that should exist
	        ArrayList<Integer> missingPageIndices = new ArrayList<>();

	        for (int i = 0; i <= pageCount; i++) {
	            Page page = FileManager.loadTablePage(tableName, i);
	            if (page == null) {
	                missingPageIndices.add(i);
	            }
	        }

	        if (missingPageIndices.isEmpty()) {
	            table.trace.add("Recovering 0 records in pages: [].");
	            FileManager.storeTable(tableName, table);
	            return;
	        }

	        Collections.sort(missingPageIndices);

	        int recordsPerPage = DBApp.dataPageSize;
	        int recordIndex = 0;
	        int recoveredCount = 0;

	        for (int missingIndex : missingPageIndices) {
	            Page newPage = new Page(recordsPerPage);
	            for (int i = 0; i < recordsPerPage && recordIndex < missingRecords.size(); i++) {
	                newPage.addRecord(missingRecords.get(recordIndex++));
	                recoveredCount++;
	            }
	            FileManager.storeTablePage(tableName, missingIndex, newPage);
	        }

	        table.trace.add("Recovering " + recoveredCount + " records in pages: " + missingPageIndices + ".");
	        FileManager.storeTable(tableName, table);

	    } catch (Exception e) {
	        System.out.println("Error during record recovery: " + e.getMessage());
	    }
	}




	
	 /**
     * Creates a bitmap index for the specified column of a table and persists it.
     * @param tableName the name of the table
     * @param colName   the column to index
     */
    public static void createBitMapIndex(String tableName, String colName) {
        try {
            // Load table
            Table table = FileManager.loadTable(tableName);
            if (table == null) {
                System.out.println("Error: Table '" + tableName + "' does not exist.");
                return;
            }
            // Find column index
            String[] cols = table.getColumnNames();
            int colIndex = -1;
            for (int i = 0; i < cols.length; i++) {
                if (cols[i].equalsIgnoreCase(colName)) {
                    colIndex = i;
                    break;
                }
            }
            if (colIndex == -1) {
                System.out.println("Error: Column '" + colName + "' not found in table '" + tableName + "'.");
                return;
            }
            // Build bitmap index
            long start = System.currentTimeMillis();
            BitmapIndex bitmap = new BitmapIndex();
            ArrayList<String[]> allRecs = table.getAllRecords();
            for (String[] rec : allRecs) {
                bitmap.addValue(rec[colIndex]);
            }
            // Persist the bitmap index
            boolean stored = FileManager.storeTableIndex(tableName, colName, bitmap);
            if (stored) {
            	long exec = System.currentTimeMillis() - start;
            	table.trace.add(
            	    String.format(
            	        "Index created for column: %s, execution time (mil):%d",
            	        colName, exec
            	    )
            	);
            	table.addIndex(colName);
                FileManager.storeTable(tableName, table);
            } else {
                System.out.println("Error: Failed to store bitmap index for '" + colName + "'.");
            }
        } catch (Exception e) {
            System.out.println("Error during bitmap index creation: " + e.getMessage());
        }
    }

	
    /**
     * Retrieves the bitmap bitstream for a specific value in a column index.
     * @param tableName the table containing the index
     * @param colName   the column name of the index
     * @param value     the column value to query
     * @return string representation of the bitstream, e.g. "10110"
     */
    public static String getValueBits(String tableName, String colName, String value) {
        String bits = "";
        try {
            // Load the bitmap index
            BitmapIndex bitmap = FileManager.loadTableIndex(tableName, colName);
            if (bitmap == null) {
                System.out.println("Error: Bitmap index on '" + colName + "' does not exist for table '" + tableName + "'.");
                return bits;
            }
            // Retrieve bitstring
            bits = bitmap.getBitString(value);
            // Log trace
            Table table = FileManager.loadTable(tableName);
            if (table != null) {
                table.trace.add(
                	    String.format(
                	        "Bitmap of the value of %s from the %s index: %s",
                	        value, colName, bits
                	    )
                	);

                FileManager.storeTable(tableName, table);
            }
        } catch (Exception e) {
            System.out.println("Error retrieving bitstring: " + e.getMessage());
        }
        return bits;
    }
	
    public static ArrayList<String[]> selectIndex(String tableName, String[] cols, String[] vals) {
        long start = System.currentTimeMillis();

        // Call the original select method
        ArrayList<String[]> result = select(tableName, cols, vals);

        // Load the table again to update the trace
        Table table = FileManager.loadTable(tableName);
        Set<String> indexedColumns = table.getIndexedColumns();

        List<String> indexed = new ArrayList<>();
        ArrayList<String> nonIndexed = new ArrayList<>();

        int indexedSelectionCount = 0;

        for (int i = 0; i < cols.length; i++) {
            if (indexedColumns.contains(cols[i])) {
                indexed.add(cols[i]);
                BitmapIndex index = table.getIndexForColumn(cols[i]);
                if (index != null) {
                    indexedSelectionCount += BitmapIndex.toPositions(index.getBits(vals[i])).size();
                }
            } else {
                nonIndexed.add(cols[i]);
            }
        }

        long end = System.currentTimeMillis();

        String trace = "Select index condition:" + Arrays.toString(cols) + "->" + Arrays.toString(vals);

        if (!indexed.isEmpty()) {
        	Collections.sort(indexed);
            trace += ", Indexed columns: " + indexed;
        }
        if (!nonIndexed.isEmpty()) {
        	Collections.sort(nonIndexed);
        	trace += ", Non Indexed: " + nonIndexed;
        }

        trace += ", Indexed selection count: " + indexedSelectionCount;
        trace += ", Final count: " + result.size();
        trace += ", execution time (mil):" + (end - start);

        // Replace the last trace (which was added by `select(...)`)
        if (!table.trace.isEmpty()) {
            table.trace.set(table.trace.size() - 1, trace);
        } else {
            table.trace.add(trace);
        }

        FileManager.storeTable(tableName, table);
        return result;
    }






    
    
	
	public static void main(String []args) throws IOException{
		
//		FileManager.reset(); 
//		String[] cols = {"id","name","major","semester","gpa"}; 
//		createTable("student", cols); 
//		String[] r1 = {"1", "stud1", "CS", "5", "0.9"}; 
//		insert("student", r1);
//		String[] r2 = {"2", "stud2", "BI", "7", "1.2"}; 
//		insert("student", r2); 
//		String[] r3 = {"3", "stud3", "CS", "2", "2.4"}; 
//		insert("student", r3); 
//		createBitMapIndex("student", "gpa"); 
//		createBitMapIndex("student", "major"); 
//		System.out.println("Bitmap of the value of CS from the major index: "+getValueBits("student", "major", "CS")); 
//		System.out.println("Bitmap of the value of 1.2 from the gpa index: "+getValueBits("student", "gpa", "1.2")); 
//		String[] r4 = {"4", "stud4", "CS", "9", "1.2"}; 
//		insert("student", r4); 
//		String[] r5 = {"5", "stud5", "BI", "4", "3.5"}; 
//		insert("student", r5);
//		System.out.println("After new insertions:"); 
//		System.out.println("Bitmap of the value of CS from the major index: "+getValueBits("student", "major", "CS")); 
//		System.out.println("Bitmap of the value of 1.2 from the gpa index: "+getValueBits("student", "gpa", "1.2")); 
//		System.out.println("Output of selection using index when all columns of the select conditions are indexed:"); 
//		ArrayList<String[]> result1 = selectIndex("student", new String[] {"major","gpa"}, new String[] {"CS","1.2"}); 
//		for (String[] array : result1) { 
//			for (String str : array) { 
//				System.out.print(str + " "); 
//			} 
//			System.out.println(); 
//		} 
//		System.out.println("Last trace of the table: "+getLastTrace("student")); 
//		System.out.println("--------------------------------"); 
//		System.out.println("Output of selection using index when only one column of the columns of the select conditions are indexed:"); 
//		ArrayList<String[]> result2 = selectIndex("student", new String[] {"major","semester"}, new String[] {"CS","5"}); 
//		for (String[] array : result2) { 
//			for (String str : array) { 
//				System.out.print(str + " "); 
//			} 
//			System.out.println(); 
//		} 
//		System.out.println("Last trace of the table: "+getLastTrace("student")); 
//		System.out.println("--------------------------------");
//		System.out.println("Output of selection using index when some of the columns of the select conditions are indexed:"); 
//		ArrayList<String[]> result3 = selectIndex("student", new String[] {"major","semester","gpa" }, new String[] {"CS","5", "0.9"}); 
//		for (String[] array : result3) { 
//			for (String str : array) { 
//				System.out.print(str + " "); 
//			} 
//			System.out.println(); 
//		} 
//		System.out.println("Last trace of the table: "+getLastTrace("student")); 
//		System.out.println("--------------------------------"); 
//		System.out.println("Full Trace of the table:"); 
//		System.out.println(getFullTrace("student")); 
//		System.out.println("--------------------------------"); 
//		System.out.println("The trace of the Tables Folder:"); 
//		System.out.println(FileManager.trace());
//		
//		--------------------------------------------------------------------------------------------------
//		FileManager.reset(); 
//		String[] cols = {"id","name","major","semester","gpa"}; 
//		createTable("student", cols); 
//		String[] r1 = {"1", "stud1", "CS", "5", "0.9"}; 
//		insert("student", r1); 
//		String[] r2 = {"2", "stud2", "BI", "7", "1.2"}; 
//		insert("student", r2); 
//		String[] r3 = {"3", "stud3", "CS", "2", "2.4"}; 
//		insert("student", r3); 
//		String[] r4 = {"4", "stud4", "CS", "9", "1.2"}; 
//		insert("student", r4); 
//		String[] r5 = {"5", "stud5", "BI", "4", "3.5"}; 
//		insert("student", r5);
//		
//		////////This is the code used to delete pages from the table 
//		System.out.println("File Manager trace before deleting pages: "+FileManager.trace()); 
//		String path = FileManager.class.getResource("FileManager.class").toString(); 
//		File directory = new File(path.substring(6,path.length()-17) + File.separator + "Tables//student" + File.separator); 
//		File[] contents = directory.listFiles(); 
//		int[] pageDel = {0,2};
//		for(int i=0;i<pageDel.length;i++) { 
//			contents[pageDel[i]].delete(); 
//		} 
//		////////End of deleting pages code
//		
//		System.out.println("File Manager trace after deleting pages: "+FileManager.trace()); 
//		ArrayList<String[]> tr = validateRecords("student"); 
//		System.out.println("Missing records count: "+tr.size()); 
//		recoverRecords("student", tr); 
//		System.out.println("--------------------------------"); 
//		System.out.println("Recovering the missing records."); 
//		tr = validateRecords("student"); 
//		System.out.println("Missing record count: "+tr.size()); 
//		System.out.println("File Manager trace after recovering missing records: "+FileManager.trace()); 
//		System.out.println("--------------------------------"); 
//		System.out.println("Full trace of the table: "); 
//		System.out.println(getFullTrace("student"));
//		FileManager.reset(); 
//		--------------------------------------------------------------------------------------------
		
		
		
	}
	
	
	
}
