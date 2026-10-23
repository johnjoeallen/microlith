package com.acme.catalog.api;

import io.macroservice.platform.rpc.RpcApi;
import io.macroservice.platform.rpc.Idempotent;

@RpcApi("catalog")
public interface CatalogApi {

    @Idempotent
    Product getProduct(String sku);

    @Idempotent
    boolean productExists(String sku);
}
