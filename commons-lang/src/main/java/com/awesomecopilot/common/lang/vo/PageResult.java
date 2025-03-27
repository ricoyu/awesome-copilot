package com.awesomecopilot.common.lang.vo;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;
import java.util.List;

public class PageResult<T> implements Serializable {


	public PageResult() {

	}

	public PageResult(int pageNum, int pageSize, int totalCount, List<T> data) {
		this.pageNum = pageNum;
		this.pageSize = pageSize;
		this.totalCount = totalCount;
		this.data = data;
		updatePagingStatus();
	}
	/**
	 * 当前第几页, 从1开始
	 */
	private int pageNum = 1;

	/**
	 * 每页多少条记录
	 */
	private int pageSize = 10;

	/**
	 * 总共有多少页，通过查询结果返回给你显示用
	 */
	private int totalPages = 0;

	/**
	 * 总共有多少条记录，通过查询结果返回给你显示用"
	 */
	@JsonProperty("total")
	private int totalCount = 0;

	/**
	 * 还有没有下一页
	 */
	private boolean hasNextPage = false;

	/**
	 * 还有没有前一页
	 */
	private boolean hasPreviousPage = false;

	private List<T> data;

	public int getPageNum() {
		return pageNum;
	}

	public void setPageNum(int pageNum) {
		this.pageNum = pageNum;
	}

	public int getPageSize() {
		return pageSize;
	}

	public void setPageSize(int pageSize) {
		this.pageSize = pageSize;
	}

	public int getTotalPages() {
		return totalPages;
	}

	public void setTotalPages(int totalPages) {
		this.totalPages = totalPages;
	}

	public int getTotalCount() {
		return totalCount;
	}

	public void setTotalCount(int totalCount) {
		this.totalCount = totalCount;
	}

	public List<T> getData() {
		return data;
	}

	public void setData(List<T> data) {
		this.data = data;
	}

	private void updatePagingStatus() {
		this.totalPages = (int) Math.ceil(this.totalCount / (double) this.pageSize);

		// not record found or there is only one page
		if (this.totalPages == 0 || this.totalPages == 1) {
			this.hasNextPage = false;
			this.hasPreviousPage = false;
		} else {// more than one page
			if (this.pageNum < this.totalPages) {
				this.hasNextPage = true;
			} else {
				this.hasNextPage = false;
			}

			if (this.pageNum == 1) {
				this.hasPreviousPage = false;
			} else {
				this.hasPreviousPage = true;
			}
		}

	}
}
