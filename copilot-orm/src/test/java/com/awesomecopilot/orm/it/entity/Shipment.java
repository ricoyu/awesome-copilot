package com.awesomecopilot.orm.it.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

import java.io.Serializable;
import java.util.Objects;

/**
 * 集成测试用实体: 复合主键(双 @Id + @IdClass)。用来验证批量 merge
 * 对复合主键实体不被"单列主键预热"误伤（评审 deleg_df7e37a8 F1）。
 *
 * @author Rico Yu
 */
@Entity
@Table(name = "shipment")
@IdClass(Shipment.ShipmentKey.class)
public class Shipment {

	@Id
	@Column(name = "PART_NO")
	private Long partNo;

	@Id
	@Column(name = "SEQ_NO")
	private Integer seqNo;

	@Column(name = "NAME")
	private String name;

	public Shipment() {
	}

	public Shipment(Long partNo, Integer seqNo, String name) {
		this.partNo = partNo;
		this.seqNo = seqNo;
		this.name = name;
	}

	public Long getPartNo() {
		return partNo;
	}

	public void setPartNo(Long partNo) {
		this.partNo = partNo;
	}

	public Integer getSeqNo() {
		return seqNo;
	}

	public void setSeqNo(Integer seqNo) {
		this.seqNo = seqNo;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	/** 复合主键键类 */
	public static class ShipmentKey implements Serializable {
		private Long partNo;
		private Integer seqNo;

		public ShipmentKey() {
		}

		public ShipmentKey(Long partNo, Integer seqNo) {
			this.partNo = partNo;
			this.seqNo = seqNo;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (!(o instanceof ShipmentKey)) return false;
			ShipmentKey that = (ShipmentKey) o;
			return Objects.equals(partNo, that.partNo) && Objects.equals(seqNo, that.seqNo);
		}

		@Override
		public int hashCode() {
			return Objects.hash(partNo, seqNo);
		}
	}
}
