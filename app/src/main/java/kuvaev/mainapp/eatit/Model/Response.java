package kuvaev.mainapp.eatit.Model;

import com.google.android.gms.common.api.Result;

import java.util.List;

public class Response {
    public long multicast_id;
    public int success;
    public int failure;
    public int canonical_ids;
    public List<Result> results;
}
