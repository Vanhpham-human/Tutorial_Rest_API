package com.fptaptech.catalog.product;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
public class ProductController implements ProductApi {

    private final ProductService service;

    public ProductController(ProductService service) {
        this.service = service;
    }

    @Override
    public List<ProductResponse> findAll() {
        return service.findAll();
    }

    @Override
    public ProductResponse findById(Long id) {
        return service.findById(id);
    }

    @Override
    public ResponseEntity<ProductResponse> create(ProductRequest request) {
        ProductResponse created = service.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(created.id()).toUri();
        return ResponseEntity.created(location).body(created);
    }

    @Override
    public ProductResponse update(Long id, ProductRequest request) {
        return service.update(id, request);
    }

    @Override
    public void delete(Long id) {
        service.delete(id);
    }
}
