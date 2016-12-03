package parser;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

/**
 * Created by Pratik on 29-10-2016.
 */

public class ResponseMultiPath {
    public ArrayList<String> getresponse(String response){
        String res="";
        ArrayList<String> list = new ArrayList<>();

        Map<Integer,String> listRoutes = new TreeMap<Integer,String>();
         try {
            JSONObject repsJson = new JSONObject(response);
            JSONArray array = repsJson.getJSONArray("routes");

            for (int i=0;i<array.length();i++) {

                   list.add(array.get(i).toString());
                   listRoutes.put(array.length()-i,array.get(i).toString());
            }
        }catch (JSONException e)
        {
            e.printStackTrace();
        }
        return list;
    }
}
