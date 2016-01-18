import java.util.ArrayList;

public class TimeUtils {

    // to convert Milliseconds into DD HH:MM:SS format.

    public static String getDateFromMsec(long diffMSec) {
        int left = 0;
        int ss = 0;
        int mm = 0;
        int hh = 0;
        int dd = 0;
        left = (int) (diffMSec / 1000);
        ss = left % 60;
        left = (int) left / 60;
        if (left > 0) {
            mm = left % 60;
            left = (int) left / 60;
            if (left > 0) {
                hh = left % 24;
                left = (int) left / 24;
                if (left > 0) {
                    dd = left;
                }
            }
        }
        String diff = Integer.toString(dd) + " : " + Integer.toString(hh) + " : "
                + Integer.toString(mm) + " : " + Integer.toString(ss);
        return diff;

    }

}
