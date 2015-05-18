package fr.upem.jarret.http;

import java.nio.charset.Charset;
import java.util.*;

import fr.upem.jarret.http.HTTPException;

/**
 * @author carayol
 *         Class representing a HTTP header
 */

public class HTTPHeader {

	/**
	 * Supported versions of the HTTP Protocol
	 */

	private static final String[] LIST_SUPPORTED_VERSIONS = new String[]{"HTTP/1.0", "HTTP/1.1", "HTTP/1.2"};
	public static final Set<String> SUPPORTED_VERSIONS = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(LIST_SUPPORTED_VERSIONS)));


	private final String response;
	private final String version;
	private final int code;
	private final Map<String, String> fields;


	private HTTPHeader(String response,String version,int code,Map<String, String> fields) throws HTTPException {
		this.response = response;
		this.version = version;
		this.code = code;
		this.fields = Collections.unmodifiableMap(fields);
	}

	public static HTTPHeader create(String response, Map<String, String> fields)
			throws HTTPException {
		String[] tokens = response.split(" ");
		// Treatment of the response line
		HTTPException.ensure(tokens.length >= 2, "Badly formed response:\n" + response);
		String version = tokens[0];
		HTTPException.ensure(HTTPHeader.SUPPORTED_VERSIONS.contains(version),
				"Unsupported version in response:\n" + response);
		int code = 0;
		try {
			code = Integer.valueOf(tokens[1]);
			HTTPException.ensure(code >= 100 && code < 600, "Invalid code in response:\n"
					+ response);
		} catch (NumberFormatException e) {
			HTTPException.ensure(false, "Invalid response:\n" + response);
		}
		Map<String, String> fieldsCopied = new HashMap<>();
		for (String s : fields.keySet())
			fieldsCopied.put(s, fields.get(s).trim());
		return new HTTPHeader(response, version, code, fieldsCopied);
	}
	public String getResponse() {
		return response;
	}

	public String getVersion() {
		return version;
	}

	public int getCode() {
		return code;
	}

	public Map<String, String> getFields() {
		return fields;
	}
	/**
	 * 
	 * @param statusLine
	 * @param map
	 * @return
	 * @throws HTTPException 
	 */

	public static HTTPHeader createHeaderServer(String response,
			HashMap<String, String> fields) throws HTTPException {
		String[] tokens = response.split(" ");
		// Treatment of the response line
		HTTPException.ensure(tokens.length >= 2, "Badly formed response:\n" + response);
		String version = tokens[2];

		HTTPException.ensure(HTTPHeader.SUPPORTED_VERSIONS.contains(version),
				"Unsupported version in response:\n" + response);
		int code = 0;

		Map<String, String> fieldsCopied = new HashMap<>();
		for (String s : fields.keySet()) {
			fieldsCopied.put(s, fields.get(s).trim());
		}
		return new HTTPHeader(response, version, code, fieldsCopied);
	}



	/**
	 * @return the value of the Content-Length field in the header
	 *         -1 if the field does not exists
	 * @throws HTTPError when the value of Content-Length is not a number
	 */
	public int getContentLength() throws HTTPException {
		String s = fields.get("Content-Length");
		if (s == null) return -1;
		else {
			try {
				return Integer.valueOf(s.trim());
			} catch (NumberFormatException e) {
				throw new HTTPException("Invalid Content-Length field value :\n" + s);
			}
		}
	}

	/**
	 * @return the Content-Type
	 *         null if there is no Content-Type field
	 */
	public String getContentType() {
		String s = fields.get("Content-Type");
		if (s != null) {
			return s.split(";")[0].trim();
		} else
			return null;
	}

	/**
	 * @return the charset corresponding to the Content-Type field
	 *         null if charset is unknown or unavailable on the JVM
	 */
	public Charset getCharset() {
		Charset cs = null;
		String s = fields.get("Content-Type");
		if (s == null) return cs;
		for (String t : s.split(";")) {
			if (t.contains("charset=")) {
				try {
					cs= Charset.forName(t.split("=")[1].trim());
					
				} catch (Exception e) {
					// If the Charset is unknown or unavailable we turn null
				}
				return cs;
			}
		}
		return cs;
	}

	/**
	 * @return true if the header correspond to a chunked response
	 */
	public boolean isChunkedTransfer() {
		return fields.containsKey("Transfer-Encoding") && fields.get("Transfer-Encoding").trim().equals("chunked");
	}

	public String toString() {
		return response + "\n"
				+ version + " " + code + "\n"
				+ fields.toString();
	}



}
