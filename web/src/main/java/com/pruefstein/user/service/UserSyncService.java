package com.pruefstein.user.service;

import com.pruefstein.user.domain.AppUser;
import com.pruefstein.user.repository.UserRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class UserSyncService
{
	@Inject
	UserRepository userRepository;

	@Transactional
	public void syncUser(String subject, String email, String firstName, String lastName)
	{
		userRepository.findBySubject(subject).ifPresentOrElse(
			user -> {
				if (email != null)
				{
					user.setMail(email);
				}
				if (firstName != null)
				{
					user.setFirstname(firstName);
				}
				if (lastName != null)
				{
					user.setLastname(lastName);
				}
			},
			() -> {
				AppUser user = new AppUser();
				user.setOidcSubject(subject);
				user.setMail(email);
				user.setFirstname(firstName != null ? firstName : subject);
				user.setLastname(lastName != null ? lastName : "");
				userRepository.persist(user);
			});
	}
}
