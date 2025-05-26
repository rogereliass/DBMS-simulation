package DBMS;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class Table implements Serializable {

    private static final long serialVersionUID = 1L;

    private String tableName;
    public String[] columnNames;
    public ArrayList<Page> pages;
    public int pageSize;
    public ArrayList<String> trace = new ArrayList<>();
    private Set<String> indexedColumns = new HashSet<>();
    private Map<String, BitmapIndex> columnIndexes = new HashMap<>();  // Store BitmapIndex for each indexed column

    private final int defaultPageSize = 5;

    // Constructor for initializing a Table
    public Table(String tableName, String[] columnNames, int pageSize) {
        this.tableName = tableName;
        this.columnNames = columnNames;
        this.pageSize = pageSize;
        this.pages = new ArrayList<>();
    }

    public Table(String tableName, String[] columnNames) {
        this.tableName = tableName;
        this.columnNames = columnNames;
        this.pageSize = defaultPageSize;
        this.pages = new ArrayList<>();
    }

    public String getTableName() {
        return tableName;
    }

    public String[] getColumnNames() {
        return columnNames;
    }

    public ArrayList<Page> getPages() {
        return pages;
    }
    
    public int getPageNumber() {
        return this.pages.size() - 1;
    }


    public void addRecord(String[] record) {
        // If no page exists or the last page is full
        if (pages.isEmpty() || pages.get(pages.size() - 1).isFull()) {
            pages.add(new Page(pageSize));
        }

        pages.get(pages.size() - 1).addRecord(record);

        // Update indexes for the newly added record
        for (String indexedCol : indexedColumns) {
            String value = record[Arrays.asList(columnNames).indexOf(indexedCol)];
            BitmapIndex index = columnIndexes.get(indexedCol);
            if (index != null) {
                index.addValue(value);  // Add value to the BitmapIndex for the column
            }
        }
    }

    public ArrayList<String[]> getAllRecords() {
        ArrayList<String[]> allRecords = new ArrayList<>();
        for (Page page : pages) {
            allRecords.addAll(page.getRecords());
        }
        return allRecords;
    }

    public String[] getRecord(int pageIndex, int recordIndex) {
        if (pageIndex < 0 || pageIndex >= pages.size()) {
            throw new IndexOutOfBoundsException("Page index out of bounds.");
        }

        Page p1 = pages.get(pageIndex);

        if (recordIndex < 0 || recordIndex >= p1.getRecordCount()) {  //note the limit here for testing
            throw new IndexOutOfBoundsException("Record index out of bounds.");
        }
        return pages.get(pageIndex).getRecord(recordIndex);
    }

    // Add index for a column and populate the BitmapIndex
    public void addIndex(String colName) {
        indexedColumns.add(colName);
        BitmapIndex newIndex = new BitmapIndex();
        columnIndexes.put(colName, newIndex);

        // Populate the index with current data in the table
        ArrayList<String[]> allRecords = getAllRecords();
        for (int i = 0; i < allRecords.size(); i++) {
            newIndex.addValue(allRecords.get(i)[Arrays.asList(columnNames).indexOf(colName)]);
        }
    }

    // Get the BitmapIndex for a column
    public BitmapIndex getIndexForColumn(String colName) {
        return columnIndexes.get(colName);
    }

    public boolean hasIndex(String colName) {
        return indexedColumns.contains(colName);
    }

    public Set<String> getIndexedColumns() {
        return indexedColumns;
    }
}
