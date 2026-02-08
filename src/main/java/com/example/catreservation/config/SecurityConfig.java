// セキュリティ設定クラスを置くパッケージ
package com.example.catreservation.config;

// @Bean アノテーション（DI コンテナに登録）
import org.springframework.context.annotation.Bean;
// 設定クラスの印
import org.springframework.context.annotation.Configuration;
// HTTP セキュリティ設定のビルダー
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
// WebSecurity を有効化するアノテーション
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
// ユーザ情報を提供するインタフェース
import org.springframework.security.core.userdetails.UserDetailsService;
// ユーザ未発見時の例外
import org.springframework.security.core.userdetails.UsernameNotFoundException;
// パスワードを平文扱いする開発用エンコーダ（本番では非推奨）
import org.springframework.security.crypto.password.NoOpPasswordEncoder;
// パスワードエンコードの抽象
import org.springframework.security.crypto.password.PasswordEncoder;
// セキュリティフィルタチェーン本体
import org.springframework.security.web.SecurityFilterChain;

import com.example.catreservation.repository.UserRepository;

@Configuration // このクラスが設定クラスであることを宣言
@EnableWebSecurity // Spring Security の Web セキュリティを有効化
// ★補足: メソッドレベルの @PreAuthorize を有効化したい場合は @EnableMethodSecurity を併用推奨
public class SecurityConfig {
	@Bean // SecurityFilterChain を DI コンテナに登録
	// HttpSecurity で認可/認証/ログイン/ログアウト等を構築
	public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		// リクエストごとの認可ルール定義
		http.authorizeHttpRequests(authorize -> authorize
				// ログイン画面・静的ファイルは未認証でも許可
				.requestMatchers("/login", "/css/**", "/js/**").permitAll()
				// /admin 配下は ADMIN ロールのみ
				.requestMatchers("/admin/**").hasRole("ADMIN")
				// /staff 配下は STAFF か ADMIN
				.requestMatchers("/staff/**").hasAnyRole("STAFF", "ADMIN")
				// それ以外は認証必須
				.anyRequest().authenticated())
				//フォームログインの設定
				.formLogin(form -> form
						//ログインページの URL（Controller 側に @GetMapping("/login") が必要）
						.loginPage("/login")
						//認証成功時の遷移先（常に /dashboard へ）
						.defaultSuccessUrl("/dashboard", true)
						//ログイン自体は誰でもアクセス可能
						.permitAll())
				//ログアウトの設定
				.logout(logout -> logout
						//ログアウトのエンドポイント（POST を推奨）
						.logoutUrl("/logout")
						//ログアウト成功後はログイン画面にメッセージ付きで戻る
						.logoutSuccessUrl("/login?logout")
						//誰でも叩ける
						.permitAll());
		//★注意: CSRF はデフォルト有効。全ての POST フォームに CSRF hidden を入れないと 403 になる。
		//上記設定から SecurityFilterChain を構築して返す
		return http.build();
	}

	@Bean // UserDetailsService を DI コンテナに登録（Spring Security がユーザ検索に使用）
	//UserRepository を利用してメールアドレスで検索
	public UserDetailsService userDetailsService(UserRepository userRepository) {
		//ログインフォームの "username" は実質メールアドレスとして扱う
		return email -> userRepository.findByEmail(email)
				//Spring Security 標準の User をビルド
				.map(user -> org.springframework.security.core.userdetails.User.builder()
						//ユーザ名＝メールアドレス
						.username(user.getEmail())
						//DB 上のパスワード（NoOp の場合は平文、本番はハッシュ）
						.password(user.getPassword())
						//ここで付与するロール。roles() は自動で "ROLE_" 接頭辞を付けてくれる
						.roles(user.getRole())
						.build())
				//見つからなければ例外
				.orElseThrow(() -> new UsernameNotFoundException("User not found: " + email));
	}

	@Bean // パスワードエンコーダを DI コンテナに登録
	public PasswordEncoder passwordEncoder() {
		//★開発用: 平文で照合。動作確認は早いが本番では絶対に NG。
		return NoOpPasswordEncoder.getInstance();
		//推奨: return new BCryptPasswordEncoder(); に切替し、data.sql も BCrypt ハッシュへ置換すること。
	}
}