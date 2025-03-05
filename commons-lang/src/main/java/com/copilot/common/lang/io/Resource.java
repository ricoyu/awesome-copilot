package com.copilot.common.lang.io;

import com.copilot.common.lang.utils.Assert;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URI;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.Charset;

public interface Resource extends InputStreamSource {

	boolean exists();

	default boolean isReadable() {
		return exists();
	}

	default boolean isOpen() {
		return false;
	}

	default boolean isFile() {
		return false;
	}

	URL getURL() throws IOException;

	/**
	 * Return a URI handle for this resource.
	 * @throws IOException if the resource cannot be resolved as URI,
	 * i.e. if the resource is not available as a descriptor
	 * @since 2.5
	 */
	URI getURI() throws IOException;

	File getFile() throws IOException;

	default ReadableByteChannel readableChannel() throws IOException {
		return Channels.newChannel(getInputStream());
	}

	default byte[] getContentAsByteArray() throws IOException {
		return copyToByteArray(getInputStream());
	}

	default String getContentAsString(Charset charset) throws IOException {
		return copyToString(new InputStreamReader(getInputStream(), charset));
	}

	default byte[] copyToByteArray(InputStream in) throws IOException {
		if (in == null) {
			return new byte[0];
		}

		try (in) {
			return in.readAllBytes();
		}
	}

	default String copyToString(Reader in) throws IOException {
		if (in == null) {
			return "";
		}

		StringWriter out = new StringWriter(8192);
		copy(in, out);
		return out.toString();
	}

	default int copy(Reader in, Writer out) throws IOException {
		Assert.notNull(in, "No Reader specified");
		Assert.notNull(out, "No Writer specified");

		try {
			int charCount = 0;
			char[] buffer = new char[8192];
			int charsRead;
			while ((charsRead = in.read(buffer)) != -1) {
				out.write(buffer, 0, charsRead);
				charCount += charsRead;
			}
			out.flush();
			return charCount;
		}
		finally {
			close(in);
			close(out);
		}
	}

	default void close(Closeable closeable) {
		try {
			closeable.close();
		}
		catch (IOException ex) {
			// ignore
		}
	}

	long contentLength() throws IOException;

	long lastModified() throws IOException;

	Resource createRelative(String relativePath) throws IOException;

	String getFilename();

	String getDescription();

}