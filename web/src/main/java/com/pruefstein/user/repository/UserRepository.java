package com.pruefstein.user.repository;

import com.pruefstein.user.domain.AppUser;
import io.quarkus.hibernate.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class UserRepository implements PanacheRepository<AppUser>
{
}
