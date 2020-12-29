package edu.pku.PKUToolMan.service;

import edu.pku.PKUToolMan.dao.OrderDAO;
import edu.pku.PKUToolMan.entity.Order;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class OrderServiceImpl implements OrderService {
    @Autowired
    private OrderDAO orderDAO;

    @Override
    public void createOrder(Order order) {
        orderDAO.createOrder(order);
    }

    @Override
    @Transactional(propagation = Propagation.SUPPORTS)
    public List<Order> getMyOrderList(int userId) {
        return orderDAO.getMyOrderList(userId);
    }

    @Override
    public void updateOrder(Order order) {
        orderDAO.updateOrder(order);
    }

    @Override
    public void deleteOrder(int orderId) {
        orderDAO.deleteOrder(orderId);
    }

    @Override
    @Transactional(propagation = Propagation.SUPPORTS)
    public Order queryOrder(int orderId) {
        return orderDAO.queryOrder(orderId);
    }

    @Override
    @Transactional(propagation = Propagation.SUPPORTS)
    public List<Order> getAllCreatedOrderList() {
        return orderDAO.getAllCreatedOrderList();
    }

}
