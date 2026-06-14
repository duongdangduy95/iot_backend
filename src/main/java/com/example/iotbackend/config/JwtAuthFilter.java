package com.example.iotbackend.config;

import com.example.iotbackend.service.CustomUserDetailsService;
import com.example.iotbackend.service.jwt.JwtService;
//import com.example.iotbackend.security.WebUserDetailsService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final CustomUserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");

        System.out.println("HEADER: " + authHeader);

        // 1. Kiểm tra nhanh Header, nếu rỗng thì cho đi tiếp luôn
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        // 2. Bọc toàn bộ phần xử lý Token trong try-catch để phòng ngừa Token rác từ Client gửi lên
        try {
            String token = authHeader.substring(7);
            // Nếu token rác, hàm extractEmail có thể ném ra ExpiredJwtException hoặc MalformedJwtException
            String email = jwtService.extractEmail(token);

            if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {

                UserDetails userDetails = userDetailsService.loadUserByUsername(email);

                // Đoạn ép kiểu này rất dễ lỗi nếu userDetails không đúng Class thực tế, bọc try-catch sẽ lo hết
                if (jwtService.isValid(token, (com.example.iotbackend.entity.User) ((com.example.iotbackend.security.WebUserDetails) userDetails).getUser())) {

                    UsernamePasswordAuthenticationToken authToken =
                            new UsernamePasswordAuthenticationToken(
                                    userDetails,
                                    null,
                                    userDetails.getAuthorities()
                            );

                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            }
        } catch (Exception e) {
            // In ra log để bạn biết lỗi gì (ví dụ: Token hết hạn, Token không hợp lệ)
            System.out.println("Lỗi xử lý JWT Token: " + e.getMessage());
            // Không ném lỗi ra ngoài, cứ để trống ở đây để chạy xuống dòng doFilter tiếp tục hành trình
        }

        // Luôn luôn đảm bảo dòng này được chạy trong mọi tình huống
        filterChain.doFilter(request, response);
    }
}