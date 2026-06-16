package com.sakura.taskmanager.config;

import com.sakura.taskmanager.utils.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableWebSecurity
public class SecurityConfig {
    @Bean
    public BCryptPasswordEncoder passwordEncoder(){
        return new BCryptPasswordEncoder(10);
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http,JwtAuthenticationFilter jwtAuthenticationFilter) throws Exception {
        http
                .csrf(csrf -> csrf.disable())  // 只保留一个
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/user/login", "/user/register", "/uploads/**").permitAll()  // 放行登录注册
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * JWT 认证过滤器
     */
    @Component
    public  class JwtAuthenticationFilter extends OncePerRequestFilter {
        @Autowired
        private StringRedisTemplate stringRedisTemplate;

        @Override
        protected void doFilterInternal(HttpServletRequest request,
                                        HttpServletResponse response,
                                        FilterChain filterChain) throws ServletException, IOException {

            // 从请求头中获取 token
            String authorization = request.getHeader("Authorization");

            if (authorization != null && authorization.startsWith("Bearer ")) {
                String token = authorization.substring(7);

                try {

                    // 解析 token
                    Map<String, Object> claims = JwtUtil.parseToken(token);
                    String username = (String) claims.get("username");
                    Integer id = (Integer) claims.get("id");
                    ValueOperations<String, String> operations = stringRedisTemplate.opsForValue();
                    String redisToken = operations.get(token);
                    if (redisToken == null) {
                        //失效
                        throw new RuntimeException();
                    }

                    // 创建认证对象
                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(id, token, null);

                    // 存入用户详情
                    Map<String, Object> details = new HashMap<>();
                    details.put("username", username);
                    details.put("id", id);
                    authentication.setDetails(details);

                    // 设置到 SecurityContext
                    SecurityContextHolder.getContext().setAuthentication(authentication);

                    filterChain.doFilter(request, response);

                } catch (Exception e) {
                    // token 无效，返回 401
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.setContentType("application/json");
                    response.getWriter().write("{\"error\":\"Invalid or expired token\"}");
                    return;  // 注意：这里直接返回，不继续执行
                }
            } else {
                // 没有 token 但访问需要认证的接口
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json");
                response.getWriter().write("{\"error\":\"Missing authentication token\"}");
                return;
            }
        }

        // 可选：重写 shouldNotFilter 方法来放行特定路径（更优雅的方式）
        @Override
        protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
            // 放行登录和注册接口（不需要 token）
            String path = request.getRequestURI();
            return path.equals("/user/login") || path.equals("/user/register") || path.startsWith("/uploads/");
        }
    }
}
