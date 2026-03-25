package com.pruefstein.user.repository;

import com.pruefstein.user.domain.User;
import io.quarkus.hibernate.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class UserRepository implements PanacheRepository<User>
{
}
