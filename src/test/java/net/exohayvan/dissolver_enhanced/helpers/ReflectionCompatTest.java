package net.exohayvan.dissolver_enhanced.helpers;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ReflectionCompatTest {
    @Test
    void invokesInheritedPrivateNoArgMethodWithExpectedType() {
        String value = ReflectionCompat.invokeNoArg(new MethodChild(), String.class, "hiddenValue");

        assertThat(value).isEqualTo("method");
    }

    @Test
    void readsInheritedPrivateFieldWithExpectedType() {
        String value = ReflectionCompat.readField(new FieldChild(), String.class);

        assertThat(value).isEqualTo("field");
    }

    private static class MethodParent {
        @SuppressWarnings("unused")
        private String hiddenValue() {
            return "method";
        }
    }

    private static final class MethodChild extends MethodParent {
    }

    private static class FieldParent {
        @SuppressWarnings("unused")
        private final String value = "field";
    }

    private static final class FieldChild extends FieldParent {
    }
}
