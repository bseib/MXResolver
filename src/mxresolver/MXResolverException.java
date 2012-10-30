package mxresolver;

public class MXResolverException extends Exception {
	private static final long serialVersionUID = 1L;

	public MXResolverException() {
	}

	public MXResolverException(String message) {
		super(message);
	}

	public MXResolverException(Throwable cause) {
		super(cause);
	}

	public MXResolverException(String message, Throwable cause) {
		super(message, cause);
	}

	public MXResolverException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

}
