package com.example.catreservation.entity;

import java.time.LocalDate;
import java.time.LocalTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

// このクラスが JPA エンティティであることを示す
@Entity
// テーブル名を shifts に指定
@Table(name = "shifts")
// Lombok：getter/setter 等を自動生成
@Data
// 引数なしコンストラクタ
@NoArgsConstructor
// 全フィールドコンストラクタ
@AllArgsConstructor
public class Shift {

	// 主キー
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	// スタッフ（users テーブルへの FK）
	@ManyToOne
	@JoinColumn(name = "staff_id", nullable = false)
	private User staff;

	// シフト日
	@Column(nullable = false)
	private LocalDate date;

	// 開始時刻
	@Column(name = "start_time", nullable = false)
	private LocalTime startTime;

	// 終了時刻
	@Column(name = "end_time", nullable = false)
	private LocalTime endTime;
}