package anonymous.migrationhelper.data;

import anonymous.migrationhelper.util.MathUtils;
import org.apache.tomcat.util.buf.HexUtils;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Created by xuyul on 2020/2/28.
 */
public class GetSetHelper {

    public static <A, B> void abstractSetter(
            A aValue, Consumer<A> aSetter, Consumer<B> bSetter, Function<A, B> mapper
    ) {
        B bValue = mapper.apply(aValue);
        aSetter.accept(aValue);
        bSetter.accept(bValue);
    }

    public static void berNumberByteSetter(
            byte[] byteValue, Consumer<byte[]> byteSetter, Consumer<List<Long>> listSetter
    ) {
        abstractSetter(byteValue, byteSetter, listSetter, MathUtils::unberNumberList);
    }

    public static void berNumberListSetter(
            List<Long> listValue, Consumer<byte[]> byteSetter, Consumer<List<Long>> listSetter
    ) {
        abstractSetter(listValue, listSetter, byteSetter, MathUtils::berNumberList);
    }

    public static void hexByteSetter(
            byte[] byteValue, Consumer<byte[]> byteSetter, Consumer<String> stringSetter
    ) {
        abstractSetter(byteValue, byteSetter, stringSetter, HexUtils::toHexString);
    }

    public static void hexStringSetter(
            String stringValue, Consumer<byte[]> byteSetter, Consumer<String> stringSetter
    ) {
        abstractSetter(stringValue, stringSetter, byteSetter, HexUtils::fromHexString);
    }
}
