package com.pruefstein.user.repository;

import java.util.Optional;

import com.pruefstein.user.domain.AppUser;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class UserRepository implements PanacheRepository<AppUser>
{
	public Optional<AppUser> findBySubject(String keycloakSubject)
	{
		return find("keycloakSubject", keycloakSubject).firstResultOptional();
	}
}
