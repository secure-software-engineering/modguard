package moduleanalysis.utils;

import soot.PointsToSet;
import soot.util.Numberable;

import java.util.Objects;

/**
 * Created by adann on 12.11.17.
 */
public class FieldViolationReport extends AnalysisReport {


    private String field;
    private String fieldValue;
    private String type;
    private String id;

    public FieldViolationReport(String message) {
        super(message, null, null);
        String[] split = message.split(", ");
        if (split.length != 4) {
            throw new RuntimeException("String does not match");

        }
        field = split[0].trim();
        fieldValue = split[1].trim();
        type = split[2].trim();
        id = split[3].trim();

    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj instanceof FieldViolationReport) {
            boolean sameField = this.field.equals (((FieldViolationReport) obj).field);
            boolean sameType = this.type.equals (((FieldViolationReport) obj).type);
            boolean oneIsNullType = false;
            if (sameField) {
                oneIsNullType = this.type.equals("null_type") || ((FieldViolationReport) obj).type.equals("null_type");
            }


            return sameField && (sameType || oneIsNullType);
        }

        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(field);
    }
}
