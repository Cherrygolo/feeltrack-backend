package ld.feeltrack_backend.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import ld.feeltrack_backend.entity.Customer;
import ld.feeltrack_backend.service.CustomerService;


@RestController
@RequestMapping(path = "customer")
public class CustomerController {

    private final CustomerService customerService;

    public CustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }

    @PostMapping(consumes = APPLICATION_JSON_VALUE)
    public ResponseEntity<Customer> createCustomer(@Valid @RequestBody Customer customer) {
        Customer createdCustomer = this.customerService.createCustomer(customer);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdCustomer);
    }

    @ResponseStatus(HttpStatus.NO_CONTENT)
    @DeleteMapping(path = "{id}")
    public void deleteCustomer(@PathVariable int id) {
        this.customerService.deleteCustomer(id);
    }

    @GetMapping(produces = APPLICATION_JSON_VALUE)
    public List<Customer> getAllCustomers() {
        return this.customerService.getAllCustomers();
    }

    @GetMapping(path = "{id}", produces = APPLICATION_JSON_VALUE)
    public Customer getCustomerById(@PathVariable int id) {
        return this.customerService.getCustomerById(id);
    }

    @PutMapping(path = "{id}", consumes = APPLICATION_JSON_VALUE)
    public ResponseEntity<Customer> updateCustomer(@PathVariable int id, @RequestBody Customer customer) {
        
        Customer updatedCustomer = this.customerService.updateCustomer(id, customer);
        return ResponseEntity.ok(updatedCustomer);
    }
}
