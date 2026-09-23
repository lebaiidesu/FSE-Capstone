package com.bank.ledger.service;

import com.bank.ledger.dto.AuthRequest;
import com.bank.ledger.dto.AuthResponse;
import com.bank.ledger.model.oracle.Customer;
import com.bank.ledger.repository.oracle.CustomerRepository;
import com.bank.ledger.security.JwtTokenProvider;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AuthService {

    private final CustomerRepository customerRepository;
    private final JwtTokenProvider jwtTokenProvider;

    public AuthService(CustomerRepository customerRepository, JwtTokenProvider jwtTokenProvider) {
        this.customerRepository = customerRepository;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    public AuthResponse authenticate(AuthRequest request) {
        Customer customer = customerRepository.findByUsername(request.getUsername())
                .orElseGet(() -> {
                    // Seed fallback
                    Customer c = new Customer(request.getUsername(), "hash", "Juan", "Dela Cruz", "juan@paypink.ph", "+63 917 888 1234");
                    return customerRepository.save(c);
                });

        List<String> roles = List.of("ROLE_CUSTOMER", "ROLE_RETAIL_USER");
        String token = jwtTokenProvider.generateToken(customer.getCustomerId(), customer.getUsername(), roles);

        return new AuthResponse(
                token,
                86400000L,
                customer.getCustomerId(),
                customer.getUsername(),
                customer.getFirstName() + " " + customer.getLastName(),
                roles
        );
    }
}
