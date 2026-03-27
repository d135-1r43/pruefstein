package com.pruefstein.dashboard.api;

import com.pruefstein.compliance.repository.ComplianceItemRepository;
import com.pruefstein.user.repository.UserRepository;
import io.quarkiverse.renarde.Controller;
import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;
import jakarta.inject.Inject;
import jakarta.ws.rs.Path;

public class Dashboard extends Controller
{

	@Inject
	UserRepository userRepository;

	@Inject
	ComplianceItemRepository itemRepository;

	@CheckedTemplate
	public static class Templates
	{
		public static native TemplateInstance index(long userCount, long itemCount);
	}

	@Path("/")
	public TemplateInstance index()
	{
		return Templates.index(userRepository.count(), itemRepository.count());
	}
}
