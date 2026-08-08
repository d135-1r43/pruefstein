package com.pruefstein.compliance.repository;

import java.util.List;

import com.pruefstein.compliance.domain.ComplianceGroup;
import com.pruefstein.compliance.domain.ComplianceItem;
import com.pruefstein.compliance.domain.ExpressionCheck;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
@TestTransaction
class ComplianceItemRepositoryTest
{
	@Inject
	ComplianceGroupRepository groupRepository;

	@Inject
	ComplianceItemRepository itemRepository;

	@Test
	void testPersistWithGroupAndFindById()
	{
		// given
		ComplianceGroup group = new ComplianceGroup();
		group.setName("A.10 Cryptography");
		groupRepository.persist(group);

		ExpressionCheck item = new ExpressionCheck();
		item.setName("Disk encryption enabled");
		item.setQuery("SELECT encrypted FROM mounts WHERE path = '/';");
		item.setExpectedExpression("results[0].encrypted == \"1\"");
		item.setGroup(group);
		itemRepository.persist(item);

		// when
		ExpressionCheck found = (ExpressionCheck)itemRepository.findById(item.id);

		// then
		assertNotNull(found);
		assertEquals("Disk encryption enabled", found.getName());
		assertEquals("SELECT encrypted FROM mounts WHERE path = '/';", found.getQuery());
		assertEquals("results[0].encrypted == \"1\"", found.getExpectedExpression());
		assertEquals(group.id, found.getGroup().id);
	}

	@Test
	void testFindByIdReturnsNullForUnknownId()
	{
		// given (empty DB)

		// when / then
		assertNull(itemRepository.findById(Long.MAX_VALUE));
	}

	@Test
	void testListByGroup()
	{
		// given
		ComplianceGroup group = new ComplianceGroup();
		group.setName("A.12 Operations Security");
		groupRepository.persist(group);

		ComplianceGroup otherGroup = new ComplianceGroup();
		otherGroup.setName("Other");
		groupRepository.persist(otherGroup);

		ExpressionCheck item1 = new ExpressionCheck();
		item1.setName("Firewall enabled");
		item1.setQuery("SELECT global_state FROM alf;");
		item1.setExpectedExpression("results[0].global_state == \"1\"");
		item1.setGroup(group);
		itemRepository.persist(item1);

		ExpressionCheck item2 = new ExpressionCheck();
		item2.setName("Auto-update enabled");
		item2.setQuery("SELECT value FROM preferences WHERE key = 'AutomaticCheckEnabled';");
		item2.setExpectedExpression("results[0].value == \"1\"");
		item2.setGroup(group);
		itemRepository.persist(item2);

		ExpressionCheck itemOther = new ExpressionCheck();
		itemOther.setName("Other group item");
		itemOther.setQuery("SELECT 1;");
		itemOther.setExpectedExpression("results.size() > 0");
		itemOther.setGroup(otherGroup);
		itemRepository.persist(itemOther);

		// when
		List<ComplianceItem> items = itemRepository.list("group", group);

		// then
		assertEquals(2, items.size());
		assertTrue(items.stream().anyMatch(i -> "Firewall enabled".equals(i.getName())));
		assertTrue(items.stream().anyMatch(i -> "Auto-update enabled".equals(i.getName())));
	}

	@Test
	void testDeleteById()
	{
		// given
		ComplianceGroup group = new ComplianceGroup();
		group.setName("Temp");
		groupRepository.persist(group);

		ExpressionCheck item = new ExpressionCheck();
		item.setName("Temp Item");
		item.setQuery("SELECT 1;");
		item.setExpectedExpression("results.size() > 0");
		item.setGroup(group);
		itemRepository.persist(item);
		Long id = item.id;

		// when
		itemRepository.deleteById(id);

		// then
		assertNull(itemRepository.findById(id));
	}

	@Test
	void testUpdateFields()
	{
		// given
		ComplianceGroup group = new ComplianceGroup();
		group.setName("Group");
		groupRepository.persist(group);

		ExpressionCheck item = new ExpressionCheck();
		item.setName("Original");
		item.setQuery("SELECT 1;");
		item.setExpectedExpression("results.size() > 0");
		item.setGroup(group);
		itemRepository.persist(item);

		// when
		item.setName("Updated");
		item.setQuery("SELECT 2;");
		item.setExpectedExpression("results.size() == 1");

		// then
		ExpressionCheck found = (ExpressionCheck)itemRepository.findById(item.id);
		assertEquals("Updated", found.getName());
		assertEquals("SELECT 2;", found.getQuery());
		assertEquals("results.size() == 1", found.getExpectedExpression());
	}
}
