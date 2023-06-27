package com.mobichord.ftps.data;

import javax.validation.Constraint;
import javax.validation.Payload;
import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = DecryptValidator.class)
@Target({ ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface DecryptValidationAnnotation {
    String message() default "cryptoCfgId is Required if encrypt is true";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
