package com.awesomecopilot.common.lang.io;

import com.awesomecopilot.common.lang.io.annotations.Nullable;
import com.awesomecopilot.common.lang.utils.Assert;
import com.awesomecopilot.common.lang.utils.StringUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.charset.Charset;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

public class FileSystemResource extends AbstractResource implements WritableResource {

	private final String path;

	@Nullable
	private final File file;

	private final Path filePath;


	/**
	 * Create a new {@code FileSystemResource} from a file path.
	 * <p>Note: When building relative resources via {@link #createRelative},
	 * it makes a difference whether the specified resource base path here
	 * ends with a slash or not. In the case of "C:/dir1/", relative paths
	 * will be built underneath that root: e.g. relative path "dir2" &rarr;
	 * "C:/dir1/dir2". In the case of "C:/dir1", relative paths will apply
	 * at the same directory level: relative path "dir2" &rarr; "C:/dir2".
	 * @param path a file path
	 * @see #FileSystemResource(Path)
	 */
	public FileSystemResource(String path) {
		Assert.notNull(path, "Path must not be null");
		this.path = StringUtils.cleanPath(path);
		this.file = new File(path);
		this.filePath = this.file.toPath();
	}

	/**
	 * Create a new {@code FileSystemResource} from a {@link File} handle.
	 * <p>Note: When building relative resources via {@link #createRelative},
	 * the relative path will apply <i>at the same directory level</i>:
	 * e.g. new File("C:/dir1"), relative path "dir2" &rarr; "C:/dir2"!
	 * If you prefer to have relative paths built underneath the given root directory,
	 * use the {@link #FileSystemResource(String) constructor with a file path}
	 * to append a trailing slash to the root path: "C:/dir1/", which indicates
	 * this directory as root for all relative paths.
	 * @param file a File handle
	 * @see #FileSystemResource(Path)
	 * @see #getFile()
	 */
	public FileSystemResource(File file) {
		Assert.notNull(file, "File must not be null");
		this.path = StringUtils.cleanPath(file.getPath());
		this.file = file;
		this.filePath = file.toPath();
	}

	/**
	 * Create a new {@code FileSystemResource} from a {@link Path} handle,
	 * performing all file system interactions via NIO.2 instead of {@link File}.
	 * general {@link FileSystemResource} conventions, in particular in terms of
	 * path cleaning and {@link #createRelative(String)} handling.
	 * <p>Note: When building relative resources via {@link #createRelative},
	 * the relative path will apply <i>at the same directory level</i>:
	 * e.g. Paths.get("C:/dir1"), relative path "dir2" &rarr; "C:/dir2"!
	 * If you prefer to have relative paths built underneath the given root directory,
	 * use the {@link #FileSystemResource(String) constructor with a file path}
	 * to append a trailing slash to the root path: "C:/dir1/", which indicates
	 * this directory as root for all relative paths. Alternatively, consider
	 * resolution in {@code createRelative}, always nesting relative paths.
	 * @param filePath a Path handle to a file
	 * @since 5.1
	 * @see #FileSystemResource(File)
	 */
	public FileSystemResource(Path filePath) {
		Assert.notNull(filePath, "Path must not be null");
		this.path = StringUtils.cleanPath(filePath.toString());
		this.file = null;
		this.filePath = filePath;
	}

	/**
	 * locating the specified path.
	 * <p>This is an alternative to {@link #FileSystemResource(String)},
	 * performing all file system interactions via NIO.2 instead of {@link File}.
	 * @param fileSystem the FileSystem to locate the path within
	 * @param path a file path
	 * @since 5.1.1
	 * @see #FileSystemResource(File)
	 */
	public FileSystemResource(FileSystem fileSystem, String path) {
		Assert.notNull(fileSystem, "FileSystem must not be null");
		Assert.notNull(path, "Path must not be null");
		this.path = StringUtils.cleanPath(path);
		this.file = null;
		this.filePath = fileSystem.getPath(this.path).normalize();
	}


	/**
	 * Return the file path for this resource.
	 */
	public final String getPath() {
		return this.path;
	}

	/**
	 * This implementation returns whether the underlying file exists.
	 * @see java.io.File#exists()
	 * @see java.nio.file.Files#exists(Path, java.nio.file.LinkOption...)
	 */
	@Override
	public boolean exists() {
		return (this.file != null ? this.file.exists() : Files.exists(this.filePath));
	}

	/**
	 * This implementation checks whether the underlying file is marked as readable
	 * (and corresponds to an actual file with content, not to a directory).
	 * @see java.io.File#canRead()
	 * @see java.io.File#isDirectory()
	 * @see java.nio.file.Files#isReadable(Path)
	 * @see java.nio.file.Files#isDirectory(Path, java.nio.file.LinkOption...)
	 */
	@Override
	public boolean isReadable() {
		return (this.file != null ? this.file.canRead() && !this.file.isDirectory() :
				Files.isReadable(this.filePath) && !Files.isDirectory(this.filePath));
	}

	/**
	 * This implementation opens an NIO file stream for the underlying file.
	 * @see java.nio.file.Files#newInputStream(Path, java.nio.file.OpenOption...)
	 */
	@Override
	public InputStream getInputStream() throws IOException {
		try {
			return Files.newInputStream(this.filePath);
		}
		catch (NoSuchFileException ex) {
			throw new FileNotFoundException(ex.getMessage());
		}
	}

	@Override
	public byte[] getContentAsByteArray() throws IOException {
		try {
			return Files.readAllBytes(this.filePath);
		}
		catch (NoSuchFileException ex) {
			throw new FileNotFoundException(ex.getMessage());
		}
	}

	@Override
	public String getContentAsString(Charset charset) throws IOException {
		try {
			// 读取文件所有字节，然后转换为字符串
			byte[] fileBytes = Files.readAllBytes(this.filePath);
			String content = new String(fileBytes, charset);
			return content;
		}
		catch (NoSuchFileException ex) {
			throw new FileNotFoundException(ex.getMessage());
		}
	}

	/**
	 * This implementation checks whether the underlying file is marked as writable
	 * (and corresponds to an actual file with content, not to a directory).
	 * @see java.io.File#canWrite()
	 * @see java.io.File#isDirectory()
	 * @see java.nio.file.Files#isWritable(Path)
	 * @see java.nio.file.Files#isDirectory(Path, java.nio.file.LinkOption...)
	 */
	@Override
	public boolean isWritable() {
		return (this.file != null ? this.file.canWrite() && !this.file.isDirectory() :
				Files.isWritable(this.filePath) && !Files.isDirectory(this.filePath));
	}

	/**
	 * This implementation opens a FileOutputStream for the underlying file.
	 * @see java.nio.file.Files#newOutputStream(Path, java.nio.file.OpenOption...)
	 */
	@Override
	public OutputStream getOutputStream() throws IOException {
		return Files.newOutputStream(this.filePath);
	}

	/**
	 * This implementation returns a URL for the underlying file.
	 * @see java.io.File#toURI()
	 * @see java.nio.file.Path#toUri()
	 */
	@Override
	public URL getURL() throws IOException {
		return (this.file != null ? this.file.toURI().toURL() : this.filePath.toUri().toURL());
	}

	/**
	 * This implementation returns a URI for the underlying file.
	 * @see java.io.File#toURI()
	 * @see java.nio.file.Path#toUri()
	 */
	@Override
	public URI getURI() throws IOException {
		if (this.file != null) {
			return this.file.toURI();
		}
		else {
			URI uri = this.filePath.toUri();
			// Normalize URI? See https://github.com/spring-projects/spring-framework/issues/29275
			String scheme = uri.getScheme();
			if (ResourceUtils.URL_PROTOCOL_FILE.equals(scheme)) {
				try {
					uri = new URI(scheme, uri.getPath(), null);
				}
				catch (URISyntaxException ex) {
					throw new IOException("Failed to normalize URI: " + uri, ex);
				}
			}
			return uri;
		}
	}

	/**
	 * This implementation always indicates a file.
	 */
	@Override
	public boolean isFile() {
		return true;
	}

	/**
	 * This implementation returns the underlying File reference.
	 */
	@Override
	public File getFile() {
		return (this.file != null ? this.file : this.filePath.toFile());
	}

	/**
	 * This implementation opens a FileChannel for the underlying file.
	 * @see java.nio.channels.FileChannel
	 */
	@Override
	public ReadableByteChannel readableChannel() throws IOException {
		try {
			return FileChannel.open(this.filePath, StandardOpenOption.READ);
		}
		catch (NoSuchFileException ex) {
			throw new FileNotFoundException(ex.getMessage());
		}
	}

	/**
	 * This implementation opens a FileChannel for the underlying file.
	 * @see java.nio.channels.FileChannel
	 */
	@Override
	public WritableByteChannel writableChannel() throws IOException {
		return FileChannel.open(this.filePath, StandardOpenOption.WRITE);
	}

	/**
	 * This implementation returns the underlying File/Path length.
	 */
	@Override
	public long contentLength() throws IOException {
		if (this.file != null) {
			long length = this.file.length();
			if (length == 0L && !this.file.exists()) {
				throw new FileNotFoundException(getDescription() +
						" cannot be resolved in the file system for checking its content length");
			}
			return length;
		}
		else {
			try {
				return Files.size(this.filePath);
			}
			catch (NoSuchFileException ex) {
				throw new FileNotFoundException(ex.getMessage());
			}
		}
	}

	/**
	 * This implementation returns the underlying File/Path last-modified time.
	 */
	@Override
	public long lastModified() throws IOException {
		if (this.file != null) {
			return super.lastModified();
		}
		else {
			try {
				return Files.getLastModifiedTime(this.filePath).toMillis();
			}
			catch (NoSuchFileException ex) {
				throw new FileNotFoundException(ex.getMessage());
			}
		}
	}

	/**
	 * This implementation creates a FileSystemResource, applying the given path
	 * relative to the path of the underlying file of this resource descriptor.
	 */
	@Override
	public Resource createRelative(String relativePath) {
		String pathToUse = StringUtils.applyRelativePath(this.path, relativePath);
		return (this.file != null ? new FileSystemResource(pathToUse) :
				new FileSystemResource(this.filePath.getFileSystem(), pathToUse));
	}

	/**
	 * This implementation returns the name of the file.
	 * @see java.io.File#getName()
	 * @see java.nio.file.Path#getFileName()
	 */
	@Override
	public String getFilename() {
		return (this.file != null ? this.file.getName() : this.filePath.getFileName().toString());
	}

	/**
	 * This implementation returns a description that includes the absolute
	 * path of the file.
	 * @see java.io.File#getAbsolutePath()
	 * @see java.nio.file.Path#toAbsolutePath()
	 */
	@Override
	public String getDescription() {
		return "file [" + (this.file != null ? this.file.getAbsolutePath() : this.filePath.toAbsolutePath()) + "]";
	}


	/**
	 * This implementation compares the underlying file paths.
	 * @see #getPath()
	 */
	@Override
	public boolean equals(@Nullable Object other) {
		return (this == other || (other instanceof FileSystemResource && this.path.equals(((FileSystemResource)other).path)));
	}

	/**
	 * This implementation returns the hash code of the underlying file path.
	 * @see #getPath()
	 */
	@Override
	public int hashCode() {
		return this.path.hashCode();
	}

}