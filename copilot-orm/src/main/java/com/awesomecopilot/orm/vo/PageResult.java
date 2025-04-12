package com.awesomecopilot.orm.vo;

import com.awesomecopilot.common.lang.vo.Page;
import com.awesomecopilot.common.lang.vo.Result;
import com.awesomecopilot.common.lang.vo.Results;

import java.util.List;

public class PageResult<T> {

	private Page page;

	private List<T> data;

	public Page getPage() {
		return page;
	}

	public void setPage(Page page) {
		this.page = page;
	}

	public List<T> getData() {
		return data;
	}

	public void setData(List<T> data) {
		this.data = data;
	}

	public int totalCount() {
		return this.page.getTotalCount();
	}

	public Result toResult() {
		return Results.<List<T>>success()
				.page(this.page)
				.data(this.data).build();
	}
}
