import java.io.Serializable;

public class ShowRoute implements Serializable
{
    public int hopCount;
    public String path;
    public ShowRoute(int hopCount,String path)
    {
        this.hopCount=hopCount;
        this.path=path;

    }
}
