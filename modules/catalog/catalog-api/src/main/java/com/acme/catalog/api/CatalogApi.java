package com.acme.catalog.api;

import io.macroservice.platform.rpc.RpcApi;
import io.macroservice.platform.rpc.Idempotent;
import io.swagger.v3.oas.annotations.Operation;

@RpcApi("catalog")
public interface CatalogApi {

    @Idempotent
    @Operation(summary = "Gets a product by SKU")
    Product getProduct(String sku);

    @Idempotent
    @Operation(summary = "Tells whether a SKU exists in the catalog")
    boolean productExists(String sku);
}
