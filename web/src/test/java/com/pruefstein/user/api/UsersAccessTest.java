package com.pruefstein.user.api;

import com.pruefstein.user.domain.AppUser;
import com.pruefstein.user.repository.UserRepository;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import jakarta.inject.Inject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.lessThan;

@QuarkusTest
class UsersAccessTest
{
	@Inject
	UserRepository userRepository;

	@AfterEach
	void tearDown()
	{
		QuarkusTransaction.requiringNew().run(() -> userRepository.delete("mail", "access-test@example.com"));
	}

	@Test
	@TestSecurity(user = "alice", roles = {})
	void nonAdminCannotCreateUser()
	{
		// given (regular user without admin role)

		// when / then
		given()
			.contentType("application/x-www-form-urlencoded")
			.formParam("firstname", "Test")
			.formParam("lastname", "User")
			.formParam("mail", "access-test@example.com")
			.when().post("/Users/create")
			.then()
			.statusCode(403);
	}

	@Test
	@TestSecurity(user = "alice", roles = {})
	void nonAdminCannotUpdateUser()
	{
		// given
		Long[] ids = new Long[1];
		QuarkusTransaction.requiringNew().run(() -> {
			AppUser user = new AppUser();
			user.setFirstname("Before");
			user.setLastname("Update");
			user.setMail("access-test@example.com");
			userRepository.persist(user);
			ids[0] = user.id;
		});

		// when / then
		given()
			.contentType("application/x-www-form-urlencoded")
			.formParam("id", ids[0])
			.formParam("firstname", "After")
			.formParam("lastname", "Update")
			.formParam("mail", "access-test@example.com")
			.when().post("/Users/update")
			.then()
			.statusCode(403);

		QuarkusTransaction.requiringNew().run(() -> userRepository.deleteById(ids[0]));
	}

	@Test
	@TestSecurity(user = "alice", roles = {})
	void nonAdminCannotDeleteUser()
	{
		// given
		Long[] ids = new Long[1];
		QuarkusTransaction.requiringNew().run(() -> {
			AppUser user = new AppUser();
			user.setFirstname("To");
			user.setLastname("Delete");
			user.setMail("access-test@example.com");
			userRepository.persist(user);
			ids[0] = user.id;
		});

		// when / then
		given()
			.contentType("application/x-www-form-urlencoded")
			.formParam("id", ids[0])
			.when().post("/Users/delete")
			.then()
			.statusCode(403);

		QuarkusTransaction.requiringNew().run(() -> userRepository.deleteById(ids[0]));
	}

	@Test
	@TestSecurity(user = "admin", roles = { "admin" })
	void adminCanCreateUser()
	{
		// given (admin user)

		// when / then — admin is not blocked (200 or 302, not 403)
		given()
			.contentType("application/x-www-form-urlencoded")
			.formParam("firstname", "Test")
			.formParam("lastname", "User")
			.formParam("mail", "access-test@example.com")
			.when().post("/Users/create")
			.then()
			.statusCode(lessThan(400));
	}
}
