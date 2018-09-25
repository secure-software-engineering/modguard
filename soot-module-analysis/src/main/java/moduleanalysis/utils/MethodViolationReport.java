package moduleanalysis.utils;

import java.util.Objects;

/**
 * Created by adann on 12.11.17.
 */
public class MethodViolationReport extends AnalysisReport {


    private String type;
    private String method;
    private String id;

    public MethodViolationReport(String message) {
        super(message, null, null);
        String[] split = message.split(", ");
        if (split.length != 3) {
            throw new RuntimeException("String does not match");

        }
        type = split[0].trim();
        method = split[1].trim();
        id = split[2].trim();

    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj instanceof MethodViolationReport) {
            boolean sameType = this.type.equals(((MethodViolationReport) obj).type);

            boolean sameMethod = this.method.equals(((MethodViolationReport) obj).method);
            boolean oneIsNullType = false;
            if (sameMethod) {
                oneIsNullType = this.type.equals("null_type") || ((MethodViolationReport) obj).type.equals("null_type");
            }

            return sameMethod && (sameType || oneIsNullType);
        }

        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(method);
    }

}
