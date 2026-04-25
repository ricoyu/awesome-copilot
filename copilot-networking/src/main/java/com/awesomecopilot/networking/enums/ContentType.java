package com.awesomecopilot.networking.enums;

/**
 * HTTP Content-Type头信息
 * <p/>
 * Copyright: Copyright (c) 2026-04-25 12:52
 * <p/>
 * Company: Sexy Uncle Inc.
 * <p/>
 
 * @author Rico Yu  ricoyu520@gmail.com
 * @version 1.0
 */
public enum ContentType {

	APPLICATION_JSON("application/json"),
	APPLICATION_JSON_UTF8("application/json;charset=UTF-8"),
	APPLICATION_XML("application/xml"),
	APPLICATION_XML_UTF8("application/xml;charset=UTF-8"),
	TEXT_HTML("text/html"),
	TEXT_HTML_UTF8("text/html;charset=UTF-8"),
	TEXT_PLAIN("text/plain"),
	TEXT_PLAIN_UTF8("text/plain;charset=UTF-8"),
	APPLICATION_FORM_URLENCODED("application/x-www-form-urlencoded"),
	APPLICATION_FORM_URLENCODED_UTF8("application/x-www-form-urlencoded;charset=UTF-8"),
	MULTIPART_FORM_DATA("multipart/form-data"),
	APPLICATION_OCTET_STREAM("application/octet-stream"),
	IMAGE_JPEG("image/jpeg"),
	IMAGE_PNG("image/png"),
	IMAGE_GIF("image/gif"),
	IMAGE_WEBP("image/webp"),
	TEXT_CSS("text/css"),
	TEXT_JAVASCRIPT("text/javascript"),
	APPLICATION_JAVASCRIPT("application/javascript"),
	APPLICATION_PDF("application/pdf"),
	APPLICATION_ZIP("application/zip"),
	APPLICATION_GZIP("application/gzip"),
	TEXT_CSV("text/csv"),
	TEXT_CSV_UTF8("text/csv;charset=UTF-8");

	private final String value;

	ContentType(String value) {
		this.value = value;
	}

	public String getValue() {
		return value;
	}

	@Override
	public String toString() {
		return value;
	}
}