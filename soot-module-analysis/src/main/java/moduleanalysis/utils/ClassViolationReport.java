package moduleanalysis.utils;

import java.util.Objects;

/**
 * Created by adann on 12.11.17.
 */
public class ClassViolationReport extends AnalysisReport {


    private String type;
    private String actualType;
    private String id;

    public ClassViolationReport(String message) {
        super(message, null, null);
        String[] split = message.split(", ");
        if (split.length != 3) {
            throw new RuntimeException("String does not match");

        }
        type = split[0].trim();
        actualType = split[1].trim();
        id = split[2].trim();
    }


    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj instanceof ClassViolationReport) {
            boolean sameType = this.type.equals(((ClassViolationReport) obj).type);
            boolean sameActualType = this.actualType.equals(((ClassViolationReport) obj).actualType);
            boolean oneIsNullType = false;
            if (sameActualType) {
                oneIsNullType = this.type.equals("null_type") || ((ClassViolationReport) obj).type.equals("null_type");
            }
            return sameActualType && (sameType || oneIsNullType);
        }

        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(actualType);
    }
}
