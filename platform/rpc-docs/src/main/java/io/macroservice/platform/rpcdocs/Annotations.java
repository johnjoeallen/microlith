package io.macroservice.platform.rpcdocs;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;

/**
 * Reads attributes of the optional Swagger annotations by name, so that this module and the {@code -api} artifacts
 * need no dependency on swagger-annotations: an API author who wants descriptions adds the annotations jar (provided).
 */
final class Annotations {

    static final String SCHEMA = "io.swagger.v3.oas.annotations.media.Schema";
    static final String OPERATION = "io.swagger.v3.oas.annotations.Operation";
    static final String PARAMETER = "io.swagger.v3.oas.annotations.Parameter";

    private Annotations() {
    }

    /** The non-blank String attribute {@code attribute} of the first annotation named {@code annotation}, or null. */
    static String text(AnnotatedElement element, String annotation, String attribute) {
        if (element == null) {
            return null;
        }
        for (Annotation candidate : element.getAnnotations()) {
            if (candidate.annotationType().getName().equals(annotation)) {
                try {
                    Object value = candidate.annotationType().getMethod(attribute).invoke(candidate);
                    if (value instanceof String text && !text.isBlank()) {
                        return text;
                    }
                } catch (ReflectiveOperationException e) {
                    throw new IllegalStateException("Cannot read %s.%s".formatted(annotation, attribute), e);
                }
            }
        }
        return null;
    }

    /** True when the element carries any annotation whose simple name is {@code Nullable}. */
    static boolean nullable(AnnotatedElement... elements) {
        for (AnnotatedElement element : elements) {
            if (element == null) {
                continue;
            }
            for (Annotation candidate : element.getAnnotations()) {
                if (candidate.annotationType().getSimpleName().equals("Nullable")) {
                    return true;
                }
            }
        }
        return false;
    }
}
