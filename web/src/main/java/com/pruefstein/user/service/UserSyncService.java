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

	/**
	 * Creates or refreshes the local record for an OIDC identity and returns
	 * it, so callers that need the user — a mail address, for instance — do not
	 * have to look it up again.
	 */
	@Transactional
	public AppUser syncUser(String subject, String email, String firstName, String lastName)
	{
		return userRepository.findBySubject(subject)
			.map(user -> {
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
				return user;
			})
			.orElseGet(() -> {
				AppUser user = new AppUser();
				user.setOidcSubject(subject);
				user.setMail(email);
				user.setFirstname(firstName != null ? firstName : subject);
				user.setLastname(lastName != null ? lastName : "");
				userRepository.persist(user);
				return user;
			});
	}
}
