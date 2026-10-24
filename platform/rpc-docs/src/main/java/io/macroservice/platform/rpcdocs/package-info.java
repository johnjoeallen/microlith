/**
 * Generates OpenAPI 3.1 and OpenRPC documents from {@code @RpcApi} interfaces by reflection. swagger-core is not used
 * because no release supports Jackson 3; the record schemas are built here and follow Jackson's record conventions.
 */
package io.macroservice.platform.rpcdocs;
