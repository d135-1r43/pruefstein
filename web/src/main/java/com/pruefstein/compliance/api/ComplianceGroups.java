package com.pruefstein.compliance.api;

import com.pruefstein.compliance.domain.ComplianceGroup;
import com.pruefstein.compliance.domain.ComplianceItem;
import com.pruefstein.compliance.repository.ComplianceGroupRepository;
import com.pruefstein.compliance.repository.ComplianceItemRepository;
import io.quarkiverse.renarde.Controller;
import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;
import io.quarkus.runtime.LaunchMode;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.constraints.NotBlank;
import jakarta.ws.rs.POST;
import org.jboss.resteasy.reactive.RestForm;
import org.jboss.resteasy.reactive.RestPath;

import java.util.List;

public class ComplianceGroups extends Controller
{

	@Inject
	ComplianceGroupRepository groupRepository;

	@Inject
	ComplianceItemRepository itemRepository;

	@CheckedTemplate
	public static class Templates
	{
		public static native TemplateInstance index(List<ComplianceGroup> groups);

		public static native TemplateInstance show(ComplianceGroup group, List<ComplianceItem> items, boolean devMode);
	}

	// ── Groups
	// ────────────────────────────────────────────────────────────────

	public TemplateInstance index()
	{
		return Templates.index(groupRepository.listAll());
	}

	public TemplateInstance show(@RestPath Long id)
	{
		ComplianceGroup group = groupRepository.findById(id);
		if (group == null)
		{
			notFound();
			return null;
		}
		List<ComplianceItem> items = itemRepository.list("group", group);
		boolean devMode = LaunchMode.current() == LaunchMode.DEVELOPMENT;
		return Templates.show(group, items, devMode);
	}

	@POST
	@Transactional
	public void create(@RestForm @NotBlank String name)
	{
		if (validationFailed())
		{
			index();
			return;
		}
		ComplianceGroup group = new ComplianceGroup();
		group.setName(name);
		groupRepository.persist(group);
		index();
	}

	@POST
	@Transactional
	public void update(@RestForm Long id, @RestForm @NotBlank String name)
	{
		if (validationFailed())
		{
			index();
			return;
		}
		ComplianceGroup group = groupRepository.findById(id);
		if (group == null)
		{
			notFound();
			return;
		}
		group.setName(name);
		index();
	}

	@POST
	@Transactional
	public void delete(@RestForm Long id)
	{
		groupRepository.deleteById(id);
		index();
	}

	// ── Items
	// ─────────────────────────────────────────────────────────────────

	@POST
	@Transactional
	public void createItem(
		@RestForm Long groupId,
		@RestForm @NotBlank String name,
		@RestForm @NotBlank String query,
		@RestForm @NotBlank String expectedExpression)
	{
		if (validationFailed())
		{
			show(groupId);
			return;
		}
		ComplianceGroup group = groupRepository.findById(groupId);
		if (group == null)
		{
			notFound();
			return;
		}
		ComplianceItem item = new ComplianceItem();
		item.setName(name);
		item.setQuery(query);
		item.setExpectedExpression(expectedExpression);
		item.setGroup(group);
		itemRepository.persist(item);
		show(groupId);
	}

	@POST
	@Transactional
	public void updateItem(
		@RestForm Long id,
		@RestForm @NotBlank String name,
		@RestForm @NotBlank String query,
		@RestForm @NotBlank String expectedExpression)
	{
		if (validationFailed())
		{
			index();
			return;
		}
		ComplianceItem item = itemRepository.findById(id);
		if (item == null)
		{
			notFound();
			return;
		}
		item.setName(name);
		item.setQuery(query);
		item.setExpectedExpression(expectedExpression);
		show(item.getGroup().id);
	}

	@POST
	@Transactional
	public void deleteItem(@RestForm Long id)
	{
		ComplianceItem item = itemRepository.findById(id);
		if (item == null)
		{
			notFound();
			return;
		}
		Long groupId = item.getGroup().id;
		itemRepository.deleteById(id);
		show(groupId);
	}
}
