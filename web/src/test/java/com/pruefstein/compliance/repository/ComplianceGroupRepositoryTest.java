package com.pruefstein.compliance.repository;

import com.pruefstein.compliance.domain.ComplianceGroup;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
@TestTransaction
class ComplianceGroupRepositoryTest
{
	@Inject
	ComplianceGroupRepository repository;

	@Test
	void testPersistAndFindById()
	{
		// given
		ComplianceGroup group = new ComplianceGroup();
		group.setName("A.10 Cryptography");
		repository.persist(group);

		// when
		ComplianceGroup found = repository.findById(group.id);

		// then
		assertNotNull(found);
		assertEquals("A.10 Cryptography", found.getName());
	}

	@Test
	void testFindByIdReturnsNullForUnknownId()
	{
		// given (empty DB)

		// when / then
		assertNull(repository.findById(Long.MAX_VALUE));
	}

	@Test
	void testListAll()
	{
		// given
		ComplianceGroup g1 = new ComplianceGroup();
		g1.setName("Group A");
		repository.persist(g1);

		ComplianceGroup g2 = new ComplianceGroup();
		g2.setName("Group B");
		repository.persist(g2);

		// when
		List<ComplianceGroup> all = repository.listAll();

		// then
		assertTrue(all.stream().anyMatch(g -> "Group A".equals(g.getName())));
		assertTrue(all.stream().anyMatch(g -> "Group B".equals(g.getName())));
	}

	@Test
	void testDeleteById()
	{
		// given
		ComplianceGroup group = new ComplianceGroup();
		group.setName("To Delete");
		repository.persist(group);
		Long id = group.id;

		// when
		repository.deleteById(id);

		// then
		assertNull(repository.findById(id));
	}

	@Test
	void testUpdateName()
	{
		// given
		ComplianceGroup group = new ComplianceGroup();
		group.setName("Original Name");
		repository.persist(group);

		// when
		group.setName("Updated Name");

		// then
		ComplianceGroup found = repository.findById(group.id);
		assertEquals("Updated Name", found.getName());
	}
}
