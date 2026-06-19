package com.seal.seal_backend.auth.security;

import com.seal.seal_backend.domain.entity.User;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

@Getter
public class UserPrincipal implements UserDetails {

    private final Long id;
    private final String email;
    private final String passwordHash;
    private final Collection<? extends GrantedAuthority> authorities;

    private UserPrincipal(Long id, String email, String passwordHash,
                          Collection<? extends GrantedAuthority> authorities) {
        this.id = id;
        this.email = email;
        this.passwordHash = passwordHash;
        this.authorities = authorities;
    }

    public static UserPrincipal from(User user) {
        String roleCode = (user.getPrimaryRole() != null)
                ? user.getPrimaryRole().getCode()
                : "TEAM_MEMBER";
        // ROLE_ prefix makes hasRole('COORDINATOR') work in @PreAuthorize
        List<GrantedAuthority> auths = List.of(new SimpleGrantedAuthority("ROLE_" + roleCode));
        return new UserPrincipal(user.getId(), user.getEmail(), user.getPasswordHash(), auths);
    }

    @Override public String getUsername()  { return email; }
    @Override public String getPassword()  { return passwordHash; }
    @Override public Collection<? extends GrantedAuthority> getAuthorities() { return authorities; }
    @Override public boolean isAccountNonExpired()   { return true; }
    @Override public boolean isAccountNonLocked()    { return true; }
    @Override public boolean isCredentialsNonExpired() { return true; }
    @Override public boolean isEnabled()             { return true; }
}
