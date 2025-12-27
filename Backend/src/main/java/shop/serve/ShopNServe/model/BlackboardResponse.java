package shop.serve.ShopNServe.model;

import java.util.Map;

public record BlackboardResponse(
        boolean ok,
        Map<String, Object> data
) {}
