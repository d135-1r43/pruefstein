package com.pruefstein.user.api;

import com.pruefstein.user.domain.AppUser;
import com.pruefstein.user.repository.UserRepository;
import io.quarkiverse.renarde.Controller;
import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.ws.rs.POST;
import org.jboss.resteasy.reactive.RestForm;

import java.util.List;

public class Users extends Controller
{

	@Inject
	UserRepository userRepository;

	@CheckedTemplate
	public static class Templates
	{
		public static native TemplateInstance index(List<AppUser> appUsers);
	}

	public TemplateInstance index()
	{
		return Templates.index(userRepository.listAll());
	}

	@POST
	@Transactional
	public void create(
		@RestForm @NotBlank String firstname,
		@RestForm @NotBlank String lastname,
		@RestForm @Email @NotBlank String mail)
	{
		if (validationFailed())
		{
			index();
			return;
		}
		AppUser appUser = new AppUser();
		appUser.setFirstname(firstname);
		appUser.setLastname(lastname);
		appUser.setMail(mail);
		userRepository.persist(appUser);
		index();
	}

	@POST
	@Transactional
	public void update(
		@RestForm Long id,
		@RestForm @NotBlank String firstname,
		@RestForm @NotBlank String lastname,
		@RestForm @Email @NotBlank String mail)
	{
		if (validationFailed())
		{
			index();
			return;
		}
		AppUser appUser = userRepository.findById(id);
		if (appUser == null)
		{
			notFound();
			return;
		}
		appUser.setFirstname(firstname);
		appUser.setLastname(lastname);
		appUser.setMail(mail);
		index();
	}

	@POST
	@Transactional
	public void delete(@RestForm Long id)
	{
		userRepository.deleteById(id);
		index();
	}
}
