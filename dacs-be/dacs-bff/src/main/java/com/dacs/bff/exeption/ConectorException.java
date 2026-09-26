package com.dacs.bff.exeption;

public class ConectorException extends GenericException {

	private static final long serialVersionUID = -8240990866638394860L;

	public ConectorException(IEnumError error) {
        super(error);
    }

    public ConectorException(IEnumError error, Object... params) {
        super(error, params);
    }
}
