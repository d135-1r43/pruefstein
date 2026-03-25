package com.pruefstein.repository;

import com.pruefstein.model.User;
import io.quarkus.hibernate.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class UserRepository implements PanacheRepository<User>
{
}
