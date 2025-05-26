package DBMS;

import java.io.Serializable;
import java.util.ArrayList;

public class Page implements Serializable {
	
	private static final long serialVersionUID = 1L;
	
    private ArrayList<String[]> records;
    private int maxRecords;
    
    public Page(int maxRecords) {
        this.maxRecords = maxRecords;
        this.records = new ArrayList<>();
    }
    
    public boolean isFull() {
        return records.size() >= maxRecords;
    }
    
    public void addRecord(String[] record) {
        if (isFull()) {
            throw new IllegalStateException("Cannot add record: Page is full.");
        }
        records.add(record);
    }
    
    public String[] getRecord(int index) {
        if (index < 0 || index >= records.size()) {
            throw new IndexOutOfBoundsException("Record index out of bounds.");
        }
        return records.get(index);
    }
    
    public ArrayList<String[]> getRecords() {
        return records;
    }
    
    public int getRecordCount() {
        return records.size();
    }
}
