package ch.inftec.ju.util;

/**
 * Base class for ju specific exceptions.
 * @author Martin
 *
 */
public class JuException extends Exception {

	public JuException() {
	}

	public JuException(String message) {
		super(message);
	}

	public JuException(Throwable message) {
		super(message);
	}

	public JuException(String message, Throwable cause) {
		super(message, cause);
	}

	public JuException(String formatMessage, Object... formatArgs) {
		super(String.format(formatMessage, formatArgs));
	}
	
	public JuException(String formatMessage, Throwable cause, Object... formatArgs) {
		super(String.format(formatMessage, formatArgs), cause);
	}
}
