package it.uniroma3.siwprogetto.config;

import it.uniroma3.siwprogetto.service.CustomUserDetailsService;
import it.uniroma3.siwprogetto.service.UserService;
import it.uniroma3.siwprogetto.util.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(securedEnabled = true)
public class WebSecurityConfig {

    private final CustomUserDetailsService customUserDetailsService;
    private final AuthenticationSuccessHandler customAuthenticationSuccessHandler;
    private final AuthenticationFailureHandler customAuthenticationFailureHandler;
    private final PasswordEncoder passwordEncoder;
    private UserService userService;

    @Autowired
    public WebSecurityConfig(CustomUserDetailsService customUserDetailsService,
                             AuthenticationSuccessHandler customAuthenticationSuccessHandler,
                             AuthenticationFailureHandler customAuthenticationFailureHandler,
                             PasswordEncoder passwordEncoder) {
        this.customUserDetailsService = customUserDetailsService;
        this.customAuthenticationSuccessHandler = customAuthenticationSuccessHandler;
        this.customAuthenticationFailureHandler = customAuthenticationFailureHandler;
        this.passwordEncoder = passwordEncoder;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/","/forgot-password", "/reset-password", "/index", "/login", "/register","/products", "/css/**", "/image/**","/js/**", "favicon.ico").permitAll()
                        .requestMatchers(HttpMethod.POST, "/login", "/register", "/forgot-password", "/reset-password").permitAll()
                        .requestMatchers("/account").authenticated() // Solo utenti autenticati possono accedere a /account
                        //.requestMatchers("/admin/**").hasAnyAuthority(SecurityConstants.ADMIN_ROLE)   //lasciato per il futuro
                        .requestMatchers("/manutenzione/**").hasAnyAuthority(SecurityConstants.ADMIN_ROLE)
                        .anyRequest().authenticated()

                )
                .exceptionHandling(exceptionHandling -> exceptionHandling.accessDeniedHandler((request, response, accessDeniedException) -> response.sendRedirect("/index")))
                .formLogin(formLogin -> formLogin
                        .loginPage("/login").failureUrl("/login?error=true").permitAll()
                        .loginProcessingUrl("/perform_login")
                        .usernameParameter("username")
                        .passwordParameter("password")
                        .successHandler(customAuthenticationSuccessHandler)
                        .failureHandler(customAuthenticationFailureHandler)
                        .defaultSuccessUrl("/account", true)
                )
                .rememberMe(rememberMe -> rememberMe // Aggiunta di Remember Me
                        .key("uniqueAndSecretKey")
                        .tokenValiditySeconds(86400) // 24 ore
                        .rememberMeParameter("remember-me")
                        .userDetailsService(customUserDetailsService)
                )
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessHandler((request, response, authentication) -> response.sendRedirect("/"))
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID")
                        .clearAuthentication(true)
                        .logoutRequestMatcher(new AntPathRequestMatcher("/logout"))
                        .permitAll()
                );
        return http.build();
    }

    @Bean
    public SecurityUtils securityUtils() {
        return new SecurityUtils();
    }


    @Autowired
    public void configureAuth(AuthenticationManagerBuilder auth) throws Exception {
        auth.userDetailsService(customUserDetailsService)
                .passwordEncoder(passwordEncoder);
    }

	@Autowired
	public void setUserService(UserService userService) {
        this.userService = userService;
    }
}