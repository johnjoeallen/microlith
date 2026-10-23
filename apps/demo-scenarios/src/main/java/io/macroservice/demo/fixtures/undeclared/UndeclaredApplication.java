package io.macroservice.demo.fixtures.undeclared;

import org.springframework.modulith.Modulithic;

/** Root of a deliberately broken fixture: module x may depend on nothing but uses y. */
@Modulithic
public class UndeclaredApplication {
}
