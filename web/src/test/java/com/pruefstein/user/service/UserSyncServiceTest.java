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
		userSyncService.syncUser("sub-new", "new@example.com", "Alice", "Smith");

		AppUser user = userRepository.findBySubject("sub-new").orElseThrow();
		assertEquals("sub-new", user.getKeycloakSubject());
		assertEquals("new@example.com", user.getMail());
		assertEquals("Alice", user.getFirstname());
		assertEquals("Smith", user.getLastname());
	}

	@Test
	void updatesExistingUserFields()
	{
		userSyncService.syncUser("sub-existing", "old@example.com", "Bob", "Jones");
		userSyncService.syncUser("sub-existing", "new@example.com", "Robert", "Jones");

		AppUser user = userRepository.findBySubject("sub-existing").orElseThrow();
		assertEquals("new@example.com", user.getMail());
		assertEquals("Robert", user.getFirstname());
	}

	@Test
	void usesSubjectAsFirstnameWhenFirstnameIsNull()
	{
		userSyncService.syncUser("sub-nofirst", "x@example.com", null, "Doe");

		AppUser user = userRepository.findBySubject("sub-nofirst").orElseThrow();
		assertEquals("sub-nofirst", user.getFirstname());
		assertEquals("Doe", user.getLastname());
	}

	@Test
	void usesEmptyStringAsLastnameWhenLastnameIsNull()
	{
		userSyncService.syncUser("sub-nolast", "x@example.com", "Jane", null);

		AppUser user = userRepository.findBySubject("sub-nolast").orElseThrow();
		assertEquals("Jane", user.getFirstname());
		assertEquals("", user.getLastname());
	}

	@Test
	void doesNotOverwriteFieldsWithNullOnUpdate()
	{
		userSyncService.syncUser("sub-nullupdate", "orig@example.com", "Orig", "Name");
		userSyncService.syncUser("sub-nullupdate", null, null, null);

		AppUser user = userRepository.findBySubject("sub-nullupdate").orElseThrow();
		assertEquals("orig@example.com", user.getMail());
		assertEquals("Orig", user.getFirstname());
		assertEquals("Name", user.getLastname());
	}
}
