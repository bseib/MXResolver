package resolver;

public class ResolverException extends Exception {
	private static final long serialVersionUID = 1L;

	public ResolverException() {
	}

	public ResolverException(String message) {
		super(message);
	}

	public ResolverException(Throwable cause) {
		super(cause);
	}

	public ResolverException(String message, Throwable cause) {
		super(message, cause);
	}

	public ResolverException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

}
