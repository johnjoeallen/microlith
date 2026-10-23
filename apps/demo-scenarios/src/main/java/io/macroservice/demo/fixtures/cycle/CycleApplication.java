package io.macroservice.demo.fixtures.cycle;

import org.springframework.modulith.Modulithic;

/** Root of a deliberately broken fixture: module a depends on b and b on a. */
@Modulithic
public class CycleApplication {
}
