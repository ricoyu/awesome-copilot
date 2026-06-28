package com.awesomecopilot.search8x.builder.bulk;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.BulkResponse;
import co.elastic.clients.elasticsearch.core.bulk.BulkOperation;
import co.elastic.clients.elasticsearch.core.bulk.BulkResponseItem;
import co.elastic.clients.json.JsonData;
import com.awesomecopilot.json.jackson.JacksonUtils;
import com.awesomecopilot.search8x.exception.DocumentSaveException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * ES 8.x 批量处理器，基于 ElasticsearchClient 实现<p>
 * 提供异步批量索引功能，支持分批处理和并发执行<p>
 *
 * @author Rico Yu ricoyu520@gmail.com
 * @version 1.0
 */
public class ESBulkProcessor {
	
	private static final Logger log = LoggerFactory.getLogger(ESBulkProcessor.class);
	
	private final ElasticsearchClient client;
	private final int batchSize;
	private final int concurrentRequests;
	private final ExecutorService executorService;
	
	/**
	 * 构造函数
	 *
	 * @param client             ElasticsearchClient
	 * @param batchSize          每批处理的文档数量，默认 1000
	 * @param concurrentRequests 并发请求数，默认 16
	 */
	public ESBulkProcessor(ElasticsearchClient client, int batchSize, int concurrentRequests) {
		this.client = client;
		this.batchSize = batchSize > 0 ? batchSize : 1000;
		this.concurrentRequests = concurrentRequests > 0 ? concurrentRequests : 16;
		this.executorService = Executors.newFixedThreadPool(this.concurrentRequests);
	}
	
	/**
	 * 默认构造函数，使用默认配置
	 *
	 * @param client ElasticsearchClient
	 */
	public ESBulkProcessor(ElasticsearchClient client) {
		this(client, 1000, 16);
	}
	
	/**
	 * 批量索引文档，支持自动分批和并发处理
	 *
	 * @param index 索引名称
	 * @param docs  文档列表
	 * @return CompletableFuture<Void> 异步完成标识
	 */
	public CompletableFuture<Void> bulkIndexAsync(String index, List<?> docs) {
		if (docs == null || docs.isEmpty()) {
			return CompletableFuture.completedFuture(null);
		}
		
		// 将文档分批
		List<List<?>> batches = partition(docs, batchSize);
		
		log.info("开始批量索引，总记录数: {}, 分批数: {}, 每批大小: {}", docs.size(), batches.size(), batchSize);
		
		// 并行处理每个批次
		List<CompletableFuture<Void>> futures = new ArrayList<>();
		for (List<?> batch : batches) {
			CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
				processBatch(index, batch);
			}, executorService);
			futures.add(future);
		}
		
		// 等待所有批次完成
		return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
	}
	
	/**
	 * 同步批量索引文档
	 *
	 * @param index 索引名称
	 * @param docs  文档列表
	 */
	public void bulkIndex(String index, List<?> docs) {
		try {
			bulkIndexAsync(index, docs).join();
		} catch (Exception e) {
			log.error("批量索引失败", e);
			throw new DocumentSaveException("Failed to bulk index documents", e);
		}
	}
	
	/**
	 * 处理单个批次
	 *
	 * @param index 索引名称
	 * @param batch 批次文档列表
	 */
	private void processBatch(String index, List<?> batch) {
		long startTime = System.currentTimeMillis();
		long executionId = Thread.currentThread().getId();
		
		log.info("序号: {}, 开始执行 {} 条数据批量操作", executionId, batch.size());
		
		try {
			BulkRequest.Builder bulkBuilder = new BulkRequest.Builder();
			
			for (Object doc : batch) {
				String docJson = toJson(doc);
				String id = extractId(doc);
				
				BulkOperation operation;
				if (id != null) {
					operation = BulkOperation.of(op -> op
							.index(idx -> idx
									.index(index)
									.id(id)
									.document(JsonData.of(docJson))
							)
					);
				} else {
					operation = BulkOperation.of(op -> op
							.index(idx -> idx
									.index(index)
									.document(JsonData.of(docJson))
							)
					);
				}
				bulkBuilder.operations(operation);
			}
			
			// 执行批量请求
			BulkResponse response = client.bulk(bulkBuilder.build());
			long elapsed = System.currentTimeMillis() - startTime;
			
			if (response.errors()) {
				log.error("序号: {} 批量操作存在错误，总记录数: {}, 耗时: {}ms", executionId, batch.size(), elapsed);
				for (BulkResponseItem item : response.items()) {
					if (item.error() != null) {
						log.error("文档 {} 失败: {}", item.id(), item.error().reason());
					}
				}
			} else {
				log.info("序号: {}, 执行 {} 条数据批量操作成功, 共耗费{}毫秒", executionId, batch.size(), elapsed);
			}
		} catch (Exception e) {
			long elapsed = System.currentTimeMillis() - startTime;
			log.error("序号: {} 批量操作失败, 总记录数: {}, 耗时: {}ms, 报错信息为: {}", 
					executionId, batch.size(), elapsed, e.getMessage(), e);
			throw new DocumentSaveException("Failed to process batch", e);
		}
	}
	
	/**
	 * 将列表分批
	 *
	 * @param list      原始列表
	 * @param batchSize 每批大小
	 * @return 分批后的列表
	 */
	private List<List<?>> partition(List<?> list, int batchSize) {
		List<List<?>> partitions = new ArrayList<>();
		for (int i = 0; i < list.size(); i += batchSize) {
			partitions.add(list.subList(i, Math.min(i + batchSize, list.size())));
		}
		return partitions;
	}
	
	/**
	 * 提取文档 ID（如果存在）
	 *
	 * @param doc 文档对象
	 * @return 文档 ID，如果不存在则返回 null
	 */
	private String extractId(Object doc) {
		// 这里可以根据实际需求实现 ID 提取逻辑
		// 例如通过反射获取 @Id 注解的字段
		return null;
	}
	
	/**
	 * 将对象转换为 JSON 字符串
	 *
	 * @param obj 对象
	 * @return JSON 字符串
	 */
	private String toJson(Object obj) {
		try {
			return JacksonUtils.toJson(obj);
		} catch (Exception e) {
			throw new DocumentSaveException("Failed to serialize document to JSON", e);
		}
	}
	
	/**
	 * 关闭线程池
	 */
	public void shutdown() {
		if (executorService != null && !executorService.isShutdown()) {
			executorService.shutdown();
		}
	}
}