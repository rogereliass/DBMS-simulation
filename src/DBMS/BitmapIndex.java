package DBMS;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * BitmapIndex maintains a bitmap for each distinct column value.
 * Each bit corresponds to a record insertion order.
 */
public class BitmapIndex implements Serializable {
    private static final long serialVersionUID = 1L;

    // Map from column value to its bitmap represented as a list of booleans
    private HashMap<String, ArrayList<Boolean>> index;
    // Number of records inserted so far (i.e., length of each bitmap)
    private int recordCount;

    /**
     * Constructs an empty BitmapIndex.
     */
    public BitmapIndex() {
        this.index = new HashMap<>();
        this.recordCount = 0;
    }
    
    // Get the list of record indices (or positions) that match the given value
    public List<Integer> getRecordsForValue(String value) {
        List<Boolean> bits = getBits(value);
        return toPositions(bits);
    }

    /**
     * Adds a new record value to the index.
     * Extends all existing bitmaps by one (0), sets 1 for the current value.
     * @param value the column value of the new record
     */
    public void addValue(String value) {
        recordCount++;

        // Extend all existing bitmaps with 0 for the new record
        for (ArrayList<Boolean> bitmap : index.values()) {
            bitmap.add(Boolean.FALSE);
        }

        // Update or create the bitmap for the new value
        ArrayList<Boolean> bits = index.get(value);
        if (bits == null) {
            bits = new ArrayList<>();
            for (int i = 0; i < recordCount - 1; i++) {
                bits.add(Boolean.FALSE);
            }
            bits.add(Boolean.TRUE); // set last bit
            index.put(value, bits);
        } else {
            bits.set(recordCount - 1, Boolean.TRUE); // set last bit to 1
        }
    }

    /**
     * Retrieves the bitmap for a specific value as a string of 0s and 1s.
     * @param value the column value to query
     * @return a string like "10110"
     */
    public String getBitString(String value) {
        List<Boolean> bits = getBits(value);
        StringBuilder sb = new StringBuilder(bits.size());
        for (Boolean b : bits) {
            sb.append(b ? '1' : '0');
        }
        return sb.toString();
    }

    /**
     * Returns the bitmap as a list of booleans for a given value.
     * If value has never been inserted, returns a list of all false of correct length.
     * @param value the column value
     * @return list of booleans
     */
    public List<Boolean> getBits(String value) {
        ArrayList<Boolean> bits = index.get(value);
        if (bits == null) {
            // Value not present — return all 0s of same length
            ArrayList<Boolean> zeros = new ArrayList<>(recordCount);
            for (int i = 0; i < recordCount; i++) {
                zeros.add(Boolean.FALSE);
            }
            return zeros;
        }
        return bits;
    }

    /**
     * Returns the current record count (length of bitmaps).
     */
    public int getRecordCount() {
        return recordCount;
    }

    // Logical AND of two bitmaps
    public static List<Boolean> and(List<Boolean> a, List<Boolean> b) {
        int size = Math.min(a.size(), b.size());
        List<Boolean> result = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            result.add(a.get(i) && b.get(i));
        }
        return result;
    }

    // Logical OR of two bitmaps
    public static List<Boolean> or(List<Boolean> a, List<Boolean> b) {
        int size = Math.min(a.size(), b.size());
        List<Boolean> result = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            result.add(a.get(i) || b.get(i));
        }
        return result;
    }

    // Convert bitmap to positions (indexes) where bit is true
    public static List<Integer> toPositions(List<Boolean> bits) {
        List<Integer> positions = new ArrayList<>();
        for (int i = 0; i < bits.size(); i++) {
            if (bits.get(i)) {
                positions.add(i);
            }
        }
        return positions;
    }
}
