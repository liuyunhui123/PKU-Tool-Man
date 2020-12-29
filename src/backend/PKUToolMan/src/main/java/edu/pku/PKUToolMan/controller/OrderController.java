package edu.pku.PKUToolMan.controller;

import edu.pku.PKUToolMan.entity.Order;
import edu.pku.PKUToolMan.entity.OrderState;
import edu.pku.PKUToolMan.service.OrderServiceImpl;
import edu.pku.PKUToolMan.utils.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/order")
public class OrderController {

    @Autowired
    private OrderServiceImpl orderService;

    private final static DateTimeFormatter df = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private final static Set<Integer> curReceiveOrderSet = new HashSet<>();

    @PostMapping("/add")
    public Result createOrder(@RequestBody Map<String, Object> map) {
        int userId = (Integer)map.get("userID");
        String place = map.get("place").toString();
        String destination = map.get("destination").toString();
        String description = map.get("description").toString();
        // get startTime
        DateTimeFormatter df = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        LocalDateTime startTime = LocalDateTime.parse(map.get("startTime").toString(), df);
        LocalDateTime endTime = LocalDateTime.parse(map.get("endTime").toString(), df);
        String type = map.get("type").toString();

        Order order = new Order(userId, place, destination, startTime, endTime, description, type);
        order.setState(OrderState.CREATED.ordinal());

        try {
            orderService.createOrder(order);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.RESPONSE_ERROR().message("create order failed, unable to insert");
        }
        // TODO: 这里其实可以考虑返回order.orderId: order.getOrderId()
        return Result.SUCCESS();
    }

    @PostMapping("/myorderlist")
    public Result getMyOrderList(@RequestBody Map<String, Object> map) {
        int userId = (Integer)map.get("userID");
        List<Order> orderList;
        try {
            orderList = orderService.getMyOrderList(userId);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.RESPONSE_ERROR().message("get myOrderList failed");
        }
        boolean flag = false;
        for(Order order : orderList) {
            if (LocalDateTime.now().isAfter( order.getEndTime() ) &&
                    order.getState() == OrderState.CREATED.ordinal()) {
                order.setState(OrderState.CANCELLED.ordinal());
                try{
                    orderService.updateOrder(order);
                } catch (Exception e) {
                    e.printStackTrace();
                    flag = true;
                }
            }
        }
        if (flag) return Result.RESPONSE_ERROR().message("update order error when getting my order list");
        return Result.SUCCESS().data("orders", orderList);
    }

    // 加入了是否有重复接单、订单已结束、已取消之类的检查
    @PostMapping("/receive")
    public Result receiveOrder(@RequestBody Map<String, Object> map) {
        int orderId = (Integer)map.get("orderID");
        int toolManId = (Integer)map.get("toolManID");
        Order order;
        try {
            order = orderService.queryOrder(orderId);
            if (order == null) {
                return Result.RESPONSE_ERROR().message("No suitable result found.");
            }
        } catch (Exception e) {
            e.printStackTrace();
            return Result.RESPONSE_ERROR().message("query order failed when receiving order");
        }

        while(curReceiveOrderSet.contains(orderId)) {
            // waiting
        }
        curReceiveOrderSet.add(orderId);
        if (LocalDateTime.now().isAfter( order.getEndTime() ) &&
                order.getState() == OrderState.CREATED.ordinal()) {
            curReceiveOrderSet.remove(new Integer(orderId));
            order.setState(OrderState.CANCELLED.ordinal());
            try{
                orderService.updateOrder(order);
            } catch (Exception e) {
                e.printStackTrace();
                return Result.RESPONSE_ERROR().message("update order error when receiving order");
            }
            return Result.AUTH_ERROR().message("overtime order");
        }
        if (order.getToolManId() != -1 || order.getState() != OrderState.CREATED.ordinal()) {
            curReceiveOrderSet.remove(new Integer(orderId));
            return Result.AUTH_ERROR().message("duplicated receiving or finished/cancelled/deleted order");
        }

        order.setToolManId(toolManId);
        order.setState(OrderState.EXECUTING.ordinal());
        try {
            orderService.updateOrder(order);
        } catch (Exception e) {
            curReceiveOrderSet.remove(new Integer(orderId));
            e.printStackTrace();
            return Result.RESPONSE_ERROR().message("update order failed when receiving order");
        }
        curReceiveOrderSet.remove(new Integer(orderId));
        return Result.SUCCESS();
    }

    @PostMapping("/complete")
    public Result completeOrder(@RequestBody Map<String, Object> map) {
        int orderId = (Integer)map.get("orderID");
        int userId = (Integer)map.get("userID");
        Order order = null;
        try {
            order = orderService.queryOrder(orderId);
            if (order == null) {
                return Result.RESPONSE_ERROR().message("No suitable result found.");
            }
        } catch (Exception e) {
            e.printStackTrace();
            return Result.RESPONSE_ERROR().message("query order failed when completing order");
        }
        if (userId != order.getUserId()) {
            return Result.AUTH_ERROR().message("Permission denied when completing order");
        }
        if (order.getState() != OrderState.EXECUTING.ordinal()) {
            return Result.AUTH_ERROR().message("wrong order state");
        }
        order.setState(OrderState.FINISHED.ordinal());
        try {
            orderService.updateOrder(order);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.RESPONSE_ERROR().message("update order failed when completing order");
        }
        return Result.SUCCESS();
    }

    // 与cancel区分, 可以是用户删除已完成的订单，或管理员删除? 这里的"权限检查"需要更明确一些
    @PostMapping("/delete")
    public Result deleteOrder(@RequestBody Map<String, Object> map) {
        // TODO 权限检查
        Order order;
        int orderId = (Integer)map.get("orderID");
        int userId = (Integer)map.get("userID");
        try{
            order = orderService.queryOrder(orderId);
            if (order == null) {
                return Result.RESPONSE_ERROR().message("No suitable order found.");
            }
        } catch (Exception e) {
            e.printStackTrace();
            return Result.RESPONSE_ERROR().message("query order failed when completing order");
        }
        if (userId != order.getUserId()) {
            return Result.AUTH_ERROR().message("Permission denied when deleting order");
        }

        try{
            orderService.deleteOrder(orderId);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.RESPONSE_ERROR().message("delete order failed when deleting order");
        }
        return Result.SUCCESS();
    }

    // 暂定为用户取消已发布的订单，可以设置为距离发布三分钟以内，并且尚未有人接单(CREATED状态)
    @PostMapping("/cancel")
    public Result cancelOrder(@RequestBody Map<String, Object> map) {
        Order order;
        int orderId = (Integer)map.get("orderID");
        try {
            order = orderService.queryOrder(orderId);
            if (order == null) {
                return Result.RESPONSE_ERROR().message("No suitable result found.");
            }
        } catch (Exception e) {
            e.printStackTrace();
            return Result.RESPONSE_ERROR().message("query order failed when cancelling order");
        }
        if (order.getState() == OrderState.CREATED.ordinal()) {  // 并且发布时间不到三分钟
            order.setState(OrderState.CANCELLED.ordinal());
            try {
                orderService.updateOrder(order);
            } catch (Exception e) {
                return Result.RESPONSE_ERROR().message("update order failed when cancelling order");
            }
            return Result.SUCCESS();
        }
        return Result.AUTH_ERROR().message("order having been received or 3 min time limit exceeded");
    }

    @PostMapping("/query")
    public Result queryOrder(@RequestBody Map<String, Object> map) {
        Order order;
        int orderId = (Integer)map.get("orderID");
        try {
            order = orderService.queryOrder(orderId);
            if (order == null) {
                return Result.RESPONSE_ERROR().message("No suitable result found.");
            }
        } catch (Exception e) {
            e.printStackTrace();
            return Result.RESPONSE_ERROR().message("query order failed when querying order");
        }

        if (LocalDateTime.now().isAfter( order.getEndTime() ) &&
                order.getState() == OrderState.CREATED.ordinal()) {
            order.setState(OrderState.CANCELLED.ordinal());
            try{
                orderService.updateOrder(order);
            } catch (Exception e) {
                e.printStackTrace();
                return Result.RESPONSE_ERROR().message("update order error when querying order");
            }
        }
        return Result.SUCCESS().data("order", order);
    }

    @PostMapping("/orderlist")
    public Result getAllOrderList() {
        List<Order> orders;
        try {
            orders = orderService.getAllCreatedOrderList();
        } catch (Exception e) {
            return Result.RESPONSE_ERROR().message("query order failed when getting all orderlist");
        }
        boolean flag = false;
        for(Order order : orders) {
            if (LocalDateTime.now().isAfter( order.getEndTime() ) &&
                    order.getState() == OrderState.CREATED.ordinal()) {
                order.setState(OrderState.CANCELLED.ordinal());
                try{
                    orderService.updateOrder(order);
                } catch (Exception e) {
                    e.printStackTrace();
                    flag = true;
                }
                orders.remove(order);
            }
        }
        if (flag) return Result.RESPONSE_ERROR().message("update order error when getting orderlist");
        return Result.SUCCESS().data("orders", orders);
    }

}
