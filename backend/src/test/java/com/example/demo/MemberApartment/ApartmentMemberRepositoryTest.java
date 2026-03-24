package com.example.demo.MemberApartment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.TestPropertySource;

import com.example.demo.Apartment.ApartmentEntity;
import com.example.demo.Apartment.ApartmentState;
import com.example.demo.User.Role;
import com.example.demo.User.UserEntity;
import com.example.demo.MemberApartment.MemberRole;

import jakarta.persistence.EntityManager;

@DataJpaTest
@TestPropertySource(properties = "spring.sql.init.mode=never")
public class ApartmentMemberRepositoryTest {
    
    @Autowired
    private ApartmentMemberRepository apartmentMemberRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    public void findByApartmentId_ReturnsMembersForApartment() {
        UserEntity landlord = persistUser("landlord-a@test.com", Role.LANDLORD);
        UserEntity tenant1 = persistUser("tenant-a@test.com", Role.TENANT);
        UserEntity tenant2 = persistUser("tenant-b@test.com", Role.TENANT);

        ApartmentEntity apartment = persistApartment("A1", "Madrid Centro", 500.0, ApartmentState.ACTIVE, landlord);
        ApartmentMemberEntity member2 = persistApartmentMember(tenant1, apartment);
        ApartmentMemberEntity member3 = persistApartmentMember(tenant2, apartment);

        List<ApartmentMemberEntity> members = apartmentMemberRepository.findByApartmentId(apartment.getId());

        assertEquals(2, members.size());
        assertTrue(members.stream().anyMatch(m -> m.getId().equals(member2.getId())));
        assertTrue(members.stream().anyMatch(m -> m.getId().equals(member3.getId())));
    }
    
    @Test
    public void findByApartmentIdAndEndDateIsNull_ReturnsOnlyActiveMembers() {
        UserEntity landlord = persistUser("landlord-a@test.com", Role.LANDLORD);
        UserEntity tenant1 = persistUser("tenant-a@test.com", Role.TENANT);
        UserEntity tenant2 = persistUser("tenant-b@test.com", Role.TENANT);
        UserEntity tenant3 = persistUser("tenant-c@test.com", Role.TENANT);

        ApartmentEntity apartment = persistApartment("A1", "Madrid Centro", 500.0, ApartmentState.ACTIVE, landlord);
        ApartmentMemberEntity member1 = persistApartmentMember(tenant1, apartment);
        ApartmentMemberEntity member2 = persistApartmentMember(tenant2, apartment);
        ApartmentMemberEntity member3 = persistApartmentMember(tenant3, apartment);
        member2.setEndDate(LocalDate.now().minusDays(1));
        entityManager.merge(member2);
        entityManager.flush();
        List<ApartmentMemberEntity> activeMembers = apartmentMemberRepository.findByApartmentIdAndEndDateIsNull(apartment.getId());
        assertEquals(2, activeMembers.size());
        assertTrue(activeMembers.stream().anyMatch(m -> m.getId().equals(member1.getId())));
        assertTrue(activeMembers.stream().anyMatch(m -> m.getId().equals(member3.getId())));
    }

    @Test
    public void existsByApartmentIdAndUserId_ReturnsTrueWhenExists() {
        UserEntity landlord = persistUser("landlord-a@test.com", Role.LANDLORD);
        UserEntity tenant = persistUser("tenant-a@test.com", Role.TENANT);

        ApartmentEntity apartment = persistApartment("A1", "Madrid Centro", 500.0, ApartmentState.ACTIVE, landlord);
        persistApartmentMember(tenant, apartment);

        boolean exists = apartmentMemberRepository.existsByApartmentIdAndUserId(apartment.getId(), tenant.getId());
        assertTrue(exists);
    }

    @Test
    public void existsByApartmentIdAndUserId_ReturnsFalseWhenNotExists() {
        UserEntity landlord = persistUser("landlord-a@test.com", Role.LANDLORD);
        UserEntity tenant = persistUser("tenant-a@test.com", Role.TENANT);

        ApartmentEntity apartment = persistApartment("A1", "Madrid Centro", 500.0, ApartmentState.ACTIVE, landlord);

        boolean exists = apartmentMemberRepository.existsByApartmentIdAndUserId(apartment.getId(), tenant.getId());
        assertFalse(exists);
    }

    @Test
    public void findActiveApartmentMembers_ReturnsOnlyActiveMembers() {
        UserEntity landlord = persistUser("landlord-a@test.com", Role.LANDLORD);
        UserEntity tenant1 = persistUser("tenant-a@test.com", Role.TENANT);
        UserEntity tenant2 = persistUser("tenant-b@test.com", Role.TENANT);
        UserEntity tenant3 = persistUser("tenant-c@test.com", Role.TENANT);

        ApartmentEntity apartment1 = persistApartment("A1", "Madrid Centro", 500.0, ApartmentState.ACTIVE, landlord);
        persistApartmentMember(tenant1, apartment1);
        persistApartmentMember(tenant2, apartment1);
        persistApartmentMember(tenant3, apartment1, LocalDate.now().minusDays(1));

        List<ApartmentMemberEntity> activeMembers = apartmentMemberRepository.findActiveApartmentMembers(apartment1.getId());
        assertEquals(2, activeMembers.size());
        assertTrue(activeMembers.stream().anyMatch(m -> m.getUser().getId().equals(tenant1.getId())));
        assertTrue(activeMembers.stream().anyMatch(m -> m.getUser().getId().equals(tenant2.getId())));
    }

    @Test
    public void findCurrentTenantsByApartmentId_ReturnsOnlyActiveMembers() {
        UserEntity landlord = persistUser("landlord-a@test.com", Role.LANDLORD);
        UserEntity tenant1 = persistUser("tenant-a@test.com", Role.TENANT);
        UserEntity tenant2 = persistUser("tenant-b@test.com", Role.TENANT);
        UserEntity tenant3 = persistUser("tenant-c@test.com", Role.TENANT);

        ApartmentEntity apartment1 = persistApartment("A1", "Madrid Centro", 500.0, ApartmentState.ACTIVE, landlord);
        persistApartmentMember(tenant1, apartment1);
        persistApartmentMember(tenant2, apartment1);
        persistApartmentMember(tenant3, apartment1, LocalDate.now().minusDays(1));

        List<ApartmentMemberEntity> currentTenants = apartmentMemberRepository.findCurrentTenantsByApartmentId(apartment1.getId());
        assertEquals(2, currentTenants.size());
    }

    @Test
    public void findLastMembershipByUserId_ReturnsMostRecentMembership() {
        UserEntity landlord = persistUser("landlord-a@test.com", Role.LANDLORD);
        UserEntity tenant = persistUser("tenant-a@test.com", Role.TENANT);
        ApartmentEntity apartment1 = persistApartment("A1", "Madrid Centro", 500.0, ApartmentState.ACTIVE, landlord);
        ApartmentEntity apartment2 = persistApartment("A2", "Madrid Centro", 600.0, ApartmentState.ACTIVE, landlord);

        ApartmentMemberEntity member1 = persistApartmentMember(tenant, apartment1, LocalDate.now().minusMonths(3), LocalDate.now().minusMonths(2));
        ApartmentMemberEntity member2 = persistApartmentMember(tenant, apartment2, LocalDate.now().minusDays(20), LocalDate.now().minusDays(2));
        
        Optional<ApartmentMemberEntity> lastMembership = apartmentMemberRepository.findLastMembershipByUserId(tenant.getId());

        assertTrue(lastMembership.isPresent());
        assertEquals(member2.getId(), lastMembership.get().getId());
    }

    @Test
    public void findLastMembershipByUserId_ReturnsEmptyWhenNoMemberships() {
        UserEntity tenant = persistUser("tenant-a@test.com", Role.TENANT);
        Optional<ApartmentMemberEntity> lastMembership = apartmentMemberRepository.findLastMembershipByUserId(tenant.getId());
        assertTrue(lastMembership.isEmpty());
    }

    @Test
    public void findByUserIdAndApartmentId_ReturnsMostRecentMembership() {
        UserEntity landlord = persistUser("landlord-a@test.com", Role.LANDLORD);
        UserEntity tenant = persistUser("tenant-a@test.com", Role.TENANT);
        ApartmentEntity apartment1 = persistApartment("A1", "Madrid Centro", 500.0, ApartmentState.ACTIVE, landlord);
        ApartmentEntity apartment2 = persistApartment("A2", "Madrid Centro", 600.0, ApartmentState.ACTIVE, landlord);

        ApartmentMemberEntity member1 = persistApartmentMember(tenant, apartment1, LocalDate.now().minusMonths(2), LocalDate.now().minusMonths(1));
        ApartmentMemberEntity member2 = persistApartmentMember(tenant, apartment2, LocalDate.now().minusDays(20), null);

        Optional<ApartmentMemberEntity> foundMember = apartmentMemberRepository.findByUserIdAndApartmentId(tenant.getId(), apartment2.getId());

        assertTrue(foundMember.isPresent());
        assertEquals(member2.getId(), foundMember.get().getId());
    }

    @Test
    public void findByUserIdAndApartmentId_ReturnsEmptyWhenNoMembership() {
        UserEntity tenant = persistUser("tenant-a@test.com", Role.TENANT);
        ApartmentEntity apartment = persistApartment("A1", "Madrid Centro", 500.0, ApartmentState.ACTIVE, tenant);
        Optional<ApartmentMemberEntity> foundMember = apartmentMemberRepository.findByUserIdAndApartmentId(tenant.getId(), apartment.getId());
        assertTrue(foundMember.isEmpty());
    }

    @Test
    public void findActiveMembershipsByUserId_ReturnsOnlyActiveMembershipsOrderedByJoinDate() {
        UserEntity landlord = persistUser("landlord-a@test.com", Role.LANDLORD);
        UserEntity tenant = persistUser("tenant-a@test.com", Role.TENANT);
        ApartmentEntity apartment1 = persistApartment("A1", "Madrid Centro", 500.0, ApartmentState.ACTIVE, landlord);
        ApartmentEntity apartment2 = persistApartment("A2", "Madrid Centro", 600.0, ApartmentState.ACTIVE, landlord);

        ApartmentMemberEntity member1 = persistApartmentMember(tenant, apartment1, LocalDate.now().minusMonths(2), LocalDate.now().minusMonths(1));
        ApartmentMemberEntity member2 = persistApartmentMember(tenant, apartment2, LocalDate.now().minusDays(20), null);

        List<ApartmentMemberEntity> activeMemberships = apartmentMemberRepository.findActiveMembershipsByUserId(tenant.getId());

        assertEquals(1, activeMemberships.size());
        assertEquals(member2.getId(), activeMemberships.get(0).getId());
    }

    @Test
    public void findOverlappingMemberships_ReturnsMembershipsWithOverlappingDates() {
        UserEntity landlord = persistUser("landlord-a@test.com", Role.LANDLORD);
        UserEntity tenant = persistUser("tenant-a@test.com", Role.TENANT);
        ApartmentEntity apartment = persistApartment("A1", "Madrid Centro", 500.0, ApartmentState.ACTIVE, landlord);

        ApartmentMemberEntity member1 = persistApartmentMember(tenant, apartment, LocalDate.now().minusMonths(2), LocalDate.now().minusMonths(1));
        ApartmentMemberEntity member2 = persistApartmentMember(tenant, apartment, LocalDate.now().minusDays(20), null);
        ApartmentMemberEntity member3 = persistApartmentMember(tenant, apartment, LocalDate.now().minusMonths(12), LocalDate.now().minusMonths(6));

        List<ApartmentMemberEntity> overlappingMemberships = apartmentMemberRepository.findOverlappingMemberships(tenant.getId(), apartment.getId(), LocalDate.now().minusDays(30), LocalDate.now().plusDays(10), LocalDate.now().minusMonths(3));

        assertEquals(2, overlappingMemberships.size());
        assertEquals(member1.getId(), overlappingMemberships.get(0).getId());
        assertEquals(member2.getId(), overlappingMemberships.get(1).getId());
    }

    @Test
    public void findOtherOverlappingMemberships_ReturnsMembershipsWithOverlappingDatesExcludingUser() {
        UserEntity landlord = persistUser("landlord-a@test.com", Role.LANDLORD);
        UserEntity tenant = persistUser("tenant-a@test.com", Role.TENANT);
        UserEntity otherTenant = persistUser("tenant-b@test.com", Role.TENANT);
        ApartmentEntity apartment = persistApartment("A1", "Madrid Centro", 500.0, ApartmentState.ACTIVE, landlord);

        ApartmentMemberEntity member1 = persistApartmentMember(tenant, apartment, LocalDate.now().minusMonths(2), LocalDate.now().minusMonths(1));
        ApartmentMemberEntity member2 = persistApartmentMember(otherTenant, apartment, LocalDate.now().minusDays(20), null);

        List<ApartmentMemberEntity> overlappingMemberships = apartmentMemberRepository.findOtherOverlappingMemberships(tenant.getId(), apartment.getId(), LocalDate.now().minusDays(30), LocalDate.now().plusDays(10), LocalDate.now().minusMonths(3));

        assertEquals(1, overlappingMemberships.size());
        assertEquals(member2.getId(), overlappingMemberships.get(0).getId());
    }

    @Test
    public void findAllByUserId_ReturnsAllMembershipsForUser() {
        UserEntity landlord = persistUser("landlord-a@test.com", Role.LANDLORD);
        UserEntity tenant = persistUser("tenant-a@test.com", Role.TENANT);
        ApartmentEntity apartment = persistApartment("A1", "Madrid Centro", 500.0, ApartmentState.ACTIVE, landlord);

        persistApartmentMember(tenant, apartment, LocalDate.now().minusMonths(2), LocalDate.now().minusMonths(1));
        persistApartmentMember(tenant, apartment, LocalDate.now().minusDays(20), null);

        List<ApartmentMemberEntity> allMemberships = apartmentMemberRepository.findAllByUserId(tenant.getId());

        assertEquals(2, allMemberships.size());
    }

    @Test
    public void findPastTenantMembershipsByUserIdAndApartmentId_ReturnsPastTenantMemberships() {
        UserEntity landlord = persistUser("landlord-a@test.com", Role.LANDLORD);
        UserEntity tenant = persistUser("tenant-a@test.com", Role.TENANT);
        ApartmentEntity apartment = persistApartment("A1", "Madrid Centro", 500.0, ApartmentState.ACTIVE, landlord);

        persistApartmentMember(tenant, apartment, LocalDate.now().minusMonths(2), LocalDate.now().minusMonths(1));

        List<ApartmentMemberEntity> pastMemberships = apartmentMemberRepository.findPastTenantMembershipsByUserIdAndApartmentId(landlord.getId(), apartment.getId(), LocalDate.now().minusMonths(3));

        assertEquals(1, pastMemberships.size());
    }

    @Test
    public void findPastLandlordMembershipsByUserIdAndApartmentId_ReturnsPastLandlordMemberships() {
        UserEntity landlord = persistUser("landlord-a@test.com", Role.LANDLORD);
        UserEntity landlord2 = persistUser("landlord-b@test.com", Role.LANDLORD);
        UserEntity tenant = persistUser("tenant-a@test.com", Role.TENANT);
        ApartmentEntity apartment = persistApartment("A1", "Madrid Centro", 500.0, ApartmentState.ACTIVE, landlord);
        ApartmentEntity apartment2 = persistApartment("A2", "Madrid Centro", 600.0, ApartmentState.ACTIVE, landlord2);

        persistApartmentMember(tenant, apartment, LocalDate.now().minusMonths(2), LocalDate.now().minusMonths(1));
        persistApartmentMember(tenant, apartment2, LocalDate.now().minusMonths(12), LocalDate.now().minusMonths(6));
        List<ApartmentMemberEntity> pastMemberships = apartmentMemberRepository.findPastLandlordMembershipsByUserIdAndApartmentId(landlord.getId(), apartment.getId(), LocalDate.now().minusMonths(3));

        assertEquals(1, pastMemberships.size());
    }

    @Test
    public void findLastApartmentsByTenantIdAndApartmentId_ReturnsLastApartmentsForTenant() {
        UserEntity landlord = persistUser("landlord-a@test.com", Role.LANDLORD);
        UserEntity tenant = persistUser("tenant-a@test.com", Role.TENANT);
        ApartmentEntity apartment = persistApartment("A1", "Madrid Centro", 500.0, ApartmentState.ACTIVE, landlord);
        ApartmentEntity apartment2 = persistApartment("A2", "Madrid Centro", 600.0, ApartmentState.ACTIVE, landlord);

        persistApartmentMember(tenant, apartment, LocalDate.now().minusMonths(2), LocalDate.now().minusMonths(1));
        persistApartmentMember(tenant, apartment2, LocalDate.now().minusMonths(12), null);
        List<ApartmentEntity> lastMemberships = apartmentMemberRepository.findLastApartmentsByTenantIdAndApartmentId(tenant.getId(), LocalDate.now());

        assertEquals(1, lastMemberships.size());
    }

    @Test
    public void findLastApartmentsByLandlordIdAndApartmentId_ReturnsLastApartmentsForLandlord() {
        UserEntity landlord = persistUser("landlord-a@test.com", Role.LANDLORD);
        UserEntity tenant = persistUser("tenant-a@test.com", Role.TENANT);
        ApartmentEntity apartment = persistApartment("A1", "Madrid Centro", 500.0, ApartmentState.ACTIVE, landlord);
        ApartmentEntity apartment2 = persistApartment("A2", "Madrid Centro", 600.0, ApartmentState.ACTIVE, landlord);

        persistApartmentMember(tenant, apartment, LocalDate.now().minusMonths(2), LocalDate.now());
        persistApartmentMember(tenant, apartment2, LocalDate.now().minusMonths(12), LocalDate.now());
        List<ApartmentEntity> lastMemberships = apartmentMemberRepository.findLastApartmentsByLandlordIdAndApartmentId(landlord.getId(), LocalDate.now().minusMonths(1));

        assertEquals(2, lastMemberships.size());
    }

    @Test
    public void findFirstByUserIdAndEndDateIsNullOrderByJoinDateDesc_ReturnsMostRecentActiveMembership() {
        UserEntity landlord = persistUser("landlord-a@test.com", Role.LANDLORD);
        UserEntity tenant = persistUser("tenant-a@test.com", Role.TENANT);
        ApartmentEntity apartment = persistApartment("A1", "Madrid Centro", 500.0, ApartmentState.ACTIVE, landlord);

        persistApartmentMember(tenant, apartment, LocalDate.now().minusMonths(2), LocalDate.now().minusMonths(1));
        persistApartmentMember(tenant, apartment, LocalDate.now().minusDays(20), null);
        Optional<ApartmentMemberEntity> mostRecentMembership = apartmentMemberRepository.findFirstByUserIdAndEndDateIsNullOrderByJoinDateDesc(tenant.getId());

        assertEquals(apartment, mostRecentMembership.get().getApartment());
    }

    @Test
    public void findFirstByUserIdAndEndDateIsNullOrderByJoinDateDesc_ReturnsEmptyWhenNoActiveMembership() {
        UserEntity tenant = persistUser("tenant-a@test.com", Role.TENANT);
        Optional<ApartmentMemberEntity> mostRecentMembership = apartmentMemberRepository.findFirstByUserIdAndEndDateIsNullOrderByJoinDateDesc(tenant.getId());

        assertFalse(mostRecentMembership.isPresent());
    }

    @Test
    public void existsByUserIdAndRole_ReturnsTrueWhenExists() {
        UserEntity tenant = persistUser("tenant-a@test.com", Role.TENANT);
        UserEntity landlord = persistUser("landlord-a@test.com", Role.LANDLORD);
        ApartmentEntity apartment = persistApartment("A1", "Madrid Centro", 500.0, ApartmentState.ACTIVE, landlord);

        ApartmentMemberEntity member = persistApartmentMember(tenant, apartment, MemberRole.HOMEBODY);
        boolean exists = apartmentMemberRepository.existsByUserIdAndRole(member.getUser().getId(), MemberRole.HOMEBODY);
        assertTrue(exists);
    }

    @Test
    public void existsByUserIdAndRole_ReturnsFalseWhenNotExists() {
        UserEntity landlord = persistUser("landlord-a@test.com", Role.LANDLORD);
        UserEntity tenant = persistUser("tenant-b@test.com", Role.TENANT);

        ApartmentEntity apartment = persistApartment("A1", "Madrid Centro", 500.0, ApartmentState.ACTIVE, landlord);
        ApartmentMemberEntity member = persistApartmentMember(tenant, apartment, MemberRole.HOMEBODY);
        boolean exists = apartmentMemberRepository.existsByUserIdAndRole(member.getUser().getId(), MemberRole.RENTER);
        assertFalse(exists);
    }


    private UserEntity persistUser(String email, Role role) {
        UserEntity user = new UserEntity();
        user.setEmail(email);
        user.setPassword("encoded-password");
        user.setRole(role);
        entityManager.persist(user);
        entityManager.flush();
        return user;
    }

    private ApartmentMemberEntity persistApartmentMember(UserEntity tenant, ApartmentEntity apartment, MemberRole memberRole) {
        ApartmentMemberEntity member = new ApartmentMemberEntity();
        member.setUser(tenant);
        member.setApartment(apartment);
        member.setJoinDate(LocalDate.now());
        member.setRole(memberRole);
        entityManager.persist(member);
        entityManager.flush();
        return member;
    }

    private ApartmentEntity persistApartment(String title, String ubication, Double price, ApartmentState state, UserEntity owner) {
        ApartmentEntity apartment = new ApartmentEntity();
        apartment.setTitle(title);
        apartment.setDescription("desc " + title);
        apartment.setPrice(price);
        apartment.setBills("bills");
        apartment.setUbication(ubication);
        apartment.setState(state);
        apartment.setUser(owner);
        entityManager.persist(apartment);
        entityManager.flush();
        return apartment;
    }

    private ApartmentMemberEntity persistApartmentMember(UserEntity tenant, ApartmentEntity apartment) {
        ApartmentMemberEntity member = new ApartmentMemberEntity();
        member.setUser(tenant);
        member.setApartment(apartment);
        member.setJoinDate(LocalDate.now());
        entityManager.persist(member);
        entityManager.flush();
        return member;
    }

    private ApartmentMemberEntity persistApartmentMember(UserEntity tenant, ApartmentEntity apartment, LocalDate endDate) {
        ApartmentMemberEntity member = new ApartmentMemberEntity();
        member.setUser(tenant);
        member.setApartment(apartment);
        member.setJoinDate(LocalDate.now());
        member.setEndDate(endDate);
        entityManager.persist(member);
        entityManager.flush();
        return member;
    }

    private ApartmentMemberEntity persistApartmentMember(UserEntity tenant, ApartmentEntity apartment, LocalDate joinDate, LocalDate endDate) {
        ApartmentMemberEntity member = new ApartmentMemberEntity();
        member.setUser(tenant);
        member.setApartment(apartment);
        member.setJoinDate(joinDate);
        member.setEndDate(endDate);
        entityManager.persist(member);
        entityManager.flush();
        return member;
    }
}
