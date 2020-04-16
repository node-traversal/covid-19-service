package io.nodetraversal.poi;

import lombok.Getter;

@Getter
public class ConnectionFailedException extends RuntimeException {

    private final String url;
    private final int statusCode;

    public ConnectionFailedException(String url, int statusCode) {
        super("Failed to connect to '" + url + "', status=" + statusCode);

        this.url = url;
        this.statusCode = statusCode;
    }
}
