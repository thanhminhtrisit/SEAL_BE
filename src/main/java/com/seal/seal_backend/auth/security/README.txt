M1 puts JWT plumbing here: JwtTokenProvider, JwtAuthFilter, UserPrincipal (implements UserDetails),
and wires JwtAuthFilter into config/SecurityConfig. Keep token secret/expiry in .env (already present).
