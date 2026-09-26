package com.dacs.backend.exception;

public class BackendException extends GenericException {

	private static final long serialVersionUID = -8240990866638394860L;

	public BackendException(IEnumError error) {
        super(error);
    }

    public BackendException(IEnumError error, Object... params) {
        super(error, params);
    }
}
