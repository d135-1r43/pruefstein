package com.pruefstein.user.service;

import com.pruefstein.user.domain.AppUser;
import com.pruefstein.user.repository.UserRepository;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
@TestTransaction
class UserSyncServiceTest
{
	@Inject
	UserSyncService userSyncService;

	@Inject
	UserRepository userRepository;

	@Test
	void createsNewUserWhenNotFound()
	{
		// given (no user with subject "sub-new" exists)

		// when
		userSyncService.syncUser("sub-new", "new@example.com", "Alice", "Smith");

		// then
		AppUser user = userRepository.findBySubject("sub-new").orElseThrow();
		assertEquals("sub-new", user.getOidcSubject());
		assertEquals("new@example.com", user.getMail());
		assertEquals("Alice", user.getFirstname());
		assertEquals("Smith", user.getLastname());
	}

	@Test
	void updatesExistingUserFields()
	{
		// given
		userSyncService.syncUser("sub-existing", "old@example.com", "Bob", "Jones");

		// when
		userSyncService.syncUser("sub-existing", "new@example.com", "Robert", "Jones");

		// then
		AppUser user = userRepository.findBySubject("sub-existing").orElseThrow();
		assertEquals("new@example.com", user.getMail());
		assertEquals("Robert", user.getFirstname());
	}

	@Test
	void usesSubjectAsFirstnameWhenFirstnameIsNull()
	{
		// given (no pre-existing user)

		// when
		userSyncService.syncUser("sub-nofirst", "x@example.com", null, "Doe");

		// then
		AppUser user = userRepository.findBySubject("sub-nofirst").orElseThrow();
		assertEquals("sub-nofirst", user.getFirstname());
		assertEquals("Doe", user.getLastname());
	}

	@Test
	void usesEmptyStringAsLastnameWhenLastnameIsNull()
	{
		// given (no pre-existing user)

		// when
		userSyncService.syncUser("sub-nolast", "x@example.com", "Jane", null);

		// then
		AppUser user = userRepository.findBySubject("sub-nolast").orElseThrow();
		assertEquals("Jane", user.getFirstname());
		assertEquals("", user.getLastname());
	}

	@Test
	void doesNotOverwriteFieldsWithNullOnUpdate()
	{
		// given
		userSyncService.syncUser("sub-nullupdate", "orig@example.com", "Orig", "Name");

		// when
		userSyncService.syncUser("sub-nullupdate", null, null, null);

		// then
		AppUser user = userRepository.findBySubject("sub-nullupdate").orElseThrow();
		assertEquals("orig@example.com", user.getMail());
		assertEquals("Orig", user.getFirstname());
		assertEquals("Name", user.getLastname());
	}
}
