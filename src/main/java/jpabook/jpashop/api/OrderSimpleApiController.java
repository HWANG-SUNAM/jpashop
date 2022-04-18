package jpabook.jpashop.api;

import jpabook.jpashop.domain.Address;
import jpabook.jpashop.domain.Order;
import jpabook.jpashop.domain.OrderStatus;
import jpabook.jpashop.repository.OrderRepository;
import jpabook.jpashop.repository.OrderSearch;
import jpabook.jpashop.repository.order.simplequery.OrderSimpleQueryDto;
import jpabook.jpashop.repository.order.simplequery.OrderSimpleQueryRepository;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * xToOne(ManyToOne, OneToOne)
 * Order
 * Order -> Member
 * Order -> Delivery
 */
@RestController
@RequiredArgsConstructor
public class OrderSimpleApiController {

    private final OrderRepository orderRepository;
    private final OrderSimpleQueryRepository orderSimpleQueryRepository;

    /**
     * 1. Json 컨버트할때 무한 루프에 빠지지 않도록 양방향 관계중 한 쪽에 @JsonIgnore 선언
     * 2. lazy로 선언된 필드는 proxy 객체(new ByteByddyInterceptor();)가 들어가기 때문에 오류 남. Hibernate5Module을 bean으로 선언해야 해결됨
     *
     * 하지만 엔티티를 직접 노출하는 것은 바람직하지 않고 DTO로 응답해야하므로 위의 것들이 의미가 없다.
     */
    @GetMapping("/api/v1/simple-orders")
    public List<Order> ordersV1() {
        List<Order> all = orderRepository.findAllByString(new OrderSearch()); // JPQL은 Order만 가져옴
        return all;
    }

    /**
     * N + 1 문제 존재
     * N + 1 -> 1 + 회원 N + 배송 N
     *
     * 만약 1번째 Order도, 2번째 Order 같은 유저가 했으면 2번째 Order 루프돌 때, .getName() 관련 쿼리가 실행되지 않음. 영속성 컨텍스트에 이미 유저(같은 id)가 존재하기 때문
     */
    @GetMapping("/api/v2/simple-orders")
    public List<SimpleOrderDto> ordersV2() {
        // ORDER 2개
        List<Order> orders = orderRepository.findAllByString(new OrderSearch());

        List<SimpleOrderDto> result = orders.stream()
                .map(o -> new SimpleOrderDto(o))
                .collect(Collectors.toList());

        return result;
    }

    /**
     * 페치 조인 적용하여 N + 1 문제 해결
     */
    @GetMapping("/api/v3/simple-orders")
    public List<SimpleOrderDto> ordersV3() {
        List<Order> orders = orderRepository.findAllWithMemberDelivery();

        List<SimpleOrderDto> result = orders.stream()
                .map(o -> new SimpleOrderDto(o))
                .collect(Collectors.toList());

        return result;
    }

    /**
     * new 명령어를 사용해서 JPQL의 결과를 DTO로 즉시 변환
     * 일반 join은 주체가 되는 엔티티 데이터밖에 못가져오지만 DTO를 직접 조회하는 방식은 가져올 수 있는 것으로 보인다.
     * repository 재사용성이 떨어짐
     */
    @GetMapping("/api/v4/simple-orders")
    public List<OrderSimpleQueryDto> ordersV4() {
        return orderSimpleQueryRepository.findOrderDtos();
    }

    @Data
    static class SimpleOrderDto {
        private Long orderId;
        private String name;
        private LocalDateTime orderDate;
        private OrderStatus orderStatus;
        private Address address;

        public SimpleOrderDto(Order order) {
            orderId = order.getId();
            name = order.getMember().getName();
            orderDate = order.getOrderDate();
            orderStatus = order.getStatus();
            address = order.getDelivery().getAddress();
        }
    }
}
