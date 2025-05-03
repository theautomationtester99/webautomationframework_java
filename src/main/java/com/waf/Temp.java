package com.waf;

import java.util.HashMap;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.hubspot.jinjava.Jinjava;

public class Temp {
    public static void main(String[] args) {
        // Create a map to hold the data
        Map<String, Object> tableData = new HashMap<>();

        // Create a nested map to represent dictionary-like structure
        Map<String, Object> innerData = new HashMap<>();
        innerData.put("sno", 1);

        // Add the nested map to the main map
        tableData.put("key", innerData);
        System.out.println(tableData);
        // Pass the map to Jinjava
        String decryptedTemplate = "{% for retry_key, retry_data in tableData %} Key: {{ retry_key }}, SNO: {{ retry_data.sno }} {% endfor %}";
        Jinjava jinjava = new Jinjava();
        String renderedTemplate = jinjava.render(decryptedTemplate, tableData);

        // Output the rendered template
        System.out.println(renderedTemplate);

        String data = "{\r\n" + //
                "    \"guide_links\": [\r\n" + //
                "        {\r\n" + //
                "            \"name\": \"Ye User Guide\",\r\n" + //
                "            \"link\": \"http://guides.dataverse.org/en/latest/user/\"\r\n" + //
                "        },\r\n" + //
                "        {\r\n" + //
                "            \"name\": \"Developer Guide\",\r\n" + //
                "            \"link\": \"http://guides.dataverse.org/en/latest/developers/\"\r\n" + //
                "        },\r\n" + //
                "        {\r\n" + //
                "            \"name\": \"Installation Guide\",\r\n" + //
                "            \"link\": \"http://guides.dataverse.org/en/latest/installation/\"\r\n" + //
                "        },\r\n" + //
                "        {\r\n" + //
                "            \"name\": \"The Old API Guide\",\r\n" + //
                "            \"link\": \"http://guides.dataverse.org/en/latest/api/\"\r\n" + //
                "        }\r\n" + //
                "\r\n" + //
                "    ]\r\n" + //
                "}";
        Gson gson = new Gson();
        java.lang.reflect.Type type = new TypeToken<Map<String, Object>>() {
        }.getType();
        Map<String, Object> map = gson.fromJson(data, type);
        System.out.println(map);
    }
}
