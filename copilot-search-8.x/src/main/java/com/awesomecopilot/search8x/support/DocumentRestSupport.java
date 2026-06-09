package com.awesomecopilot.search8x.support;

import com.awesomecopilot.search8x.exception.DocumentDeleteException;
import com.awesomecopilot.search8x.exception.DocumentGetException;
import com.awesomecopilot.search8x.exception.DocumentSaveException;
import com.awesomecopilot.search8x.exception.DocumentUpdateException;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.support.WriteRequest;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.action.update.UpdateResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.xcontent.XContentType;
import org.elasticsearch.search.fetch.subphase.FetchSourceContext;
import org.elasticsearch.index.reindex.BulkByScrollResponse;
import org.elasticsearch.index.reindex.DeleteByQueryRequest;
import org.elasticsearch.index.query.QueryBuilder;

import java.io.IOException;

/**
 * 文档 CRUD REST 适配 (替代 transport-client 的 prepareIndex/prepareGet 等)。
 */
public final class DocumentRestSupport {

	private DocumentRestSupport() {
	}

	public static IndexResponse index(RestHighLevelClient client, String index, String id, String json, boolean create) {
		try {
			IndexRequest request = new IndexRequest(index);
			if (id != null) {
				request.id(id);
			}
			request.source(json, XContentType.JSON);
			if (create) {
				request.opType(IndexRequest.OpType.CREATE);
			}
			return client.index(request, RequestOptions.DEFAULT);
		} catch (IOException e) {
			throw new DocumentSaveException(e);
		}
	}

	public static GetResponse get(RestHighLevelClient client, String index, String id, boolean fetchSource) {
		try {
			GetRequest request = new GetRequest(index, id);
			request.fetchSourceContext(new FetchSourceContext(fetchSource));
			return client.get(request, RequestOptions.DEFAULT);
		} catch (IOException e) {
			throw new DocumentGetException(e);
		}
	}

	public static DeleteResponse delete(RestHighLevelClient client, String index, String id) {
		try {
			return client.delete(new DeleteRequest(index, id), RequestOptions.DEFAULT);
		} catch (IOException e) {
			throw new DocumentDeleteException(e);
		}
	}

	public static UpdateResponse update(RestHighLevelClient client, String index, String id, String json, boolean upsert) {
		try {
			UpdateRequest request = new UpdateRequest(index, id);
			request.doc(json, XContentType.JSON);
			if (upsert) {
				request.docAsUpsert(true);
			}
			request.setRefreshPolicy(WriteRequest.RefreshPolicy.IMMEDIATE);
			return client.update(request, RequestOptions.DEFAULT);
		} catch (IOException e) {
			throw new DocumentUpdateException(e);
		}
	}

	public static BulkResponse bulk(RestHighLevelClient client, BulkRequest request) {
		try {
			return client.bulk(request, RequestOptions.DEFAULT);
		} catch (IOException e) {
			throw new DocumentSaveException(e);
		}
	}

	public static long deleteByQuery(RestHighLevelClient client, String index, QueryBuilder query) {
		try {
			DeleteByQueryRequest request = new DeleteByQueryRequest(index);
			request.setQuery(query);
			BulkByScrollResponse response = client.deleteByQuery(request, RequestOptions.DEFAULT);
			return response.getDeleted();
		} catch (IOException e) {
			throw new DocumentDeleteException(e);
		}
	}
}
