package org.jedkos.ceptic.endpoint;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

public class EndpointPattern implements Serializable {

    private final Pattern pattern;
    private final List<String> variables;

    public EndpointPattern(Pattern pattern, List<String> variables) {
        this.pattern = pattern;
        if (variables != null) {
            this.variables = variables;
        } else {
            this.variables = new ArrayList<>();
        }
    }

    public Pattern getPattern() {
        return pattern;
    }

    public List<String> getVariables() {
        return variables;
    }

    @Override
    public boolean equals(Object o) {
        // true if refers to this object
        if (this == o) {
            return true;
        }
        // false is object is null or not of same class
        if (o == null || getClass() != o.getClass())
            return false;
        EndpointPattern fromO = (EndpointPattern)o;
        // true if all components match, false otherwise
        return pattern.pattern().equals(fromO.pattern.pattern());
    }

    @Override
    public int hashCode() {
        return Objects.hash(pattern.pattern());
    }

}
