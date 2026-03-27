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
class ComplianceGroupRepositoryTest {

    @Inject
    ComplianceGroupRepository repository;

    @Test
    void testPersistAndFindById() {
        ComplianceGroup group = new ComplianceGroup();
        group.setName("A.10 Cryptography");
        repository.persist(group);

        ComplianceGroup found = repository.findById(group.id);
        assertNotNull(found);
        assertEquals("A.10 Cryptography", found.getName());
    }

    @Test
    void testFindByIdReturnsNullForUnknownId() {
        assertNull(repository.findById(Long.MAX_VALUE));
    }

    @Test
    void testListAll() {
        ComplianceGroup g1 = new ComplianceGroup();
        g1.setName("Group A");
        repository.persist(g1);

        ComplianceGroup g2 = new ComplianceGroup();
        g2.setName("Group B");
        repository.persist(g2);

        List<ComplianceGroup> all = repository.listAll();
        assertTrue(all.stream().anyMatch(g -> "Group A".equals(g.getName())));
        assertTrue(all.stream().anyMatch(g -> "Group B".equals(g.getName())));
    }

    @Test
    void testDeleteById() {
        ComplianceGroup group = new ComplianceGroup();
        group.setName("To Delete");
        repository.persist(group);
        Long id = group.id;

        repository.deleteById(id);

        assertNull(repository.findById(id));
    }

    @Test
    void testUpdateName() {
        ComplianceGroup group = new ComplianceGroup();
        group.setName("Original Name");
        repository.persist(group);

        group.setName("Updated Name");

        ComplianceGroup found = repository.findById(group.id);
        assertEquals("Updated Name", found.getName());
    }
}
