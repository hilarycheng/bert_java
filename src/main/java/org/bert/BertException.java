package org.bert;

public class BertException extends Exception {
	
    private static final long serialVersionUID = 0;

    private Throwable cause;

    public BertException(String message) {
        super(message);
    }

    public BertException(Throwable cause) {
        super(cause.getMessage());
        this.cause = cause;
    }

    public Throwable getCause() {
        return this.cause;
    }
}
