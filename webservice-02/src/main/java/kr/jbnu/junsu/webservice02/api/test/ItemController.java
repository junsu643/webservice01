package kr.jbnu.junsu.webservice02.api.test;

import jakarta.validation.Valid;
import kr.jbnu.junsu.webservice02.api.common.ApiException;
import kr.jbnu.junsu.webservice02.api.common.ApiResponse;
import kr.jbnu.junsu.webservice02.api.dto.ItemDto;
import kr.jbnu.junsu.webservice02.api.request.ItemCreateRequest;
import kr.jbnu.junsu.webservice02.api.request.ItemUpdateRequest;
import kr.jbnu.junsu.webservice02.api.request.PriceUpdateRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@RestController
@RequestMapping("/api/test/items")
public class ItemController {

    private static final int MAX_ITEMS = 100;
    private static final int MAX_PAGE_SIZE = 100;

    private final Map<Long, ItemDto> store = new ConcurrentHashMap<>();
    private final AtomicLong sequence = new AtomicLong(1L);




    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> getItems(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestHeader(value = "X-Simulate-Error", required = false) String simulate
    ) {
        simulateErrorIfRequested(simulate);

        if (page < 0 || size < 1 || size > MAX_PAGE_SIZE) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "page는 0 이상, size는 1~" + MAX_PAGE_SIZE + " 사이여야 합니다.");
        }

        List<ItemDto> filtered = store.values().stream()
                .filter(item -> keyword == null
                        || (item.getName() != null && item.getName().contains(keyword)))
                .sorted(Comparator.comparing(ItemDto::getId))
                .toList();

        List<ItemDto> items = filtered.stream()
                .skip((long) page * size)
                .limit(size)
                .toList();

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("items", items);
        data.put("page", page);
        data.put("size", size);
        data.put("totalCount", filtered.size());

        return ResponseEntity.ok(ApiResponse.success(data));
    }


    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ItemDto>> getItem(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(findOrThrow(id)));
    }




    @PostMapping
    public ResponseEntity<ApiResponse<ItemDto>> createItem(@Valid @RequestBody ItemCreateRequest request) {
        if (store.size() >= MAX_ITEMS) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE,
                    "저장 공간이 가득 찼습니다. 잠시 후 다시 시도해주세요.");
        }
        checkDuplicateName(request.getName(), null);

        ItemDto item = new ItemDto();
        item.setId(sequence.getAndIncrement());
        item.setName(request.getName());
        item.setPrice(request.getPrice());
        store.put(item.getId(), item);

        return ResponseEntity
                .created(URI.create("/api/test/items/" + item.getId()))
                .body(ApiResponse.success(item));
    }


    @PostMapping("/with-header")
    public ResponseEntity<ApiResponse<Map<String, Object>>> createItemWithHeader(
            @Valid @RequestBody ItemCreateRequest request,
            @RequestHeader("X-USER-ID") String userId,
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("name", request.getName());
        data.put("price", request.getPrice());
        data.put("userId", userId);
        data.put("authorizationProvided", authorization != null);

        return ResponseEntity.ok(ApiResponse.success(data));
    }




    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ItemDto>> updateItem(
            @PathVariable Long id,
            @Valid @RequestBody ItemUpdateRequest request
    ) {
        ItemDto item = findOrThrow(id);

        if (request.getName() != null) {
            checkDuplicateName(request.getName(), id);
            item.setName(request.getName());
        }
        if (request.getPrice() != null) {
            item.setPrice(request.getPrice());
        }
        return ResponseEntity.ok(ApiResponse.success(item));
    }


    @PutMapping("/{id}/price")
    public ResponseEntity<ApiResponse<ItemDto>> updatePrice(
            @PathVariable Long id,
            @Valid @RequestBody PriceUpdateRequest request
    ) {
        ItemDto item = findOrThrow(id);
        item.setPrice(request.price());
        return ResponseEntity.ok(ApiResponse.success(item));
    }




    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> deleteItem(@PathVariable Long id) {
        if (store.remove(id) == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, "id=" + id + " 아이템을 찾을 수 없습니다.");
        }
        return ResponseEntity.ok(ApiResponse.success(Map.of("deletedId", id)));
    }


    @DeleteMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> deleteAllItems() {
        int count = store.size();
        store.clear();
        return ResponseEntity.ok(ApiResponse.success(Map.of("deletedCount", count)));
    }



    private ItemDto findOrThrow(Long id) {
        ItemDto item = store.get(id);
        if (item == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, "id=" + id + " 아이템을 찾을 수 없습니다.");
        }
        return item;
    }


    private void checkDuplicateName(String name, Long excludeId) {
        boolean duplicated = store.values().stream()
                .anyMatch(i -> name.equals(i.getName()) && !i.getId().equals(excludeId));
        if (duplicated) {
            throw new ApiException(HttpStatus.CONFLICT, "이미 존재하는 이름입니다: " + name);
        }
    }


    private void simulateErrorIfRequested(String simulate) {
        if ("500".equals(simulate)) {
            throw new IllegalStateException("의도적으로 발생시킨 서버 오류");
        }
        if ("503".equals(simulate)) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "서비스가 일시적으로 사용 불가합니다.");
        }
    }
}