package com.pkcorporate.service;

import com.pkcorporate.dto.request.CreateCustomerRequest;
import com.pkcorporate.dto.response.CustomerResponse;
import com.pkcorporate.entity.Customer;
import com.pkcorporate.entity.User;
import com.pkcorporate.exception.BusinessException;
import com.pkcorporate.repository.CustomerRepository;
import com.pkcorporate.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final UserRepository userRepository;

    public CustomerResponse createCustomer(CreateCustomerRequest request, UUID agentId) {
        if (customerRepository.existsByEmailAndAgentId(request.getEmail().toLowerCase(), agentId)) {
            throw new BusinessException("Customer with this email is already registered under your account");
        }

        User agent = userRepository.findById(agentId)
                .orElseThrow(() -> new BusinessException("Agent not found"));

        Customer customer = Customer.builder()
                .name(request.getName().trim())
                .email(request.getEmail().trim().toLowerCase())
                .phone(request.getPhone().trim())
                .company(request.getCompany() != null ? request.getCompany().trim() : null)
                .gstin(request.getGstin() != null ? request.getGstin().trim() : null)
                .address(request.getAddress() != null ? request.getAddress().trim() : null)
                .city(request.getCity() != null ? request.getCity().trim() : null)
                .state(request.getState() != null ? request.getState().trim() : null)
                .pincode(request.getPincode() != null ? request.getPincode().trim() : null)
                .notes(request.getNotes() != null ? request.getNotes().trim() : null)
                .active(true)
                .agent(agent)
                .build();

        Customer saved = customerRepository.save(customer);
        log.info("Registered new customer {} ({}) for agent {}", saved.getName(), saved.getEmail(), agent.getName());
        return mapToResponse(saved);
    }

    @Transactional(readOnly = true)
    public Page<CustomerResponse> getCustomers(UUID userId, String role, String query, Pageable pageable) {
        Page<Customer> customers;
        if ("ADMIN".equalsIgnoreCase(role)) {
            if (query != null && !query.trim().isEmpty()) {
                customers = customerRepository.searchAll(query.trim(), pageable);
            } else {
                customers = customerRepository.findAll(pageable);
            }
        } else {
            // Must be Agent
            if (query != null && !query.trim().isEmpty()) {
                customers = customerRepository.searchByAgent(userId, query.trim(), pageable);
            } else {
                customers = customerRepository.findByAgentId(userId, pageable);
            }
        }
        return customers.map(this::mapToResponse);
    }

    private CustomerResponse mapToResponse(Customer customer) {
        return CustomerResponse.builder()
                .id(customer.getId())
                .name(customer.getName())
                .email(customer.getEmail())
                .phone(customer.getPhone())
                .company(customer.getCompany())
                .gstin(customer.getGstin())
                .address(customer.getAddress())
                .city(customer.getCity())
                .state(customer.getState())
                .pincode(customer.getPincode())
                .active(customer.isActive())
                .notes(customer.getNotes())
                .agentId(customer.getAgent() != null ? customer.getAgent().getId() : null)
                .agentName(customer.getAgent() != null ? customer.getAgent().getName() : null)
                .createdAt(customer.getCreatedAt())
                .updatedAt(customer.getUpdatedAt())
                .build();
    }
}
