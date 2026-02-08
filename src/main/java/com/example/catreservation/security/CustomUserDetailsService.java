package com.example.catreservation.security;

import java.util.List;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.example.catreservation.entity.User;
import com.example.catreservation.repository.UserRepository;

@Service
public class CustomUserDetailsService implements UserDetailsService {

	private final UserRepository userRepository;

	public CustomUserDetailsService(UserRepository userRepository) {
		this.userRepository = userRepository;
	}

	/**
	 * Spring Security がログイン時に必ず呼ぶメソッド
	 * username = login画面で入力された値（今回は email）
	 */
	@Override
	public UserDetails loadUserByUsername(String username)
			throws UsernameNotFoundException {

		// email を使って users テーブルから取得
		User user = userRepository.findByEmail(username)
				.orElseThrow(() -> new UsernameNotFoundException("ユーザーが見つかりません: " + username));

		// Spring Security 用の UserDetails を返す
		return new org.springframework.security.core.userdetails.User(
				user.getEmail(), // ログインID
				user.getPassword(), // パスワード
				List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole())));
	}
}