package it.uniroma3.siwprogetto.config;

import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
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
                .csrf(csrf -> csrf
                        .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                .ignoringRequestMatchers("/rest/api/**"))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/", "/forgot-password", "/reset-password", "/index", "/login", "/register", "/products", "/css/**", "/image/**", "/js/**", "favicon.ico","/static/**", "/Uploads/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/login", "/register", "/forgot-password", "/reset-password").permitAll()
                        .requestMatchers("/cart/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/cart/**").permitAll()
                        .requestMatchers("/account").authenticated()
                        .requestMatchers(HttpMethod.POST, "/rest/api/dealers").hasAuthority(SecurityConstants.DEALER_ROLE)
                        .requestMatchers("/manutenzione/private").hasAnyAuthority(SecurityConstants.PRIVATE_ROLE)
                        .requestMatchers("/manutenzione/dealer").hasAuthority(SecurityConstants.DEALER_ROLE)
                        .requestMatchers(HttpMethod.POST, "/rest/api/dealers").authenticated() // Richiede autenticazione per /rest/api/dealers POST
                        .requestMatchers("/rest/dealers-page", "/rest/api/dealers").authenticated()// richiede autenticazione per /rest/api/dealers Get
                        .anyRequest().authenticated()
                )
                .exceptionHandling(exceptionHandling -> exceptionHandling.accessDeniedHandler((request, response, accessDeniedException) -> response.sendRedirect("/index")))
                .formLogin(formLogin -> formLogin
                        .loginPage("/login").failureUrl("/login?error=true").permitAll()
                        .defaultSuccessUrl("/rest/dealers-page",true)
                        .loginProcessingUrl("/perform_login")
                        .usernameParameter("username")
                        .passwordParameter("password")
                        .successHandler(customAuthenticationSuccessHandler)
                        .failureHandler(customAuthenticationFailureHandler)
                        .defaultSuccessUrl("/account", true)
                )
                .rememberMe(rememberMe -> rememberMe
                        .key("uniqueAndSecretKey")
                        .tokenValiditySeconds(86400)
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

}