package com.pruefstein.compliance.api;

import java.util.List;

import com.pruefstein.compliance.domain.AppBlacklistCheck;
import com.pruefstein.compliance.domain.ComplianceGroup;
import com.pruefstein.compliance.domain.ComplianceItem;
import com.pruefstein.compliance.domain.ExpressionCheck;
import com.pruefstein.compliance.repository.ComplianceGroupRepository;
import com.pruefstein.compliance.repository.ComplianceItemRepository;
import com.pruefstein.compliance.service.CheckResolver;
import com.pruefstein.compliance.service.CheckResolver.ResolvedCheck;
import io.quarkiverse.renarde.Controller;
import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;
import io.quarkus.runtime.LaunchMode;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.constraints.NotBlank;
import jakarta.ws.rs.POST;
import org.jboss.resteasy.reactive.RestForm;
import org.jboss.resteasy.reactive.RestPath;

@RolesAllowed("**")
public class ComplianceGroups extends Controller
{
	@Inject
	ComplianceGroupRepository groupRepository;

	@Inject
	ComplianceItemRepository itemRepository;

	@Inject
	CheckResolver checkResolver;

	@CheckedTemplate
	public static class Templates
	{
		public static native TemplateInstance index(List<ComplianceGroup> groups);

		public static native TemplateInstance show(ComplianceGroup group, List<CheckRow> items, boolean devMode);
	}

	/**
	 * A check paired with its resolved query and expression, so the template
	 * does not have to know which checks store their SQL and which generate it.
	 */
	public record CheckRow(ComplianceItem check, String query, String expression)
	{
		public Long getId()
		{
			return check.id;
		}

		public String getName()
		{
			return check.getName();
		}

		public boolean isEditable()
		{
			return check.isEditable();
		}

		public String getQuery()
		{
			return query;
		}

		public String getExpression()
		{
			return expression;
		}
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
		List<CheckRow> items = itemRepository.list("group", group).stream()
			.filter(check -> !(check instanceof AppBlacklistCheck))
			.map(check -> {
				ResolvedCheck resolved = checkResolver.resolve(check);
				return new CheckRow(check, resolved.query(), resolved.expression());
			})
			.toList();
		boolean devMode = LaunchMode.current() == LaunchMode.DEVELOPMENT;
		return Templates.show(group, items, devMode);
	}

	@POST
	@Transactional
	@RolesAllowed("${pruefstein.security.admin-role:admin}")
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
	@RolesAllowed("${pruefstein.security.admin-role:admin}")
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
	@RolesAllowed("${pruefstein.security.admin-role:admin}")
	public void delete(@RestForm Long id)
	{
		groupRepository.deleteById(id);
		index();
	}

	// ── Items
	// ─────────────────────────────────────────────────────────────────

	@POST
	@Transactional
	@RolesAllowed("${pruefstein.security.admin-role:admin}")
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
		ExpressionCheck item = new ExpressionCheck();
		item.setName(name);
		item.setQuery(query);
		item.setExpectedExpression(expectedExpression);
		item.setGroup(group);
		itemRepository.persist(item);
		show(groupId);
	}

	@POST
	@Transactional
	@RolesAllowed("${pruefstein.security.admin-role:admin}")
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
		// Generated checks own their SQL; editing them here would be silently
		// overwritten on the next run.
		if (!(item instanceof ExpressionCheck expressionCheck))
		{
			badRequest();
			return;
		}
		expressionCheck.setName(name);
		expressionCheck.setQuery(query);
		expressionCheck.setExpectedExpression(expectedExpression);
		show(item.getGroup().id);
	}

	@POST
	@Transactional
	@RolesAllowed("${pruefstein.security.admin-role:admin}")
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
