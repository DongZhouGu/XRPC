package com.dzgu.xrpc.exception;


public class SerializeException extends RuntimeException {
    public SerializeException(String message) {
        super(message);
    }
    public SerializeException(String message,String detail) {
        super(message + ":" + detail);
    }
}
