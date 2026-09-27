package com.smarthire.service;

import com.smarthire.dto.AuthResponse;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class OAuthLoginCodeService {
    private final ConcurrentHashMap<String, Entry> codes = new ConcurrentHashMap<>();
    public String create(AuthResponse response) { purgeExpired(); String code=UUID.randomUUID().toString(); codes.put(code,new Entry(response,Instant.now().plusSeconds(60))); return code; }
    public AuthResponse consume(String code) { Entry entry=codes.remove(code); if(entry==null || entry.expiresAt().isBefore(Instant.now())) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Google sign-in expired. Please try again."); return entry.response(); }
    private void purgeExpired(){Instant now=Instant.now();codes.entrySet().removeIf(item->item.getValue().expiresAt().isBefore(now));}
    private record Entry(AuthResponse response, Instant expiresAt) { }
}
