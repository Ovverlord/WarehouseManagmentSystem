package com.bsuir.WarehouseManagementSystem.controller;


import com.bsuir.WarehouseManagementSystem.model.Box;
import com.bsuir.WarehouseManagementSystem.model.Order;
import com.bsuir.WarehouseManagementSystem.model.User;
import com.bsuir.WarehouseManagementSystem.repository.OrderRepository;
import com.bsuir.WarehouseManagementSystem.service.OrderService;
import com.bsuir.WarehouseManagementSystem.service.ProductService;
import org.aspectj.weaver.ast.Or;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

@Controller

public class OrderController {
    @Autowired
    private ProductService productService;

    @Autowired
    private OrderService orderService;

    @PreAuthorize("hasAuthority('USER')")
    @GetMapping("/order/create")
    public String createOrder(Model model){

        model.addAttribute("products",productService.findAll());

        return "createOrder";
    }

    @PreAuthorize("hasAuthority('USER')")
    @PostMapping("/order/create")
    public String saveNewOrder(@AuthenticationPrincipal User user,
                            @RequestParam String selectProductId,
                            @ModelAttribute Order order){

        orderService.saveNewOrder(order, user, Long.valueOf(selectProductId));

        return "createOrder";
    }

    @PreAuthorize("hasAuthority('USER')")
    @GetMapping("/getOrders")
    public String getOrders(@AuthenticationPrincipal User user,
                            Model model){

        List<Order> ordersList = orderService.getUserOrders(user.getId());
        model.addAttribute("orders",ordersList);

        return "userOrdersList";
    }


    @PreAuthorize("hasAuthority('USER')")
    @GetMapping("/order/{id}/remove")
    public String removeOrder(@AuthenticationPrincipal User user,
                              @PathVariable(value = "id") Long orderId){

        Order order = orderService.getOrderById(orderId);

        if(!order.getStatus().equals("Подтвержден")){
            orderService.removeOrder(orderId);
        }

        return "redirect:/orders";
    }


    @PreAuthorize("hasAuthority('CHECKMAN')")
    @GetMapping("/orders/getAll")
    public String getAllOrder(Model model){

        List<Order> ordersList = orderService.getAll();
        model.addAttribute("orders",ordersList);

        return "orderList";
    }

    @PreAuthorize("hasAuthority('CHECKMAN')")
    @GetMapping("/orders/getAllUnchecked")
    public String getAllUncheckedOrder(Model model){

        List<Order> ordersList = orderService.getAllUnchecked();
        model.addAttribute("orders",ordersList);

        return "uncheckedOrders";
    }

    @PreAuthorize("hasAuthority('CHECKMAN')")
    @PostMapping("/order/decline")
    public String declineOrder(@RequestParam(value="orderId") Long orderId){
        Order order = orderService.getOrderById(orderId);
        order.setStatus("Отклонен");
        orderService.save(order);

        return "redirect:/orders/getAllUnchecked";
    }

    @PreAuthorize("hasAuthority('CHECKMAN')")
    @PostMapping("/order/confirm")
    public String confirmOrder(@RequestParam(value="orderId") Long orderId){

        Order order = orderService.getOrderById(orderId);
        if(order.getQuantity() <= productService.getProductsQuantity(order.getProduct().getId())){
            order.setStatus("Подтвержден");
            orderService.save(order);
            productService.productsSelect(order.getProduct().getId(),order.getQuantity());
        }

        return "redirect:/orders/getAllUnchecked";
    }

    @PostMapping("/findOrder")
    public String findOrder(Model model,@RequestParam String filter){

        if(filter.isEmpty()){
            model.addAttribute("orders",orderService.getAll());
        }
        else{
            try{
                Order order = orderService.findById(Long.valueOf(filter));
                model.addAttribute("orders",order);
            }
            catch (NoSuchElementException ex){
                List<Order> emptyList = new ArrayList<>();
                model.addAttribute("orders",emptyList);
            }
        }

        return "orderList";
    }

//    @PreAuthorize("hasAuthority('USER')")
//    @GetMapping("/order/{id}/edit")
//    public String editOrders(@AuthenticationPrincipal User user,
//                             @PathVariable(value = "id") Long orderId,
//                            Model model){
//
//        Order order = orderService.getOrderById(orderId);
//        model.addAttribute("order",order);
//        return "ordersList";
//    }
}
