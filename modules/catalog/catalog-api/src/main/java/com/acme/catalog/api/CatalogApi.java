package com.acme.catalog.api;

import io.macroservice.platform.rpc.Cached;
import io.macroservice.platform.rpc.Idempotent;
import io.macroservice.platform.rpc.RpcApi;
import io.swagger.v3.oas.annotations.Operation;

@RpcApi("catalog")
public interface CatalogApi {

    @Idempotent
    @Cached
    @Operation(summary = "Gets a product by SKU")
    Product getProduct(String sku);

    @Idempotent
    @Cached
    @Operation(summary = "Tells whether a SKU exists in the catalog")
    boolean productExists(String sku);
}
