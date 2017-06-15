package net.vatri.ecommerce.controllers;

import net.vatri.ecommerce.hateoas.ProductResource;
import net.vatri.ecommerce.models.Product;
import net.vatri.ecommerce.models.ProductImage;
import net.vatri.ecommerce.services.EcommerceService;
import net.vatri.ecommerce.storage.StorageService;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.hateoas.ResourceSupport;
import org.springframework.hateoas.config.EnableHypermediaSupport;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.Validator;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.Valid;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/product")
@EnableHypermediaSupport(type = EnableHypermediaSupport.HypermediaType.HAL)
public class ProductController extends CoreController{

    @Autowired
    private EcommerceService ecommerceService;

    @Autowired
    private StorageService storageService;

    @Autowired
    private SessionFactory sessionFactory;

    @Autowired Validator productValidator;

    @InitBinder
    protected void initBinder(WebDataBinder binder){
        binder.addValidators(productValidator);
    }

    private ResourceSupport createResource(Product p){
        ProductResource productResource = new ProductResource();
        productResource.id = p.getId();
        productResource.name = p.getName();
        productResource.price = p.getPrice();
        productResource.description = p.getDescription();
        productResource.group = p.getGroup();

        // Add HAL link
        productResource.add(createHateoasLink(p.getId()));

        return productResource;
    }

    @GetMapping
    public List<ProductResource> index() {
        List<Product> res = ecommerceService.getProducts();
        List<ProductResource> output = new ArrayList<ProductResource>();
        res.forEach((p)->{
            ProductResource pr = (ProductResource) createResource(p);
            output.add(pr);
        });
        return output;
    }

    @PostMapping
    public Product create(@RequestBody @Valid Product product){
        return ecommerceService.saveProduct(product);
    }

    @GetMapping("/{id}")
    public ResourceSupport view(@PathVariable("id") long id){
        Product p = ecommerceService.getProduct(id);

        ProductResource productResource = (ProductResource) createResource(p);

        return productResource;
    }

    @PostMapping(value = "/{id}")
    public Product edit(@PathVariable("id") long id, @RequestBody @Valid Product product){

        Product updatedProduct = ecommerceService.getProduct(id);

        if(updatedProduct == null){
            return null;
        }

        updatedProduct.setName(product.getName());
        updatedProduct.setPrice(product.getPrice());
        updatedProduct.setDescription(product.getDescription());

        return ecommerceService.saveProduct(updatedProduct);
    }

    @GetMapping("/{id}/images")
    public List<ProductImage> viewImages(@PathVariable("id") String productId){
        Session session = sessionFactory.openSession();
        List<ProductImage> list = session.createQuery("FROM ProductImage WHERE product_id = :product_id")
                .setLong("product_id", Long.parseLong(productId))
                .list();
        session.close();
        return list;
    }

    @GetMapping("/image/{id}")
    @ResponseBody
    public ResponseEntity<Resource> serveFile(@PathVariable("id") String id) {

        Session session = sessionFactory.openSession();
        ProductImage image = (ProductImage) session.get(ProductImage.class, Long.parseLong(id));

        session.close();

        // Relative path to StorageProperties.rootLocation
        String path = "product-images/" + image.getProductId() + "/";

        Resource file = storageService.loadAsResource(path+image.getPath());
        String mimeType = "image/png";
        try {
            mimeType = file.getURL().openConnection().getContentType();
        } catch (IOException e){
            System.out.println("Can't get file mimeType. " + e.getMessage());
        }
        return ResponseEntity
                .ok()
//                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\""+file.getFilename()+"\"")
                .header(HttpHeaders.CONTENT_TYPE, mimeType)
                .body(file);
    }

    @PostMapping("/{id}/uploadimage")
    public String handleFileUpload(@PathVariable("id") String id, @RequestParam("file") MultipartFile file) {

        // Relative path to the rootLocation in storageService
        String path = "/product-images/" + id;
        String filename = storageService.store(file, path);

        return ecommerceService.addProductImage(id, filename);
    }

    // Todo: add delete method

}