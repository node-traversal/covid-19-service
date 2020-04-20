package io.nodetraversal.covid19.jhucsse.service.csv;

import java.lang.annotation.*;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface AccessorAlias {
    String alias() default "";
}
