package com.pruefstein.compliance.repository;

import com.pruefstein.compliance.domain.ComplianceGroup;
import com.pruefstein.compliance.domain.ComplianceItem;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.util.List;

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
		ComplianceGroup group = new ComplianceGroup();
		group.setName("A.10 Cryptography");
		groupRepository.persist(group);

		ComplianceItem item = new ComplianceItem();
		item.setName("Disk encryption enabled");
		item.setQuery("SELECT encrypted FROM mounts WHERE path = '/';");
		item.setExpectedExpression("results[0].encrypted == \"1\"");
		item.setGroup(group);
		itemRepository.persist(item);

		ComplianceItem found = itemRepository.findById(item.id);
		assertNotNull(found);
		assertEquals("Disk encryption enabled", found.getName());
		assertEquals("SELECT encrypted FROM mounts WHERE path = '/';", found.getQuery());
		assertEquals("results[0].encrypted == \"1\"", found.getExpectedExpression());
		assertEquals(group.id, found.getGroup().id);
	}

	@Test
	void testFindByIdReturnsNullForUnknownId()
	{
		assertNull(itemRepository.findById(Long.MAX_VALUE));
	}

	@Test
	void testListByGroup()
	{
		ComplianceGroup group = new ComplianceGroup();
		group.setName("A.12 Operations Security");
		groupRepository.persist(group);

		ComplianceGroup otherGroup = new ComplianceGroup();
		otherGroup.setName("Other");
		groupRepository.persist(otherGroup);

		ComplianceItem item1 = new ComplianceItem();
		item1.setName("Firewall enabled");
		item1.setQuery("SELECT global_state FROM alf;");
		item1.setExpectedExpression("results[0].global_state == \"1\"");
		item1.setGroup(group);
		itemRepository.persist(item1);

		ComplianceItem item2 = new ComplianceItem();
		item2.setName("Auto-update enabled");
		item2.setQuery("SELECT value FROM preferences WHERE key = 'AutomaticCheckEnabled';");
		item2.setExpectedExpression("results[0].value == \"1\"");
		item2.setGroup(group);
		itemRepository.persist(item2);

		ComplianceItem itemOther = new ComplianceItem();
		itemOther.setName("Other group item");
		itemOther.setQuery("SELECT 1;");
		itemOther.setExpectedExpression("results.size() > 0");
		itemOther.setGroup(otherGroup);
		itemRepository.persist(itemOther);

		List<ComplianceItem> items = itemRepository.list("group", group);
		assertEquals(2, items.size());
		assertTrue(items.stream().anyMatch(i -> "Firewall enabled".equals(i.getName())));
		assertTrue(items.stream().anyMatch(i -> "Auto-update enabled".equals(i.getName())));
	}

	@Test
	void testDeleteById()
	{
		ComplianceGroup group = new ComplianceGroup();
		group.setName("Temp");
		groupRepository.persist(group);

		ComplianceItem item = new ComplianceItem();
		item.setName("Temp Item");
		item.setQuery("SELECT 1;");
		item.setExpectedExpression("results.size() > 0");
		item.setGroup(group);
		itemRepository.persist(item);
		Long id = item.id;

		itemRepository.deleteById(id);

		assertNull(itemRepository.findById(id));
	}

	@Test
	void testUpdateFields()
	{
		ComplianceGroup group = new ComplianceGroup();
		group.setName("Group");
		groupRepository.persist(group);

		ComplianceItem item = new ComplianceItem();
		item.setName("Original");
		item.setQuery("SELECT 1;");
		item.setExpectedExpression("results.size() > 0");
		item.setGroup(group);
		itemRepository.persist(item);

		item.setName("Updated");
		item.setQuery("SELECT 2;");
		item.setExpectedExpression("results.size() == 1");

		ComplianceItem found = itemRepository.findById(item.id);
		assertEquals("Updated", found.getName());
		assertEquals("SELECT 2;", found.getQuery());
		assertEquals("results.size() == 1", found.getExpectedExpression());
	}
}
