package com.company;
import java.io.*;
import java.util.Properties;
import java.util.Stack;

import org.json.*;
import java.net.URL;
import java.net.HttpURLConnection;

public class Main {

    private static String endpoint;
    private static Properties configFile;
    private static int pages;
    private static int total;
    private static int perPage;


    public static void main(String[] args) throws Exception {
        setupEndpoint();
	    JSONArray[] json_array = setupData();

	    //Now, we are almost ready for validation; I'll quickly find the id's for the root nodes,
        //so DFS starts on the root nodes; this will take time proportional to the nodes
        Stack<Integer> roots = findRootNodes(json_array);

        //We are ready to validate the menus; to validate, I'll use a simple DFS. The method will return a JSON Object
        //that will have the fields: "valid_menus" and "invalid_menus" as required
        JSONObject result = DFS_Validation(json_array, roots);
        System.out.println(result.toString());

    }

    //Here, I'll grab the information from the config file; the config file will let me alternate
    //between the challenges without hard coding the endpoints
    private static void setupEndpoint() throws FileNotFoundException{
        configFile = new Properties();
        try{

            configFile.load(
                    new FileInputStream("config.properties"));

        } catch(IOException e){

            throw new FileNotFoundException("Couldn't find the config.properties file");

        }

        String challenge_id = getProperty("CUR_CHALLENGE");
        String api_address = getProperty("ENDPOINT_" + challenge_id);
        endpoint = api_address;
        return;

    }

    //getProperty method to get properties from the config.properties file
    private static String getProperty(String key){
        String value = configFile.getProperty(key);
        return value;
    }

    //setupData function will deal with getting the JSON objects
    //It will GET the objects and return an array of the objects
    private static JSONArray[] setupData() throws Exception{
        int firstPage = 1;
        String pag = "pagination";
        String per = "per_page";
        String tot = "total";
        String menus = "menus";

        JSONArray[] arr = null;

        try {
            JSONObject json = getRequest(firstPage);
            JSONObject pagination = json.getJSONObject(pag);
            double per_page = pagination.getDouble(per);
            double total_value = pagination.getDouble(tot);
            pages = (int) Math.ceil(total_value / per_page);
            total = (int) total_value;
            perPage = (int) per_page;

            arr = new JSONArray[pages];

            arr[0] = json.getJSONArray(menus);

            //We've already done the first page, to continue to page 2
            for(int i = 2; i <= pages; i++){
                json = getRequest(i);
                arr[i-1] = json.getJSONArray(menus);
            }

        } catch(Exception e){
            e.printStackTrace();
            throw new Exception("Exception in setupData function");
        }

        return arr;

    }

    //GET request on the endpoint; page parameter will indicate which page we will be doing the GET request on
    //I'll convert the response to a JSON object
    private static JSONObject getRequest(int page) throws Exception{
        String url = endpoint + page;
        URL obj = new URL(url);
        HttpURLConnection con = (HttpURLConnection) obj.openConnection();
        con.setRequestMethod("GET");

        BufferedReader in = new BufferedReader(
                new InputStreamReader(con.getInputStream()));

        String input;
        StringBuffer response = new StringBuffer();

        //Keep reading until there's nothing left to read; append to the StringBuffer
        while((input = in.readLine()) != null){
            response.append(input);
        }

        //Close connection
        in.close();

        JSONObject json = new JSONObject(response.toString());
        return json;

    }

    private static Stack<Integer> findRootNodes(JSONArray[] arr) throws JSONException{
        Stack<Integer> stack = new Stack<Integer>();

        try {
            for (int i = 0; i < pages; i++) {
                for (int j = 0; j < perPage; j++) {
                    JSONObject obj = arr[i].getJSONObject(j);
                    int id = obj.getInt("id");
                    if(!obj.has("parent_id")){
                        //Thus, we have a root node; push the ID onto the stack
                        stack.push(id);
                    }
                    if(id == total){
                        //we are done, break;
                        break;
                    }

                }
            }
        }catch(JSONException e){
            throw new JSONException("Error with parsing root jsons");
        }

        return stack;
    }

    private static JSONObject DFS_Validation(JSONArray[] arr, Stack<Integer> roots) throws JSONException{
        JSONObject obj = new JSONObject();
        JSONArray valid_menus = new JSONArray();
        JSONArray invalid_menus = new JSONArray();

        boolean[] visited = new boolean[total];

        try {
            //iterate through all the roots
            while (!roots.empty()) {

                //Second stack for all the children
                Stack<Integer> stack = new Stack<Integer>();

                //Assume validMenu unless cycle is located; cycle -> a child will be visited twice
                boolean validMenu = true;

                JSONObject menu = new JSONObject();
                JSONArray children = new JSONArray();

                //pop the root, mark it as visited, then add its children to the children stack
                //Also, add root_id to the menu json_object
                int root_id = roots.pop();
                visited[root_id-1] = true;
                menu.put("root_id", root_id);

                stack = putNeighboursOnStack(arr, root_id, stack);

                while(!stack.empty()){
                    //While there are children of the root, this will run; if a node has been visited twice, then
                    //we don't have a valid menu. Thus, I set validMenu to false
                    int id = stack.pop();
                    if(visited[id-1]){
                        validMenu = false;
                        children.put(id);
                    }
                    else{
                        visited[id-1] = true;
                        children.put(id);
                        stack = putNeighboursOnStack(arr, id, stack);
                    }
                }

                //Add the children array to the json_object
                menu.put("children", children);

                //if valid, add the menu object to the valid_menu arr; else, to the invalid_menu array
                if(validMenu){
                    valid_menus.put(menu);
                }
                else{
                    invalid_menus.put(menu);
                }

            }
            //add the two arrays to the retured object at the end
            obj.put("valid_menus", valid_menus);
            obj.put("invalid_menus", invalid_menus);

        }catch(JSONException j){
            throw new JSONException("Error in parsing json in DFS_Validation");
        }

        return obj;

    }

    private static Stack<Integer> putNeighboursOnStack(JSONArray[] arr, int id, Stack<Integer> stack) throws JSONException{
        //Consider: id = 10, perPage = 5;
        //The item is on page 2 -> 10.0 / 5.0 = 2
        //The offset is (mod 5) - 1, since we are dealing with arrays; 10 % 5 = 0; 0 - 1 = -1, so we have to wrap around back to 5
        int page = (int) Math.ceil((double) id / (double) perPage);
        int offset = (id % perPage) - 1;
        if(offset < 0) offset = perPage - 1;

        JSONArray children = arr[page-1].getJSONObject(offset).getJSONArray("child_ids");
        for(int i = 0; i < children.length(); i++){
            stack.push(children.getInt(i));
        }

        return stack;

    }

}
