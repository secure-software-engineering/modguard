package moduleanalysis.utils;

import soot.PointsToSet;
import soot.util.Numberable;

/**
 * Created by adann on 12.11.17.
 */
public class AnalysisReport<T extends Numberable> {


    public void setMessage(String message) {
        this.message = message;
    }

    private String message;
    private PointsToSet pts;
    private T sootObject;

    public AnalysisReport(String message, PointsToSet pts, T sootObject) {
        this.message = message;
        this.pts = pts;
        this.sootObject = sootObject;
    }

    public String getMessage() {
        return message;
    }

    public PointsToSet getPts() {
        return pts;
    }

    public T getSootObject() {
        return sootObject;
    }


    @Override
    public String toString() {
        return message;
    }


    //checks if they reference the same soot element

/*      @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj instanceof AnalysisReport) {
            if (this.pts == ((AnalysisReport) obj).pts)
                return true;
            else if (this.sootObject == ((AnalysisReport) obj).sootObject) {
                if (sootObject instanceof SootClass) {
                    return this.message.equals(((AnalysisReport) obj).message);

                } else {
                    return true;
                }

            } else
                return this.message.equals(((AnalysisReport) obj).getMessage());
        } else
            return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(sootObject);
    }*/
}
