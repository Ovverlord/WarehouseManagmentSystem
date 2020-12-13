package com.bsuir.WarehouseManagementSystem.service;


import com.bsuir.WarehouseManagementSystem.model.Box;
import com.bsuir.WarehouseManagementSystem.model.Position;
import com.bsuir.WarehouseManagementSystem.model.Product;
import com.bsuir.WarehouseManagementSystem.repository.BoxGetters;
import com.bsuir.WarehouseManagementSystem.repository.BoxRepository;
import com.bsuir.WarehouseManagementSystem.repository.PositionRepository;
import com.bsuir.WarehouseManagementSystem.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.PostMapping;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ProductService {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private BoxRepository boxRepository;

    @Autowired
    private PositionRepository positionRepository;

    @Autowired
    private PositionService positionService;

    public List<Product> findAll(){return productRepository.findAll();}

    public Product getProductById(Long productId){
        return productRepository.findById(productId).orElseThrow();
    }

//    public List<Product> getAllProducts(){
//        return productRepository.getAllProducts();
//    }

    public Integer getProductsQuantity(Long productId){
        return productRepository.getProductsQuantity(productId);
    }

    public void save(Product product){
        productRepository.save(product);
    }

    public void editProduct(Product product){
        Product updatedProduct = productRepository.findById(product.getId()).orElseThrow();
        updatedProduct.setDescription(product.getDescription());
        productRepository.save(updatedProduct);
    }


    public void removeProduct(Long productId){

        List<BoxGetters> list = boxRepository.getBoxesAndPositionId(productId);

        Map<Long,Integer> map = new HashMap<>();
        for(BoxGetters obj : list){
            map.put(obj.getPositionId(),obj.getBoxesAmount());
        }

        for (Map.Entry<Long, Integer> entry : map.entrySet()) {

            positionService.reducePositionFullness(entry.getKey(),entry.getValue());
            //reducePositionFullness(entry.getKey(),entry.getValue());
        }

        productRepository.deleteById(productId);
    }




    public void acceptProduct(Long productId,Integer receivedProductsQuantity){

        List<BoxGetters> uncompletedBoxesIdList = boxRepository.getUncompletedBoxesByProductId(productId);

        if(uncompletedBoxesIdList.size() > 0){
            Integer uncompletedPlacesInBoxesAmount = boxRepository.getUncompletedPlacesInBoxesAmount(productId);

            if(receivedProductsQuantity > uncompletedPlacesInBoxesAmount){
                Integer productsForNewBoxesQuantity = receivedProductsQuantity - uncompletedPlacesInBoxesAmount;
                receivedProductsQuantity = receivedProductsQuantity - productsForNewBoxesQuantity;

                productsPlacement(productsForNewBoxesQuantity,productId);
            }

            productsPlacement(uncompletedBoxesIdList,receivedProductsQuantity);
        }

        else{
            productsPlacement(receivedProductsQuantity,productId);
        }
    }

    public void productsPlacement(List<BoxGetters> uncompletedBoxesIdList,
                                  Integer receivedProductsQuantity){

        for(BoxGetters obj : uncompletedBoxesIdList){

            Box box = boxRepository.getBoxById(obj.getBoxId());

            if(receivedProductsQuantity <= box.getCapacity() - box.getFullness()){
                box.setFullness(box.getFullness() + receivedProductsQuantity);
            }
            else{
                receivedProductsQuantity = receivedProductsQuantity - (box.getCapacity() - box.getFullness());
                box.setFullness(box.getCapacity());
            }

            boxRepository.save(box);
        }
    }

    public void productsPlacement(Integer productsForNewBoxesQuantity,Long productId){
        Double requiredBoxesQuantity = Math.ceil((double)productsForNewBoxesQuantity/boxRepository.getBoxCapacity());

        for(int i=0; i < requiredBoxesQuantity; i++){
            createAndPlaceBox(productId,productsForNewBoxesQuantity);
            productsForNewBoxesQuantity = productsForNewBoxesQuantity - boxRepository.getBoxCapacity();
        }

    }

    public void createAndPlaceBox(Long productId,Integer productsForNewBoxesQuantity){
        List<Long> positionIdList = positionRepository.getUncompletedPositionId();

        if(positionIdList.size() > 0){
            Long positionId = positionIdList.get(0);

            Position position = positionRepository.findById(positionId).orElseThrow();
            Product product = productRepository.findById(productId).orElseThrow();

            if(productsForNewBoxesQuantity <= boxRepository.getBoxCapacity()){
                Box box = new Box(productsForNewBoxesQuantity,
                        boxRepository.getBoxCapacity(),
                        position,product);

                boxRepository.save(box);
            }

            else{
                Box box = new Box(boxRepository.getBoxCapacity(),
                        boxRepository.getBoxCapacity(),
                        position,product);

                boxRepository.save(box);
            }

            position.setFullness(position.getFullness()+1);
            positionRepository.save(position);
        }

//        else{
//            System.out.println("net mesta");
//        }
    }


    public void productsSelect(Long productId,Integer quantity){
        List<Box> boxesList = boxRepository.getBoxIdByProduct(productId);

        for(Box box : boxesList){
            if(quantity>0){
                if(box.getFullness() <= quantity){
                    quantity = quantity - box.getFullness();
                    Long positionId = box.getPosition().getId();
                    positionService.reducePositionFullness(positionId,1);
                    boxRepository.deleteById(box.getId());
                }
                else{
                    box.setFullness(box.getFullness()-quantity);
                    boxRepository.save(box);
                    quantity = 0;
                }
            }

        }
    }
}
