package com.leadflow.dao;

import com.leadflow.model.SystemSnapshot;

import java.io.*;

public class SnapshotIO {

    /**
     * Serializes a SystemSnapshot object to the specified file path.
     *
     * @param snapshot snapshot object to serialize
     * @param filePath destination file path (e.g., backup.ser)
     * @throws IOException if writing fails
     */
    public void exportSnapshot(SystemSnapshot snapshot, String filePath) throws IOException {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(filePath))) {
            oos.writeObject(snapshot);
        }
    }

    /**
     * Deserializes a SystemSnapshot object from disk and verifies class compatibility.
     *
     * @param filePath source .ser file path
     * @return restored SystemSnapshot instance
     * @throws IOException            if file cannot be read
     * @throws ClassNotFoundException if serialized class definition is incompatible
     */
    public SystemSnapshot importSnapshot(String filePath) throws IOException, ClassNotFoundException {
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(filePath))) {
            Object obj = ois.readObject();
            if (!(obj instanceof SystemSnapshot)) {
                throw new InvalidClassException("File does not contain a valid SystemSnapshot object.");
            }
            return (SystemSnapshot) obj;
        }
    }
}