package org.example.catalog;

import com.jayway.jsonpath.JsonPath;
import org.example.catalog.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
class ProductControllerTests {

    private static final String URL = "/api/v1/products";

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private ProductRepository productRepository;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
        productRepository.deleteAll();
    }

    @Test
    void createAndGetProduct() throws Exception {

        String id = createProduct("Air Max 90", "shoes", "129.99");

        mockMvc.perform(get(URL + "/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Air Max 90"))
                .andExpect(jsonPath("$.category").value("shoes"))
                .andExpect(jsonPath("$.price").value(129.99));
    }

    @Test
    void createReturnsLocationHeader() throws Exception {

        mockMvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("Pegasus 41", "shoes", "139.99")))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"));
    }

    @Test
    void createWithInvalidBodyReturnsBadRequest() throws Exception {

        String body = """
                {"name": "", "category": "shoes", "price": -5}
                """;

        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateProduct() throws Exception {

        String id = createProduct("Tech Fleece", "hoodies", "109.99");

        // read once so the old value is in the cache, update must evict it
        mockMvc.perform(get(URL + "/" + id)).andExpect(status().isOk());

        mockMvc.perform(put(URL + "/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("Tech Fleece", "hoodies", "89.99")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.price").value(89.99));

        mockMvc.perform(get(URL + "/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.price").value(89.99));
    }

    @Test
    void deleteProduct() throws Exception {

        String id = createProduct("Club Cap", "accessories", "24.99");

        mockMvc.perform(delete(URL + "/" + id)).andExpect(status().isNoContent());
        mockMvc.perform(get(URL + "/" + id)).andExpect(status().isNotFound());
        mockMvc.perform(delete(URL + "/" + id)).andExpect(status().isNotFound());
    }

    @Test
    void getUnknownProductReturnsNotFound() throws Exception {

        mockMvc.perform(get(URL + "/" + UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }

    @Test
    void filterByCategoryAndPriceRange() throws Exception {

        createProduct("Shoe A", "shoes", "50.00");
        createProduct("Shoe B", "shoes", "100.00");
        createProduct("Shoe C", "shoes", "200.00");
        createProduct("Hoodie", "hoodies", "100.00");

        mockMvc.perform(get(URL)
                        .param("category", "shoes")
                        .param("minPrice", "60")
                        .param("maxPrice", "250")
                        .param("sortBy", "price"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content[0].name").value("Shoe B"))
                .andExpect(jsonPath("$.content[1].name").value("Shoe C"));
    }

    @Test
    void pagination() throws Exception {

        for (int i = 1; i <= 5; i++) {
            createProduct("Product " + i, "shoes", i + "0.00");
        }

        mockMvc.perform(get(URL)
                        .param("page", "1")
                        .param("size", "2")
                        .param("sortBy", "price"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.content[0].name").value("Product 3"))
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.totalElements").value(5))
                .andExpect(jsonPath("$.totalPages").value(3));
    }

    @Test
    void invalidListParametersReturnBadRequest() throws Exception {

        mockMvc.perform(get(URL).param("minPrice", "100").param("maxPrice", "10"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get(URL).param("size", "1000"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get(URL).param("sortBy", "description"))
                .andExpect(status().isBadRequest());
    }

    private String createProduct(String name, String category, String price) throws Exception {

        String response = mockMvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(name, category, price)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return JsonPath.read(response, "$.id");
    }

    private String json(String name, String category, String price) {
        return """
                {"name": "%s", "description": "test product", "category": "%s", "price": %s}
                """.formatted(name, category, price);
    }
}
