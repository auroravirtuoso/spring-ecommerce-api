package net.vatri.ecommerce.controllers;

import net.vatri.ecommerce.models.Order;
import net.vatri.ecommerce.services.EcommerceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.Validator;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/order")
public class OrderController {

    @Autowired
    private EcommerceService ecommerceService;

    @Autowired
    Validator orderValidator;

    @InitBinder
    protected void initBinder(WebDataBinder binder){
        binder.addValidators(orderValidator);
    }

    @RequestMapping(method = RequestMethod.GET)
    public List<Order> index() {
        return ecommerceService.getOrders();
    }

    @PostMapping
    public Order create(@RequestBody @Valid Order order){

        // Required by Hibernate ORM to save properly
        if(order.getItems() !=null){
            order.getItems().forEach(item -> item.setOrder(order));
        }
        return ecommerceService.saveOrder(order);
    }

    @RequestMapping("/{id}")
    public Order view(@PathVariable("id") long id){
        return ecommerceService.getOrder(id);
    }

    @RequestMapping(value = "/{id}", method = RequestMethod.POST)
    public Order edit(@PathVariable("id") long id, @RequestBody @Valid Order order){

        Order updatedOrder = ecommerceService.getOrder(id);

        if(updatedOrder== null){
            return null;
        }


        return ecommerceService.saveOrder(updatedOrder);
    }
}
