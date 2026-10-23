package com.fptaptech.catalog.product;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.List;

@Tag(name = "Products", description = "Quản lý danh mục sản phẩm")
@RequestMapping("/api/v1/products")
public interface ProductApi {

    @Operation(operationId = "listProducts", summary = "Lấy danh sách sản phẩm")
    @ApiResponse(responseCode = "200", description = "Danh sách sản phẩm (có thể rỗng)")
    @GetMapping
    List<ProductResponse> findAll();

    @Operation(operationId = "getProduct", summary = "Lấy chi tiết sản phẩm")
    @ApiResponse(responseCode = "200", description = "Tìm thấy sản phẩm")
    @ApiResponse(responseCode = "404", description = "Sản phẩm không tồn tại",
            content = @Content(mediaType = "application/problem+json",
                    schema = @Schema(implementation = ProblemDetail.class)))
    @GetMapping("/{id}")
    ProductResponse findById(
            @Parameter(description = "ID sản phẩm", example = "1")
            @PathVariable Long id);

    @Operation(operationId = "createProduct", summary = "Tạo sản phẩm mới")
    @ApiResponse(responseCode = "201", description = "Đã tạo",
            headers = @Header(name = "Location", description = "URI của sản phẩm vừa tạo"))
    @ApiResponse(responseCode = "400", description = "Dữ liệu không hợp lệ",
            content = @Content(mediaType = "application/problem+json",
                    schema = @Schema(implementation = ProblemDetail.class)))
    @PostMapping
    ResponseEntity<ProductResponse> create(@Valid @RequestBody ProductRequest request);

    @Operation(operationId = "updateProduct", summary = "Cập nhật toàn bộ sản phẩm")
    @ApiResponse(responseCode = "200", description = "Đã cập nhật")
    @ApiResponse(responseCode = "400", description = "Dữ liệu không hợp lệ",
            content = @Content(mediaType = "application/problem+json",
                    schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "404", description = "Sản phẩm không tồn tại",
            content = @Content(mediaType = "application/problem+json",
                    schema = @Schema(implementation = ProblemDetail.class)))
    @PutMapping("/{id}")
    ProductResponse update(
            @Parameter(description = "ID sản phẩm", example = "1") @PathVariable Long id,
            @Valid @RequestBody ProductRequest request);

    @Operation(operationId = "deleteProduct", summary = "Xóa sản phẩm")
    @ApiResponse(responseCode = "204", description = "Đã xóa")
    @ApiResponse(responseCode = "404", description = "Sản phẩm không tồn tại",
            content = @Content(mediaType = "application/problem+json",
                    schema = @Schema(implementation = ProblemDetail.class)))
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(@Parameter(description = "ID sản phẩm", example = "1") @PathVariable Long id);
}
