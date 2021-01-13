package ink.ptms.sandalphon.util;

import java.util.ArrayList;
import java.util.List;

/**
 * @Author sky
 * @Since 2020-03-16 20:48
 */
public class UtilsJ {

    public static List<String> toPrintEffect(String args) {
        List<String> line = new ArrayList<>();
        args = args.replace("&", "§");
        int length = args.split("").length;
        for (int i = 0; i < length; ++i) {
            String _line = args.substring(0, length - i);
            if (_line.endsWith("§")) {
                ++i;
            } else {
                line.add(0, _line + (_line.equals(args) ? "" : "__"));
            }
        }
        return line;
    }
}
