package net.nhatanhn.adapters;

public interface TextStorageClient {
    String read(long id) throws Exception;

    void write(long id, String content) throws Exception;

    void delete(long id) throws Exception;
}