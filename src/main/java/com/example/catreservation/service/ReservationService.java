// サービスクラスのパッケージ配置
package com.example.catreservation.service;

// 日付・時刻 API（LocalDate/LocalTime）
import java.time.LocalDate;
import java.time.LocalTime;
// 可変長のスロット生成やフィルタに使用するコレクション
import java.util.ArrayList;
import java.util.List;
// 統計返却用の Map など
import java.util.Map;
// 存在しない可能性のある値を安全に扱うコンテナ
import java.util.Optional;
// 集約やフィルタのための Stream 操作
import java.util.stream.Collectors;

// サービス層のステレオタイプ（DI 管理対象）
import org.springframework.stereotype.Service;
// トランザクション境界の宣言（同一メソッド内を 1 トランザクションに）
import org.springframework.transaction.annotation.Transactional;

// 予約エンティティの参照（作成/更新/返却）
import com.example.catreservation.entity.Reservation;
// ユーザエンティティ（顧客・スタッフの特定）
import com.example.catreservation.entity.User;
// 予約テーブルへの永続化・検索を担う JPA リポジトリ
import com.example.catreservation.repository.ReservationRepository;
// シフトテーブルへのアクセス（空き判定に必須）
import com.example.catreservation.repository.ShiftRepository;
// ユーザテーブルへのアクセス（ID/メール→User 解決）
import com.example.catreservation.repository.UserRepository;

// 業務ロジックをまとめるサービスクラス
@Service
public class ReservationService {
	// 予約の CRUD・クエリを扱うリポジトリ
	private final ReservationRepository reservationRepository;
	// ユーザ解決（顧客/スタッフ）に使用
	private final UserRepository userRepository;
	// シフト有無・時間内判定のために参照
	private final ShiftRepository shiftRepository;

	//依存性のコンストラクタ注入（テスト容易性と不変性のため final）
	public ReservationService(ReservationRepository reservationRepository, UserRepository userRepository,
			ShiftRepository shiftRepository) {
		//フィールドへ予約リポジトリを設定
		this.reservationRepository = reservationRepository;
		//フィールドへユーザリポジトリを設定
		this.userRepository = userRepository;
		//フィールドへシフトリポジトリを設定
		this.shiftRepository = shiftRepository;
	}

	//指定ユーザの予約履歴（新しい順）を取得
	public List<Reservation> getUserReservations(User user) {
		//ユーザ紐づきの予約を日付降順→時間降順で返す
		return reservationRepository.findByUserOrderByDateDescTimeSlotDesc(user);
	}

	//予約を ID で 1 件取得（存在しなければ Optional.empty）
	public Optional<Reservation> getReservationById(Long id) {
		//JPA の findById を委譲
		return reservationRepository.findById(id);
	}

	//全予約の一覧を取得（管理者用）
	public List<Reservation> getAllReservations() {
		//reservation テーブルの全件を返す
		return reservationRepository.findAll();
	}

	//期間指定で予約を抽出（統計・フィルタ表示用）
	public List<Reservation> getReservationsByDateRange(LocalDate startDate, LocalDate endDate) {
		//startDate <= record_date <= endDate の範囲で抽出
		return reservationRepository.findByDateBetween(startDate, endDate);
	}

	//予約作成（同一スロットの二重予約/シフト内チェックを含む）
	@Transactional
	public Reservation createReservation(User customer, Long staffId, LocalDate date, LocalTime timeSlot, String menu) {
		//スタッフ ID から User を取得（見つからなければ 400 相当の業務例外）
		User staff = userRepository.findById(staffId)
				.orElseThrow(() -> new IllegalArgumentException("Staff not found"));
		//該当日のスタッフシフトを取得し、その時間帯内かを判定（下限以上・上限未満をチェック）
		boolean staffHasShift = shiftRepository.findByStaffAndDate(staff, date)
				.map(shift -> !timeSlot.isBefore(shift.getStartTime())
						&& !timeSlot.isAfter(shift.getEndTime().minusMinutes(1))) // 1 分単位の上限未満判定
				.orElse(false);
		//シフトがない or 時間外なら予約不可
		if (!staffHasShift) {
			//利用不可メッセージで業務例外（画面で表示される想定）
			throw new IllegalStateException("Staff is not available at this time.");
		}
		//同一スタッフ・同一日付・同一時間に予約が既にあるなら弾く（二重予約防止）
		if (reservationRepository.findByDateAndTimeSlotAndStaff(date, timeSlot, staff).isPresent()) {
			//既予約メッセージで業務例外
			throw new IllegalStateException("This time slot is already booked.");
		}
		//新規の予約エンティティを構築
		Reservation reservation = new Reservation();
		//予約した顧客を紐付け
		reservation.setUser(customer);
		//担当スタッフを紐付け
		reservation.setStaff(staff);
		//予約日を設定
		reservation.setDate(date);
		//予約時間を設定
		reservation.setTimeSlot(timeSlot);
		//予約メニューを設定
		reservation.setMenu(menu);
		//ステータス初期値を「予約済」に（DB デフォルトとも整合）
		reservation.setStatus("予約済");
		//リポジトリに保存して生成レコード（ID 付）を返す
		return reservationRepository.save(reservation);
	}

	//予約更新（別スロットへの変更時も競合/シフト内を厳密チェック）
	@Transactional
	public Reservation updateReservation(Long reservationId, LocalDate newDate, LocalTime newTimeSlot,
			String newMenu) {
		//対象予約を ID で取得（なければ 400 相当の業務例外）
		Reservation reservation = reservationRepository.findById(reservationId)
				.orElseThrow(() -> new IllegalArgumentException("Reservation not found"));
		//変更先スロットが、同じスタッフ・同じ日・同じ時間の他予約と衝突しないか
		if (reservationRepository.findByDateAndTimeSlotAndStaff(newDate, newTimeSlot,
				reservation.getStaff())
				.filter(r -> !r.getId().equals(reservationId)) // 自分自身なら許容
				.isPresent()) {
			//競合ありの場合は業務例外
			throw new IllegalStateException("This new time slot is already booked.");
		}
		//変更先がスタッフのシフト時間内かをチェック
		boolean staffHasShift = shiftRepository.findByStaffAndDate(reservation.getStaff(), newDate)
				.map(shift -> !newTimeSlot.isBefore(shift.getStartTime())
						&& !newTimeSlot.isAfter(shift.getEndTime().minusMinutes(1))) // 上限未満チェック
				.orElse(false);
		//シフト外なら更新不可
		if (!staffHasShift) {
			//利用不可メッセージで業務例外
			throw new IllegalStateException("Staff is not available at this new time.");
		}
		//問題なければ、日付・時間・メニューを更新
		reservation.setDate(newDate);
		reservation.setTimeSlot(newTimeSlot);
		reservation.setMenu(newMenu);
		//保存して最新状態を返す
		return reservationRepository.save(reservation);
	}

	//予約キャンセル（物理削除はせずステータス更新）
	@Transactional
	public void cancelReservation(Long reservationId) {
		//対象予約を ID で取得
		Reservation reservation = reservationRepository.findById(reservationId)
				.orElseThrow(() -> new IllegalArgumentException("Reservation not found"));
		//ステータスを「キャンセル済」に変更
		reservation.setStatus("キャンセル済");
		//上書き保存
		reservationRepository.save(reservation);
	}

	//スタッフ一覧（ロール=STAFF のみ）を取得
	public List<User> getAllStaffs() {
		//ユーザテーブルから "STAFF" ロールのユーザを返す
		return userRepository.findByRole("STAFF");
	}

	//指定スタッフ・日付の空き時間枠一覧を計算して返す（30 分刻み）
	public List<LocalTime> getAvailableTimeSlots(Long staffId, LocalDate date) {
		//スタッフ ID から User を取得（存在しなければ 400 相当）
		User staff = userRepository.findById(staffId)
				.orElseThrow(() -> new IllegalArgumentException("Staff not found"));
		//指定日のスタッフシフトを取得（なければ空リスト＝空きなし）
		Optional<com.example.catreservation.entity.Shift> staffShift = shiftRepository.findByStaffAndDate(staff, date);
		//シフト未設定なら空き枠なし
		if (staffShift.isEmpty()) {
			//空のイミュータブルリストを返す
			return List.of(); // No shift, no available slots
		}
		//シフト開始時刻を取得
		LocalTime shiftStart = staffShift.get().getStartTime();
		//シフト終了時刻を取得
		LocalTime shiftEnd = staffShift.get().getEndTime();
		//シフト時間内を 30 分刻みでスロット化（終了時刻は含めない）
		List<LocalTime> allPossibleSlots = generateTimeSlots(shiftStart, shiftEnd, 30);
		// 30 minute intervals
		//当日のスタッフ予約を取得（開始=終了=date でその日のみ抽出）
		List<Reservation> bookedSlots = reservationRepository.findByStaffAndDateBetween(staff, date,
				date);
		//既予約スロットを除外して空きのみを返す
		return allPossibleSlots.stream()
				.filter(slot -> bookedSlots.stream().noneMatch(res -> res.getTimeSlot().equals(slot)))
				.collect(Collectors.toList());
	}

	//開始時刻から終了時刻未満まで、指定分刻みで LocalTime のリストを生成
	private List<LocalTime> generateTimeSlots(LocalTime start, LocalTime end, int intervalMinutes) {
		//返却用のリストを用意
		List<LocalTime> slots = new ArrayList<>();
		//現在ポインタを開始時刻に設定
		LocalTime current = start;
		//終了時刻「未満」までループ（end を含めない設計）
		while (current.isBefore(end)) {
			// 現在時刻を候補に追加
			slots.add(current);
			// interval 分だけ進める
			current = current.plusMinutes(intervalMinutes);
		}
		// 生成した候補を返す
		return slots;
	}

	// 期間内の予約をメニュー名で集計し、件数マップを返す
	public Map<String, Long> getReservationCountByMenu(LocalDate startDate, LocalDate endDate) {
		// 期間内の予約を取得
		List<Reservation> reservations = reservationRepository.findByDateBetween(startDate, endDate);
		// menu をキーに件数をカウント（null メニューは null キーとして集計されうる点に注意）
		return reservations.stream()
				.collect(Collectors.groupingBy(Reservation::getMenu, Collectors.counting()));
	}

	// 期間内の予約をスタッフ名で集計（null スタッフを除外）
	public Map<String, Long> getReservationCountByStaff(LocalDate startDate, LocalDate endDate) {
		// 期間内の予約を取得
		List<Reservation> reservations = reservationRepository.findByDateBetween(startDate, endDate);
		// staff != null のデータのみ集計し、スタッフ表示名をキーに件数をカウント
		return reservations.stream()
				.filter(r -> r.getStaff() != null)
				.collect(Collectors.groupingBy(r -> r.getStaff().getName(), Collectors.counting()));
	}
}