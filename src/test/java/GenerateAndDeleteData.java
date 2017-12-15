import core.DataUtil;
import org.junit.jupiter.api.Test;

public class GenerateAndDeleteData {

    @Test
    public void populate() {
        DataUtil.populate();
    }

    @Test
    public void clean() {
        DataUtil.clean();
    }
}
